package com.lemon.erp.modules.ecom.assistant.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.ai.AiTextGenerator;
import com.lemon.erp.framework.ai.AiTextRequest;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantViews;
import com.lemon.erp.modules.ecom.assistant.dto.OpsReportContent;
import com.lemon.erp.modules.ecom.assistant.entity.EcomOpsReport;
import com.lemon.erp.modules.ecom.assistant.entity.EcomOpsReportStore;
import com.lemon.erp.modules.ecom.assistant.mapper.EcomOpsReportMapper;
import com.lemon.erp.modules.ecom.assistant.mapper.EcomOpsReportStoreMapper;
import com.lemon.erp.modules.ecom.assistant.model.OpsReportAiStatus;
import com.lemon.erp.modules.ecom.assistant.model.OpsReportSource;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.oper.scope.EcomStoreScope;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 运营日报：按店铺范围汇总一天的数据（{@link EcomOpsReportBuilder}，不依赖 AI），AI 已配置并开启「AI 写作」时再生成「要点与建议」
 * （经 {@link AiTextGenerator}，模型调用在事务之外）；同一天同一范围一份，重新生成时覆盖。
 * 列表与详情只显示店铺全部可见的日报（店铺范围存在 ecom_ops_report_store，由店铺数据权限过滤）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomOpsReportService {

    /** 范围为全部店铺时的标识 */
    static final String SCOPE_ALL = "ALL";
    /** 要点文字的消息键前缀（ecom.text.asst.hl.&lt;code&gt;） */
    static final String HIGHLIGHT_PREFIX = "ecom.text.asst.hl.";
    /** 给模型的数据中每个列表最多带的行数 */
    static final int AI_ROWS = 10;

    private final EcomOpsReportMapper reportMapper;
    private final EcomOpsReportStoreMapper reportStoreMapper;
    private final EcomOpsReportBuilder builder;
    private final EcomStoreService storeService;
    private final ObjectProvider<AiTextGenerator> generator;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    // ---------------------------------------------------------------- 查询

    public PageResult<EcomOpsReport> page(long current, long size, LocalDate begin, LocalDate end, Long storeId) {
        Page<EcomOpsReport> page = Page.of(current, size);
        reportMapper.selectPageList(page, begin, end, storeId);
        return PageResult.of(page);
    }

    public EcomOpsReport get(Long id) {
        return EcomStoreScope.require(() -> reportMapper.selectVisible(id), "ecom.error.asst.reportNotFound");
    }

    /** 详情：日报 + 结构化内容（要点按当前语言生成文字） */
    public AssistantViews.ReportDetail detail(Long id) {
        EcomOpsReport r = get(id);
        OpsReportContent content = parse(r.getContent());
        for (OpsReportContent.Highlight h : content.getHighlights()) {
            h.setText(Messages.get(HIGHLIGHT_PREFIX + h.getCode(), h.getArgs().toArray()));
        }
        r.setContent(null);
        return new AssistantViews.ReportDetail(r, content);
    }

    @Transactional
    public void delete(List<Long> ids) {
        ids.forEach(this::get);
        reportStoreMapper.delete(Wrappers.<EcomOpsReportStore>lambdaQuery().in(EcomOpsReportStore::getReportId, ids));
        reportMapper.deleteByIds(ids);
    }

    // ---------------------------------------------------------------- 生成

    /**
     * 手动「立即生成」：店铺为空时为当前用户可见的全部启用店铺；指定的店铺必须可见
     *
     * @param date 为空取昨天
     * @param ai   为 false 时不生成 AI 建议
     */
    public EcomOpsReport generateManual(LocalDate date, Collection<Long> storeIds, boolean ai) {
        LocalDate day = date == null ? LocalDate.now().minusDays(1) : date;
        if (day.isAfter(LocalDate.now())) {
            throw new BizException("ecom.error.asst.futureDate");
        }
        List<EcomStore> stores;
        if (storeIds == null || storeIds.isEmpty()) {
            stores = storeService.options();
        } else {
            stores = new ArrayList<>();
            for (Long id : new LinkedHashSet<>(storeIds)) {
                if (!EcomStoreScope.canAccess(id)) {
                    throw new BizException("ecom.error.storeNotFound");
                }
                stores.add(storeService.get(id));
            }
        }
        if (stores.isEmpty()) {
            throw new BizException("ecom.error.asst.noStores");
        }
        return generate(day, stores, OpsReportSource.MANUAL, ai);
    }

    /**
     * 生成并保存（同一天同一范围覆盖）
     *
     * @param stores 范围内的店铺（调用方已校验）
     */
    public EcomOpsReport generate(LocalDate day, List<EcomStore> stores, OpsReportSource source, boolean ai) {
        List<EcomStore> sorted = stores.stream().sorted(Comparator.comparing(EcomStore::getId)).toList();
        OpsReportContent content = builder.build(day, sorted, LocalDateTime.now());
        EcomOpsReport r = new EcomOpsReport();
        r.setReportDate(day);
        r.setScopeKey(scopeKey(sorted.stream().map(EcomStore::getId).toList()));
        r.setScopeName(scopeName(sorted));
        r.setStoreCount(sorted.size());
        r.setSource(source);
        fillColumns(r, content);
        r.setSummary(summary(content));
        r.setContent(write(content));
        r.setAiSample(0);
        applyAdvice(r, content, ai);
        r.setCreateBy(SecurityUtils.getUserIdOrNull());
        return save(r, sorted);
    }

    /** 店铺范围标识：全部启用店铺为 ALL，否则为排序后店铺 ID 的 MD5 */
    public String scopeKey(Collection<Long> storeIds) {
        List<Long> ids = storeIds.stream().distinct().sorted().toList();
        Set<Long> all = EcomStoreScope.unrestricted(() -> storeService.options()).stream().map(EcomStore::getId).collect(Collectors.toSet());
        if (!all.isEmpty() && all.size() == ids.size() && all.containsAll(ids)) {
            return SCOPE_ALL;
        }
        return DigestUtils.md5DigestAsHex(ids.stream().map(String::valueOf).collect(Collectors.joining(",")).getBytes(StandardCharsets.UTF_8));
    }

    private String scopeName(List<EcomStore> stores) {
        if (SCOPE_ALL.equals(scopeKey(stores.stream().map(EcomStore::getId).toList()))) {
            return Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.asst.allStores");
        }
        String names = stores.stream().map(EcomStore::getName).collect(Collectors.joining(Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.asst.sep")));
        return names.length() > 250 ? names.substring(0, 250) + "…" : names;
    }

    static void fillColumns(EcomOpsReport r, OpsReportContent c) {
        r.setOrders((int) c.getKpi().getOrders());
        r.setSales(c.getKpi().getSales());
        r.setProfit(c.getKpi().getProfit());
        r.setSalesChange(EcomOpsReportBuilder.change(c.getKpi().getSales(), c.getPrev().getSales()));
        r.setProfitChange(EcomOpsReportBuilder.change(c.getKpi().getProfit(), c.getPrev().getProfit()));
        r.setAbnormalCount(Math.max(c.getAbnormalTotal(), c.getAbnormalSkus().size()));
        r.setBadReviewCount(c.getBadReviews().size());
        r.setStockRiskCount(Math.max(c.getStockRiskTotal(), c.getStockRisks().size()));
        r.setAdAlertCount(Math.max(c.getAdAlertTotal(), c.getAdAlerts().size()));
        r.setTodoCount((int) c.getTodos().stream().mapToLong(OpsReportContent.Todo::count).sum());
    }

    /** 摘要（默认语言，列表与推送使用） */
    static String summary(OpsReportContent c) {
        OpsReportContent.Kpi k = c.getKpi();
        String s = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.asst.summary", k.getOrders(), EcomOpsReportBuilder.money(k.getSales()),
                signed(EcomOpsReportBuilder.change(k.getSales(), c.getPrev().getSales())), EcomOpsReportBuilder.money(k.getProfit()),
                signed(EcomOpsReportBuilder.change(k.getProfit(), c.getPrev().getProfit())), Math.max(c.getAbnormalTotal(), c.getAbnormalSkus().size()),
                c.getBadReviews().size(), Math.max(c.getStockRiskTotal(), c.getStockRisks().size()), Math.max(c.getAdAlertTotal(), c.getAdAlerts().size()));
        return s.length() > 500 ? s.substring(0, 500) : s;
    }

    private static String signed(BigDecimal v) {
        return v == null ? "-" : (v.signum() > 0 ? "+" : "") + v.toPlainString() + "%";
    }

    private EcomOpsReport save(EcomOpsReport r, List<EcomStore> stores) {
        return transactionTemplate.execute(s -> {
            EcomOpsReport old = reportMapper.selectOne(Wrappers.<EcomOpsReport>lambdaQuery().eq(EcomOpsReport::getReportDate, r.getReportDate())
                    .eq(EcomOpsReport::getScopeKey, r.getScopeKey()));
            if (old != null) {
                r.setId(old.getId());
                r.setPushCount(old.getPushCount());
                r.setPushedTime(old.getPushedTime());
                reportMapper.updateById(r);
                reportStoreMapper.delete(Wrappers.<EcomOpsReportStore>lambdaQuery().eq(EcomOpsReportStore::getReportId, old.getId()));
            } else {
                r.setPushCount(0);
                reportMapper.insert(r);
            }
            List<EcomOpsReportStore> items = stores.stream().map(st -> {
                EcomOpsReportStore x = new EcomOpsReportStore();
                x.setReportId(r.getId());
                x.setStoreId(st.getId());
                return x;
            }).toList();
            if (!items.isEmpty()) {
                reportStoreMapper.insertBatch(items);
            }
            return r;
        });
    }

    /** 重新生成 AI 建议（日报数据不变） */
    public EcomOpsReport regenerateAdvice(Long id) {
        EcomOpsReport r = get(id);
        OpsReportContent content = parse(r.getContent());
        AiTextGenerator g = generator.getIfAvailable();
        if (g == null || !g.isAvailable()) {
            throw new BizException("ecom.error.asst.aiUnavailable", g == null ? Messages.get("ecom.error.fix.aiMissing") : g.unavailableReason());
        }
        applyAdvice(r, content, true);
        r.setAiSample(0);
        reportMapper.update(Wrappers.<EcomOpsReport>lambdaUpdate().set(EcomOpsReport::getAiStatus, r.getAiStatus())
                .set(EcomOpsReport::getAiAdvice, r.getAiAdvice()).set(EcomOpsReport::getAiError, r.getAiError())
                .set(EcomOpsReport::getAiSample, 0).eq(EcomOpsReport::getId, id));
        return r;
    }

    // ---------------------------------------------------------------- AI 建议

    /** 生成 AI 要点与建议：不可用时为 NONE（记录原因），调用失败为 FAILED，不影响日报本身 */
    private void applyAdvice(EcomOpsReport r, OpsReportContent content, boolean ai) {
        r.setAiAdvice(null);
        AiTextGenerator g = generator.getIfAvailable();
        if (!ai) {
            r.setAiStatus(OpsReportAiStatus.NONE);
            r.setAiError(null);
            return;
        }
        if (g == null || !g.isAvailable()) {
            r.setAiStatus(OpsReportAiStatus.NONE);
            r.setAiError(clip(g == null ? Messages.get(Messages.DEFAULT_LOCALE, "ecom.error.fix.aiMissing") : g.unavailableReason(), 500));
            return;
        }
        try {
            String text = g.generate(AiTextRequest.of(Messages.get(Messages.DEFAULT_LOCALE, "ecom.prompt.asst.dailyAdvice"), aiInput(content))
                    .withLanguage(Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.asst.adviceLanguage")));
            r.setAiStatus(OpsReportAiStatus.SUCCESS);
            r.setAiAdvice(text);
            r.setAiError(null);
        } catch (RuntimeException e) {
            log.warn("运营日报 AI 建议生成失败：{}", e.getMessage()); // i18n-ignore 日志
            r.setAiStatus(OpsReportAiStatus.FAILED);
            r.setAiError(clip(e.getMessage(), 500));
        }
    }

    /** 给模型的数据：日报的精简版（各列表最多 {@value #AI_ROWS} 行，评价内容截断），金额为本位币 */
    String aiInput(OpsReportContent c) {
        OpsReportContent brief = new OpsReportContent();
        brief.setDate(c.getDate());
        brief.setStores(c.getStores());
        brief.setKpi(c.getKpi());
        brief.setPrev(c.getPrev());
        brief.setAvg7(c.getAvg7());
        brief.setStoreRows(head(c.getStoreRows()));
        brief.setAbnormalSkus(head(c.getAbnormalSkus()));
        brief.setBadReviews(head(c.getBadReviews()));
        brief.setStockRisks(head(c.getStockRisks()));
        brief.setAdAlerts(head(c.getAdAlerts()));
        brief.setTodos(c.getTodos());
        return Messages.get(Messages.DEFAULT_LOCALE, "ecom.prompt.asst.dailyData") + "\n" + write(brief);
    }

    private static <T> List<T> head(List<T> list) {
        return list.size() > AI_ROWS ? list.subList(0, AI_ROWS) : list;
    }

    // ---------------------------------------------------------------- JSON

    String write(OpsReportContent c) {
        return objectMapper.writeValueAsString(c);
    }

    /** 日报的结构化内容 */
    public OpsReportContent content(EcomOpsReport report) {
        return parse(report.getContent());
    }

    OpsReportContent parse(String json) {
        if (json == null || json.isBlank()) {
            return new OpsReportContent();
        }
        return objectMapper.readValue(json, OpsReportContent.class);
    }

    private static String clip(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
