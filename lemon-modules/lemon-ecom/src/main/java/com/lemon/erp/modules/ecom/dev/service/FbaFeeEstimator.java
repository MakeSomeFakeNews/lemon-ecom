package com.lemon.erp.modules.ecom.dev.service;

import com.lemon.erp.modules.ecom.dev.model.DevFbaSizeTier;

import java.util.Arrays;

/**
 * 亚马逊美国站 FBA 配送费估算（非服装、非危险品），按包装尺寸和重量确定尺寸分段：
 * <ul>
 *   <li>小号标准：最长边 ≤ 15"、次长边 ≤ 12"、最短边 ≤ 0.75"、单件重量 ≤ 16 oz，按单件重量计费；</li>
 *   <li>大号标准：最长边 ≤ 18"、次长边 ≤ 14"、最短边 ≤ 8"、发货重量 ≤ 20 lb；</li>
 *   <li>大件：最长边 ≤ 59"、次长边 ≤ 33"、最短边 ≤ 33"、长度 + 围长 ≤ 130"、发货重量 ≤ 50 lb；其余为超大件。</li>
 * </ul>
 * 大号标准及以上的发货重量 = max(单件重量, 体积重 = 长 × 宽 × 高(英寸) ÷ 139)，按档位向上取整。
 * 费率取 2024 年 2 月起的美国站标准费率（售价 10–50 美元），售价低于 10 美元按低价 FBA 减 0.77 美元；
 * 旺季附加费、仓储费、入库配置费不在其中。亚马逊每年调整费率，这里的数值需按卖家中心最新费率表核对，必要时在试算中手填配送费。
 */
public final class FbaFeeEstimator {

    static final double CM_PER_INCH = 2.54;
    static final double LB_PER_KG = 2.20462;
    /** 低价 FBA 的售价上限（美元）与减免 */
    static final double LOW_PRICE_LIMIT = 10;
    static final double LOW_PRICE_DISCOUNT = 0.77;

    /** 小号标准：重量上限（oz）与费用 */
    private static final double[][] SMALL = {{2, 3.22}, {4, 3.40}, {6, 3.58}, {8, 3.77}, {10, 3.96}, {12, 4.15}, {14, 4.33}, {16, 4.52}};
    /** 大号标准 3 lb 以内：重量上限（lb）与费用 */
    private static final double[][] LARGE = {{0.25, 3.86}, {0.5, 4.08}, {0.75, 4.24}, {1, 4.75}, {1.25, 5.40}, {1.5, 5.69}, {1.75, 6.10},
            {2, 6.39}, {2.25, 6.66}, {2.5, 6.95}, {2.75, 7.21}, {3, 7.48}};
    /** 大号标准 3–20 lb：3 lb 的费用 + 超出部分每 0.5 lb */
    private static final double LARGE_OVER_BASE = 7.48;
    private static final double LARGE_OVER_STEP = 0.16;

    /**
     * 估算结果
     *
     * @param shippingWeightLb 计费的发货重量（lb）
     * @param fee              配送费（美元）
     */
    public record Estimate(DevFbaSizeTier tier, double shippingWeightLb, double fee) {
    }

    private FbaFeeEstimator() {
    }

    /**
     * @param price 售价（美元），用于低价 FBA 判断，可为 0
     * @return 尺寸或重量缺失时返回 null
     */
    public static Estimate estimate(Double lengthCm, Double widthCm, Double heightCm, Double weightKg, double price) {
        if (lengthCm == null || widthCm == null || heightCm == null || weightKg == null || weightKg <= 0) {
            return null;
        }
        double[] d = {lengthCm / CM_PER_INCH, widthCm / CM_PER_INCH, heightCm / CM_PER_INCH};
        Arrays.sort(d);
        double shortest = d[0];
        double median = d[1];
        double longest = d[2];
        double unitLb = weightKg * LB_PER_KG;
        double dimLb = longest * median * shortest / 139.0;
        double shipLb = Math.max(unitLb, dimLb);
        double fee;
        DevFbaSizeTier tier;
        if (longest <= 15 && median <= 12 && shortest <= 0.75 && unitLb * 16 <= 16) {
            tier = DevFbaSizeTier.SMALL_STANDARD;
            double oz = unitLb * 16;
            fee = SMALL[SMALL.length - 1][1];
            for (double[] row : SMALL) {
                if (oz <= row[0] + 1e-9) {
                    fee = row[1];
                    break;
                }
            }
            shipLb = unitLb;
        } else if (longest <= 18 && median <= 14 && shortest <= 8 && shipLb <= 20) {
            tier = DevFbaSizeTier.LARGE_STANDARD;
            if (shipLb <= 3 + 1e-9) {
                fee = LARGE[LARGE.length - 1][1];
                for (double[] row : LARGE) {
                    if (shipLb <= row[0] + 1e-9) {
                        fee = row[1];
                        break;
                    }
                }
            } else {
                fee = LARGE_OVER_BASE + Math.ceil((shipLb - 3) / 0.5 - 1e-9) * LARGE_OVER_STEP;
            }
        } else if (longest <= 59 && median <= 33 && shortest <= 33 && longest + 2 * (median + shortest) <= 130 && shipLb <= 50) {
            tier = DevFbaSizeTier.LARGE_BULKY;
            fee = 9.61 + Math.max(0, Math.ceil(shipLb - 1 - 1e-9)) * 0.38;
        } else {
            tier = DevFbaSizeTier.EXTRA_LARGE;
            if (shipLb <= 50) {
                fee = 26.33 + Math.max(0, Math.ceil(shipLb - 1 - 1e-9)) * 0.38;
            } else if (shipLb <= 70) {
                fee = 40.12 + Math.max(0, Math.ceil(shipLb - 51 - 1e-9)) * 0.75;
            } else if (shipLb <= 150) {
                fee = 54.81 + Math.max(0, Math.ceil(shipLb - 71 - 1e-9)) * 0.75;
            } else {
                fee = 194.95 + Math.max(0, Math.ceil(shipLb - 151 - 1e-9)) * 0.19;
            }
        }
        if (price > 0 && price < LOW_PRICE_LIMIT) {
            fee = Math.max(0, fee - LOW_PRICE_DISCOUNT);
        }
        return new Estimate(tier, Math.round(shipLb * 100) / 100.0, Math.round(fee * 100) / 100.0);
    }
}
