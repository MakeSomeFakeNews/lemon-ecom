package com.lemon.erp.modules.ecom.publish.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.publish.entity.EcomPriceTemplate;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 定价公式（纯计算，可单元测试）：
 * <pre>
 * 落地成本（店铺币种） = (商品成本 + 头程 + 其他成本) ÷ 汇率
 * 售价 = (落地成本 + 固定费) ÷ (1 − (佣金率 + 其他费率 + 目标利润率) / 100)，再按尾数向上取整（如 .99），不低于最低售价
 * 利润 = 售价 × (1 − (佣金率 + 其他费率) / 100) − 固定费 − 落地成本，利润率 = 利润 ÷ 售价
 * </pre>
 * 成本、头程、其他成本为本位币，汇率为「1 店铺币种 = rate 本位币」，固定费为店铺币种
 */
public final class PriceCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;

    private PriceCalculator() {
    }

    /** 模板参数（目标利润率可被调价规则的最低利润率 / 目标利润率覆盖） */
    public record Params(BigDecimal firstLegCost, BigDecimal otherCost, BigDecimal commissionRate, BigDecimal otherRate, BigDecimal fixedFee,
                         BigDecimal targetMargin, BigDecimal priceTail, BigDecimal minPrice) {

        public static Params of(EcomPriceTemplate t) {
            return new Params(Decimals.nz(t.getFirstLegCost()), Decimals.nz(t.getOtherCost()), Decimals.nz(t.getCommissionRate()),
                    Decimals.nz(t.getOtherRate()), Decimals.nz(t.getFixedFee()), Decimals.nz(t.getTargetMargin()), t.getPriceTail(), t.getMinPrice());
        }

        public Params withMargin(BigDecimal margin) {
            return new Params(firstLegCost, otherCost, commissionRate, otherRate, fixedFee, margin, priceTail, minPrice);
        }
    }

    /** 利润估算（店铺币种） */
    public record Profit(BigDecimal profit, BigDecimal margin, BigDecimal fees, BigDecimal landedCost) {
    }

    /** 按模板的目标利润率计算建议售价 */
    public static BigDecimal price(BigDecimal cost, BigDecimal rate, Params p) {
        return price(cost, rate, p, p.targetMargin());
    }

    /**
     * 按指定利润率计算售价
     *
     * @param cost 商品成本（本位币 / 件）
     * @param rate 汇率（1 店铺币种 = rate 本位币）
     */
    public static BigDecimal price(BigDecimal cost, BigDecimal rate, Params p, BigDecimal margin) {
        BigDecimal denominator = BigDecimal.ONE.subtract(p.commissionRate().add(p.otherRate()).add(Decimals.nz(margin)).divide(Decimals.HUNDRED, MC));
        if (denominator.signum() <= 0) {
            throw new BizException("ecom.error.pub.rateTooHigh");
        }
        BigDecimal raw = landed(cost, rate, p).add(p.fixedFee()).divide(denominator, MC);
        BigDecimal price = applyTail(raw, p.priceTail());
        if (p.minPrice() != null && price.compareTo(p.minPrice()) < 0) {
            price = p.minPrice().setScale(2, RoundingMode.HALF_UP);
        }
        return price;
    }

    /** 售价对应的利润与利润率 */
    public static Profit profit(BigDecimal price, BigDecimal cost, BigDecimal rate, Params p) {
        BigDecimal landed = landed(cost, rate, p);
        BigDecimal fees = price.multiply(p.commissionRate().add(p.otherRate())).divide(Decimals.HUNDRED, MC).add(p.fixedFee());
        BigDecimal profit = price.subtract(fees).subtract(landed);
        BigDecimal margin = price.signum() == 0 ? null : profit.multiply(Decimals.HUNDRED).divide(price, 2, RoundingMode.HALF_UP);
        return new Profit(profit.setScale(2, RoundingMode.HALF_UP), margin, fees.setScale(2, RoundingMode.HALF_UP), landed.setScale(2, RoundingMode.HALF_UP));
    }

    /** 落地成本（店铺币种）= (成本 + 头程 + 其他成本) ÷ 汇率 */
    static BigDecimal landed(BigDecimal cost, BigDecimal rate, Params p) {
        if (rate == null || rate.signum() <= 0) {
            throw new BizException("ecom.error.pub.rateInvalid");
        }
        return Decimals.nz(cost).add(p.firstLegCost()).add(p.otherCost()).divide(rate, MC);
    }

    /**
     * 尾数：tail 为空时四舍五入到两位小数；否则取「整数部分 + tail」，小于原价时加 1（向上取到 x.99 这样的价格）
     */
    public static BigDecimal applyTail(BigDecimal price, BigDecimal tail) {
        if (tail == null) {
            return price.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal t = tail.setScale(2, RoundingMode.HALF_UP);
        BigDecimal candidate = price.setScale(0, RoundingMode.FLOOR).add(t);
        if (candidate.compareTo(price.setScale(2, RoundingMode.HALF_UP)) < 0) {
            candidate = candidate.add(BigDecimal.ONE);
        }
        return candidate.setScale(2, RoundingMode.HALF_UP);
    }
}
