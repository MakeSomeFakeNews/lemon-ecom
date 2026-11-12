package com.lemon.erp.modules.ecom.assistant.tool;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantRows;
import com.lemon.erp.modules.ecom.assistant.mapper.EcomAssistantQueryMapper;
import com.lemon.erp.modules.ecom.assistant.service.EcomOpsReportBuilder;
import com.lemon.erp.modules.ecom.dto.EcomReports;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.service.EcomReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 销售与利润对比：本期与对比期（默认紧挨着的上一个同样长度的期间，即环比）按店铺 / 平台 / 站点 / SKU 汇总，给出变化与变化率；
 * 结果第一行为合计（含期间与对比期）。SKU 维度另给出利润变化的分解 effect*（销售额、平台费用、退款、商品成本、物流、广告各自对利润的影响）、单价与单位成本变化、
 * 本期退货件数与差评数，便于回答「哪些 SKU 利润下滑、原因是什么」。复用利润分析的口径（本位币，利润已扣广告），店铺数据权限生效
 */
@Component
@RequiredArgsConstructor
public class EcomSalesCompareTool implements AiTool {

    enum Dimension {
        STORE, PLATFORM, MARKETPLACE, SKU
    }

    enum SortBy {
        SALES, PROFIT, SALES_CHANGE, PROFIT_CHANGE
    }

    private final EcomReportService reportService;
    private final EcomAssistantQueryMapper queryMapper;
    private final AssistantToolSupport support;

    @Override
    public String name() {
        return "ecom_sales_compare";
    }

    @Override
    public String labelKey() {
        return "ecom.ai.asst.tool.salesCompare";
    }

    @Override
    public String descriptionKey() {
        return "ecom.ai.asst.tool.salesCompare.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        AiSchema s = AiSchema.object()
                .prop("startDate", AiSchema.date("ecom.ai.asst.param.startDate"), true)
                .prop("endDate", AiSchema.date("ecom.ai.asst.param.endDate"), true)
                .prop("compareStartDate", AiSchema.date("ecom.ai.asst.param.compareStartDate"), false)
                .prop("compareEndDate", AiSchema.date("ecom.ai.asst.param.compareEndDate"), false)
                .prop("dimension", AiSchema.enumOf("ecom.ai.asst.param.dimension", names(Dimension.values())), false);
        AssistantToolSupport.scopeProps(s)
                .prop("keyword", AiSchema.string("ecom.ai.param.skuKeyword").maxLength(64), false)
                .prop("sortBy", AiSchema.enumOf("ecom.ai.asst.param.sortBy", names(SortBy.values())), false)
                .prop("declineOnly", AiSchema.bool("ecom.ai.asst.param.declineOnly"), false);
        return AssistantToolSupport.topProp(s).build();
    }

    @Override
    public String permission() {
        return "ecom:report:list";
    }

    /** 一行的指标（本期 / 对比期） */
    static final class Agg {
        long orders;
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal sales = BigDecimal.ZERO;
        BigDecimal fees = BigDecimal.ZERO;
        BigDecimal refund = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal shipping = BigDecimal.ZERO;
        BigDecimal adSpend = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;

        void add(Long o, BigDecimal q, BigDecimal s, BigDecimal f, BigDecimal r, BigDecimal c, BigDecimal sh, BigDecimal ad, BigDecimal p) {
            orders += o == null ? 0 : o;
            qty = qty.add(Decimals.nz(q));
            sales = sales.add(Decimals.nz(s));
            fees = fees.add(Decimals.nz(f));
            refund = refund.add(Decimals.nz(r));
            cost = cost.add(Decimals.nz(c));
            shipping = shipping.add(Decimals.nz(sh));
            adSpend = adSpend.add(Decimals.nz(ad));
            profit = profit.add(Decimals.nz(p));
        }
    }

    @Override
    public Object execute(AiToolArgs args) {
        LocalDate begin = args.getDate("startDate");
        LocalDate end = args.getDate("endDate");
        if (begin == null || end == null || begin.isAfter(end)) {
            throw new BizException("ecom.error.asst.rangeInvalid");
        }
        long days = ChronoUnit.DAYS.between(begin, end) + 1;
        if (days > 366) {
            throw new BizException("ecom.error.asst.rangeTooLong", 366);
        }
        LocalDate cBegin = args.getDate("compareStartDate");
        LocalDate cEnd = args.getDate("compareEndDate");
        if (cBegin == null || cEnd == null) {
            cEnd = begin.minusDays(1);
            cBegin = cEnd.minusDays(days - 1);
        }
        AssistantToolSupport.Scope scope = support.scope(args);
        Dimension dim = args.has("dimension") ? Dimension.valueOf(args.getString("dimension")) : Dimension.STORE;
        SortBy sortBy = args.has("sortBy") ? SortBy.valueOf(args.getString("sortBy")) : SortBy.SALES;
        boolean declineOnly = Boolean.TRUE.equals(args.getBoolean("declineOnly"));
        int top = AssistantToolSupport.top(args, 30);
        String keyword = args.getString("keyword");

        Map<String, Agg> cur = new LinkedHashMap<>();
        Map<String, Agg> prev = new HashMap<>();
        Map<String, Map<String, Object>> keys = new LinkedHashMap<>();
        if (dim == Dimension.SKU) {
            collectSku(reportService.bySku(begin, end, scope.single(), keyword), scope, cur, keys);
            collectSku(reportService.bySku(cBegin, cEnd, scope.single(), keyword), scope, prev, keys);
        } else {
            Map<Long, EcomStore> stores = new HashMap<>();
            scope.stores().forEach(s -> stores.put(s.getId(), s));
            Function<EcomReports.StoreRow, String> key = switch (dim) {
                case PLATFORM -> r -> String.valueOf(r.getPlatform());
                case MARKETPLACE -> r -> r.getPlatform() + "/" + r.getMarketplace();
                default -> r -> String.valueOf(r.getStoreId());
            };
            collectStore(reportService.byStore(begin, end, scope.single()), scope, key, dim, cur, keys);
            collectStore(reportService.byStore(cBegin, cEnd, scope.single()), scope, key, dim, prev, keys);
        }
        Map<String, Long> returnQty = new HashMap<>();
        Map<String, Long> badReviews = new HashMap<>();
        if (dim == Dimension.SKU) {
            for (AssistantRows.SkuReturn r : queryMapper.selectSkuReturns(scope.ids(), begin.atStartOfDay(), end.plusDays(1).atStartOfDay())) {
                returnQty.put(r.getStoreId() + "|" + r.getSellerSku(), Decimals.nz(r.getQty()).longValue());
            }
            for (AssistantRows.ReviewSkuRow r : queryMapper.selectReviewSkus(scope.ids(), begin, end, keyword)) {
                badReviews.put(r.getStoreId() + "|" + r.getSellerSku(), r.getBadReviews());
            }
        }

        Agg total = new Agg();
        Agg totalPrev = new Agg();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> e : keys.entrySet()) {
            Agg c = cur.getOrDefault(e.getKey(), new Agg());
            Agg p = prev.getOrDefault(e.getKey(), new Agg());
            total.add(c.orders, c.qty, c.sales, c.fees, c.refund, c.cost, c.shipping, c.adSpend, c.profit);
            totalPrev.add(p.orders, p.qty, p.sales, p.fees, p.refund, p.cost, p.shipping, p.adSpend, p.profit);
            Map<String, Object> row = new LinkedHashMap<>(e.getValue());
            fill(row, c, p);
            if (dim == Dimension.SKU) {
                row.put("avgPrice", unit(c.sales, c.qty));
                row.put("prevAvgPrice", unit(p.sales, p.qty));
                row.put("unitCost", unit(c.cost, c.qty));
                row.put("prevUnitCost", unit(p.cost, p.qty));
                row.put("adSpend", Decimals.money(c.adSpend));
                row.put("prevAdSpend", Decimals.money(p.adSpend));
                row.put("refund", Decimals.money(c.refund));
                row.put("prevRefund", Decimals.money(p.refund));
                row.put("returnQty", returnQty.getOrDefault(e.getKey(), 0L));
                row.put("badReviews", badReviews.getOrDefault(e.getKey(), 0L));
                // 利润变化分解：各项对利润的影响（正数为增加利润）
                row.put("effectSales", Decimals.money(c.sales.subtract(p.sales)));
                row.put("effectPlatformFees", Decimals.money(p.fees.subtract(c.fees)));
                row.put("effectRefund", Decimals.money(p.refund.subtract(c.refund)));
                row.put("effectProductCost", Decimals.money(p.cost.subtract(c.cost)));
                row.put("effectShipping", Decimals.money(p.shipping.subtract(c.shipping)));
                row.put("effectAdSpend", Decimals.money(p.adSpend.subtract(c.adSpend)));
            }
            if (declineOnly && sortBy != SortBy.SALES_CHANGE && c.profit.compareTo(p.profit) >= 0) {
                continue;
            }
            if (declineOnly && sortBy == SortBy.SALES_CHANGE && c.sales.compareTo(p.sales) >= 0) {
                continue;
            }
            rows.add(row);
        }
        if (declineOnly) {
            // 只看下降：按降幅（差额）从大到小
            String diff = sortBy == SortBy.SALES_CHANGE ? "salesDiff" : "profitDiff";
            rows.sort(Comparator.comparing(r -> (BigDecimal) r.get(diff)));
        } else {
            String field = switch (sortBy) {
                case PROFIT -> "profit";
                case SALES_CHANGE -> "salesDiff";
                case PROFIT_CHANGE -> "profitDiff";
                default -> "sales";
            };
            rows.sort(Comparator.comparing((Map<String, Object> r) -> (BigDecimal) r.get(field)).reversed());
        }
        // 第一行为合计（期间、对比期写在合计行），之后为明细
        Map<String, Object> t = new LinkedHashMap<>();
        t.put(dim == Dimension.SKU ? "sellerSku" : dim == Dimension.STORE ? "storeName" : "platform", Messages.get("ecom.text.asst.total"));
        t.put("period", begin + " ~ " + end);
        t.put("comparePeriod", cBegin + " ~ " + cEnd);
        fill(t, total, totalPrev);
        t.put("adSpend", Decimals.money(total.adSpend));
        t.put("prevAdSpend", Decimals.money(totalPrev.adSpend));
        List<Map<String, Object>> out = new ArrayList<>();
        out.add(t);
        out.addAll(AssistantToolSupport.head(rows, top));
        return AiToolResult.rows(out, (long) rows.size() + 1);
    }

    private static void fill(Map<String, Object> row, Agg c, Agg p) {
        row.put("orders", c.orders);
        row.put("prevOrders", p.orders);
        row.put("qty", Decimals.qty(c.qty).stripTrailingZeros().toPlainString());
        row.put("prevQty", Decimals.qty(p.qty).stripTrailingZeros().toPlainString());
        row.put("sales", Decimals.money(c.sales));
        row.put("prevSales", Decimals.money(p.sales));
        row.put("salesDiff", Decimals.money(c.sales.subtract(p.sales)));
        row.put("salesChangePercent", EcomOpsReportBuilder.change(c.sales, p.sales));
        row.put("profit", Decimals.money(c.profit));
        row.put("prevProfit", Decimals.money(p.profit));
        row.put("profitDiff", Decimals.money(c.profit.subtract(p.profit)));
        row.put("profitChangePercent", EcomOpsReportBuilder.change(c.profit, p.profit));
        row.put("marginPercent", Decimals.percent(c.profit, c.sales, 2));
        row.put("prevMarginPercent", Decimals.percent(p.profit, p.sales, 2));
    }

    private static BigDecimal unit(BigDecimal amount, BigDecimal qty) {
        return qty == null || qty.signum() == 0 ? null : amount.divide(qty, 2, RoundingMode.HALF_UP);
    }

    private static void collectSku(List<EcomReports.SkuRow> rows, AssistantToolSupport.Scope scope, Map<String, Agg> into,
                                   Map<String, Map<String, Object>> keys) {
        for (EcomReports.SkuRow r : rows) {
            if (!scope.contains(r.getStoreId())) {
                continue;
            }
            String k = r.getStoreId() + "|" + r.getSellerSku();
            into.computeIfAbsent(k, x -> new Agg()).add(r.getOrders(), r.getQty(), r.getSales(), r.getFees(), r.getRefund(), r.getCost(),
                    r.getShipping(), r.getAdSpend(), r.getProfit());
            keys.computeIfAbsent(k, x -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("storeName", r.getStoreName());
                m.put("sellerSku", r.getSellerSku());
                m.put("title", r.getTitle() == null || r.getTitle().length() <= 80 ? r.getTitle() : r.getTitle().substring(0, 80) + "…");
                m.put("materialCode", r.getMaterialCode());
                return m;
            });
        }
    }

    private static void collectStore(List<EcomReports.StoreRow> rows, AssistantToolSupport.Scope scope, Function<EcomReports.StoreRow, String> key,
                                     Dimension dim, Map<String, Agg> into, Map<String, Map<String, Object>> keys) {
        for (EcomReports.StoreRow r : rows) {
            if (!scope.contains(r.getStoreId())) {
                continue;
            }
            String k = key.apply(r);
            into.computeIfAbsent(k, x -> new Agg()).add(r.getOrders(), r.getQty(), r.getSales(), r.getFees(), r.getRefund(), r.getCost(),
                    r.getShipping(), r.getAdSpend(), r.getProfit());
            keys.computeIfAbsent(k, x -> {
                Map<String, Object> m = new LinkedHashMap<>();
                if (dim == Dimension.STORE) {
                    m.put("storeName", r.getStoreName());
                }
                m.put("platform", r.getPlatform());
                if (dim != Dimension.PLATFORM) {
                    m.put("marketplace", r.getMarketplace());
                }
                return m;
            });
        }
    }

    private static String[] names(Enum<?>[] values) {
        String[] out = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = values[i].name();
        }
        return out;
    }
}
