package com.lemon.erp.modules.ecom.assistant.tool;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.service.EcomAdAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 广告表现：view = OVERVIEW（按店铺的花费、广告销售额、ACOS、TACOS、CTR、CPC、CVR 及环比，本位币，第一行为合计）、CAMPAIGN（活动排行与环比、日预算，
 * 店铺币种）、SKU（SKU 的广告花费、ACOS、TACOS）、WASTE_TERMS（高花费 / 多点击零转化的搜索词，是否已否定）、TOP_TERMS（出单最多的搜索词）。
 * 复用广告分析的查询，权限与「广告数据」页面一致
 */
@Component
@RequiredArgsConstructor
public class EcomAdsInsightTool implements AiTool {

    enum View {
        OVERVIEW, CAMPAIGN, SKU, WASTE_TERMS, TOP_TERMS
    }

    private final EcomAdAnalysisService analysisService;
    private final AssistantToolSupport support;

    @Override
    public String name() {
        return "ecom_ads_insight";
    }

    @Override
    public String labelKey() {
        return "ecom.ai.asst.tool.ads";
    }

    @Override
    public String descriptionKey() {
        return "ecom.ai.asst.tool.ads.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        AiSchema s = AiSchema.object()
                .prop("view", AiSchema.enumOf("ecom.ai.asst.param.adsView", View.OVERVIEW.name(), View.CAMPAIGN.name(), View.SKU.name(),
                        View.WASTE_TERMS.name(), View.TOP_TERMS.name()), true);
        AssistantToolSupport.dateProps(s);
        AssistantToolSupport.scopeProps(s)
                .prop("keyword", AiSchema.string("ecom.ai.asst.param.adsKeyword").maxLength(64), false)
                .prop("minClicks", AiSchema.integer("ecom.ai.asst.param.minClicks").min(1).max(1000), false);
        return AssistantToolSupport.topProp(s).build();
    }

    @Override
    public String permission() {
        return "ecom:ad:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        View view = View.valueOf(args.getString("view"));
        LocalDate end = AssistantToolSupport.end(args);
        LocalDate begin = AssistantToolSupport.begin(args, end, 7);
        AssistantToolSupport.Scope scope = support.scope(args);
        int top = AssistantToolSupport.top(args, 30);
        String keyword = args.getString("keyword");
        switch (view) {
            case OVERVIEW -> {
                return overview(scope, begin, end);
            }
            case CAMPAIGN -> {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (AdsViews.HierarchyRow r : analysisService.hierarchy(EcomAdLevel.CAMPAIGN, scope.single(), null, null, begin, end, keyword).getRows()) {
                    if (!scope.contains(r.getStoreId())) {
                        continue;
                    }
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("storeName", r.getStoreName());
                    m.put("campaign", r.getName());
                    m.put("adType", r.getAdType());
                    m.put("status", r.getStatus());
                    m.put("currency", r.getCurrency());
                    m.put("dailyBudget", r.getBidOrBudget());
                    metrics(m, r, r.getPrev());
                    m.put("spendBase", r.getBaseSpend());
                    rows.add(m);
                }
                rows.sort(Comparator.comparing((Map<String, Object> m) -> Decimals.nz((BigDecimal) m.get("spendBase"))).reversed());
                return AiToolResult.rows(AssistantToolSupport.head(rows, top), (long) rows.size());
            }
            case SKU -> {
                List<AdsViews.TacosRow> rows = analysisService.tacos(scope.single(), begin, end, keyword).stream()
                        .filter(r -> scope.contains(r.getStoreId())).toList();
                return AiToolResult.rows(AssistantToolSupport.head(rows, top), (long) rows.size());
            }
            default -> {
                boolean waste = view == View.WASTE_TERMS;
                Integer minClicks = args.has("minClicks") ? args.getInt("minClicks", 10) : null;
                List<Map<String, Object>> rows = new ArrayList<>();
                for (AdsViews.SearchTermRow t : analysisService.searchTerms(scope.single(), null, begin, end, keyword, waste, minClicks, null)) {
                    if (!scope.contains(t.getStoreId()) || (!waste && t.getOrders() == 0)) {
                        continue;
                    }
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("storeName", t.getStoreName());
                    m.put("searchTerm", t.getSearchTerm());
                    m.put("campaign", t.getCampaignName());
                    m.put("adGroup", t.getAdGroupName());
                    m.put("keywordText", t.getKeywordText());
                    m.put("matchType", t.getMatchType());
                    m.put("currency", t.getCurrency());
                    metrics(m, t, null);
                    if (waste) {
                        m.put("negated", t.isNegated());
                    }
                    rows.add(m);
                }
                if (waste) {
                    rows.sort(Comparator.comparing((Map<String, Object> m) -> (BigDecimal) m.get("spend")).reversed());
                } else {
                    rows.sort(Comparator.comparing((Map<String, Object> m) -> (Long) m.get("orders")).reversed());
                }
                return AiToolResult.rows(AssistantToolSupport.head(rows, top), (long) rows.size());
            }
        }
    }

    /** 按店铺的概览（本位币），第一行为合计 */
    private AiToolResult overview(AssistantToolSupport.Scope scope, LocalDate begin, LocalDate end) {
        List<Map<String, Object>> rows = new ArrayList<>();
        AdsViews.Metrics total = new AdsViews.Metrics();
        AdsViews.Metrics totalPrev = new AdsViews.Metrics();
        BigDecimal totalSales = BigDecimal.ZERO;
        for (EcomStore s : scope.stores()) {
            AdsViews.Overview o = analysisService.overview(s.getId(), begin, end);
            if (o.getCurrent().getSpend().signum() == 0 && o.getPrev().getSpend().signum() == 0) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("storeName", s.getName());
            m.put("platform", s.getPlatform());
            m.put("marketplace", s.getMarketplace());
            metrics(m, o.getCurrent(), o.getPrev());
            m.put("totalSales", o.getTotalSales());
            m.put("tacosPercent", o.getTacos());
            m.put("wasteTerms", o.getWasteTerms());
            m.put("wasteSpend", o.getWasteSpend());
            rows.add(m);
            total.add(o.getCurrent());
            totalPrev.add(o.getPrev());
            totalSales = totalSales.add(Decimals.nz(o.getTotalSales()));
        }
        total.fill();
        totalPrev.fill();
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("storeName", Messages.get("ecom.text.asst.total"));
        t.put("period", begin + " ~ " + end);
        t.put("currency", ExchangeRateProvider.BASE);
        metrics(t, total, totalPrev);
        t.put("totalSales", Decimals.money(totalSales));
        t.put("tacosPercent", AdsViews.Metrics.ratio(total.getSpend(), totalSales, true));
        List<Map<String, Object>> out = new ArrayList<>();
        out.add(t);
        out.addAll(rows);
        return AiToolResult.rows(out);
    }

    private static void metrics(Map<String, Object> m, AdsViews.Metrics c, AdsViews.Metrics p) {
        m.put("impressions", c.getImpressions());
        m.put("clicks", c.getClicks());
        m.put("spend", Decimals.money(c.getSpend()));
        m.put("adSales", Decimals.money(c.getSales()));
        m.put("orders", c.getOrders());
        m.put("acosPercent", c.getAcos());
        m.put("ctrPercent", c.getCtr());
        m.put("cpc", c.getCpc());
        m.put("cvrPercent", c.getCvr());
        if (p != null) {
            m.put("prevSpend", Decimals.money(p.getSpend()));
            m.put("prevAdSales", Decimals.money(p.getSales()));
            m.put("prevAcosPercent", p.getAcos());
        }
    }
}
