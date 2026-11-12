package com.lemon.erp.modules.ecom.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 断货日的销量补偿（纯计算）：断货当天卖不出去，销量偏低，直接参与平均会低估真实需求。
 * <ul>
 *   <li>预测：断货日的销量替换为前后 {@link #WINDOW} 天内非断货日的日均（少于 {@link #MIN_REFERENCE} 天时取整段历史中非断货日的日均），
 *       只调高不调低（断货当天如果仍然卖得比参考值多，保留实际值）；全部是断货日时不调整</li>
 *   <li>补货的加权日均：每个统计期间的日均 = 非断货日的销量 ÷ 非断货天数（全部断货时保留原值；不低于原值——
 *       快照在日终记录，卖完最后一件的那天也记为断货，当天的销量不能因此被丢掉）</li>
 * </ul>
 */
public final class StockoutCalc {

    /** 参考窗口（断货日前后各多少天） */
    static final int WINDOW = 14;
    /** 参考窗口内至少的非断货天数 */
    static final int MIN_REFERENCE = 3;

    private StockoutCalc() {
    }

    /**
     * 补偿后的日销量（不修改入参）
     *
     * @param history  每天的销量
     * @param stockout 与 history 对齐，true 为断货日；为空时原样返回副本
     */
    public static double[] adjust(double[] history, boolean[] stockout) {
        double[] out = history.clone();
        if (stockout == null) {
            return out;
        }
        double allSum = 0;
        int allCount = 0;
        for (int i = 0; i < history.length; i++) {
            if (!isStockout(stockout, i)) {
                allSum += history[i];
                allCount++;
            }
        }
        if (allCount == 0) {
            return out;
        }
        double allMean = allSum / allCount;
        for (int i = 0; i < history.length; i++) {
            if (!isStockout(stockout, i)) {
                continue;
            }
            double sum = 0;
            int count = 0;
            for (int j = Math.max(0, i - WINDOW); j <= Math.min(history.length - 1, i + WINDOW); j++) {
                if (!isStockout(stockout, j)) {
                    sum += history[j];
                    count++;
                }
            }
            double ref = count >= MIN_REFERENCE ? sum / count : allMean;
            out[i] = Math.max(history[i], ref);
        }
        return out;
    }

    /** 断货天数 */
    public static int count(boolean[] stockout) {
        int n = 0;
        if (stockout != null) {
            for (boolean b : stockout) {
                if (b) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * 排除断货日后的期间销量：非断货日的销量 ÷ 非断货天数 × 期间天数，不低于原销量（全部断货或没有断货时返回原销量）
     *
     * @param total         期间销量合计
     * @param stockoutSales 期间内断货日的销量
     * @param days          期间天数
     * @param stockoutDays  期间内的断货天数
     */
    public static BigDecimal excludeStockout(BigDecimal total, BigDecimal stockoutSales, int days, int stockoutDays) {
        if (total == null || stockoutDays <= 0 || stockoutDays >= days) {
            return total;
        }
        BigDecimal normal = total.subtract(stockoutSales == null ? BigDecimal.ZERO : stockoutSales).max(BigDecimal.ZERO);
        return normal.multiply(BigDecimal.valueOf(days)).divide(BigDecimal.valueOf((long) days - stockoutDays), 4, RoundingMode.HALF_UP).max(total);
    }

    private static boolean isStockout(boolean[] stockout, int i) {
        return i < stockout.length && stockout[i];
    }
}
