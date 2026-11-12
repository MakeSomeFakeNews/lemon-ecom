package com.lemon.erp.modules.ecom.fba.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.fba.connector.AmazonReportsClient;
import com.lemon.erp.modules.ecom.fba.connector.TsvReport;
import com.lemon.erp.modules.ecom.fba.dto.FbaRequests;
import com.lemon.erp.modules.ecom.fba.entity.EcomAmzReport;
import com.lemon.erp.modules.ecom.fba.mapper.EcomAmzReportMapper;
import com.lemon.erp.modules.ecom.fba.model.AmzReportStatus;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import com.lemon.erp.modules.ecom.service.EcomSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 亚马逊报告任务：
 * <ol>
 *   <li>创建：先登记任务，再调用 createReport（接口调用不在事务中），失败时任务记为失败并提示；</li>
 *   <li>轮询：定时任务 {@link AmzReportPollJob} 取到期的任务调用 getReport，生成中的按次数逐渐拉长间隔（1、2、3…最长 30 分钟），
 *       完成后 getReportDocument 下载（GZIP 解压）、按制表符解析，交给报告类型对应的 {@link AmzReportHandler} 写入；
 *       亚马逊返回 CANCELLED（通常是该范围没有数据）记为已取消，FATAL 记为失败；超过 {@value #MAX_POLLS} 次仍未完成记为失败；</li>
 *   <li>上传：没有接口授权时，在卖家平台下载报告文件后上传，按同样的处理器写入。</li>
 * </ol>
 * 处理器写入失败时任务记为失败并保留原因，可重新创建；同一报告重复处理时各处理器按业务键去重（覆盖快照、流水按行摘要、赔偿按编号）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmzReportService {

    static final int MAX_POLLS = 120;
    private static final int MAX_POLL_INTERVAL_MINUTES = 30;
    /** 按日期的报告不填日期时默认的天数 */
    static final int DEFAULT_DAYS = 30;
    private static final int MESSAGE_MAX = 500;
    private static final Set<AmzReportStatus> RUNNING = EnumSet.of(AmzReportStatus.SUBMITTED, AmzReportStatus.PROCESSING);

    private final EcomAmzReportMapper reportMapper;
    private final AmazonReportsClient client;
    private final EcomSyncService syncService;
    private final FbaSupport support;
    /** 各 FBA 服务实现的处理器，用时再取，避免循环依赖 */
    private final ObjectProvider<AmzReportHandler> handlers;

    public PageResult<EcomAmzReport> page(long current, long size, Long storeId, AmzReportType type, AmzReportStatus status) {
        Page<EcomAmzReport> page = Page.of(current, size);
        reportMapper.selectPageList(page, storeId, type, status);
        return PageResult.of(page);
    }

    public EcomAmzReport get(Long id) {
        EcomAmzReport r = id == null ? null : reportMapper.selectById(id);
        if (r == null) {
            throw new BizException("ecom.error.fba.reportNotFound");
        }
        return r;
    }

    /** 创建报告任务并调用 createReport，返回任务 ID */
    public Long create(FbaRequests.ReportCreate req) {
        EcomStore store = support.amazonStore(req.storeId());
        ConnectorContext ctx = syncService.requireContext(store);
        LocalDate end = req.dataEnd() == null ? LocalDate.now() : req.dataEnd();
        LocalDate start = req.dataStart() == null ? end.minusDays(DEFAULT_DAYS) : req.dataStart();
        if (start.isAfter(end)) {
            throw new BizException("ecom.error.fba.dateRangeInvalid");
        }
        EcomAmzReport r = new EcomAmzReport();
        r.setStoreId(store.getId());
        r.setReportType(req.reportType());
        if (req.reportType().isDated()) {
            r.setDataStart(start);
            r.setDataEnd(end);
        }
        r.setStatus(AmzReportStatus.SUBMITTED);
        r.setUploaded(0);
        r.setRowCount(0);
        r.setAppliedCount(0);
        r.setPollCount(0);
        reportMapper.insert(r);
        try {
            String reportId = client.createReport(ctx, req.reportType(), r.getDataStart(), r.getDataEnd());
            r.setReportId(reportId);
            r.setNextPollTime(LocalDateTime.now().plusMinutes(1));
            reportMapper.updateById(r);
        } catch (RuntimeException e) {
            fail(r, e);
            throw e;
        }
        return r.getId();
    }

    /** 为店铺创建一组报告（同步库存健康、赔偿等），某个失败时记录原因并继续，返回成功创建的任务 ID */
    public List<Long> createAll(Long storeId, List<AmzReportType> types, int days) {
        List<Long> ids = new ArrayList<>();
        BizException first = null;
        for (AmzReportType type : types) {
            try {
                ids.add(create(new FbaRequests.ReportCreate(storeId, type, LocalDate.now().minusDays(days), LocalDate.now())));
            } catch (BizException e) {
                log.warn("create amazon report {} for store {} failed: {}", type, storeId, e.getMessage());
                if (first == null) {
                    first = e;
                }
            }
        }
        if (ids.isEmpty() && first != null) {
            throw first;
        }
        return ids;
    }

    /** 查询一次状态，完成时下载并处理；返回最新的任务 */
    public EcomAmzReport poll(Long id) {
        EcomAmzReport r = get(id);
        if (!RUNNING.contains(r.getStatus()) || r.getReportId() == null) {
            return r;
        }
        try {
            EcomStore store = support.amazonStore(r.getStoreId());
            ConnectorContext ctx = syncService.requireContext(store);
            AmazonReportsClient.ReportState state = client.getReport(ctx, r.getReportId());
            r.setPollCount(r.getPollCount() == null ? 1 : r.getPollCount() + 1);
            if (state.done()) {
                r.setDocumentId(state.documentId());
                String text = client.downloadDocument(ctx, state.documentId());
                process(r, store, text, LocalDate.now());
            } else if (state.cancelled()) {
                r.setStatus(AmzReportStatus.CANCELLED);
                r.setMessage(Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.fba.reportCancelled"));
                r.setFinishTime(LocalDateTime.now());
                r.setNextPollTime(null);
                reportMapper.updateById(r);
            } else if (state.fatal()) {
                r.setStatus(AmzReportStatus.FAILED);
                r.setMessage(Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.fba.reportFatal"));
                r.setFinishTime(LocalDateTime.now());
                r.setNextPollTime(null);
                reportMapper.updateById(r);
            } else if (r.getPollCount() >= MAX_POLLS) {
                r.setStatus(AmzReportStatus.FAILED);
                r.setMessage(Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.fba.reportTimeout"));
                r.setFinishTime(LocalDateTime.now());
                r.setNextPollTime(null);
                reportMapper.updateById(r);
            } else {
                r.setStatus(AmzReportStatus.PROCESSING);
                r.setNextPollTime(LocalDateTime.now().plusMinutes(Math.min(MAX_POLL_INTERVAL_MINUTES, r.getPollCount())));
                reportMapper.updateById(r);
            }
        } catch (RuntimeException e) {
            fail(r, e);
        }
        return get(id);
    }

    /** 轮询到期的任务（定时任务），返回处理的任务数 */
    public int pollDue() {
        List<EcomAmzReport> due = Queries.limit(reportMapper, Wrappers.<EcomAmzReport>lambdaQuery()
                .in(EcomAmzReport::getStatus, RUNNING).isNotNull(EcomAmzReport::getReportId)
                .le(EcomAmzReport::getNextPollTime, LocalDateTime.now()).orderByAsc(EcomAmzReport::getNextPollTime), 50);
        for (EcomAmzReport r : due) {
            poll(r.getId());
        }
        return due.size();
    }

    /** 上传报告文件并立即处理，返回任务 ID；处理失败时任务记为失败并提示原因 */
    public Long upload(FbaRequests.ReportUpload req) {
        EcomStore store = support.amazonStore(req.storeId());
        EcomAmzReport r = new EcomAmzReport();
        r.setStoreId(store.getId());
        r.setReportType(req.reportType());
        r.setStatus(AmzReportStatus.PROCESSING);
        r.setUploaded(1);
        r.setRowCount(0);
        r.setAppliedCount(0);
        r.setPollCount(0);
        reportMapper.insert(r);
        process(r, store, req.content(), req.snapshotDate() == null ? LocalDate.now() : req.snapshotDate());
        EcomAmzReport saved = get(r.getId());
        if (saved.getStatus() == AmzReportStatus.FAILED) {
            throw new BizException("ecom.error.fba.reportProcessFailed", saved.getMessage());
        }
        return r.getId();
    }

    /** 解析并交给处理器；成功记为已完成（行数、写入条数），失败记为失败 */
    void process(EcomAmzReport r, EcomStore store, String text, LocalDate snapshotDate) {
        try {
            TsvReport report = TsvReport.parse(text);
            AmzReportHandler handler = handlers.orderedStream().filter(h -> h.types().contains(r.getReportType())).findFirst()
                    .orElseThrow(() -> new BizException("ecom.error.fba.reportNoHandler", r.getReportType().name()));
            int applied = handler.apply(store, r.getReportType(), report, snapshotDate);
            r.setStatus(AmzReportStatus.DONE);
            r.setRowCount(report.rows().size());
            r.setAppliedCount(applied);
            r.setMessage(null);
            r.setFinishTime(LocalDateTime.now());
            r.setNextPollTime(null);
            reportMapper.updateById(r);
        } catch (RuntimeException e) {
            fail(r, e);
        }
    }

    private void fail(EcomAmzReport r, RuntimeException e) {
        log.warn("amazon report {} ({}) failed: {}", r.getId(), r.getReportType(), e.getMessage());
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        r.setStatus(AmzReportStatus.FAILED);
        r.setMessage(msg.length() <= MESSAGE_MAX ? msg : msg.substring(0, MESSAGE_MAX));
        r.setFinishTime(LocalDateTime.now());
        r.setNextPollTime(null);
        reportMapper.updateById(r);
    }

    /** 删除任务（只删任务记录，已写入的业务数据保留） */
    public void delete(List<Long> ids) {
        reportMapper.deleteByIds(ids);
    }
}
