package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.dto.EcomReports;
import com.lemon.erp.modules.ecom.mapper.EcomAdCampaignMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdGroupMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdTargetMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdsAnalysisMapper;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * 广告分析：层级报表（活动 → 广告组 → 投放，含环比 / 同比）、概览、趋势、搜索词（高花费零转化）、SKU 维度 TACOS。
 * 层级报表的金额为店铺币种；概览、趋势、TACOS 折本位币（按统计截止日所在月的汇率，与利润分析的广告花费口径一致）。
 * TACOS 的销售额取利润分析（{@link EcomReportService}，只读调用）
 */
@Service
@RequiredArgsConstructor
public class EcomAdAnalysisService {

    /** 搜索词列表最多返回的行数 */
    static final int SEARCH_TERM_LIMIT = 500;
    /** 默认的「零转化」判定：点击不少于 10 次且没有订单 */
    static final int DEFAULT_WASTE_CLICKS = 10;

    private final EcomAdsAnalysisMapper analysisMapper;
    private final EcomAdCampaignMapper campaignMapper;
    private final EcomAdGroupMapper groupMapper;
    private final EcomAdTargetMapper targetMapper;
    private final EcomReportService reportService;
    private final ExchangeRateProvider rateProvider;

    /** 统计区间：为空时默认近 30 天（截至昨天） */
    public record Range(LocalDate begin, LocalDate end) {
        public static Range of(LocalDate begin, LocalDate end) {
            LocalDate e = end == null ? LocalDate.now().minusDays(1) : end;
            LocalDate b = begin == null ? e.minusDays(29) : begin;
            if (b.isAfter(e)) {
                throw new BizException("ecom.error.ads.rangeInvalid");
            }
            if (ChronoUnit.DAYS.between(b, e) > 366) {
                throw new BizException("ecom.error.ads.rangeTooLong");
            }
            return new Range(b, e);
        }

        public long days() {
            return ChronoUnit.DAYS.between(begin, end) + 1;
        }

        /** 环比：紧挨着的上一个同样长度的期间 */
        public Range prev() {
            return new Range(begin.minusDays(days()), begin.minusDays(1));
        }

        /** 同比：去年同期 */
        public Range yoy() {
            return new Range(begin.minusYears(1), end.minusYears(1));
        }
    }

    // ---------------------------------------------------------------- 层级报表

    /**
     * @param level      CAMPAIGN：店铺下的活动；AD_GROUP：活动下的广告组（campaignId 必填）；TARGET：广告组下的投放（adGroupId 必填）
     */
    public AdsViews.Hierarchy hierarchy(EcomAdLevel level, Long storeId, Long campaignId, Long adGroupId, LocalDate begin, LocalDate end, String keyword) {
        Range r = Range.of(begin, end);
        List<Long> stores = storeId == null ? null : List.of(storeId);
        List<AdsViews.RuleEntity> entities = switch (level) {
            case CAMPAIGN -> campaignMapper.selectEntities(stores, trim(keyword), null, campaignId, true);
            case AD_GROUP -> {
                if (campaignId == null) {
                    throw new BizException("ecom.error.ads.campaignRequired");
                }
                yield groupMapper.selectEntities(stores, null, null, campaignId, true);
            }
            case TARGET -> {
                if (adGroupId == null && campaignId == null) {
                    throw new BizException("ecom.error.ads.adGroupRequired");
                }
                yield targetMapper.selectEntities(stores, null, null, campaignId, adGroupId, true);
            }
        };
        Map<Long, AdsViews.Metrics> cur = metrics(level, stores, campaignId, adGroupId, r);
        Map<Long, AdsViews.Metrics> prev = metrics(level, stores, campaignId, adGroupId, r.prev());
        Map<Long, AdsViews.Metrics> yoy = metrics(level, stores, campaignId, adGroupId, r.yoy());
        AdsViews.Hierarchy h = new AdsViews.Hierarchy();
        h.setPrevBegin(r.prev().begin());
        h.setPrevEnd(r.prev().end());
        h.setYoyBegin(r.yoy().begin());
        h.setYoyEnd(r.yoy().end());
        String kw = level == EcomAdLevel.CAMPAIGN || keyword == null ? null : keyword.trim().toLowerCase();
        Map<String, Optional<BigDecimal>> rates = new HashMap<>();
        for (AdsViews.RuleEntity e : entities) {
            if (kw != null && !kw.isEmpty() && (e.getName() == null || !e.getName().toLowerCase().contains(kw))) {
                continue;
            }
            AdsViews.HierarchyRow row = new AdsViews.HierarchyRow();
            row.setLevel(level);
            row.setId(e.getId());
            row.setStoreId(e.getStoreId());
            row.setStoreName(e.getStoreName());
            row.setCurrency(e.getCurrency());
            row.setCampaignId(e.getCampaignId());
            row.setCampaignName(e.getCampaignName());
            row.setAdGroupId(e.getAdGroupId());
            row.setName(e.getName());
            row.setAdType(e.getAdType());
            row.setStatus(e.getStatus());
            row.setTargetType(e.getTargetType());
            row.setPlatformId(e.getPlatformId());
            row.setBidOrBudget(level == EcomAdLevel.CAMPAIGN ? e.getBudget() : e.getBid());
            row.setBaseBid(e.getBaseBid());
            row.add(cur.get(e.getId())).fill();
            BigDecimal baseRate = rates.computeIfAbsent(String.valueOf(e.getCurrency()), c -> Optional.ofNullable(baseRateOrNull(e.getCurrency(), r.end()))).orElse(null);
            if (baseRate != null) {
                row.setBaseSpend(Decimals.money(Decimals.nz(row.getSpend()).multiply(baseRate)));
                row.setBaseSales(Decimals.money(Decimals.nz(row.getSales()).multiply(baseRate)));
            }
            row.setPrev(new AdsViews.Metrics().add(prev.get(e.getId())).fill());
            row.setYoy(new AdsViews.Metrics().add(yoy.get(e.getId())).fill());
            h.getRows().add(row);
        }
        // 按本位币花费排序（不同币种可比），没有汇率的按原币
        h.getRows().sort(Comparator.comparing((AdsViews.HierarchyRow x) -> x.getBaseSpend() != null ? x.getBaseSpend() : Decimals.nz(x.getSpend())).reversed());
        return h;
    }

    /** 某一层级各对象在区间内的指标（id → 指标，比率未计算） */
    public Map<Long, AdsViews.Metrics> metrics(EcomAdLevel level, Collection<Long> storeIds, Long campaignId, Long adGroupId, Range r) {
        List<Map<String, Object>> list = switch (level) {
            case CAMPAIGN -> analysisMapper.selectCampaignMetrics(storeIds, campaignId, r.begin(), r.end());
            case AD_GROUP -> analysisMapper.selectAdGroupMetrics(storeIds, campaignId, r.begin(), r.end());
            case TARGET -> analysisMapper.selectTargetMetrics(storeIds, campaignId, adGroupId, r.begin(), r.end());
        };
        Map<Long, AdsViews.Metrics> map = new HashMap<>();
        for (Map<String, Object> m : list) {
            if (m.get("id") != null) {
                map.put(((Number) m.get("id")).longValue(), toMetrics(m));
            }
        }
        return map;
    }

    // ---------------------------------------------------------------- 概览与趋势

    public AdsViews.Overview overview(Long storeId, LocalDate begin, LocalDate end) {
        Range r = Range.of(begin, end);
        AdsViews.Overview o = new AdsViews.Overview();
        o.setCurrent(storeTotals(storeId, r, r.end()));
        o.setPrev(storeTotals(storeId, r.prev(), r.end()));
        o.setYoy(storeTotals(storeId, r.yoy(), r.end()));
        BigDecimal sales = BigDecimal.ZERO;
        for (EcomReports.StoreRow s : reportService.byStore(r.begin(), r.end(), storeId)) {
            sales = sales.add(Decimals.nz(s.getSales()));
        }
        o.setTotalSales(Decimals.money(sales));
        o.setTacos(AdsViews.Metrics.ratio(o.getCurrent().getSpend(), sales, true));
        o.setCampaigns(analysisMapper.countCampaigns(storeId, false));
        o.setEnabledCampaigns(analysisMapper.countCampaigns(storeId, true));
        o.setPendingSuggestions(analysisMapper.countPendingLogs(storeId));
        BigDecimal wasteSpend = BigDecimal.ZERO;
        long waste = 0;
        for (AdsViews.SearchTermRow t : searchTerms(storeId, null, r.begin(), r.end(), null, true, null, null)) {
            if (!t.isNegated()) {
                waste++;
                wasteSpend = wasteSpend.add(rate(t.getCurrency(), r.end()).multiply(Decimals.nz(t.getSpend())));
            }
        }
        o.setWasteTerms(waste);
        o.setWasteSpend(Decimals.money(wasteSpend));
        return o;
    }

    private AdsViews.Metrics storeTotals(Long storeId, Range r, LocalDate rateDate) {
        AdsViews.Metrics total = new AdsViews.Metrics();
        for (Map<String, Object> m : analysisMapper.selectStoreTotals(storeId, r.begin(), r.end())) {
            total.add(toMetrics(m).scale(rate((String) m.get("currency"), rateDate)));
        }
        return total.fill();
    }

    /** 按天的花费、销售额、ACOS（本位币） */
    public List<AdsViews.TrendRow> trend(Long storeId, LocalDate begin, LocalDate end) {
        Range r = Range.of(begin, end);
        Map<LocalDate, AdsViews.TrendRow> byDate = new TreeMap<>();
        for (LocalDate d = r.begin(); !d.isAfter(r.end()); d = d.plusDays(1)) {
            AdsViews.TrendRow t = new AdsViews.TrendRow();
            t.setDate(d);
            byDate.put(d, t);
        }
        for (Map<String, Object> m : analysisMapper.selectDaily(storeId, r.begin(), r.end())) {
            LocalDate d = toDate(m.get("date"));
            AdsViews.TrendRow t = byDate.get(d);
            if (t != null) {
                t.add(toMetrics(m).scale(rate((String) m.get("currency"), r.end())));
            }
        }
        List<AdsViews.TrendRow> rows = new ArrayList<>(byDate.values());
        rows.forEach(AdsViews.Metrics::fill);
        return rows;
    }

    // ---------------------------------------------------------------- 搜索词

    /**
     * 搜索词汇总（按花费从高到低，最多 {@value #SEARCH_TERM_LIMIT} 行）。零转化：没有订单，且点击不少于 minClicks（默认 10）
     * 或花费不少于 minSpend（店铺币种，为空不按花费判断）
     *
     * @param wasteOnly 只返回零转化的词
     */
    public List<AdsViews.SearchTermRow> searchTerms(Long storeId, Long campaignId, LocalDate begin, LocalDate end, String keyword, boolean wasteOnly,
                                                    Integer minClicks, BigDecimal minSpend) {
        Range r = Range.of(begin, end);
        int clicks = minClicks == null ? DEFAULT_WASTE_CLICKS : Math.max(1, minClicks);
        Set<String> negatives = new HashSet<>();
        for (Map<String, Object> m : analysisMapper.selectNegatives(storeId)) {
            negatives.add(m.get("adGroupId") + "|" + m.get("text"));
        }
        List<AdsViews.SearchTermRow> out = new ArrayList<>();
        for (AdsViews.SearchTermRow t : analysisMapper.selectSearchTerms(storeId, campaignId, r.begin(), r.end(), trim(keyword), SEARCH_TERM_LIMIT)) {
            t.fill();
            boolean waste = t.getOrders() == 0
                    && (t.getClicks() >= clicks || (minSpend != null && minSpend.signum() > 0 && t.getSpend().compareTo(minSpend) >= 0));
            t.setWaste(waste);
            t.setNegated(t.getSearchTerm() != null && negatives.contains(t.getAdGroupId() + "|" + t.getSearchTerm().toLowerCase()));
            if (!wasteOnly || waste) {
                out.add(t);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- TACOS

    /**
     * SKU 维度的 TACOS = 广告花费 ÷ 订单销售额 × 100（都是本位币，取利润分析的 SKU 结果）；
     * 只有广告没有销售的 SKU 销售额为 0、TACOS 为空。ACOS 按广告报告的广告销售额计算
     */
    public List<AdsViews.TacosRow> tacos(Long storeId, LocalDate begin, LocalDate end, String keyword) {
        Range r = Range.of(begin, end);
        Map<String, AdsViews.TacosRow> rows = new LinkedHashMap<>();
        for (EcomReports.SkuRow s : reportService.bySku(r.begin(), r.end(), storeId, trim(keyword))) {
            AdsViews.TacosRow t = new AdsViews.TacosRow();
            t.setStoreId(s.getStoreId());
            t.setStoreName(s.getStoreName());
            t.setSellerSku(s.getSellerSku());
            t.setTitle(s.getTitle());
            t.setSales(Decimals.money(Decimals.nz(s.getSales())));
            t.setProfit(s.getProfit());
            t.setAdSpend(Decimals.money(Decimals.nz(s.getAdSpend())));
            t.setAdSales(BigDecimal.ZERO);
            rows.put(s.getStoreId() + "|" + s.getSellerSku(), t);
        }
        for (Map<String, Object> m : analysisMapper.selectSkuTotals(storeId, r.begin(), r.end(), trim(keyword))) {
            String key = m.get("storeId") + "|" + m.get("sellerSku");
            BigDecimal rate = rate((String) m.get("currency"), r.end());
            AdsViews.Metrics ad = toMetrics(m);
            AdsViews.TacosRow t = rows.get(key);
            if (t == null) {
                t = new AdsViews.TacosRow();
                t.setStoreId(((Number) m.get("storeId")).longValue());
                t.setStoreName((String) m.get("storeName"));
                t.setSellerSku((String) m.get("sellerSku"));
                t.setSales(BigDecimal.ZERO);
                t.setAdSpend(Decimals.money(ad.getSpend().multiply(rate)));
                rows.put(key, t);
            }
            t.setAdSales(Decimals.money(ad.getSales().multiply(rate)));
            t.setAdClicks(ad.getClicks());
            t.setAdOrders(ad.getOrders());
            t.setAcos(AdsViews.Metrics.ratio(ad.getSpend(), ad.getSales(), true));
        }
        List<AdsViews.TacosRow> out = new ArrayList<>(rows.values());
        for (AdsViews.TacosRow t : out) {
            t.setTacos(AdsViews.Metrics.ratio(t.getAdSpend(), t.getSales(), true));
        }
        out.removeIf(t -> t.getAdSpend().signum() == 0 && t.getSales().signum() == 0);
        out.sort(Comparator.comparing(AdsViews.TacosRow::getAdSpend).reversed());
        return out;
    }

    // ---------------------------------------------------------------- 工具

    /** 折本位币的汇率，没有维护时为空（层级报表只用于排序和参考，不因缺汇率报错） */
    private BigDecimal baseRateOrNull(String currency, LocalDate date) {
        return currency == null || currency.isBlank() ? BigDecimal.ONE : rateProvider.rate(currency, date);
    }

    /** 店铺币种折本位币的汇率（之前月份也没有维护时报错） */
    BigDecimal rate(String currency, LocalDate date) {
        if (currency == null || currency.isBlank()) {
            return BigDecimal.ONE;
        }
        BigDecimal rate = rateProvider.rate(currency, date);
        if (rate == null) {
            throw new BizException("ecom.error.exchangeRateMissing", currency, date.getYear() + "-" + String.format("%02d", date.getMonthValue()));
        }
        return rate;
    }

    static AdsViews.Metrics toMetrics(Map<String, Object> m) {
        AdsViews.Metrics x = new AdsViews.Metrics();
        x.setImpressions(num(m.get("impressions")).longValue());
        x.setClicks(num(m.get("clicks")).longValue());
        x.setSpend(num(m.get("spend")));
        x.setSales(num(m.get("sales")));
        x.setOrders(num(m.get("orders")).longValue());
        return x;
    }

    private static BigDecimal num(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        return v instanceof BigDecimal b ? b : new BigDecimal(v.toString());
    }

    private static LocalDate toDate(Object v) {
        if (v instanceof LocalDate d) {
            return d;
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (v instanceof java.time.LocalDateTime t) {
            return t.toLocalDate();
        }
        return v == null ? null : LocalDate.parse(v.toString().substring(0, 10));
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
