package com.lemon.erp.modules.ecom.analytics.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.analytics.dto.AnalyticsRows;
import com.lemon.erp.modules.ecom.analytics.dto.ProductViews;
import com.lemon.erp.modules.ecom.analytics.mapper.EcomAnalyticsMapper;
import com.lemon.erp.modules.ecom.analytics.model.AbcClass;
import com.lemon.erp.modules.ecom.analytics.model.AbcMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 商品分析：ABC（按销售额 / 毛利）、动销率与滞销（库龄、滞销库存金额）、售罄率与库存周转、断货天数（每日快照）、新品表现、退货率排行。
 * 统计全公司数据（与利润分析一致，不按数据权限过滤），金额为本位币
 */
@Service
@RequiredArgsConstructor
public class EcomProductAnalysisService {

    static final int MAX_DAYS = 366;
    static final int DEFAULT_DAYS = 30;
    static final BigDecimal DEFAULT_A = BigDecimal.valueOf(70);
    static final BigDecimal DEFAULT_B = BigDecimal.valueOf(90);
    /** 库龄按流水推算时最多往前看的天数，更早的库存按这一天计 */
    static final int AGE_LOOKBACK_DAYS = 730;
    static final int NEW_PRODUCT_DAYS = 30;
    static final int MAX_NEW_PRODUCTS = 500;
    static final int CHUNK = 500;

    private final EcomAnalyticsMapper analyticsMapper;

    // ---------------------------------------------------------------- ABC

    /**
     * ABC 分析：按 SKU 的销售额或毛利从高到低排序，累计占比（不含本行）低于 A 阈值为 A 类、低于 B 阈值为 B 类，其余为 C 类；
     * 指标不大于 0 的为 C 类且不计入合计
     *
     * @param aPercent A 类阈值（%），默认 70
     * @param bPercent B 类阈值（%），默认 90，须大于 A
     */
    public ProductViews.AbcResult abc(LocalDate beginDate, LocalDate endDate, Long storeId, String keyword, AbcMetric metric,
                                      BigDecimal aPercent, BigDecimal bPercent) {
        AnalyticsDates range = AnalyticsDates.of(beginDate, endDate, DEFAULT_DAYS, MAX_DAYS);
        AbcMetric by = metric != null ? metric : AbcMetric.SALES;
        BigDecimal a = aPercent != null ? aPercent : DEFAULT_A;
        BigDecimal b = bPercent != null ? bPercent : DEFAULT_B;
        if (a.signum() <= 0 || b.compareTo(a) <= 0 || b.compareTo(Decimals.HUNDRED) > 0) {
            throw new BizException("ecom.error.analytics.abcThreshold");
        }
        List<ProductViews.AbcRow> rows = new ArrayList<>();
        for (AnalyticsRows.SkuSales s : analyticsMapper.selectSkuSales(range.from(), range.to(), storeId, EcomSalesStatsService.blankToNull(keyword))) {
            ProductViews.AbcRow r = new ProductViews.AbcRow();
            r.setStoreId(s.getStoreId());
            r.setStoreName(s.getStoreName());
            r.setListingId(s.getListingId());
            r.setSellerSku(s.getSellerSku());
            r.setTitle(s.getTitle());
            r.setMaterialCode(s.getMaterialCode());
            r.setMaterialName(s.getMaterialName());
            r.setOrders(s.getOrders());
            r.setQty(Decimals.qty(s.getQty()));
            r.setSales(Decimals.money(s.getSales()));
            r.setProfit(Decimals.money(s.getProfit()));
            r.setValue(by == AbcMetric.PROFIT ? r.getProfit() : r.getSales());
            rows.add(r);
        }
        classify(rows, a, b);
        BigDecimal total = rows.stream().map(ProductViews.AbcRow::getValue).filter(Decimals::positive).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<AbcClass, int[]> counts = new EnumMap<>(AbcClass.class);
        Map<AbcClass, BigDecimal> values = new EnumMap<>(AbcClass.class);
        for (ProductViews.AbcRow r : rows) {
            counts.computeIfAbsent(r.getAbcClass(), k -> new int[1])[0]++;
            values.merge(r.getAbcClass(), r.getValue(), BigDecimal::add);
        }
        List<ProductViews.AbcSummary> summary = new ArrayList<>();
        for (AbcClass c : AbcClass.values()) {
            int n = counts.containsKey(c) ? counts.get(c)[0] : 0;
            BigDecimal v = values.getOrDefault(c, BigDecimal.ZERO);
            summary.add(new ProductViews.AbcSummary(c, n, v, Decimals.percent(v.max(BigDecimal.ZERO), total, 2),
                    Decimals.percent(BigDecimal.valueOf(n), BigDecimal.valueOf(rows.size()), 2)));
        }
        return new ProductViews.AbcResult(by, a, b, total, summary, rows);
    }

    /** 按 value 从高到低排序并分类（填写占比、累计占比与类别） */
    static void classify(List<ProductViews.AbcRow> rows, BigDecimal aPercent, BigDecimal bPercent) {
        rows.sort(Comparator.comparing(ProductViews.AbcRow::getValue).reversed()
                .thenComparing(ProductViews.AbcRow::getStoreId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProductViews.AbcRow::getSellerSku, Comparator.nullsLast(Comparator.naturalOrder())));
        BigDecimal total = rows.stream().map(ProductViews.AbcRow::getValue).filter(Decimals::positive).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cum = BigDecimal.ZERO;
        for (ProductViews.AbcRow r : rows) {
            if (!Decimals.positive(r.getValue()) || total.signum() == 0) {
                r.setSharePercent(total.signum() == 0 ? null : BigDecimal.ZERO.setScale(2));
                r.setCumSharePercent(total.signum() == 0 ? null : BigDecimal.valueOf(100).setScale(2));
                r.setAbcClass(AbcClass.C);
                continue;
            }
            BigDecimal before = cum.multiply(Decimals.HUNDRED).divide(total, 6, RoundingMode.HALF_UP);
            cum = cum.add(r.getValue());
            r.setSharePercent(Decimals.percent(r.getValue(), total, 2));
            r.setCumSharePercent(Decimals.percent(cum, total, 2));
            r.setAbcClass(before.compareTo(aPercent) < 0 ? AbcClass.A : before.compareTo(bPercent) < 0 ? AbcClass.B : AbcClass.C);
        }
    }

    // ---------------------------------------------------------------- 动销与滞销

    /**
     * 滞销：在线商品映射的物料中有库存、最近 days 天没有销量的（全部仓库的库存，金额 = 库存 × 实时移动加权成本），附库龄；
     * 动销率 = 最近 days 天有销量的在售 SKU ÷ 在售 SKU
     */
    public ProductViews.SlowResult slowMoving(int days, Long storeId, String keyword) {
        if (days < 1 || days > MAX_DAYS) {
            throw new BizException("ecom.error.analytics.daysRange", MAX_DAYS);
        }
        LocalDate today = LocalDate.now();
        LocalDate begin = today.minusDays(days - 1L);
        Map<String, Object> counts = analyticsMapper.selectMovingCounts(begin.atStartOfDay(), today.plusDays(1).atStartOfDay(), storeId);
        long active = num(counts == null ? null : counts.get("active"));
        long moving = num(counts == null ? null : counts.get("moving"));
        List<AnalyticsRows.MaterialStock> materials = analyticsMapper.selectSlowMaterials(begin.atStartOfDay(), storeId, EcomSalesStatsService.blankToNull(keyword));
        Map<Long, StockAgeCalc.Age> ages = ages(materials.stream().map(AnalyticsRows.MaterialStock::getMaterialId).toList(), today);
        List<ProductViews.SlowRow> rows = new ArrayList<>();
        BigDecimal amount = BigDecimal.ZERO;
        for (AnalyticsRows.MaterialStock m : materials) {
            ProductViews.SlowRow r = new ProductViews.SlowRow();
            r.setMaterialId(m.getMaterialId());
            r.setMaterialCode(m.getMaterialCode());
            r.setMaterialName(m.getMaterialName());
            r.setMaterialSpec(m.getMaterialSpec());
            r.setUnit(m.getUnit());
            r.setSkuCount(m.getSkuCount());
            r.setStockQty(Decimals.qty(m.getStockQty()));
            r.setUnitCost(m.getUnitCost());
            r.setStockAmount(Decimals.money(Decimals.nz(m.getStockQty()).multiply(Decimals.nz(m.getUnitCost()))));
            if (m.getLastSale() != null) {
                r.setLastSaleDate(m.getLastSale().toLocalDate());
                r.setIdleDays(ChronoUnit.DAYS.between(r.getLastSaleDate(), today));
            }
            StockAgeCalc.Age age = ages.get(m.getMaterialId());
            if (age != null) {
                r.setOldestAgeDays(age.oldestDays());
                r.setAvgAgeDays(age.avgDays());
                r.setAge0to30(Decimals.qty(age.buckets()[0]));
                r.setAge31to90(Decimals.qty(age.buckets()[1]));
                r.setAge91to180(Decimals.qty(age.buckets()[2]));
                r.setAge181to365(Decimals.qty(age.buckets()[3]));
                r.setAgeOver365(Decimals.qty(age.buckets()[4]));
            }
            amount = amount.add(r.getStockAmount());
            rows.add(r);
        }
        rows.sort(Comparator.comparing(ProductViews.SlowRow::getStockAmount).reversed().thenComparing(ProductViews.SlowRow::getMaterialCode));
        return new ProductViews.SlowResult(days, active, moving, Decimals.percent(BigDecimal.valueOf(moving), BigDecimal.valueOf(active), 2),
                rows.size(), amount, rows);
    }

    /** 物料的库龄（批次首次入库时间；没有批次的按入库流水先进先出推算） */
    Map<Long, StockAgeCalc.Age> ages(List<Long> materialIds, LocalDate today) {
        Map<Long, StockAgeCalc.Age> out = new HashMap<>();
        LocalDate since = today.minusDays(AGE_LOOKBACK_DAYS);
        for (int i = 0; i < materialIds.size(); i += CHUNK) {
            List<Long> part = materialIds.subList(i, Math.min(materialIds.size(), i + CHUNK));
            Map<Long, List<AnalyticsRows.AgeLot>> lots = analyticsMapper.selectStockBatches(part).stream()
                    .collect(Collectors.groupingBy(AnalyticsRows.AgeLot::getMaterialId));
            Map<Long, List<AnalyticsRows.AgeLot>> flows = analyticsMapper.selectInboundFlows(part, since.atStartOfDay()).stream()
                    .collect(Collectors.groupingBy(AnalyticsRows.AgeLot::getMaterialId, LinkedHashMap::new, Collectors.toList()));
            for (Long id : part) {
                out.put(id, StockAgeCalc.compute(lots.getOrDefault(id, List.of()), flows.getOrDefault(id, List.of()), today, since));
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- 售罄与周转

    /**
     * 售罄率与周转（物料，在线商品映射的物料，期间有销量或有库存的）：期初 = 当前库存 − begin 之后的库存变动，期末 = 当前库存 − end 之后的变动，
     * 期间入库 = 入库单的正数流水（不含销售退货、调拨、盘点）；售罄率 = 销量 ÷ (期初 + 入库) × 100；
     * 周转天数 = (期初 + 期末) ÷ 2 ÷ 日均销量；可售天数 = 当前库存 ÷ 日均销量
     */
    public List<ProductViews.SellThroughRow> sellThrough(LocalDate beginDate, LocalDate endDate, Long storeId, String keyword) {
        AnalyticsDates range = AnalyticsDates.of(beginDate, endDate, DEFAULT_DAYS, MAX_DAYS);
        List<AnalyticsRows.MaterialPeriod> materials = analyticsMapper.selectMaterialPeriod(range.from(), range.to(), storeId,
                EcomSalesStatsService.blankToNull(keyword));
        Map<Long, AnalyticsRows.FlowSum> flows = new HashMap<>();
        List<Long> ids = materials.stream().map(AnalyticsRows.MaterialPeriod::getMaterialId).toList();
        for (int i = 0; i < ids.size(); i += CHUNK) {
            analyticsMapper.selectFlowSums(ids.subList(i, Math.min(ids.size(), i + CHUNK)), range.from(), range.to())
                    .forEach(f -> flows.put(f.getMaterialId(), f));
        }
        BigDecimal days = BigDecimal.valueOf(range.days());
        List<ProductViews.SellThroughRow> rows = new ArrayList<>();
        for (AnalyticsRows.MaterialPeriod m : materials) {
            AnalyticsRows.FlowSum f = flows.get(m.getMaterialId());
            BigDecimal stock = Decimals.nz(m.getStockQty());
            BigDecimal opening = stock.subtract(f == null ? BigDecimal.ZERO : Decimals.nz(f.getSinceBegin()));
            BigDecimal closing = stock.subtract(f == null ? BigDecimal.ZERO : Decimals.nz(f.getSinceEnd()));
            BigDecimal inbound = f == null ? BigDecimal.ZERO : Decimals.nz(f.getInbound());
            BigDecimal sales = Decimals.nz(m.getSalesQty());
            if (sales.signum() == 0 && opening.signum() <= 0 && inbound.signum() == 0 && stock.signum() <= 0) {
                continue;
            }
            ProductViews.SellThroughRow r = new ProductViews.SellThroughRow();
            r.setMaterialId(m.getMaterialId());
            r.setMaterialCode(m.getMaterialCode());
            r.setMaterialName(m.getMaterialName());
            r.setMaterialSpec(m.getMaterialSpec());
            r.setUnit(m.getUnit());
            r.setSkuCount(m.getSkuCount());
            fillSellThrough(r, sales, opening, inbound, closing, stock, days);
            r.setStockAmount(Decimals.money(stock.multiply(Decimals.nz(m.getUnitCost()))));
            rows.add(r);
        }
        rows.sort(Comparator.comparing(ProductViews.SellThroughRow::getSalesQty).reversed().thenComparing(ProductViews.SellThroughRow::getMaterialCode));
        return rows;
    }

    /** 售罄率、周转天数、可售天数（纯计算） */
    static void fillSellThrough(ProductViews.SellThroughRow r, BigDecimal sales, BigDecimal opening, BigDecimal inbound, BigDecimal closing,
                                BigDecimal stock, BigDecimal days) {
        r.setSalesQty(Decimals.qty(sales));
        r.setOpeningQty(Decimals.qty(opening));
        r.setInboundQty(Decimals.qty(inbound));
        r.setClosingQty(Decimals.qty(closing));
        r.setStockQty(Decimals.qty(stock));
        BigDecimal daily = sales.divide(days, 4, RoundingMode.HALF_UP);
        r.setDailySales(daily);
        BigDecimal available = opening.max(BigDecimal.ZERO).add(inbound);
        r.setSellThroughPercent(Decimals.percent(sales, available, 2));
        if (daily.signum() > 0) {
            BigDecimal avgStock = opening.max(BigDecimal.ZERO).add(closing.max(BigDecimal.ZERO)).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
            r.setTurnoverDays(avgStock.divide(daily, 1, RoundingMode.HALF_UP));
            r.setCoverDays(stock.max(BigDecimal.ZERO).divide(daily, 1, RoundingMode.HALF_UP));
        }
    }

    // ---------------------------------------------------------------- 断货

    /** 断货天数（按每日快照，只列有快照的 SKU），按断货天数从多到少 */
    public List<ProductViews.StockoutRow> stockout(LocalDate beginDate, LocalDate endDate, Long storeId, String keyword) {
        AnalyticsDates range = AnalyticsDates.of(beginDate, endDate, DEFAULT_DAYS, MAX_DAYS);
        List<ProductViews.StockoutRow> rows = new ArrayList<>();
        for (AnalyticsRows.StockoutStat s : analyticsMapper.selectStockout(range.begin(), range.end(), storeId, null,
                EcomSalesStatsService.blankToNull(keyword))) {
            ProductViews.StockoutRow r = new ProductViews.StockoutRow();
            r.setListingId(s.getListingId());
            r.setStoreId(s.getStoreId());
            r.setStoreName(s.getStoreName());
            r.setSellerSku(s.getSellerSku());
            r.setTitle(s.getTitle());
            r.setFulfillment(s.getFulfillment());
            r.setMaterialCode(s.getMaterialCode());
            r.setMaterialName(s.getMaterialName());
            r.setSnapshotDays(s.getSnapshotDays());
            r.setStockoutDays(s.getStockoutDays());
            r.setStockoutPercent(Decimals.percent(BigDecimal.valueOf(nz(s.getStockoutDays())), BigDecimal.valueOf(nz(s.getSnapshotDays())), 2));
            r.setLastStockoutDate(s.getLastStockoutDate());
            rows.add(r);
        }
        rows.sort(Comparator.comparing((ProductViews.StockoutRow r) -> nz(r.getStockoutDays())).reversed()
                .thenComparing(ProductViews.StockoutRow::getSellerSku));
        return rows;
    }

    // ---------------------------------------------------------------- 新品

    /** 新品表现：首单日（全部历史中最早的订单）在范围内的 SKU，首单日起 30 天的销量、订单数、销售额与断货天数，按首单日从新到旧 */
    public List<ProductViews.NewProductRow> newProducts(LocalDate beginDate, LocalDate endDate, Long storeId, String keyword) {
        AnalyticsDates range = AnalyticsDates.of(beginDate, endDate, 90, MAX_DAYS);
        List<AnalyticsRows.FirstSale> firsts = analyticsMapper.selectFirstSales(range.from(), range.to(), storeId, EcomSalesStatsService.blankToNull(keyword))
                .stream().sorted(Comparator.comparing(AnalyticsRows.FirstSale::getFirstTime).reversed()).limit(MAX_NEW_PRODUCTS).toList();
        if (firsts.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        LocalDate minFirst = firsts.stream().map(f -> f.getFirstTime().toLocalDate()).min(Comparator.naturalOrder()).orElse(range.begin());
        LocalDate maxEnd = firsts.stream().map(f -> f.getFirstTime().toLocalDate()).max(Comparator.naturalOrder()).orElse(range.end()).plusDays(NEW_PRODUCT_DAYS);
        Map<String, List<AnalyticsRows.SalesCell>> daily = analyticsMapper.selectSkusDaily(
                        firsts.stream().map(AnalyticsRows.FirstSale::getStoreId).distinct().toList(),
                        firsts.stream().map(AnalyticsRows.FirstSale::getSellerSku).distinct().toList(),
                        minFirst.atStartOfDay(), maxEnd.atStartOfDay())
                .stream().collect(Collectors.groupingBy(c -> c.getStoreId() + "|" + c.getSellerSku()));
        List<Long> listingIds = firsts.stream().map(AnalyticsRows.FirstSale::getListingId).filter(Objects::nonNull).toList();
        Map<Long, List<LocalDate>> stockoutDates = new HashMap<>();
        // 断货天数：首单 30 天内快照可售 ≤ 0 的天数（没有快照的日期不计）
        for (int i = 0; i < listingIds.size(); i += CHUNK) {
            for (AnalyticsRows.ListingDay d : analyticsMapper.selectStockoutDays(listingIds.subList(i, Math.min(listingIds.size(), i + CHUNK)),
                    minFirst, maxEnd.minusDays(1))) {
                stockoutDates.computeIfAbsent(d.getListingId(), k -> new ArrayList<>()).add(d.getSaleDay());
            }
        }
        List<ProductViews.NewProductRow> rows = new ArrayList<>();
        for (AnalyticsRows.FirstSale f : firsts) {
            LocalDate first = f.getFirstTime().toLocalDate();
            LocalDate end = first.plusDays(NEW_PRODUCT_DAYS);
            ProductViews.NewProductRow r = new ProductViews.NewProductRow();
            r.setStoreId(f.getStoreId());
            r.setStoreName(f.getStoreName());
            r.setListingId(f.getListingId());
            r.setSellerSku(f.getSellerSku());
            r.setTitle(f.getTitle());
            r.setMaterialCode(f.getMaterialCode());
            r.setMaterialName(f.getMaterialName());
            r.setFirstOrderDate(first);
            r.setObservedDays((int) Math.min(NEW_PRODUCT_DAYS, ChronoUnit.DAYS.between(first, today) + 1));
            BigDecimal qty = BigDecimal.ZERO;
            BigDecimal sales = BigDecimal.ZERO;
            long orders = 0;
            for (AnalyticsRows.SalesCell c : daily.getOrDefault(f.getStoreId() + "|" + f.getSellerSku(), List.of())) {
                if (!c.getSaleDay().isBefore(first) && c.getSaleDay().isBefore(end)) {
                    qty = qty.add(Decimals.nz(c.getQty()));
                    sales = sales.add(Decimals.nz(c.getAmount()));
                    orders += c.getOrders() == null ? 0 : c.getOrders();
                }
            }
            r.setQty30(Decimals.qty(qty));
            r.setSales30(Decimals.money(sales));
            r.setOrders30(orders);
            r.setDailyQty(qty.divide(BigDecimal.valueOf(Math.max(1, r.getObservedDays())), 4, RoundingMode.HALF_UP));
            if (f.getListingId() != null && stockoutDates.containsKey(f.getListingId())) {
                r.setStockoutDays30((int) stockoutDates.get(f.getListingId()).stream()
                        .filter(d -> !d.isBefore(first) && d.isBefore(end)).count());
            }
            rows.add(r);
        }
        return rows;
    }

    // ---------------------------------------------------------------- 退货率

    /**
     * 退货率排行：期间内下单的订单，按 SKU 的退回件数 ÷ 销量 × 100（退货退款单不含已关闭的），各原因的退回件数与占比；
     * 有退货的排在前面，按退货率从高到低
     */
    public ProductViews.ReturnResult returnRate(LocalDate beginDate, LocalDate endDate, Long storeId, String keyword) {
        AnalyticsDates range = AnalyticsDates.of(beginDate, endDate, 90, MAX_DAYS);
        String kw = EcomSalesStatsService.blankToNull(keyword);
        Map<String, ProductViews.ReturnRow> rows = new LinkedHashMap<>();
        for (AnalyticsRows.SkuSales s : analyticsMapper.selectSkuSales(range.from(), range.to(), storeId, kw)) {
            ProductViews.ReturnRow r = new ProductViews.ReturnRow();
            r.setStoreId(s.getStoreId());
            r.setStoreName(s.getStoreName());
            r.setListingId(s.getListingId());
            r.setSellerSku(s.getSellerSku());
            r.setTitle(s.getTitle());
            r.setMaterialCode(s.getMaterialCode());
            r.setMaterialName(s.getMaterialName());
            r.setSoldQty(Decimals.qty(s.getQty()));
            r.setReturnQty(BigDecimal.ZERO);
            r.setRefund(BigDecimal.ZERO);
            rows.put(s.getStoreId() + "|" + s.getSellerSku(), r);
        }
        Map<String, BigDecimal> reasons = new LinkedHashMap<>();
        for (AnalyticsRows.SkuReturn ret : analyticsMapper.selectSkuReturns(range.from(), range.to(), storeId, kw)) {
            ProductViews.ReturnRow r = rows.get(ret.getStoreId() + "|" + ret.getSellerSku());
            if (r == null) {
                continue;
            }
            BigDecimal qty = Decimals.nz(ret.getQty());
            r.setReturnQty(r.getReturnQty().add(qty));
            r.setRefund(r.getRefund().add(Decimals.nz(ret.getRefund())));
            r.getReasons().merge(ret.getReason(), qty, BigDecimal::add);
            reasons.merge(ret.getReason(), qty, BigDecimal::add);
        }
        BigDecimal sold = BigDecimal.ZERO;
        BigDecimal returned = BigDecimal.ZERO;
        for (ProductViews.ReturnRow r : rows.values()) {
            r.setReturnQty(Decimals.qty(r.getReturnQty()));
            r.setRefund(Decimals.money(r.getRefund()));
            r.setReturnRatePercent(Decimals.percent(r.getReturnQty(), r.getSoldQty(), 2));
            r.setTopReason(r.getReasons().entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null));
            sold = sold.add(r.getSoldQty());
            returned = returned.add(r.getReturnQty());
        }
        BigDecimal totalReturned = returned;
        List<ProductViews.ReasonShare> shares = reasons.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new ProductViews.ReasonShare(e.getKey(), Decimals.qty(e.getValue()), Decimals.percent(e.getValue(), totalReturned, 2)))
                .toList();
        List<ProductViews.ReturnRow> list = new ArrayList<>(rows.values());
        list.sort(Comparator.comparing((ProductViews.ReturnRow r) -> r.getReturnQty().signum() > 0 ? 0 : 1)
                .thenComparing(r -> r.getReturnRatePercent() == null ? BigDecimal.ZERO : r.getReturnRatePercent(), Comparator.reverseOrder())
                .thenComparing(ProductViews.ReturnRow::getReturnQty, Comparator.reverseOrder())
                .thenComparing(ProductViews.ReturnRow::getSellerSku));
        return new ProductViews.ReturnResult(Decimals.qty(sold), Decimals.qty(returned), Decimals.percent(returned, sold, 2), shares, list);
    }

    // ---------------------------------------------------------------- 内部

    private static long num(Object v) {
        return v == null ? 0 : ((Number) v).longValue();
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
