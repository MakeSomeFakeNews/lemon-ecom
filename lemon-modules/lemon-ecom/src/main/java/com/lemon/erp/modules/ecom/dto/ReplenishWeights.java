package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.common.exception.BizException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 日均销量的加权：日均 = Σ(权重 × 该期间日均) ÷ Σ权重，期间为最近 7 / 14 / 30 / 60 天。
 * 全部为空时取默认 7 天 × 0.5 + 30 天 × 0.5（兼顾近期趋势与月度平稳）
 */
public record ReplenishWeights(BigDecimal w7, BigDecimal w14, BigDecimal w30, BigDecimal w60) {

    public static final ReplenishWeights DEFAULT = new ReplenishWeights(new BigDecimal("0.5"), null, new BigDecimal("0.5"), null);

    /** 全部为空时取默认；有负数或合计为 0 时报错 */
    public static ReplenishWeights of(BigDecimal w7, BigDecimal w14, BigDecimal w30, BigDecimal w60) {
        if (w7 == null && w14 == null && w30 == null && w60 == null) {
            return DEFAULT;
        }
        ReplenishWeights w = new ReplenishWeights(w7, w14, w30, w60);
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : new BigDecimal[]{w7, w14, w30, w60}) {
            if (v != null && v.signum() < 0) {
                throw new BizException("ecom.error.purchase.weightInvalid");
            }
            total = total.add(v == null ? BigDecimal.ZERO : v);
        }
        if (total.signum() <= 0) {
            throw new BizException("ecom.error.purchase.weightInvalid");
        }
        return w;
    }

    /** 加权日均（4 位小数） */
    public BigDecimal daily(BigDecimal sales7, BigDecimal sales14, BigDecimal sales30, BigDecimal sales60) {
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal[][] parts = {{w7, sales7, BigDecimal.valueOf(7)}, {w14, sales14, BigDecimal.valueOf(14)},
                {w30, sales30, BigDecimal.valueOf(30)}, {w60, sales60, BigDecimal.valueOf(60)}};
        for (BigDecimal[] p : parts) {
            if (p[0] == null || p[0].signum() == 0) {
                continue;
            }
            BigDecimal sales = p[1] == null ? BigDecimal.ZERO : p[1];
            sum = sum.add(p[0].multiply(sales).divide(p[2], 8, RoundingMode.HALF_UP));
            weight = weight.add(p[0]);
        }
        return weight.signum() == 0 ? BigDecimal.ZERO : sum.divide(weight, 4, RoundingMode.HALF_UP);
    }

    /** 需要统计的最长天数 */
    public int maxDays() {
        if (w60 != null && w60.signum() > 0) {
            return 60;
        }
        if (w30 != null && w30.signum() > 0) {
            return 30;
        }
        return w14 != null && w14.signum() > 0 ? 14 : 7;
    }
}
