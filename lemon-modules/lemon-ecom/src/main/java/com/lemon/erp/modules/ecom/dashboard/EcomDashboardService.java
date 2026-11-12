package com.lemon.erp.modules.ecom.dashboard;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.dto.EcomReports;
import com.lemon.erp.modules.ecom.mapper.EcomDashboardMapper;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.service.EcomReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * 电商看板（参考领星、积加的首页）：
 * <ul>
 *   <li>关键指标：今日（较昨日同一时段、较去年同日同一时段）、昨日（较前一天、较去年同日）、本月（较上月同期、较去年同月同期）的订单数、销售额、
 *       件数、客单价、订单利润，增长率为百分数</li>
 *   <li>实时销量：今日与昨日每小时的订单数和销售额</li>
 *   <li>排行：Top SKU（按销量 / 销售额 / 利润，利润扣除广告花费，与利润分析的 SKU 口径一致）、店铺排行（销售额、利润率）、平台占比</li>
 *   <li>待处理事项：汇总各功能的 {@link EcomDashboardTodoProvider}</li>
 * </ul>
 * 与利润分析一样统计全公司（不按数据权限），金额为本位币，不含已取消订单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomDashboardService {

    /** 排行条数上限 */
    static final int MAX_TOP = 50;

    private final EcomDashboardMapper dashboardMapper;
    private final EcomReportService reportService;
    private final List<EcomDashboardTodoProvider> todoProviders;

    /** Top SKU 的排序指标 */
    public enum SkuRankBy {
        QTY, SALES, PROFIT
    }

    // ---------------------------------------------------------------- 关键指标

    public List<EcomDashboardViews.KpiPeriod> kpi(Long storeId) {
        return kpi(storeId, LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
    }

    /** 按给定的「现在」计算（测试用） */
    public List<EcomDashboardViews.KpiPeriod> kpi(Long storeId, LocalDateTime now) {
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        LocalDateTime month = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        List<EcomDashboardViews.KpiPeriod> out = new ArrayList<>();
        out.add(period("TODAY", storeId, today, now, today.minusDays(1), now.minusDays(1), today.minusYears(1), now.minusYears(1)));
        out.add(period("YESTERDAY", storeId, today.minusDays(1), today, today.minusDays(2), today.minusDays(1),
                today.minusDays(1).minusYears(1), today.minusYears(1)));
        out.add(period("MONTH", storeId, month, now, month.minusMonths(1), now.minusMonths(1), month.minusYears(1), now.minusYears(1)));
        return out;
    }

    private EcomDashboardViews.KpiPeriod period(String key, Long storeId, LocalDateTime begin, LocalDateTime end, LocalDateTime prevBegin,
                                                LocalDateTime prevEnd, LocalDateTime lyBegin, LocalDateTime lyEnd) {
        EcomDashboardViews.Metric cur = metric(begin, end, storeId);
        EcomDashboardViews.Metric prev = metric(prevBegin, prevEnd, storeId);
        EcomDashboardViews.Metric ly = metric(lyBegin, lyEnd, storeId);
        return new EcomDashboardViews.KpiPeriod(key, begin, end, cur, prev, ly, growth(cur, prev), growth(cur, ly));
    }

    EcomDashboardViews.Metric metric(LocalDateTime begin, LocalDateTime end, Long storeId) {
        if (!end.isAfter(begin)) {
            return new EcomDashboardViews.Metric(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        Map<String, Object> m = dashboardMapper.selectKpi(begin, end, storeId);
        long orders = m == null || m.get("orders") == null ? 0 : ((Number) m.get("orders")).longValue();
        BigDecimal sales = Decimals.money(dec(m, "sales"));
        BigDecimal profit = Decimals.money(dec(m, "profit"));
        BigDecimal units = Decimals.qty(Decimals.nz(dashboardMapper.selectUnits(begin, end, storeId)));
        BigDecimal avg = orders == 0 ? BigDecimal.ZERO : sales.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
        return new EcomDashboardViews.Metric(orders, sales, units, avg, profit);
    }

    /** 各指标的增长率（%），上期为 0 时为空 */
    static Map<String, BigDecimal> growth(EcomDashboardViews.Metric cur, EcomDashboardViews.Metric base) {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        m.put("orders", rate(BigDecimal.valueOf(cur.orders()), BigDecimal.valueOf(base.orders())));
        m.put("sales", rate(cur.sales(), base.sales()));
        m.put("units", rate(cur.units(), base.units()));
        m.put("avgOrder", rate(cur.avgOrder(), base.avgOrder()));
        m.put("profit", rate(cur.profit(), base.profit()));
        return m;
    }

    /** 增长率 = (本期 − 上期) ÷ |上期| × 100，上期为 0 时为空 */
    static BigDecimal rate(BigDecimal cur, BigDecimal base) {
        if (base == null || base.signum() == 0) {
            return null;
        }
        return Decimals.nz(cur).subtract(base).multiply(BigDecimal.valueOf(100)).divide(base.abs(), 2, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------- 实时销量

    public EcomDashboardViews.Hourly hourly(Long storeId) {
        return hourly(storeId, LocalDateTime.now());
    }

    public EcomDashboardViews.Hourly hourly(Long storeId, LocalDateTime now) {
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        int hour = now.getHour();
        return new EcomDashboardViews.Hourly(hours(today, today.plusDays(1), storeId, hour), hours(today.minusDays(1), today, storeId, 23), hour);
    }

    private List<EcomDashboardViews.HourPoint> hours(LocalDateTime begin, LocalDateTime end, Long storeId, int lastHour) {
        Map<Integer, Map<String, Object>> byHour = new LinkedHashMap<>();
        for (Map<String, Object> r : dashboardMapper.selectHourly(begin, end, storeId)) {
            byHour.put(((Number) r.get("hr")).intValue(), r);
        }
        List<EcomDashboardViews.HourPoint> out = new ArrayList<>();
        for (int h = 0; h <= lastHour; h++) {
            Map<String, Object> r = byHour.get(h);
            out.add(new EcomDashboardViews.HourPoint(h, r == null ? 0 : ((Number) r.get("orders")).longValue(),
                    r == null ? BigDecimal.ZERO : Decimals.money(dec(r, "sales"))));
        }
        return out;
    }

    // ---------------------------------------------------------------- 排行

    /** Top SKU：按销量 / 销售额 / 利润从高到低（利润扣除广告花费），最多 limit 条 */
    public List<EcomDashboardViews.SkuRank> topSku(LocalDate begin, LocalDate end, Long storeId, SkuRankBy by, int limit) {
        Function<EcomReports.SkuRow, BigDecimal> key = switch (by == null ? SkuRankBy.SALES : by) {
            case QTY -> EcomReports.SkuRow::getQty;
            case PROFIT -> EcomReports.SkuRow::getProfit;
            case SALES -> EcomReports.SkuRow::getSales;
        };
        return reportService.bySku(begin, end, storeId, null).stream()
                .sorted(Comparator.comparing((EcomReports.SkuRow r) -> Decimals.nz(key.apply(r))).reversed())
                .limit(Math.max(1, Math.min(limit, MAX_TOP)))
                .map(r -> new EcomDashboardViews.SkuRank(r.getStoreId(), r.getStoreName(), r.getSellerSku(), r.getTitle(), r.getQty(), r.getSales(),
                        r.getProfit(), r.getMargin()))
                .toList();
    }

    /** 店铺排行：按销售额从高到低，带利润率（利润扣除广告花费） */
    public List<EcomDashboardViews.StoreRank> storeRank(LocalDate begin, LocalDate end) {
        return reportService.byStore(begin, end, null).stream()
                .sorted(Comparator.comparing((EcomReports.StoreRow r) -> Decimals.nz(r.getSales())).reversed())
                .map(r -> new EcomDashboardViews.StoreRank(r.getStoreId(), r.getStoreName(), r.getPlatform(), r.getMarketplace(),
                        r.getOrders() == null ? 0 : r.getOrders(), Decimals.nz(r.getSales()), Decimals.nz(r.getProfit()), r.getMargin(),
                        Decimals.nz(r.getAdSpend())))
                .toList();
    }

    /** 平台占比：按平台汇总店铺数据，销售额占比为百分数 */
    public List<EcomDashboardViews.PlatformShare> platformShare(LocalDate begin, LocalDate end) {
        Map<EcomPlatform, BigDecimal[]> sums = new EnumMap<>(EcomPlatform.class);
        Map<EcomPlatform, Long> orders = new EnumMap<>(EcomPlatform.class);
        BigDecimal total = BigDecimal.ZERO;
        for (EcomReports.StoreRow r : reportService.byStore(begin, end, null)) {
            if (r.getPlatform() == null) {
                continue;
            }
            BigDecimal[] s = sums.computeIfAbsent(r.getPlatform(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            s[0] = s[0].add(Decimals.nz(r.getSales()));
            s[1] = s[1].add(Decimals.nz(r.getProfit()));
            orders.merge(r.getPlatform(), r.getOrders() == null ? 0 : r.getOrders(), Long::sum);
            total = total.add(Decimals.nz(r.getSales()));
        }
        List<EcomDashboardViews.PlatformShare> out = new ArrayList<>();
        BigDecimal t = total;
        sums.forEach((p, s) -> out.add(new EcomDashboardViews.PlatformShare(p, orders.getOrDefault(p, 0L), Decimals.money(s[0]), Decimals.money(s[1]),
                s[0].signum() == 0 ? null : s[1].multiply(BigDecimal.valueOf(100)).divide(s[0], 2, RoundingMode.HALF_UP),
                t.signum() == 0 ? BigDecimal.ZERO : s[0].multiply(BigDecimal.valueOf(100)).divide(t, 2, RoundingMode.HALF_UP))));
        out.sort(Comparator.comparing(EcomDashboardViews.PlatformShare::sales).reversed());
        return out;
    }

    // ---------------------------------------------------------------- 待办

    /** 待处理事项：各提供者按 sort 汇总（同一 key 只保留第一个），单个提供者失败只记日志 */
    public List<EcomDashboardTodo> todos() {
        Map<String, EcomDashboardTodo> out = new LinkedHashMap<>();
        for (EcomDashboardTodoProvider p : todoProviders) {
            try {
                for (EcomDashboardTodo t : p.todos()) {
                    if (t.count() > 0) {
                        out.putIfAbsent(t.key(), t);
                    }
                }
            } catch (RuntimeException e) {
                log.warn("Dashboard todo provider {} failed", p.getClass().getSimpleName(), e);
            }
        }
        return out.values().stream().sorted(Comparator.comparingInt(EcomDashboardTodo::sort)).toList();
    }

    private static BigDecimal dec(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        if (v == null) {
            // H2 / MySQL 返回的列名大小写可能不同
            v = m == null ? null : m.get(key.toUpperCase(Locale.ROOT));
        }
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }
}
