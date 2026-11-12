package com.lemon.erp.modules.ecom.fba.service;

import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaFeeRate;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaSizeTier;
import com.lemon.erp.modules.ecom.fba.model.FbaUnitSystem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * FBA 配送费计算（纯计算）：
 * <ol>
 *   <li>三边从大到小排序为 最长边 / 次长边 / 最短边，按分段的单位制换算（英寸 = 厘米 ÷ 2.54，磅 = 千克 × 2.20462）；</li>
 *   <li>按 sort 依次匹配分段：各边、长度 + 围长（最长边 + 2 × (次长边 + 最短边)）、三边合计不超过上限，单件实重不超过重量上限；</li>
 *   <li>发货重量（取费率用）= 分段有体积重系数时取 实重 与 体积重（三边乘积 ÷ 系数，美国站 139 立方英寸 / 磅，欧洲站 5000 立方厘米 / 千克）的较大者，
 *       否则为实重（信封、日本站）；</li>
 *   <li>在分段的费率中找 重量下限 &lt; 发货重量 ≤ 重量上限 的一段：配送费 = 固定费 + 超过续重起点的部分按续重单位向上取整 × 续重单价。</li>
 * </ol>
 * 亚马逊另有按售价、服装、危险品、旺季的附加规则，没有覆盖，需要时在费率表中按自己的商品维护
 */
public final class FbaFeeCalculator {

    static final BigDecimal CM_PER_INCH = new BigDecimal("2.54");
    static final BigDecimal LB_PER_KG = new BigDecimal("2.20462");
    /** 1 立方英尺 = 28316.846592 立方厘米 */
    static final BigDecimal CM3_PER_CUFT = new BigDecimal("28316.846592");
    static final BigDecimal CM3_PER_CBM = new BigDecimal("1000000");
    private static final int SCALE = 4;

    private FbaFeeCalculator() {
    }

    /** 商品尺寸（cm）与重量（kg） */
    public record Dims(BigDecimal lengthCm, BigDecimal widthCm, BigDecimal heightCm, BigDecimal weightKg) {

        public boolean complete() {
            return positive(lengthCm) && positive(widthCm) && positive(heightCm) && weightKg != null && weightKg.signum() > 0;
        }

        /** 体积（立方厘米） */
        public BigDecimal cm3() {
            return lengthCm.multiply(widthCm).multiply(heightCm);
        }

        private static boolean positive(BigDecimal v) {
            return v != null && v.signum() > 0;
        }
    }

    /**
     * 匹配分段并计算配送费，尺寸不完整或没有匹配的分段 / 费率时返回 null
     *
     * @param tiers 站点的分段（含 rates），顺序不限，按 sort 匹配
     */
    public static FbaViews.FeeQuote quote(Dims dims, List<EcomFbaSizeTier> tiers) {
        if (dims == null || !dims.complete() || tiers == null || tiers.isEmpty()) {
            return null;
        }
        BigDecimal[] sides = {dims.lengthCm(), dims.widthCm(), dims.heightCm()};
        Arrays.sort(sides, Comparator.reverseOrder());
        List<EcomFbaSizeTier> sorted = tiers.stream().sorted(Comparator.comparing(t -> t.getSort() == null ? 0 : t.getSort())).toList();
        for (EcomFbaSizeTier t : sorted) {
            boolean imperial = t.getUnitSystem() != FbaUnitSystem.METRIC;
            BigDecimal l = imperial ? inch(sides[0]) : sides[0];
            BigDecimal m = imperial ? inch(sides[1]) : sides[1];
            BigDecimal s = imperial ? inch(sides[2]) : sides[2];
            BigDecimal girth = l.add(m.add(s).multiply(BigDecimal.valueOf(2)));
            if (over(l, t.getMaxLong()) || over(m, t.getMaxMedian()) || over(s, t.getMaxShort()) || over(girth, t.getMaxGirth())
                    || over(l.add(m).add(s), t.getMaxSidesSum())) {
                continue;
            }
            BigDecimal unitWeight = imperial ? dims.weightKg().multiply(LB_PER_KG).setScale(SCALE, RoundingMode.HALF_UP) : dims.weightKg();
            if (over(unitWeight, t.getMaxWeight())) {
                continue;
            }
            BigDecimal weight = unitWeight;
            if (t.getDimDivisor() != null && t.getDimDivisor().signum() > 0) {
                BigDecimal dimWeight = l.multiply(m).multiply(s).divide(t.getDimDivisor(), SCALE, RoundingMode.HALF_UP);
                weight = weight.max(dimWeight);
            }
            EcomFbaFeeRate rate = rateFor(t.getRates(), weight);
            if (rate == null) {
                return null;
            }
            return new FbaViews.FeeQuote(t.getCode(), t.getName(), t.getSizeCategory(), weight.setScale(3, RoundingMode.HALF_UP),
                    imperial ? "lb" : "kg", fee(rate, weight), rate.getCurrency());
        }
        return null;
    }

    static EcomFbaFeeRate rateFor(List<EcomFbaFeeRate> rates, BigDecimal weight) {
        if (rates == null) {
            return null;
        }
        return rates.stream()
                .filter(r -> weight.compareTo(nz(r.getWeightFrom())) > 0 || (nz(r.getWeightFrom()).signum() == 0 && weight.signum() == 0))
                .filter(r -> r.getWeightTo() == null || weight.compareTo(r.getWeightTo()) <= 0)
                .min(Comparator.comparing(r -> nz(r.getWeightFrom())))
                .orElse(null);
    }

    /** 固定费 + 超过续重起点的部分按续重单位向上取整 × 续重单价 */
    static BigDecimal fee(EcomFbaFeeRate r, BigDecimal weight) {
        BigDecimal fee = nz(r.getFee());
        if (r.getExtraFrom() != null && r.getExtraUnit() != null && r.getExtraUnit().signum() > 0 && r.getExtraFee() != null
                && weight.compareTo(r.getExtraFrom()) > 0) {
            BigDecimal steps = weight.subtract(r.getExtraFrom()).divide(r.getExtraUnit(), 0, RoundingMode.CEILING);
            fee = fee.add(steps.multiply(r.getExtraFee()));
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    /** 单件体积：立方英尺（CUFT）或立方米（CBM），尺寸不完整时为 null */
    public static BigDecimal volume(Dims dims, boolean cubicMeter) {
        if (dims == null || !dims.complete()) {
            return null;
        }
        return dims.cm3().divide(cubicMeter ? CM3_PER_CBM : CM3_PER_CUFT, 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal inch(BigDecimal cm) {
        return cm.divide(CM_PER_INCH, SCALE, RoundingMode.HALF_UP);
    }

    private static boolean over(BigDecimal v, BigDecimal max) {
        return max != null && v.compareTo(max) > 0;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
