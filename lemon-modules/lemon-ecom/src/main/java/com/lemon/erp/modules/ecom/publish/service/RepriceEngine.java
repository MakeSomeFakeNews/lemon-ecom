package com.lemon.erp.modules.ecom.publish.service;

import com.lemon.erp.modules.ecom.publish.model.PriceAdjustMode;
import com.lemon.erp.modules.ecom.publish.model.RepriceStrategy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 自动调价的计算（纯计算，可单元测试）：按策略得到目标价，再限制在 [底价, 最高价] 之间，四舍五入到两位小数。
 * <ul>
 *   <li>跟随最低价：目标价 = 竞品最低价（含运费）</li>
 *   <li>低于最低价：目标价 = 竞品最低价 − X（金额），或 × (1 − X%)</li>
 *   <li>跟随购物车价：已持有购物车时不调整，否则目标价 = 购物车价（含运费）</li>
 *   <li>按目标利润率：目标价 = 价格模板按目标利润率计算的价格（由调用方算好传入）</li>
 *   <li>按库存 / 销量：可售天数 = 平台库存 ÷ 日均销量（没有销量视为无穷大），高于「降价天数」时按 X 降价，低于「涨价天数」时按 X 涨价</li>
 * </ul>
 * 原因用消息键 + 参数返回，由调用方按默认语言写入日志
 */
public final class RepriceEngine {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private RepriceEngine() {
    }

    /**
     * @param lowest       竞品最低价（含运费），没有为 null
     * @param buybox       购物车价（含运费），没有为 null
     * @param targetPrice  按目标利润率计算的价格（TARGET_MARGIN）
     * @param floor        底价，没有为 null
     * @param max          最高价，没有为 null
     * @param qty          平台可售数量
     * @param dailySales   日均销量
     */
    public record Input(RepriceStrategy strategy, PriceAdjustMode adjustMode, BigDecimal adjustValue, BigDecimal currentPrice, BigDecimal lowest,
                        BigDecimal buybox, boolean buyboxWinner, BigDecimal targetPrice, BigDecimal floor, BigDecimal max, BigDecimal qty,
                        BigDecimal dailySales, Integer highDays, Integer lowDays) {
    }

    /**
     * @param newPrice 新价格（跳过或不变时为 null）
     * @param skipped  没有调价依据（缺竞品价格、成本等），reason 为原因
     */
    public record Decision(BigDecimal newPrice, boolean skipped, String reasonKey, Object[] args) {

        static Decision skip(String key, Object... args) {
            return new Decision(null, true, key, args);
        }

        static Decision keep(String key, Object... args) {
            return new Decision(null, false, key, args);
        }

        static Decision change(BigDecimal price, String key, Object... args) {
            return new Decision(price, false, key, args);
        }

        public boolean changed() {
            return newPrice != null;
        }
    }

    public static Decision decide(Input in) {
        if (in.currentPrice() == null || in.currentPrice().signum() <= 0) {
            return Decision.skip("ecom.text.pub.reprice.noPrice");
        }
        BigDecimal target;
        String key;
        Object[] args;
        switch (in.strategy()) {
            case MATCH_LOWEST -> {
                if (in.lowest() == null) {
                    return Decision.skip("ecom.text.pub.reprice.noCompetitor");
                }
                target = in.lowest();
                key = "ecom.text.pub.reprice.matchLowest";
                args = new Object[]{money(in.lowest())};
            }
            case BEAT_LOWEST -> {
                if (in.lowest() == null) {
                    return Decision.skip("ecom.text.pub.reprice.noCompetitor");
                }
                target = adjust(in.lowest(), in.adjustMode(), in.adjustValue(), false);
                key = "ecom.text.pub.reprice.beatLowest";
                args = new Object[]{money(in.lowest()), amountText(in.adjustMode(), in.adjustValue())};
            }
            case MATCH_BUYBOX -> {
                if (in.buyboxWinner()) {
                    return Decision.keep("ecom.text.pub.reprice.holdBuybox");
                }
                if (in.buybox() == null) {
                    return Decision.skip("ecom.text.pub.reprice.noBuybox");
                }
                target = in.buybox();
                key = "ecom.text.pub.reprice.matchBuybox";
                args = new Object[]{money(in.buybox())};
            }
            case TARGET_MARGIN -> {
                if (in.targetPrice() == null) {
                    return Decision.skip("ecom.text.pub.reprice.noCost");
                }
                target = in.targetPrice();
                key = "ecom.text.pub.reprice.targetMargin";
                args = new Object[]{money(in.targetPrice())};
            }
            case BY_STOCK -> {
                BigDecimal qty = in.qty() == null ? BigDecimal.ZERO : in.qty();
                BigDecimal days = in.dailySales() == null || in.dailySales().signum() <= 0 ? null : qty.divide(in.dailySales(), 1, RoundingMode.HALF_UP);
                String daysText = days == null ? "∞" : days.stripTrailingZeros().toPlainString();
                if (in.highDays() != null && (days == null ? qty.signum() > 0 : days.compareTo(BigDecimal.valueOf(in.highDays())) > 0)) {
                    target = adjust(in.currentPrice(), in.adjustMode(), in.adjustValue(), false);
                    key = "ecom.text.pub.reprice.stockHigh";
                } else if (in.lowDays() != null && days != null && days.compareTo(BigDecimal.valueOf(in.lowDays())) < 0) {
                    target = adjust(in.currentPrice(), in.adjustMode(), in.adjustValue(), true);
                    key = "ecom.text.pub.reprice.stockLow";
                } else {
                    return Decision.keep("ecom.text.pub.reprice.stockNormal", daysText);
                }
                args = new Object[]{daysText, amountText(in.adjustMode(), in.adjustValue())};
            }
            default -> {
                return Decision.skip("ecom.text.pub.reprice.noPrice");
            }
        }
        BigDecimal price = target.setScale(2, RoundingMode.HALF_UP);
        String limitKey = null;
        if (in.floor() != null && price.compareTo(in.floor()) < 0) {
            price = in.floor().setScale(2, RoundingMode.HALF_UP);
            limitKey = "floor";
        }
        if (in.max() != null && price.compareTo(in.max()) > 0) {
            price = in.max().setScale(2, RoundingMode.HALF_UP);
            limitKey = "max";
        }
        if (price.signum() <= 0) {
            return Decision.skip("ecom.text.pub.reprice.noPrice");
        }
        if (price.compareTo(in.currentPrice().setScale(2, RoundingMode.HALF_UP)) == 0) {
            return Decision.keep(key, args);
        }
        if (limitKey != null) {
            Object[] withLimit = new Object[args.length + 1];
            System.arraycopy(args, 0, withLimit, 0, args.length);
            withLimit[args.length] = money(price);
            return Decision.change(price, key + ("floor".equals(limitKey) ? "Floor" : "Max"), withLimit);
        }
        return Decision.change(price, key, args);
    }

    /** 按金额或百分比加（up）/ 减 */
    static BigDecimal adjust(BigDecimal base, PriceAdjustMode mode, BigDecimal value, boolean up) {
        BigDecimal v = value == null ? BigDecimal.ZERO : value;
        if (mode == PriceAdjustMode.PERCENT) {
            BigDecimal factor = up ? BigDecimal.ONE.add(v.divide(HUNDRED, MC)) : BigDecimal.ONE.subtract(v.divide(HUNDRED, MC));
            return base.multiply(factor, MC);
        }
        return up ? base.add(v) : base.subtract(v);
    }

    private static String amountText(PriceAdjustMode mode, BigDecimal value) {
        String v = value == null ? "0" : value.stripTrailingZeros().toPlainString();
        return mode == PriceAdjustMode.PERCENT ? v + "%" : v;
    }

    private static String money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
