package com.lemon.erp.modules.ecom.analytics.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.analytics.dto.AnalyticsRows;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 库龄推算（纯计算）：批次库存按批次首次入库时间；没有批次的库存按入库流水先进先出——当前库存是最近的那些入库，
 * 从最新的入库往前累加到等于库存为止；流水不够（早于查询起点）的部分按起点日期计
 */
public final class StockAgeCalc {

    private StockAgeCalc() {
    }

    /**
     * @param oldestDays 最老一笔的库龄（天）
     * @param avgDays    按数量加权的平均库龄（1 位小数）
     * @param buckets    0-30、31-90、91-180、181-365、365 以上的数量
     */
    public record Age(int oldestDays, BigDecimal avgDays, BigDecimal[] buckets) {
    }

    /**
     * @param lots  在库的批次（inTime 为空的是没有批次的库存数量）
     * @param flows 入库流水，按时间倒序
     * @param since 流水的查询起点（推算不到的部分按这一天）
     */
    public static Age compute(List<AnalyticsRows.AgeLot> lots, List<AnalyticsRows.AgeLot> flows, LocalDate today, LocalDate since) {
        List<long[]> parts = new ArrayList<>();
        List<BigDecimal> qtys = new ArrayList<>();
        BigDecimal unbatched = BigDecimal.ZERO;
        for (AnalyticsRows.AgeLot l : lots) {
            if (l.getInTime() == null) {
                unbatched = unbatched.add(Decimals.nz(l.getQty()));
            } else if (Decimals.positive(l.getQty())) {
                parts.add(new long[]{days(l.getInTime().toLocalDate(), today)});
                qtys.add(l.getQty());
            }
        }
        BigDecimal remaining = unbatched;
        for (AnalyticsRows.AgeLot f : flows) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal take = Decimals.nz(f.getQty()).min(remaining);
            if (take.signum() > 0) {
                parts.add(new long[]{days(f.getInTime().toLocalDate(), today)});
                qtys.add(take);
                remaining = remaining.subtract(take);
            }
        }
        if (remaining.signum() > 0) {
            parts.add(new long[]{days(since, today)});
            qtys.add(remaining);
        }
        BigDecimal[] buckets = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal weighted = BigDecimal.ZERO;
        long oldest = 0;
        for (int i = 0; i < parts.size(); i++) {
            long d = parts.get(i)[0];
            BigDecimal q = qtys.get(i);
            oldest = Math.max(oldest, d);
            total = total.add(q);
            weighted = weighted.add(q.multiply(BigDecimal.valueOf(d)));
            buckets[bucket(d)] = buckets[bucket(d)].add(q);
        }
        BigDecimal avg = total.signum() > 0 ? weighted.divide(total, 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        return new Age((int) oldest, avg, buckets);
    }

    static int bucket(long days) {
        if (days <= 30) {
            return 0;
        }
        if (days <= 90) {
            return 1;
        }
        if (days <= 180) {
            return 2;
        }
        return days <= 365 ? 3 : 4;
    }

    private static long days(LocalDate from, LocalDate today) {
        return Math.max(0, ChronoUnit.DAYS.between(from, today));
    }
}
