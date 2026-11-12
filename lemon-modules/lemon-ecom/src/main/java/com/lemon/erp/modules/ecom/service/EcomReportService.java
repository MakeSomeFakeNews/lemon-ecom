package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.ecom.dto.EcomReports;
import com.lemon.erp.modules.ecom.mapper.EcomReportMapper;
import com.lemon.erp.modules.ecom.model.ProfitBasis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 电商看板与利润分析：金额为本位币；按店铺 / SKU 的利润扣除广告花费（广告日报按店铺币种、统计截止日所在月的汇率折算）
 */
@Service
@RequiredArgsConstructor
public class EcomReportService {

    private final EcomReportMapper reportMapper;
    private final EcomReplenishService replenishService;
    private final ExchangeRateProvider rateProvider;
    private final EcomProfitService profitService;

    public EcomReports.Overview overview() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> day = reportMapper.selectTotals(today.atStartOfDay(), today.plusDays(1).atStartOfDay(), null);
        Map<String, Object> month = reportMapper.selectTotals(today.withDayOfMonth(1).atStartOfDay(), today.plusDays(1).atStartOfDay(), null);
        Map<String, Object> counts = reportMapper.selectOrderCounts(now);
        EcomReports.Overview o = new EcomReports.Overview();
        o.setTodayOrders(num(day, "orders"));
        o.setTodaySales(dec(day, "sales"));
        o.setMonthOrders(num(month, "orders"));
        o.setMonthSales(dec(month, "sales"));
        // 本月利润与利润报表（订单口径）一致：订单利润再扣除广告花费和不挂订单费用的分摊
        o.setMonthProfit(profitService.total(ProfitBasis.ORDER, today.withDayOfMonth(1), today, null).getProfit());
        o.setMonthRefund(dec(month, "refund"));
        o.setMonthFees(dec(month, "fees"));
        o.setMonthCost(dec(month, "cost"));
        o.setPendingOrders(num(counts, "pending"));
        o.setExceptionOrders(num(counts, "exception"));
        o.setOverdueOrders(num(counts, "overdue"));
        o.setInTransitInbounds(reportMapper.countInTransitInbounds());
        o.setUrgentReplenish(replenishService.countUrgent());
        return o;
    }

    /** 按天趋势：利润为订单利润扣除当天的广告花费（当月汇率）和费用分摊，与利润报表的订单口径一致 */
    public List<EcomReports.TrendRow> trend(LocalDate begin, LocalDate end, Long storeId) {
        List<EcomReports.TrendRow> rows = reportMapper.selectTrend(start(begin), endExclusive(end), storeId);
        Map<LocalDate, BigDecimal> deduct = new HashMap<>();
        for (Map<String, Object> m : reportMapper.selectAdSpendByDay(begin, end, storeId)) {
            LocalDate day = toDate(m.get("day"));
            BigDecimal rate = rateProvider.rate((String) m.get("currency"), day);
            if (rate == null) {
                throw new BizException("ecom.error.exchangeRateMissing", m.get("currency"), day.toString().substring(0, 7));
            }
            deduct.merge(day, Decimals.money(new BigDecimal(m.get("spend").toString()).multiply(rate)), BigDecimal::add);
        }
        for (Map<String, Object> m : reportMapper.selectAllocationByDay(begin, end, storeId)) {
            deduct.merge(toDate(m.get("day")), new BigDecimal(m.get("amount").toString()).negate(), BigDecimal::add);
        }
        Map<LocalDate, EcomReports.TrendRow> byDay = new java.util.TreeMap<>();
        rows.forEach(r -> byDay.put(r.getDay(), r));
        deduct.forEach((day, v) -> {
            EcomReports.TrendRow r = byDay.computeIfAbsent(day, d -> {
                EcomReports.TrendRow n = new EcomReports.TrendRow();
                n.setDay(d);
                n.setOrders(0L);
                n.setSales(BigDecimal.ZERO);
                return n;
            });
            r.setProfit(Decimals.money(Decimals.nz(r.getProfit()).subtract(v)));
        });
        return new java.util.ArrayList<>(byDay.values());
    }

    private static LocalDate toDate(Object v) {
        if (v instanceof LocalDate d) {
            return d;
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (v instanceof LocalDateTime t) {
            return t.toLocalDate();
        }
        return LocalDate.parse(v.toString().substring(0, 10));
    }

    public List<EcomReports.StoreRow> byStore(LocalDate begin, LocalDate end, Long storeId) {
        List<EcomReports.StoreRow> rows = reportMapper.selectByStore(start(begin), endExclusive(end), storeId);
        Map<Long, BigDecimal> ad = new HashMap<>();
        for (Map<String, Object> m : reportMapper.selectAdSpendByStore(begin, end, storeId)) {
            ad.put(((Number) m.get("storeId")).longValue(), toBase(m, end));
        }
        for (EcomReports.StoreRow r : rows) {
            r.setAdSpend(ad.getOrDefault(r.getStoreId(), BigDecimal.ZERO));
            r.setProfit(Decimals.money(Decimals.nz(r.getProfit()).subtract(r.getAdSpend())));
            r.setMargin(margin(r.getProfit(), r.getSales()));
        }
        return rows;
    }

    public List<EcomReports.SkuRow> bySku(LocalDate begin, LocalDate end, Long storeId, String keyword) {
        List<EcomReports.SkuRow> rows = reportMapper.selectBySku(start(begin), endExclusive(end), storeId, keyword);
        Map<String, BigDecimal> ad = new HashMap<>();
        for (Map<String, Object> m : reportMapper.selectAdSpendBySku(begin, end, storeId)) {
            ad.put(m.get("storeId") + "|" + m.get("sellerSku"), toBase(m, end));
        }
        for (EcomReports.SkuRow r : rows) {
            r.setAdSpend(ad.getOrDefault(r.getStoreId() + "|" + r.getSellerSku(), BigDecimal.ZERO));
            r.setProfit(Decimals.money(Decimals.nz(r.getProfit()).subtract(r.getAdSpend())));
            r.setSales(Decimals.money(Decimals.nz(r.getSales())));
            r.setFees(Decimals.money(Decimals.nz(r.getFees())));
            r.setRefund(Decimals.money(Decimals.nz(r.getRefund())));
            r.setShipping(Decimals.money(Decimals.nz(r.getShipping())));
            r.setMargin(margin(r.getProfit(), r.getSales()));
        }
        return rows;
    }

    public List<EcomReports.CountryRow> byCountry(LocalDate begin, LocalDate end, Long storeId) {
        return reportMapper.selectByCountry(start(begin), endExclusive(end), storeId);
    }

    public List<EcomReports.FeeRow> fees(LocalDate begin, LocalDate end, Long storeId) {
        return reportMapper.selectFees(begin, end, storeId);
    }

    /** 广告花费折本位币：按统计截止日（为空取今天）所在月的汇率，没有汇率时按 1 */
    private BigDecimal toBase(Map<String, Object> m, LocalDate end) {
        BigDecimal spend = m.get("spend") == null ? BigDecimal.ZERO : new BigDecimal(m.get("spend").toString());
        String currency = (String) m.get("currency");
        BigDecimal rate = rateProvider.rate(currency, end == null ? LocalDate.now() : end);
        return Decimals.money(spend.multiply(rate == null ? BigDecimal.ONE : rate));
    }

    static BigDecimal margin(BigDecimal profit, BigDecimal sales) {
        if (sales == null || sales.signum() == 0 || profit == null) {
            return null;
        }
        return profit.multiply(Decimals.HUNDRED).divide(sales, 2, RoundingMode.HALF_UP);
    }

    private static LocalDateTime start(LocalDate d) {
        return d == null ? null : d.atStartOfDay();
    }

    private static LocalDateTime endExclusive(LocalDate d) {
        return d == null ? null : d.plusDays(1).atStartOfDay();
    }

    private static long num(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        return v == null ? 0L : ((Number) v).longValue();
    }

    private static BigDecimal dec(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        return v == null ? BigDecimal.ZERO : Decimals.money(new BigDecimal(v.toString()));
    }
}
