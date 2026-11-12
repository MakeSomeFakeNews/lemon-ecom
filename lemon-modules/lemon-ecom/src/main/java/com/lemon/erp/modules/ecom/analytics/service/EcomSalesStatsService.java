package com.lemon.erp.modules.ecom.analytics.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.analytics.dto.AnalyticsRows;
import com.lemon.erp.modules.ecom.analytics.dto.SalesMatrix;
import com.lemon.erp.modules.ecom.analytics.dto.SalesQuery;
import com.lemon.erp.modules.ecom.analytics.dto.SkuTrend;
import com.lemon.erp.modules.ecom.analytics.entity.EcomStockSnapshot;
import com.lemon.erp.modules.ecom.analytics.mapper.EcomAnalyticsMapper;
import com.lemon.erp.modules.ecom.analytics.mapper.EcomStockSnapshotMapper;
import com.lemon.erp.modules.ecom.analytics.model.SalesDimension;
import com.lemon.erp.modules.ecom.analytics.model.SalesGrain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 销量统计：按 SKU / SPU / 物料 / 店铺 / 平台 / 国家 × 日 / 周 / 月的件数与销售额矩阵（数据库按维度和天 GROUP BY，周 / 月在内存中归并），
 * 环比、同比；单个 SKU 的销量走势叠加每日库存快照与断货。统计全公司的平台订单（与利润分析一致，不按数据权限过滤），不含已取消的订单
 */
@Service
@RequiredArgsConstructor
public class EcomSalesStatsService {

    /** 日期范围最长天数 */
    static final int MAX_DAYS = 366;
    /** 按日时最多的列数 */
    static final int MAX_DAY_COLUMNS = 92;
    static final int DEFAULT_DAYS = 30;

    private final EcomAnalyticsMapper analyticsMapper;
    private final EcomStockSnapshotMapper snapshotMapper;

    public SalesMatrix matrix(SalesQuery q) {
        SalesDimension dim = q.getDimension() != null ? q.getDimension() : SalesDimension.SKU;
        SalesGrain grain = q.getGrain() != null ? q.getGrain() : SalesGrain.DAY;
        AnalyticsDates range = AnalyticsDates.of(q.getBeginDate(), q.getEndDate(), DEFAULT_DAYS, MAX_DAYS);
        if (grain == SalesGrain.DAY && range.days() > MAX_DAY_COLUMNS) {
            throw new BizException("ecom.error.analytics.dayRangeTooLong", MAX_DAY_COLUMNS);
        }
        String keyword = blankToNull(q.getKeyword());
        List<SalesMatrix.Period> periods = periods(grain, range.begin(), range.end());
        SalesMatrix out = new SalesMatrix();
        out.setDimension(dim);
        out.setGrain(grain);
        out.setBeginDate(range.begin());
        out.setEndDate(range.end());
        out.setPeriods(periods);
        AnalyticsDates prev = range.previous();
        AnalyticsDates yoy = range.lastYear();
        out.setPrevBegin(prev.begin());
        out.setPrevEnd(prev.end());
        out.setYoyBegin(yoy.begin());
        out.setYoyEnd(yoy.end());

        Map<String, SalesMatrix.Row> rows = new LinkedHashMap<>();
        SalesMatrix.Row total = newRow("TOTAL", periods.size());
        for (AnalyticsRows.SalesCell c : analyticsMapper.selectSales(dim, range.from(), range.to(), q.getStoreId(), q.getPlatform(), keyword, true)) {
            String key = keyOf(dim, c);
            SalesMatrix.Row row = rows.computeIfAbsent(key, k -> label(newRow(k, periods.size()), c));
            int idx = periodIndex(grain, range.begin(), c.getSaleDay());
            BigDecimal qty = Decimals.nz(c.getQty());
            BigDecimal amount = Decimals.nz(c.getAmount());
            for (SalesMatrix.Row r : List.of(row, total)) {
                r.getQty().set(idx, r.getQty().get(idx).add(qty));
                r.getAmount().set(idx, r.getAmount().get(idx).add(amount));
                r.setTotalQty(r.getTotalQty().add(qty));
                r.setTotalAmount(r.getTotalAmount().add(amount));
                r.setTotalOrders(r.getTotalOrders() + (c.getOrders() == null ? 0 : c.getOrders()));
            }
        }
        for (AnalyticsRows.SalesCell c : analyticsMapper.selectSales(dim, prev.from(), prev.to(), q.getStoreId(), q.getPlatform(), keyword, false)) {
            SalesMatrix.Row row = rows.get(keyOf(dim, c));
            for (SalesMatrix.Row r : row == null ? List.of(total) : List.of(row, total)) {
                r.setPrevQty(r.getPrevQty().add(Decimals.nz(c.getQty())));
                r.setPrevAmount(r.getPrevAmount().add(Decimals.nz(c.getAmount())));
            }
        }
        for (AnalyticsRows.SalesCell c : analyticsMapper.selectSales(dim, yoy.from(), yoy.to(), q.getStoreId(), q.getPlatform(), keyword, false)) {
            SalesMatrix.Row row = rows.get(keyOf(dim, c));
            for (SalesMatrix.Row r : row == null ? List.of(total) : List.of(row, total)) {
                r.setYoyQty(r.getYoyQty().add(Decimals.nz(c.getQty())));
                r.setYoyAmount(r.getYoyAmount().add(Decimals.nz(c.getAmount())));
            }
        }
        List<SalesMatrix.Row> list = new ArrayList<>(rows.values());
        list.add(total);
        list.forEach(EcomSalesStatsService::finish);
        list.remove(list.size() - 1);
        list.sort(Comparator.comparing(SalesMatrix.Row::getTotalAmount).reversed().thenComparing(SalesMatrix.Row::getKey));
        out.setRows(list);
        out.setTotal(total);
        return out;
    }

    /** 单个 SKU 每天的销量与库存快照（默认最近 90 天） */
    public SkuTrend skuTrend(Long listingId, LocalDate beginDate, LocalDate endDate) {
        AnalyticsDates range = AnalyticsDates.of(beginDate, endDate, 90, MAX_DAYS);
        AnalyticsRows.ListingInfo info = listingInfo(listingId);
        Map<LocalDate, AnalyticsRows.SalesCell> sales = new HashMap<>();
        for (AnalyticsRows.SalesCell c : analyticsMapper.selectSkuDaily(info.getStoreId(), info.getSellerSku(), range.from(), range.to())) {
            sales.put(c.getSaleDay(), c);
        }
        Map<LocalDate, EcomStockSnapshot> snaps = new HashMap<>();
        for (EcomStockSnapshot s : snapshotMapper.selectList(Wrappers.<EcomStockSnapshot>lambdaQuery()
                .eq(EcomStockSnapshot::getListingId, listingId)
                .ge(EcomStockSnapshot::getSnapshotDate, range.begin())
                .le(EcomStockSnapshot::getSnapshotDate, range.end()))) {
            snaps.put(s.getSnapshotDate(), s);
        }
        List<SkuTrend.Point> points = new ArrayList<>();
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        int stockout = 0;
        for (LocalDate d = range.begin(); !d.isAfter(range.end()); d = d.plusDays(1)) {
            AnalyticsRows.SalesCell c = sales.get(d);
            EcomStockSnapshot s = snaps.get(d);
            BigDecimal qty = c == null ? BigDecimal.ZERO : Decimals.nz(c.getQty());
            BigDecimal amount = c == null ? BigDecimal.ZERO : Decimals.money(c.getAmount());
            Boolean out = s == null ? null : s.getSellableQty().signum() <= 0;
            if (Boolean.TRUE.equals(out)) {
                stockout++;
            }
            totalQty = totalQty.add(qty);
            totalAmount = totalAmount.add(amount);
            points.add(new SkuTrend.Point(d, qty, amount, s == null ? null : s.getPlatformQty(), s == null ? null : s.getLocalQty(),
                    s == null ? null : s.getSellableQty(), out));
        }
        return new SkuTrend(listingId, info.getStoreId(), info.getStoreName(), info.getPlatform(), info.getSellerSku(), info.getTitle(),
                info.getFulfillment(), info.getMaterialCode(), info.getMaterialName(), totalQty, totalAmount, snaps.size(), stockout, points);
    }

    AnalyticsRows.ListingInfo listingInfo(Long listingId) {
        List<AnalyticsRows.ListingInfo> list = listingId == null ? List.of() : analyticsMapper.selectListingInfo(List.of(listingId));
        if (list.isEmpty()) {
            throw new BizException("ecom.error.listingNotFound");
        }
        return list.get(0);
    }

    // ---------------------------------------------------------------- 时间段

    /** 查询范围内的时间段（周从周一开始、月从 1 日开始，首尾两段截到查询范围内） */
    static List<SalesMatrix.Period> periods(SalesGrain grain, LocalDate begin, LocalDate end) {
        List<SalesMatrix.Period> out = new ArrayList<>();
        LocalDate d = begin;
        while (!d.isAfter(end)) {
            LocalDate last = switch (grain) {
                case DAY -> d;
                case WEEK -> d.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                case MONTH -> d.with(TemporalAdjusters.lastDayOfMonth());
            };
            if (last.isAfter(end)) {
                last = end;
            }
            String key = switch (grain) {
                case DAY -> d.toString();
                case WEEK -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString();
                case MONTH -> YearMonth.from(d).toString();
            };
            out.add(new SalesMatrix.Period(key, d, last));
            d = last.plusDays(1);
        }
        return out;
    }

    /** 某天所在时间段的下标 */
    static int periodIndex(SalesGrain grain, LocalDate begin, LocalDate day) {
        return switch (grain) {
            case DAY -> (int) ChronoUnit.DAYS.between(begin, day);
            case WEEK -> (int) ChronoUnit.WEEKS.between(begin.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
            case MONTH -> (int) ChronoUnit.MONTHS.between(YearMonth.from(begin), YearMonth.from(day));
        };
    }

    // ---------------------------------------------------------------- 行

    static String keyOf(SalesDimension dim, AnalyticsRows.SalesCell c) {
        return switch (dim) {
            case SKU -> c.getStoreId() + "|" + c.getSellerSku();
            case SPU -> c.getStoreId() + "|" + c.getSpu();
            case MATERIAL -> String.valueOf(c.getMaterialId());
            case STORE -> String.valueOf(c.getStoreId());
            case PLATFORM -> String.valueOf(c.getPlatform());
            case COUNTRY -> Objects.toString(c.getCountry(), "");
        };
    }

    private static SalesMatrix.Row newRow(String key, int periods) {
        SalesMatrix.Row r = new SalesMatrix.Row();
        r.setKey(key);
        r.setQty(new ArrayList<>(Collections.nCopies(periods, BigDecimal.ZERO)));
        r.setAmount(new ArrayList<>(Collections.nCopies(periods, BigDecimal.ZERO)));
        return r;
    }

    private static SalesMatrix.Row label(SalesMatrix.Row r, AnalyticsRows.SalesCell c) {
        r.setStoreId(c.getStoreId());
        r.setStoreName(c.getStoreName());
        r.setPlatform(c.getPlatform());
        r.setSellerSku(c.getSellerSku());
        r.setListingId(c.getListingId());
        r.setTitle(c.getTitle());
        r.setSpu(c.getSpu());
        r.setSkuCount(c.getSkuCount());
        r.setMaterialId(c.getMaterialId());
        r.setMaterialCode(c.getMaterialCode());
        r.setMaterialName(c.getMaterialName());
        r.setMaterialSpec(c.getMaterialSpec());
        r.setCountry(c.getCountry());
        return r;
    }

    /** 舍入并计算环比、同比增长率 */
    private static void finish(SalesMatrix.Row r) {
        r.getQty().replaceAll(Decimals::qty);
        r.getAmount().replaceAll(Decimals::money);
        r.setTotalQty(Decimals.qty(r.getTotalQty()));
        r.setTotalAmount(Decimals.money(r.getTotalAmount()));
        r.setPrevQty(Decimals.qty(r.getPrevQty()));
        r.setPrevAmount(Decimals.money(r.getPrevAmount()));
        r.setYoyQty(Decimals.qty(r.getYoyQty()));
        r.setYoyAmount(Decimals.money(r.getYoyAmount()));
        r.setMomQtyPercent(growth(r.getTotalQty(), r.getPrevQty()));
        r.setMomAmountPercent(growth(r.getTotalAmount(), r.getPrevAmount()));
        r.setYoyQtyPercent(growth(r.getTotalQty(), r.getYoyQty()));
        r.setYoyAmountPercent(growth(r.getTotalAmount(), r.getYoyAmount()));
    }

    /** 增长率（%）=(本期 − 上期) ÷ 上期 × 100，上期为 0 时为空 */
    static BigDecimal growth(BigDecimal current, BigDecimal base) {
        return Decimals.percent(Decimals.nz(current).subtract(Decimals.nz(base)), base, 2);
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
