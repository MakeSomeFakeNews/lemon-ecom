package com.lemon.erp.modules.ecom.dev.service;

import java.util.function.DoubleUnaryOperator;

/**
 * 单件利润计算（纯计算，参数已由 {@link EcomProfitCalcService} 解析好）：
 * <pre>
 * 收入            = 售价 × 汇率
 * 税费（价内税）   = 售价 − 售价 ÷ (1 + 税率)            （销售税由平台代收，不计）
 * 佣金            = max(售价 × 佣金率, 最低佣金) + 每单固定费
 * 广告 / 退货损失  = 售价 × 广告占比 / 退货率
 * 站点成本        = 税费 + 佣金 + 配送费 + 广告 + 退货损失 + 站点其他费用（站点币种 × 汇率）
 * 落地成本        = 采购价 + 头程 + 关税 + 其他成本（本位币）
 * 利润            = 收入 − 站点成本 − 落地成本；利润率 = 利润 ÷ 收入；ROI = 利润 ÷ 落地成本
 * </pre>
 * 保本价、建议售价对售价二分求解（配送费随售价变化时也适用）
 */
public final class ProfitCalculator {

    /** 求解时售价的上限（站点币种） */
    static final double PRICE_LIMIT = 10_000_000;

    /**
     * 已解析的参数。比率为小数（15% = 0.15）
     *
     * @param fulfillment 配送费（站点币种），参数为售价
     */
    public record Input(double exchangeRate, double commissionRate, double minCommission, double fixedFee, double adRate, double returnRate,
                        boolean taxInclusive, double taxRate, double siteOther, double purchase, double head, double duty, double other,
                        DoubleUnaryOperator fulfillment) {
    }

    /** 某个售价下的结果（站点币种的费用与本位币利润） */
    public record Breakdown(double price, double revenue, double tax, double commission, double fulfillment, double ad, double returns,
                            double siteOther, double siteCost, double landed, double profit, double margin, double roi) {
    }

    private ProfitCalculator() {
    }

    public static Breakdown at(Input in, double price) {
        double fx = in.exchangeRate();
        double tax = in.taxInclusive() && in.taxRate() > 0 ? price - price / (1 + in.taxRate()) : 0;
        double commission = Math.max(price * in.commissionRate(), in.minCommission()) + in.fixedFee();
        double fulfillment = in.fulfillment() == null ? 0 : in.fulfillment().applyAsDouble(price);
        double ad = price * in.adRate();
        double returns = price * in.returnRate();
        double siteCost = tax + commission + fulfillment + ad + returns + in.siteOther();
        double landed = in.purchase() + in.head() + in.duty() + in.other();
        double revenue = price * fx;
        double profit = revenue - siteCost * fx - landed;
        double margin = revenue > 0 ? profit / revenue * 100 : 0;
        double roi = landed > 0 ? profit / landed * 100 : 0;
        return new Breakdown(price, revenue, tax, commission, fulfillment, ad, returns, in.siteOther(), siteCost, landed, profit, margin, roi);
    }

    /** 保本售价（利润 = 0），无法保本时返回 null */
    public static Double breakeven(Input in) {
        return solve(p -> at(in, p).profit());
    }

    /**
     * 目标利润率（%）下的售价，无法达到（佣金、广告、退货、税费占比之和加目标利润率超过 100%）时返回 null
     */
    public static Double priceForMargin(Input in, double marginPercent) {
        double m = marginPercent / 100;
        return solve(p -> at(in, p).profit() - m * p * in.exchangeRate());
    }

    /** f(p) 在 p 足够大时为正：从 1 开始倍增找到上界后二分 */
    static Double solve(DoubleUnaryOperator f) {
        if (f.applyAsDouble(0) >= 0) {
            return 0d;
        }
        double hi = 1;
        while (f.applyAsDouble(hi) < 0) {
            hi *= 2;
            if (hi > PRICE_LIMIT) {
                return null;
            }
        }
        double lo = hi / 2 >= 1 ? hi / 2 : 0;
        for (int i = 0; i < 100 && hi - lo > 1e-6; i++) {
            double mid = (lo + hi) / 2;
            if (f.applyAsDouble(mid) < 0) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        // 向上取到分，保证该价格下利润不为负
        return Math.ceil(hi * 100 - 1e-6) / 100;
    }
}
