package com.lemon.erp.modules.ecom.assistant.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantRows;
import com.lemon.erp.modules.ecom.assistant.dto.OpsReportContent;
import com.lemon.erp.modules.ecom.assistant.mapper.EcomAssistantQueryMapper;
import com.lemon.erp.modules.ecom.assistant.model.OpsAbnormalType;
import com.lemon.erp.modules.ecom.assistant.model.OpsAdAlertType;
import com.lemon.erp.modules.ecom.assistant.model.OpsTodoKind;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.dto.EcomReports;
import com.lemon.erp.modules.ecom.dto.ReplenishRow;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.model.ReplenishLevel;
import com.lemon.erp.modules.ecom.service.EcomAdAnalysisService;
import com.lemon.erp.modules.ecom.service.EcomReplenishService;
import com.lemon.erp.modules.ecom.service.EcomReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 运营日报的数据汇总（全部由程序计算，不依赖 AI）：复用利润分析（订单、利润、广告分摊口径一致）、补货建议、广告分析，
 * 外加本模块的汇总查询（退货、差评、待办、广告活动）。结果只包含指定店铺的数据；在接口中调用时店铺数据权限另外生效。
 * <p>
 * 异常识别规则（常量可调）：
 * <ul>
 *   <li>销量骤降：前 7 天日均件数不少于 {@value #MIN_AVG_QTY}、当天不到日均的一半，或日均不少于 1 件、当天没有销量</li>
 *   <li>利润下滑：当天有销量，前 7 天日均利润不少于 60（本位币），当天不到日均的 30%（销量骤降的不重复列出）</li>
 *   <li>退货率升高：近 7 天退货不少于 {@value #MIN_RETURNS} 件、退货率不低于 {@value #MIN_RETURN_RATE}%，且至少是此前 30 天的 2 倍</li>
 *   <li>广告：当天花费达到日预算；ACOS 不低于 30% 且是近 7 天的 1.5 倍以上（花费折本位币不少于 {@value #MIN_AD_SPEND_BASE}）；
 *       花费折本位币不少于 {@value #NO_ORDER_SPEND_BASE} 但没有订单</li>
 *   <li>断货风险：补货建议中紧急 / 预警、日均销量不低于 0.5 件的平台 SKU（紧急的在前，按日均销量从高到低）</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class EcomOpsReportBuilder {

    static final BigDecimal MIN_AVG_QTY = BigDecimal.valueOf(2);
    static final BigDecimal MIN_AVG_PROFIT = BigDecimal.valueOf(60);
    /** 利润下滑：当天利润低于日均的该比例 */
    static final BigDecimal PROFIT_FACTOR = new BigDecimal("0.3");
    static final BigDecimal MIN_RETURNS = BigDecimal.valueOf(2);
    static final BigDecimal MIN_RETURN_RATE = BigDecimal.TEN;
    static final BigDecimal HALF = new BigDecimal("0.5");
    static final BigDecimal MIN_AD_SPEND_BASE = BigDecimal.valueOf(50);
    static final BigDecimal NO_ORDER_SPEND_BASE = BigDecimal.valueOf(100);
    static final BigDecimal ACOS_FLOOR = BigDecimal.valueOf(30);
    static final BigDecimal ACOS_FACTOR = new BigDecimal("1.5");
    static final BigDecimal SEVEN = BigDecimal.valueOf(7);
    /** 断货风险只列日均销量不低于该值的 SKU（偶尔出单的不列，避免淹没重点） */
    static final BigDecimal MIN_RISK_DAILY = new BigDecimal("0.5");
    /** 各列表最多保留的行数 */
    static final int MAX_ROWS = 30;
    /** 销售额 / 利润环比变化达到该百分比时写入要点 */
    static final BigDecimal CHANGE_NOTABLE = BigDecimal.TEN;

    public static final String LEVEL_GOOD = "GOOD";
    public static final String LEVEL_WARN = "WARN";
    public static final String LEVEL_BAD = "BAD";
    public static final String LEVEL_INFO = "INFO";

    private final EcomReportService reportService;
    private final EcomReplenishService replenishService;
    private final EcomAdAnalysisService adAnalysisService;
    private final EcomAssistantQueryMapper queryMapper;
    private final ExchangeRateProvider rateProvider;

    /**
     * 汇总一天的日报
     *
     * @param day    统计日期
     * @param stores 范围内的店铺（已校验可见）
     */
    public OpsReportContent build(LocalDate day, List<EcomStore> stores, LocalDateTime now) {
        OpsReportContent c = new OpsReportContent();
        c.setDate(day);
        Set<Long> ids = new HashSet<>();
        for (EcomStore s : stores) {
            ids.add(s.getId());
            c.getStores().add(new OpsReportContent.StoreRef(s.getId(), s.getName(), s.getPlatform(), s.getMarketplace()));
        }
        if (ids.isEmpty()) {
            return c;
        }
        LocalDate prevDay = day.minusDays(1);
        LocalDate weekBegin = day.minusDays(7);

        // 核心指标与店铺
        List<EcomReports.StoreRow> cur = byStore(day, day, ids);
        List<EcomReports.StoreRow> prev = byStore(prevDay, prevDay, ids);
        List<EcomReports.StoreRow> week = byStore(weekBegin, prevDay, ids);
        Map<Long, BigDecimal[]> adCur = adByStore(ids, day, day);
        Map<Long, BigDecimal[]> adPrev = adByStore(ids, prevDay, prevDay);
        Map<Long, BigDecimal[]> adWeek = adByStore(ids, weekBegin, prevDay);
        c.setKpi(kpi(cur, adCur, returns(ids, day, day), BigDecimal.ONE));
        c.setPrev(kpi(prev, adPrev, returns(ids, prevDay, prevDay), BigDecimal.ONE));
        c.setAvg7(kpi(week, adWeek, returns(ids, weekBegin, prevDay), SEVEN));
        c.setStoreRows(storeRows(stores, cur, prev, adCur));

        abnormal(c, day, ids);
        badReviews(c, day, ids);
        stockRisks(c, ids);
        adAlerts(c, day, ids);
        todos(c, ids, now);
        highlights(c);
        return c;
    }

    // ---------------------------------------------------------------- 核心指标

    private List<EcomReports.StoreRow> byStore(LocalDate begin, LocalDate end, Set<Long> ids) {
        return reportService.byStore(begin, end, null).stream().filter(r -> ids.contains(r.getStoreId())).toList();
    }

    /** 店铺 → [广告花费（本位币）, 广告销售额（本位币）] */
    private Map<Long, BigDecimal[]> adByStore(Set<Long> ids, LocalDate begin, LocalDate end) {
        Map<Long, BigDecimal[]> map = new HashMap<>();
        for (AssistantRows.AdTotal a : queryMapper.selectAdTotals(ids, begin, end)) {
            BigDecimal rate = rate(a.getCurrency(), end);
            map.put(a.getStoreId(), new BigDecimal[]{Decimals.money(Decimals.nz(a.getSpend()).multiply(rate)),
                    Decimals.money(Decimals.nz(a.getSales()).multiply(rate))});
        }
        return map;
    }

    private long returns(Set<Long> ids, LocalDate begin, LocalDate end) {
        long n = 0;
        for (Map<String, Object> m : queryMapper.selectReturnCounts(ids, begin.atStartOfDay(), end.plusDays(1).atStartOfDay())) {
            n += ((Number) m.get("returns")).longValue();
        }
        return n;
    }

    private BigDecimal rate(String currency, LocalDate date) {
        BigDecimal r = currency == null ? null : rateProvider.rate(currency, date);
        return r == null ? BigDecimal.ONE : r;
    }

    /** 汇总（divisor 为 7 时为日均） */
    static OpsReportContent.Kpi kpi(List<EcomReports.StoreRow> rows, Map<Long, BigDecimal[]> ad, long returns, BigDecimal divisor) {
        OpsReportContent.Kpi k = new OpsReportContent.Kpi();
        long orders = 0;
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal sales = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;
        BigDecimal refund = BigDecimal.ZERO;
        for (EcomReports.StoreRow r : rows) {
            orders += r.getOrders() == null ? 0 : r.getOrders();
            qty = qty.add(Decimals.nz(r.getQty()));
            sales = sales.add(Decimals.nz(r.getSales()));
            profit = profit.add(Decimals.nz(r.getProfit()));
            refund = refund.add(Decimals.nz(r.getRefund()));
        }
        BigDecimal adSpend = BigDecimal.ZERO;
        BigDecimal adSales = BigDecimal.ZERO;
        for (BigDecimal[] a : ad.values()) {
            adSpend = adSpend.add(a[0]);
            adSales = adSales.add(a[1]);
        }
        boolean avg = divisor.compareTo(BigDecimal.ONE) != 0;
        k.setOrders(avg ? BigDecimal.valueOf(orders).divide(divisor, 0, RoundingMode.HALF_UP).longValue() : orders);
        k.setQty(div(qty, divisor));
        k.setSales(Decimals.money(div(sales, divisor)));
        k.setProfit(Decimals.money(div(profit, divisor)));
        k.setRefund(Decimals.money(div(refund, divisor)));
        k.setAdSpend(Decimals.money(div(adSpend, divisor)));
        k.setAdSales(Decimals.money(div(adSales, divisor)));
        k.setReturns(avg ? BigDecimal.valueOf(returns).divide(divisor, 0, RoundingMode.HALF_UP).longValue() : returns);
        k.setMarginPercent(Decimals.percent(profit, sales, 2));
        k.setAcosPercent(Decimals.percent(adSpend, adSales, 2));
        k.setTacosPercent(Decimals.percent(adSpend, sales, 2));
        return k;
    }

    private static BigDecimal div(BigDecimal v, BigDecimal divisor) {
        return divisor.compareTo(BigDecimal.ONE) == 0 ? v : v.divide(divisor, 4, RoundingMode.HALF_UP);
    }

    static List<OpsReportContent.StoreKpi> storeRows(List<EcomStore> stores, List<EcomReports.StoreRow> cur, List<EcomReports.StoreRow> prev,
                                                     Map<Long, BigDecimal[]> ad) {
        Map<Long, EcomReports.StoreRow> c = index(cur, EcomReports.StoreRow::getStoreId);
        Map<Long, EcomReports.StoreRow> p = index(prev, EcomReports.StoreRow::getStoreId);
        List<OpsReportContent.StoreKpi> out = new ArrayList<>();
        for (EcomStore s : stores) {
            EcomReports.StoreRow r = c.get(s.getId());
            EcomReports.StoreRow q = p.get(s.getId());
            OpsReportContent.StoreKpi k = new OpsReportContent.StoreKpi();
            k.setStoreId(s.getId());
            k.setStoreName(s.getName());
            k.setPlatform(s.getPlatform());
            k.setMarketplace(s.getMarketplace());
            k.setOrders(r == null || r.getOrders() == null ? 0 : r.getOrders());
            k.setSales(Decimals.money(r == null ? null : r.getSales()));
            k.setProfit(Decimals.money(r == null ? null : r.getProfit()));
            k.setMarginPercent(Decimals.percent(k.getProfit(), k.getSales(), 2));
            BigDecimal[] a = ad.get(s.getId());
            if (a != null) {
                k.setAdSpend(a[0]);
                k.setAcosPercent(Decimals.percent(a[0], a[1], 2));
            }
            k.setPrevSales(Decimals.money(q == null ? null : q.getSales()));
            k.setPrevProfit(Decimals.money(q == null ? null : q.getProfit()));
            k.setSalesChange(change(k.getSales(), k.getPrevSales()));
            k.setProfitChange(change(k.getProfit(), k.getPrevProfit()));
            out.add(k);
        }
        out.sort(Comparator.comparing(OpsReportContent.StoreKpi::getSales).reversed());
        return out;
    }

    /** 变化百分比 = (当前 − 基准) ÷ |基准| × 100，基准为 0 时为空 */
    public static BigDecimal change(BigDecimal current, BigDecimal base) {
        if (base == null || base.signum() == 0) {
            return null;
        }
        return Decimals.nz(current).subtract(base).multiply(Decimals.HUNDRED).divide(base.abs(), 2, RoundingMode.HALF_UP);
    }

    private static <K, T> Map<K, T> index(Collection<T> rows, Function<T, K> key) {
        Map<K, T> map = new LinkedHashMap<>();
        rows.forEach(r -> map.put(key.apply(r), r));
        return map;
    }

    // ---------------------------------------------------------------- 异常 SKU

    private void abnormal(OpsReportContent c, LocalDate day, Set<Long> ids) {
        Map<String, EcomReports.SkuRow> cur = skuIndex(reportService.bySku(day, day, null, null), ids);
        Map<String, EcomReports.SkuRow> week = skuIndex(reportService.bySku(day.minusDays(7), day.minusDays(1), null, null), ids);
        List<OpsReportContent.AbnormalSku> out = new ArrayList<>();
        for (Map.Entry<String, EcomReports.SkuRow> e : week.entrySet()) {
            EcomReports.SkuRow w = e.getValue();
            EcomReports.SkuRow t = cur.get(e.getKey());
            BigDecimal avgQty = Decimals.nz(w.getQty()).divide(SEVEN, 2, RoundingMode.HALF_UP);
            BigDecimal avgProfit = Decimals.nz(w.getProfit()).divide(SEVEN, 2, RoundingMode.HALF_UP);
            BigDecimal qty = t == null ? BigDecimal.ZERO : Decimals.nz(t.getQty());
            BigDecimal profit = t == null ? BigDecimal.ZERO : Decimals.money(t.getProfit());
            OpsAbnormalType type = null;
            BigDecimal current = null;
            BigDecimal baseline = null;
            boolean stopped = qty.signum() == 0 && avgQty.compareTo(BigDecimal.ONE) >= 0;
            if (stopped || avgQty.compareTo(MIN_AVG_QTY) >= 0 && qty.compareTo(avgQty.multiply(HALF)) < 0) {
                type = OpsAbnormalType.SALES_DROP;
                current = qty;
                baseline = avgQty;
            } else if (qty.signum() > 0 && avgProfit.compareTo(MIN_AVG_PROFIT) >= 0 && profit.compareTo(avgProfit.multiply(PROFIT_FACTOR)) < 0) {
                type = OpsAbnormalType.PROFIT_DROP;
                current = profit;
                baseline = avgProfit;
            }
            if (type != null) {
                OpsReportContent.AbnormalSku a = sku(type, w, current, baseline);
                a.setQty(qty);
                a.setAvgQty(avgQty);
                a.setAdSpend(t == null ? BigDecimal.ZERO : Decimals.money(t.getAdSpend()));
                a.setAvgAdSpend(Decimals.money(Decimals.nz(w.getAdSpend()).divide(SEVEN, 2, RoundingMode.HALF_UP)));
                a.setRefund(t == null ? BigDecimal.ZERO : Decimals.money(t.getRefund()));
                out.add(a);
            }
        }
        out.addAll(returnRateUp(day, ids));
        // 按影响排序：基准越大、降幅越大越靠前
        out.sort(Comparator.comparing((OpsReportContent.AbnormalSku a) -> Decimals.nz(a.getBaseline()).subtract(Decimals.nz(a.getCurrent())).abs()
                .multiply(a.getType() == OpsAbnormalType.SALES_DROP ? BigDecimal.TEN : BigDecimal.ONE)).reversed());
        c.setAbnormalTotal(out.size());
        c.setAbnormalSkus(out.size() > MAX_ROWS ? new ArrayList<>(out.subList(0, MAX_ROWS)) : out);
    }

    /** 近 7 天退货率与此前 30 天对比 */
    private List<OpsReportContent.AbnormalSku> returnRateUp(LocalDate day, Set<Long> ids) {
        LocalDate b7 = day.minusDays(6);
        LocalDate b30 = day.minusDays(36);
        LocalDate e30 = day.minusDays(7);
        Map<String, AssistantRows.SkuReturn> r7 = new HashMap<>();
        queryMapper.selectSkuReturns(ids, b7.atStartOfDay(), day.plusDays(1).atStartOfDay()).forEach(r -> r7.put(r.getStoreId() + "|" + r.getSellerSku(), r));
        if (r7.isEmpty()) {
            return List.of();
        }
        Map<String, AssistantRows.SkuReturn> r30 = new HashMap<>();
        queryMapper.selectSkuReturns(ids, b30.atStartOfDay(), e30.plusDays(1).atStartOfDay()).forEach(r -> r30.put(r.getStoreId() + "|" + r.getSellerSku(), r));
        Map<String, EcomReports.SkuRow> sold7 = skuIndex(reportService.bySku(b7, day, null, null), ids);
        Map<String, EcomReports.SkuRow> sold30 = skuIndex(reportService.bySku(b30, e30, null, null), ids);
        List<OpsReportContent.AbnormalSku> out = new ArrayList<>();
        for (Map.Entry<String, AssistantRows.SkuReturn> e : r7.entrySet()) {
            EcomReports.SkuRow s = sold7.get(e.getKey());
            BigDecimal returned = Decimals.nz(e.getValue().getQty());
            if (s == null || returned.compareTo(MIN_RETURNS) < 0) {
                continue;
            }
            BigDecimal rate = Decimals.percent(returned, s.getQty(), 2);
            if (rate == null || rate.compareTo(MIN_RETURN_RATE) < 0) {
                continue;
            }
            AssistantRows.SkuReturn p = r30.get(e.getKey());
            EcomReports.SkuRow ps = sold30.get(e.getKey());
            BigDecimal prevRate = p == null || ps == null ? BigDecimal.ZERO : Decimals.nz(Decimals.percent(p.getQty(), ps.getQty(), 2));
            if (prevRate.signum() > 0 && rate.compareTo(prevRate.multiply(BigDecimal.valueOf(2))) < 0) {
                continue;
            }
            OpsReportContent.AbnormalSku a = sku(OpsAbnormalType.RETURN_RATE_UP, s, rate, prevRate);
            a.setQty(returned);
            a.setRefund(Decimals.money(e.getValue().getRefundBase()));
            out.add(a);
        }
        return out;
    }

    private static OpsReportContent.AbnormalSku sku(OpsAbnormalType type, EcomReports.SkuRow s, BigDecimal current, BigDecimal baseline) {
        OpsReportContent.AbnormalSku a = new OpsReportContent.AbnormalSku();
        a.setType(type);
        a.setStoreId(s.getStoreId());
        a.setStoreName(s.getStoreName());
        a.setSellerSku(s.getSellerSku());
        a.setTitle(s.getTitle());
        a.setCurrent(current);
        a.setBaseline(baseline);
        a.setChangePercent(type == OpsAbnormalType.RETURN_RATE_UP ? Decimals.nz(current).subtract(Decimals.nz(baseline)).setScale(2, RoundingMode.HALF_UP)
                : change(current, baseline));
        return a;
    }

    private static Map<String, EcomReports.SkuRow> skuIndex(List<EcomReports.SkuRow> rows, Set<Long> ids) {
        Map<String, EcomReports.SkuRow> map = new LinkedHashMap<>();
        for (EcomReports.SkuRow r : rows) {
            if (ids.contains(r.getStoreId())) {
                map.put(r.getStoreId() + "|" + r.getSellerSku(), r);
            }
        }
        return map;
    }

    // ---------------------------------------------------------------- 差评、断货、广告、待办

    private void badReviews(OpsReportContent c, LocalDate day, Set<Long> ids) {
        for (AssistantRows.ReviewRow r : queryMapper.selectReviews(ids, day, day, 2, null, MAX_ROWS)) {
            OpsReportContent.BadReview b = new OpsReportContent.BadReview();
            b.setId(r.getId());
            b.setStoreId(r.getStoreId());
            b.setStoreName(r.getStoreName());
            b.setSellerSku(r.getSellerSku());
            b.setRating(r.getRating());
            b.setTitle(r.getTitle());
            b.setContent(clip(r.getContent(), 300));
            b.setReviewDate(r.getReviewDate());
            c.getBadReviews().add(b);
        }
    }

    private void stockRisks(OpsReportContent c, Set<Long> ids) {
        List<ReplenishRow> all = replenishService.rows(null, null, null).stream()
                .filter(r -> ids.contains(r.getStoreId()) && (r.getLevel() == ReplenishLevel.URGENT || r.getLevel() == ReplenishLevel.WARN))
                .filter(r -> Decimals.nz(r.getDailySales()).compareTo(MIN_RISK_DAILY) >= 0)
                .sorted(Comparator.comparing((ReplenishRow r) -> r.getLevel() == ReplenishLevel.URGENT ? 0 : 1)
                        .thenComparing((ReplenishRow r) -> Decimals.nz(r.getDailySales()), Comparator.reverseOrder()))
                .toList();
        c.setStockRiskTotal(all.size());
        c.setStockUrgentTotal((int) all.stream().filter(r -> r.getLevel() == ReplenishLevel.URGENT).count());
        List<ReplenishRow> rows = all.size() > MAX_ROWS ? all.subList(0, MAX_ROWS) : all;
        for (ReplenishRow r : rows) {
            OpsReportContent.StockRisk s = new OpsReportContent.StockRisk();
            s.setStoreId(r.getStoreId());
            s.setStoreName(r.getStoreName());
            s.setSellerSku(r.getSellerSku());
            s.setTitle(r.getTitle());
            s.setDailySales(r.getDailySales());
            s.setPlatformQty(r.getPlatformQty());
            s.setInTransitQty(r.getInTransitQty());
            s.setCoverDays(r.getCoverDays());
            s.setStockoutDate(r.getStockoutDate());
            s.setSuggestQty(r.getSuggestQty());
            s.setLevel(r.getLevel());
            c.getStockRisks().add(s);
        }
    }

    private void adAlerts(OpsReportContent c, LocalDate day, Set<Long> ids) {
        Map<Long, AdsViews.Metrics> today = adAnalysisService.metrics(EcomAdLevel.CAMPAIGN, ids, null, null, new EcomAdAnalysisService.Range(day, day));
        if (today.isEmpty()) {
            return;
        }
        Map<Long, AdsViews.Metrics> week = adAnalysisService.metrics(EcomAdLevel.CAMPAIGN, ids, null, null,
                new EcomAdAnalysisService.Range(day.minusDays(7), day.minusDays(1)));
        List<OpsReportContent.AdAlert> out = new ArrayList<>();
        for (AssistantRows.CampaignInfo info : queryMapper.selectCampaigns(ids)) {
            AdsViews.Metrics m = today.get(info.getId());
            if (m == null || Decimals.nz(m.getSpend()).signum() == 0) {
                continue;
            }
            BigDecimal spend = Decimals.nz(m.getSpend());
            BigDecimal spendBase = spend.multiply(rate(info.getCurrency(), day));
            BigDecimal acos = Decimals.percent(spend, m.getSales(), 2);
            AdsViews.Metrics w = week.get(info.getId());
            BigDecimal avgAcos = w == null ? null : Decimals.percent(w.getSpend(), w.getSales(), 2);
            OpsAdAlertType type = null;
            if (Decimals.positive(info.getDailyBudget()) && spend.compareTo(info.getDailyBudget()) >= 0) {
                type = OpsAdAlertType.OVER_BUDGET;
            } else if (m.getOrders() == 0 && spendBase.compareTo(NO_ORDER_SPEND_BASE) >= 0) {
                type = OpsAdAlertType.NO_ORDER;
            } else if (acos != null && avgAcos != null && spendBase.compareTo(MIN_AD_SPEND_BASE) >= 0 && acos.compareTo(ACOS_FLOOR) >= 0
                    && acos.compareTo(avgAcos.multiply(ACOS_FACTOR)) >= 0) {
                type = OpsAdAlertType.ACOS_HIGH;
            }
            if (type == null) {
                continue;
            }
            OpsReportContent.AdAlert a = new OpsReportContent.AdAlert();
            a.setType(type);
            a.setStoreId(info.getStoreId());
            a.setStoreName(info.getStoreName());
            a.setCampaignId(info.getId());
            a.setCampaignName(info.getName());
            a.setCurrency(info.getCurrency());
            a.setSpend(Decimals.money(spend));
            a.setSales(Decimals.money(m.getSales()));
            a.setOrders(m.getOrders());
            a.setBudget(info.getDailyBudget());
            a.setAcosPercent(acos);
            a.setAvgAcosPercent(avgAcos);
            out.add(a);
        }
        out.sort(Comparator.comparing((OpsReportContent.AdAlert a) -> a.getType().ordinal()).thenComparing(OpsReportContent.AdAlert::getSpend,
                Comparator.reverseOrder()));
        c.setAdAlertTotal(out.size());
        c.setAdAlerts(out.size() > MAX_ROWS ? new ArrayList<>(out.subList(0, MAX_ROWS)) : out);
    }

    /** 待处理事项：键与前端的文字、跳转地址对应 */
    private void todos(OpsReportContent c, Set<Long> ids, LocalDateTime now) {
        Map<String, Object> m = queryMapper.selectTodoCounts(ids, now, now.toLocalDate().plusDays(14));
        if (m == null) {
            return;
        }
        for (OpsTodoKind kind : OpsTodoKind.values()) {
            long n = m.get(kind.column()) instanceof Number num ? num.longValue() : 0;
            if (n > 0) {
                c.getTodos().add(new OpsReportContent.Todo(kind, n, kind.link()));
            }
        }
    }

    // ---------------------------------------------------------------- 要点

    static void highlights(OpsReportContent c) {
        List<OpsReportContent.Highlight> h = c.getHighlights();
        OpsReportContent.Kpi k = c.getKpi();
        OpsReportContent.Kpi p = c.getPrev();
        if (k.getOrders() == 0) {
            h.add(OpsReportContent.Highlight.of(LEVEL_INFO, "noOrders"));
        }
        BigDecimal sc = change(k.getSales(), p.getSales());
        if (sc != null && sc.abs().compareTo(CHANGE_NOTABLE) >= 0) {
            h.add(OpsReportContent.Highlight.of(sc.signum() > 0 ? LEVEL_GOOD : LEVEL_BAD, sc.signum() > 0 ? "salesUp" : "salesDown",
                    money(k.getSales()), sc.abs().toPlainString()));
        }
        BigDecimal pc = change(k.getProfit(), p.getProfit());
        if (pc != null && pc.abs().compareTo(CHANGE_NOTABLE) >= 0) {
            h.add(OpsReportContent.Highlight.of(pc.signum() > 0 ? LEVEL_GOOD : LEVEL_BAD, pc.signum() > 0 ? "profitUp" : "profitDown",
                    money(k.getProfit()), pc.abs().toPlainString()));
        }
        BigDecimal acos = k.getAcosPercent();
        BigDecimal avgAcos = c.getAvg7().getAcosPercent();
        if (acos != null && avgAcos != null && acos.subtract(avgAcos).compareTo(BigDecimal.valueOf(5)) >= 0) {
            h.add(OpsReportContent.Highlight.of(LEVEL_WARN, "acosUp", acos.toPlainString(), avgAcos.toPlainString()));
        }
        c.getStoreRows().stream().filter(s -> s.getSalesChange() != null && s.getPrevSales().compareTo(BigDecimal.valueOf(100)) >= 0)
                .min(Comparator.comparing(OpsReportContent.StoreKpi::getSalesChange))
                .filter(s -> s.getSalesChange().compareTo(CHANGE_NOTABLE.negate().multiply(BigDecimal.valueOf(2))) <= 0)
                .ifPresent(s -> h.add(OpsReportContent.Highlight.of(LEVEL_WARN, "storeDown", s.getStoreName(), s.getSalesChange().abs().toPlainString())));
        long salesDrop = c.getAbnormalSkus().stream().filter(a -> a.getType() == OpsAbnormalType.SALES_DROP).count();
        long profitDrop = c.getAbnormalSkus().stream().filter(a -> a.getType() == OpsAbnormalType.PROFIT_DROP).count();
        long returnUp = c.getAbnormalSkus().stream().filter(a -> a.getType() == OpsAbnormalType.RETURN_RATE_UP).count();
        if (salesDrop + profitDrop + returnUp > 0) {
            h.add(OpsReportContent.Highlight.of(LEVEL_WARN, "abnormal", salesDrop, profitDrop, returnUp));
        }
        if (!c.getBadReviews().isEmpty()) {
            h.add(OpsReportContent.Highlight.of(LEVEL_BAD, "badReviews", c.getBadReviews().size()));
        }
        int urgent = c.getStockUrgentTotal();
        if (c.getStockRiskTotal() > 0) {
            h.add(OpsReportContent.Highlight.of(urgent > 0 ? LEVEL_BAD : LEVEL_WARN, "stockRisk", urgent, c.getStockRiskTotal() - urgent));
        }
        if (c.getAdAlertTotal() > 0) {
            h.add(OpsReportContent.Highlight.of(LEVEL_WARN, "adAlerts", c.getAdAlertTotal()));
        }
        c.getTodos().stream().filter(t -> t.kind() == OpsTodoKind.ORDER_OVERDUE).findFirst()
                .ifPresent(t -> h.add(OpsReportContent.Highlight.of(LEVEL_BAD, "orderOverdue", t.count())));
    }

    /** 金额文字：千分位、两位小数 */
    public static String money(BigDecimal v) {
        return new DecimalFormat("#,##0.00").format(Decimals.nz(v));
    }

    private static String clip(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
