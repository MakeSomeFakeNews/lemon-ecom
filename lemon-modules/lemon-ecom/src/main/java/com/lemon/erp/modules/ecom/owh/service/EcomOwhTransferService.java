package com.lemon.erp.modules.ecom.owh.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.owh.connector.OwhData;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.InboundCreateRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.TransferLine;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.TransferRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.SyncResult;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhAccount;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhInbound;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhTransfer;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhTransferItem;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhWarehouse;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhInboundMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhTransferItemMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhTransferMapper;
import com.lemon.erp.modules.ecom.owh.model.OwhDiffSource;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundSource;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhOutboundStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhTransferStatus;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 海外仓调拨：海外仓之间、海外仓 ↔ 本地仓（两个都是本地仓的用库存调拨）。按现有调拨单流程：
 * <ul>
 *   <li>提交：调出仓是海外仓时向调出服务商推送调拨出库单（B2B 出库，收件人为调入仓地址：海外仓取映射上的收货地址，本地仓取仓库地址）→ 调出仓处理中；
 *   调出仓是本地仓时直接生成 ERP 在途调拨单（库存调出）→ 在途；</li>
 *   <li>同步：调出服务商出库后生成 ERP 在途调拨单（运单号、出库日期）→ 在途；调入仓是海外仓时由在途调拨单生成海外仓入库单
 *   （调入账号开启自动同步时立即推送，否则草稿，同步调拨单时推送），
 *   入库单收货完成后调拨收货（在途转可用）→ 已收货；调入仓是本地仓时「确认收货」调拨收货；</li>
 *   <li>取消：草稿直接取消，调出仓处理中的取消调出服务商的出库单（服务商已开始作业时提示截单后再同步）。</li>
 * </ul>
 * 服务商接口在事务之外调用
 */
@Service
@RequiredArgsConstructor
public class EcomOwhTransferService {

    private final EcomOwhTransferMapper transferMapper;
    private final EcomOwhTransferItemMapper itemMapper;
    private final EcomOwhInboundMapper inboundMapper;
    private final EcomOwhAccountService accountService;
    private final EcomOwhInboundService inboundService;
    private final EcomOwhSkuService skuService;
    private final EcomOwhDiffService diffService;
    private final EcomOwhSupport support;
    private final WmsStockDocService stockDocService;
    private final WmsWarehouseService warehouseService;
    private final DocNoService docNoService;
    private final TransactionTemplate transactionTemplate;

    public PageResult<EcomOwhTransfer> page(long current, long size, Long warehouseId, OwhTransferStatus status, String keyword) {
        Page<EcomOwhTransfer> page = Page.of(current, size);
        transferMapper.selectPageList(page, warehouseId, status, keyword);
        return PageResult.of(page);
    }

    public Map<String, Long> statusCounts(Long warehouseId, String keyword) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (Map<String, Object> row : transferMapper.countByStatus(warehouseId, keyword)) {
            out.put(String.valueOf(row.get("status")), ((Number) row.get("cnt")).longValue());
        }
        return out;
    }

    public EcomOwhTransfer detail(Long id) {
        EcomOwhTransfer t = id == null ? null : transferMapper.selectDetail(id);
        if (t == null) {
            throw new BizException("ecom.error.owh.transferNotFound");
        }
        t.setItems(itemMapper.selectByTransfer(id));
        return t;
    }

    // ---------------------------------------------------------------- 新建与修改

    @Transactional
    public Long create(TransferRequest req) {
        EcomOwhTransfer t = new EcomOwhTransfer();
        t.setTransferNo(docNoService.next(EcomOwhDocNos.TRANSFER));
        t.setStatus(OwhTransferStatus.DRAFT);
        fill(t, req);
        transferMapper.insert(t);
        saveItems(t.getId(), req.items());
        return t.getId();
    }

    @Transactional
    public void update(Long id, TransferRequest req) {
        EcomOwhTransfer t = transferMapper.selectForUpdate(id);
        if (t == null) {
            throw new BizException("ecom.error.owh.transferNotFound");
        }
        if (t.getStatus() != OwhTransferStatus.DRAFT) {
            throw new BizException("ecom.error.owh.transferNotDraft", t.getTransferNo());
        }
        fill(t, req);
        transferMapper.update(Wrappers.<EcomOwhTransfer>lambdaUpdate().set(EcomOwhTransfer::getFromWarehouseId, t.getFromWarehouseId())
                .set(EcomOwhTransfer::getToWarehouseId, t.getToWarehouseId()).set(EcomOwhTransfer::getFromMappingId, t.getFromMappingId())
                .set(EcomOwhTransfer::getToMappingId, t.getToMappingId()).set(EcomOwhTransfer::getProductCode, t.getProductCode())
                .set(EcomOwhTransfer::getRemark, t.getRemark()).set(EcomOwhTransfer::getTotalQty, t.getTotalQty()).eq(EcomOwhTransfer::getId, id));
        itemMapper.delete(Wrappers.<EcomOwhTransferItem>lambdaQuery().eq(EcomOwhTransferItem::getTransferId, id));
        saveItems(id, req.items());
    }

    /** 校验调出 / 调入仓（不能相同，至少一个是海外仓），写入映射、物流产品与数量合计 */
    private void fill(EcomOwhTransfer t, TransferRequest req) {
        if (Objects.equals(req.fromWarehouseId(), req.toWarehouseId())) {
            throw new BizException("ecom.error.owh.transferSameWarehouse");
        }
        warehouseService.get(req.fromWarehouseId());
        warehouseService.get(req.toWarehouseId());
        EcomOwhWarehouse from = accountService.mappingOf(req.fromWarehouseId());
        EcomOwhWarehouse to = accountService.mappingOf(req.toWarehouseId());
        if (from == null && to == null) {
            throw new BizException("ecom.error.owh.transferNoOverseas");
        }
        t.setFromWarehouseId(req.fromWarehouseId());
        t.setToWarehouseId(req.toWarehouseId());
        t.setFromMappingId(from == null ? null : from.getId());
        t.setToMappingId(to == null ? null : to.getId());
        t.setProductCode(StringUtils.hasText(req.productCode()) ? req.productCode().trim() : from == null ? null : from.getDefaultProductCode());
        t.setRemark(StringUtils.hasText(req.remark()) ? req.remark().trim() : null);
        t.setTotalQty(Decimals.qty(req.items().stream().map(TransferLine::qty).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    private void saveItems(Long transferId, List<TransferLine> lines) {
        Map<Long, BigDecimal> merged = new LinkedHashMap<>();
        lines.forEach(l -> merged.merge(l.materialId(), l.qty(), BigDecimal::add));
        List<EcomOwhTransferItem> items = new ArrayList<>();
        merged.forEach((m, q) -> {
            EcomOwhTransferItem it = new EcomOwhTransferItem();
            it.setTransferId(transferId);
            it.setMaterialId(m);
            it.setQty(Decimals.qty(q));
            items.add(it);
        });
        itemMapper.insertBatch(items);
    }

    @Transactional
    public void cancelDraft(Long id) {
        EcomOwhTransfer t = transferMapper.selectForUpdate(id);
        if (t.getStatus() != OwhTransferStatus.DRAFT) {
            throw new BizException("ecom.error.owh.transferNotDraft", t.getTransferNo());
        }
        setStatus(id, OwhTransferStatus.CANCELLED);
    }

    // ---------------------------------------------------------------- 提交、同步、收货、取消

    /** 提交：调出仓是海外仓时推送调拨出库单，否则直接生成 ERP 在途调拨单（调入仓是海外仓时并推送入库单） */
    public OwhTransferStatus submit(Long id) {
        EcomOwhTransfer t = detail(id);
        if (t.getStatus() != OwhTransferStatus.DRAFT) {
            throw new BizException("ecom.error.owh.transferNotDraft", t.getTransferNo());
        }
        try {
            if (t.getFromMappingId() == null) {
                ship(t, LocalDateTime.now(), null, null);
                return detail(id).getStatus();
            }
            EcomOwhWarehouse from = accountService.mapping(t.getFromMappingId());
            if (!StringUtils.hasText(t.getProductCode())) {
                throw new BizException("ecom.error.owh.transferProductRequired", t.getTransferNo());
            }
            EcomOwhAccount a = accountService.get(from.getAccountId());
            Map<Long, String> skus = skuService.skus(a.getId(), t.getItems().stream().map(EcomOwhTransferItem::getMaterialId).toList());
            List<OwhData.Line> lines = t.getItems().stream().map(it -> new OwhData.Line(skus.get(it.getMaterialId()), it.getMaterialName(), it.getQty())).toList();
            OwhData.OutboundDraft draft = new OwhData.OutboundDraft(t.getTransferNo(), from.getOwhCode(), t.getProductCode(), destination(t), lines, null,
                    null, null, t.getRemark());
            OwhData.OutboundResult r = support.connector(a.getProvider()).createTransferOut(support.context(a), draft);
            markPushed(id, r.owhOrderNo());
            return OwhTransferStatus.OUT_PUSHED;
        } catch (BizException e) {
            recordError(id, e.getMessage());
            throw e;
        }
    }

    /** 补录调出服务商的出库单号（服务商回调、迁移、演示数据使用）：草稿记为调出仓处理中 */
    public void recordPushed(Long id, String owhNo) {
        markPushed(id, owhNo);
    }

    private void markPushed(Long id, String owhNo) {
        transactionTemplate.executeWithoutResult(s -> {
            EcomOwhTransfer locked = transferMapper.selectForUpdate(id);
            if (locked.getStatus() != OwhTransferStatus.DRAFT) {
                throw new BizException("ecom.error.owh.transferNotDraft", locked.getTransferNo());
            }
            transferMapper.update(Wrappers.<EcomOwhTransfer>lambdaUpdate().set(EcomOwhTransfer::getOwhOutNo, owhNo)
                    .set(EcomOwhTransfer::getStatus, OwhTransferStatus.OUT_PUSHED).set(EcomOwhTransfer::getPushTime, LocalDateTime.now())
                    .set(EcomOwhTransfer::getErrorMsg, null).eq(EcomOwhTransfer::getId, id));
        });
    }

    /** 调入仓的收件信息：海外仓取映射上的收货地址（没有时取仓库地址），本地仓取仓库地址与联系人 */
    private OwhData.Receiver destination(EcomOwhTransfer t) {
        WmsWarehouse w = warehouseService.get(t.getToWarehouseId());
        EcomOwhWarehouse to = t.getToMappingId() == null ? null : accountService.mapping(t.getToMappingId());
        if (to != null) {
            return new OwhData.Receiver(firstText(to.getContact(), to.getOwhName(), w.getName()), firstText(to.getPhone(), w.getPhone()), null, to.getCountry(),
                    to.getState(), to.getCity(), to.getPostcode(), firstText(to.getAddress(), w.getAddress()));
        }
        return new OwhData.Receiver(firstText(w.getContact(), w.getName()), w.getPhone(), null, null, null, null, null, w.getAddress());
    }

    /** 同步全部需要同步的调拨单（定时任务） */
    public List<SyncResult> syncAll() {
        List<SyncResult> out = new ArrayList<>();
        for (Long id : transferMapper.selectSyncingIds()) {
            EcomOwhTransfer t = transferMapper.selectById(id);
            try {
                sync(id);
                out.add(new SyncResult(t.getTransferNo(), 1, null));
            } catch (BizException e) {
                recordError(id, e.getMessage());
                out.add(new SyncResult(t.getTransferNo(), 0, e.getMessage()));
            }
        }
        return out;
    }

    /**
     * 同步一张：调出仓处理中的查询调出服务商（已出库 → 生成在途调拨单，已取消 → 取消）；在途且调入仓是海外仓的同步入库单（没有推送成功的重推），
     * 入库单收货完成后为已收货
     */
    public OwhTransferStatus sync(Long id) {
        EcomOwhTransfer t = detail(id);
        if (t.getStatus() == OwhTransferStatus.OUT_PUSHED) {
            EcomOwhWarehouse from = accountService.mapping(t.getFromMappingId());
            EcomOwhAccount a = accountService.get(from.getAccountId());
            applyOutState(id, support.connector(a.getProvider()).queryTransferOut(support.context(a), t.getOwhOutNo()));
            return detail(id).getStatus();
        }
        if (t.getStatus() == OwhTransferStatus.IN_TRANSIT && t.getToMappingId() != null) {
            syncInbound(t);
            return detail(id).getStatus();
        }
        throw new BizException("ecom.error.owh.transferNotSyncable", t.getTransferNo());
    }

    /** 写入调出服务商的状态（同步、回调、补录使用）：已出库且有运单号时生成在途调拨单，已取消时取消调拨单 */
    public void applyOutState(Long id, OwhData.OutboundState state) {
        EcomOwhTransfer t = detail(id);
        if (t.getStatus() != OwhTransferStatus.OUT_PUSHED) {
            return;
        }
        if (state.status() == OwhOutboundStatus.OUTBOUND) {
            ship(t, state.shipTime() == null ? LocalDateTime.now() : state.shipTime(), state.carrier(), state.trackingNo());
        } else if (state.status() == OwhOutboundStatus.CANCELLED) {
            transferMapper.update(Wrappers.<EcomOwhTransfer>lambdaUpdate().set(EcomOwhTransfer::getStatus, OwhTransferStatus.CANCELLED)
                    .set(EcomOwhTransfer::getOwhStatus, state.rawStatus()).set(EcomOwhTransfer::getSyncTime, LocalDateTime.now()).eq(EcomOwhTransfer::getId, id));
        } else {
            transferMapper.update(Wrappers.<EcomOwhTransfer>lambdaUpdate().set(EcomOwhTransfer::getOwhStatus, state.rawStatus())
                    .set(EcomOwhTransfer::getSyncTime, LocalDateTime.now()).set(EcomOwhTransfer::getErrorMsg, null).eq(EcomOwhTransfer::getId, id));
        }
    }

    /**
     * 调出：生成并确认 ERP 在途调拨单（来源单号为调拨单号），调拨单为在途；调入仓是海外仓时由在途调拨单生成海外仓入库单并推送
     * （推送失败记录原因，同步时重推）
     */
    private void ship(EcomOwhTransfer t, LocalDateTime time, String carrier, String trackingNo) {
        Long inboundId = EcomOwhSupport.asSystem(() -> transactionTemplate.execute(s -> {
            EcomOwhTransfer locked = transferMapper.selectForUpdate(t.getId());
            if (locked.getStatus() != OwhTransferStatus.DRAFT && locked.getStatus() != OwhTransferStatus.OUT_PUSHED) {
                throw new BizException("ecom.error.owh.transferStatusChanged", locked.getTransferNo());
            }
            List<StockDocItemRequest> items = t.getItems().stream()
                    .map(it -> new StockDocItemRequest(it.getMaterialId(), null, null, null, null, null, it.getQty(), null, null)).toList();
            WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.TRANSFER, new StockDocRequest(null, t.getFromWarehouseId(), t.getToWarehouseId(),
                    time.toLocalDate(), Messages.get("ecom.text.owh.transferRemark", t.getTransferNo()), items, true, null), t.getTransferNo());
            Long ib = t.getToMappingId() == null ? null : inboundService.createFrom(new InboundCreateRequest(OwhInboundSource.TRANSFER, doc.getId()), false);
            transferMapper.update(Wrappers.<EcomOwhTransfer>lambdaUpdate().set(EcomOwhTransfer::getStatus, OwhTransferStatus.IN_TRANSIT)
                    .set(EcomOwhTransfer::getTransferDocId, doc.getId()).set(EcomOwhTransfer::getTransferDocNo, doc.getDocNo())
                    .set(EcomOwhTransfer::getInboundId, ib).set(EcomOwhTransfer::getShipTime, time)
                    .set(StringUtils.hasText(carrier), EcomOwhTransfer::getCarrier, carrier)
                    .set(StringUtils.hasText(trackingNo), EcomOwhTransfer::getTrackingNo, trackingNo)
                    .set(locked.getPushTime() == null, EcomOwhTransfer::getPushTime, LocalDateTime.now())
                    .set(EcomOwhTransfer::getSyncTime, LocalDateTime.now()).set(EcomOwhTransfer::getErrorMsg, null).eq(EcomOwhTransfer::getId, t.getId()));
            if (ib != null && (StringUtils.hasText(carrier) || StringUtils.hasText(trackingNo))) {
                inboundMapper.update(Wrappers.<EcomOwhInbound>lambdaUpdate().set(EcomOwhInbound::getCarrier, carrier).set(EcomOwhInbound::getTrackingNo, trackingNo)
                        .eq(EcomOwhInbound::getId, ib));
            }
            return ib;
        }));
        // 调入海外仓的账号开启了自动同步时立即推送入库单；否则保持草稿，在「海外仓入库」中手工推送（或同步调拨单时推送）
        if (inboundId != null && accountService.autoSyncAccountIds().contains(accountService.mapping(t.getToMappingId()).getAccountId())) {
            try {
                inboundService.push(inboundId);
            } catch (BizException e) {
                recordError(t.getId(), e.getMessage());
            }
        }
    }

    /** 在途、调入海外仓：入库单还是草稿的重推；已推送的同步收货；入库单收货完成且 ERP 已签收后为已收货 */
    private void syncInbound(EcomOwhTransfer t) {
        if (t.getInboundId() == null) {
            return;
        }
        EcomOwhInbound in = inboundService.detail(t.getInboundId());
        if (in.getStatus() == OwhInboundStatus.CANCELLED) {
            // 入库单已在服务商处取消 / 作废：由同一张在途调拨单重新生成入库单并推送
            Long ib = inboundService.createFrom(new InboundCreateRequest(OwhInboundSource.TRANSFER, t.getTransferDocId()), false);
            transferMapper.update(Wrappers.<EcomOwhTransfer>lambdaUpdate().set(EcomOwhTransfer::getInboundId, ib).eq(EcomOwhTransfer::getId, t.getId()));
            inboundService.push(ib);
        } else if (in.getStatus() == OwhInboundStatus.DRAFT) {
            inboundService.push(in.getId());
        } else if (in.getStatus() != OwhInboundStatus.RECEIVED || in.getErpReceived() == null || in.getErpReceived() == 0) {
            inboundService.sync(in.getId());
        }
        refreshFromInbound(t.getId());
    }

    /** 调入海外仓的入库单 ERP 签收后，对应的调拨单为已收货 */
    @org.springframework.context.event.EventListener
    public void onInboundReceived(EcomOwhInboundService.InboundReceivedEvent e) {
        for (EcomOwhTransfer t : transferMapper.selectList(Wrappers.<EcomOwhTransfer>lambdaQuery().eq(EcomOwhTransfer::getInboundId, e.inboundId())
                .eq(EcomOwhTransfer::getStatus, OwhTransferStatus.IN_TRANSIT))) {
            refreshFromInbound(t.getId());
        }
    }

    /** 入库单已收货（ERP 已签收）时调拨单为已收货（内部，入库单同步后调用） */
    public void refreshFromInbound(Long id) {
        EcomOwhTransfer t = detail(id);
        if (t.getStatus() != OwhTransferStatus.IN_TRANSIT || t.getInboundId() == null) {
            return;
        }
        EcomOwhInbound in = inboundService.detail(t.getInboundId());
        if (in.getStatus() == OwhInboundStatus.RECEIVED && in.getErpReceived() != null && in.getErpReceived() == 1) {
            transferMapper.update(Wrappers.<EcomOwhTransfer>lambdaUpdate().set(EcomOwhTransfer::getStatus, OwhTransferStatus.RECEIVED)
                    .set(EcomOwhTransfer::getReceiveTime, in.getReceiveTime() == null ? LocalDateTime.now() : in.getReceiveTime())
                    .set(EcomOwhTransfer::getErrorMsg, null).eq(EcomOwhTransfer::getId, id));
        }
    }

    /** 确认收货（调入仓是本地仓），全部收到 */
    @Transactional
    public void receive(Long id, LocalDate date) {
        receive(id, date, null, null);
    }

    /**
     * 确认收货（调入仓是本地仓）：调拨收货，在途转为本地仓可用；填写了各物料实收数量且与调拨数量不一致时按实收收货——
     * 整单收货后短少的差异出库（调出方损耗，待处理）、溢收的其他入库，差异逐物料记录原因待处理
     *
     * @param actual 物料 → 实收数量，为空全部收到
     */
    @Transactional
    public void receive(Long id, LocalDate date, Map<Long, BigDecimal> actual, String reason) {
        EcomOwhTransfer t = transferMapper.selectForUpdate(id);
        if (t == null) {
            throw new BizException("ecom.error.owh.transferNotFound");
        }
        if (t.getStatus() != OwhTransferStatus.IN_TRANSIT || t.getToMappingId() != null) {
            throw new BizException("ecom.error.owh.transferNotReceivable", t.getTransferNo());
        }
        LocalDate d = date == null ? LocalDate.now() : date;
        if (t.getShipTime() != null && d.isBefore(t.getShipTime().toLocalDate())) {
            d = t.getShipTime().toLocalDate();
        }
        stockDocService.receiveBySource(t.getTransferDocId(), d);
        if (actual != null && !actual.isEmpty()) {
            List<EcomOwhDiffService.Line> lines = new ArrayList<>();
            for (EcomOwhTransferItem it : itemMapper.selectByTransfer(id)) {
                lines.add(new EcomOwhDiffService.Line(it.getMaterialId(), it.getMaterialCode(), it.getQty(), actual.getOrDefault(it.getMaterialId(), it.getQty())));
            }
            diffService.record(new EcomOwhDiffService.Source(OwhDiffSource.TRANSFER, id, t.getTransferNo(), t.getTransferDocNo(), null, t.getToWarehouseId()),
                    lines, d, true);
            if (StringUtils.hasText(reason)) {
                diffService.noteReason(OwhDiffSource.TRANSFER, id, reason.trim());
            }
        }
        transferMapper.update(Wrappers.<EcomOwhTransfer>lambdaUpdate().set(EcomOwhTransfer::getStatus, OwhTransferStatus.RECEIVED)
                .set(EcomOwhTransfer::getReceiveTime, d.atTime(LocalDateTime.now().toLocalTime())).eq(EcomOwhTransfer::getId, id));
    }

    /**
     * 取消：草稿直接取消；调出仓处理中的取消调出服务商的出库单（服务商已开始作业时为截单，状态保持调出仓处理中，之后同步结果）
     *
     * @return 取消后的状态
     */
    public OwhTransferStatus cancel(Long id) {
        EcomOwhTransfer t = detail(id);
        if (t.getStatus() == OwhTransferStatus.DRAFT) {
            cancelDraft(id);
            return OwhTransferStatus.CANCELLED;
        }
        if (t.getStatus() != OwhTransferStatus.OUT_PUSHED) {
            throw new BizException("ecom.error.owh.transferNotCancellable", t.getTransferNo());
        }
        EcomOwhWarehouse from = accountService.mapping(t.getFromMappingId());
        EcomOwhAccount a = accountService.get(from.getAccountId());
        boolean done = support.connector(a.getProvider()).cancelOutbound(support.context(a), t.getOwhOutNo(), null);
        if (done) {
            setStatus(id, OwhTransferStatus.CANCELLED);
            return OwhTransferStatus.CANCELLED;
        }
        transferMapper.update(Wrappers.<EcomOwhTransfer>lambdaUpdate().set(EcomOwhTransfer::getErrorMsg, Messages.get("ecom.text.owh.transferIntercepting"))
                .eq(EcomOwhTransfer::getId, id));
        return OwhTransferStatus.OUT_PUSHED;
    }

    private void setStatus(Long id, OwhTransferStatus status) {
        transferMapper.update(Wrappers.<EcomOwhTransfer>lambdaUpdate().set(EcomOwhTransfer::getStatus, status).eq(EcomOwhTransfer::getId, id));
    }

    private void recordError(Long id, String message) {
        transferMapper.update(Wrappers.<EcomOwhTransfer>lambdaUpdate().set(EcomOwhTransfer::getErrorMsg, EcomOwhSupport.abbreviate(message))
                .set(EcomOwhTransfer::getSyncTime, LocalDateTime.now()).eq(EcomOwhTransfer::getId, id));
    }

    private static String firstText(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v.trim();
            }
        }
        return null;
    }
}
