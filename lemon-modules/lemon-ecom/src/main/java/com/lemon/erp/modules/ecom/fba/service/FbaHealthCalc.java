package com.lemon.erp.modules.ecom.fba.service;

import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaStorageRate;
import com.lemon.erp.modules.ecom.fba.model.FbaHealthAdvice;
import com.lemon.erp.modules.ecom.fba.model.FbaSizeCategory;
import com.lemon.erp.modules.ecom.fba.model.FbaStorageFeeType;
import com.lemon.erp.modules.ecom.fba.model.FbaVolumeUnit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 库存健康计算（纯计算）：库龄分段（按头程签收先进先出推算）、日均销量与可售天数、冗余、预估仓储费、处理建议
 */
public final class FbaHealthCalc {

    /** 库龄分段的起始天数：0–90、91–180、181–270、271–365、366 以上 */
    static final int[] BUCKET_START = {0, 91, 181, 271, 366};
    /** 超过 180 天的库存（第 3 段起）计为超龄 */
    static final int AGED_FROM_BUCKET = 2;
    /** 日均销量的统计天数 */
    public static final int VELOCITY_DAYS = 30;
    private static final int SCALE = 4;

    private FbaHealthCalc() {
    }

    /** 一笔签收：日期与件数 */
    public record Lot(LocalDate date, BigDecimal qty) {
    }

    /**
     * 按签收先进先出推算库龄：当前库存是最近签收的那些，从最新的签收往前累加到等于库存为止；
     * 签收记录不够的部分按最早一笔签收的日期计（没有任何签收记录时计入 0–90 天）
     *
     * @param lots 签收记录，按日期倒序
     * @return 5 个分段的件数
     */
    public static BigDecimal[] fifoBuckets(BigDecimal onHand, List<Lot> lots, LocalDate today) {
        BigDecimal[] buckets = zeros();
        BigDecimal remaining = onHand == null ? BigDecimal.ZERO : onHand.max(BigDecimal.ZERO);
        LocalDate oldest = null;
        for (Lot lot : lots) {
            if (remaining.signum() <= 0) {
                break;
            }
            if (lot.qty() == null || lot.qty().signum() <= 0 || lot.date() == null) {
                continue;
            }
            BigDecimal take = lot.qty().min(remaining);
            int b = bucket(days(lot.date(), today));
            buckets[b] = buckets[b].add(take);
            remaining = remaining.subtract(take);
            oldest = lot.date();
        }
        if (remaining.signum() > 0) {
            int b = oldest == null ? 0 : bucket(days(oldest, today));
            buckets[b] = buckets[b].add(remaining);
        }
        return buckets;
    }

    static int bucket(long days) {
        for (int i = BUCKET_START.length - 1; i >= 0; i--) {
            if (days >= BUCKET_START[i]) {
                return i;
            }
        }
        return 0;
    }

    private static long days(LocalDate from, LocalDate to) {
        return Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(from, to));
    }

    static BigDecimal[] zeros() {
        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
    }

    /** 行的 5 个库龄分段 */
    public static BigDecimal[] buckets(FbaViews.HealthRow r) {
        return new BigDecimal[]{nz(r.getAge0To90()), nz(r.getAge91To180()), nz(r.getAge181To270()), nz(r.getAge271To365()), nz(r.getAge365Plus())};
    }

    /**
     * 计算一行的日均、可售天数、冗余、仓储费与建议
     *
     * @param excessDays 冗余阈值：可售天数超过它的部分为冗余
     * @param quote      配送费试算结果（取尺寸类别），为空时不估算仓储费
     * @param rates      店铺站点的仓储费费率
     */
    public static void compute(FbaViews.HealthRow r, int excessDays, FbaViews.FeeQuote quote, List<EcomFbaStorageRate> rates, LocalDate today) {
        BigDecimal daily;
        if (r.getSold30() != null && r.getSold30().signum() > 0) {
            daily = r.getSold30().divide(BigDecimal.valueOf(VELOCITY_DAYS), SCALE, RoundingMode.HALF_UP);
        } else if (r.getUnitsShipped30() != null && r.getUnitsShipped30().signum() > 0) {
            daily = r.getUnitsShipped30().divide(BigDecimal.valueOf(VELOCITY_DAYS), SCALE, RoundingMode.HALF_UP);
        } else {
            daily = BigDecimal.ZERO;
        }
        r.setDailySales(daily);
        BigDecimal available = nz(r.getAvailableQty());
        r.setDaysOfSupply(daily.signum() > 0 ? available.divide(daily, 1, RoundingMode.HALF_UP) : null);
        BigDecimal excess = available.subtract(daily.multiply(BigDecimal.valueOf(excessDays))).setScale(0, RoundingMode.FLOOR);
        r.setExcessQty(excess.signum() > 0 ? excess : BigDecimal.ZERO);
        BigDecimal[] buckets = buckets(r);
        BigDecimal aged = BigDecimal.ZERO;
        for (int i = AGED_FROM_BUCKET; i < buckets.length; i++) {
            aged = aged.add(buckets[i]);
        }
        r.setAgedQty(aged);
        if (quote != null) {
            r.setSizeCategory(quote.sizeCategory());
            r.setTierName(quote.tierName());
        }
        FbaFeeCalculator.Dims dims = new FbaFeeCalculator.Dims(r.getLengthCm(), r.getWidthCm(), r.getHeightCm(), r.getWeightKg());
        r.setUnitVolume(FbaFeeCalculator.volume(dims, false));
        FbaSizeCategory category = quote == null ? null : quote.sizeCategory();
        if (category != null && dims.complete()) {
            BigDecimal onHand = available.add(nz(r.getReservedQty())).add(nz(r.getUnfulfillableQty()));
            r.setMonthlyFee(monthlyFee(onHand, dims, category, rates, today.getMonthValue()));
            r.setAgedFee(agedFee(buckets, dims, category, rates));
        }
        r.setAdvice(advice(r));
    }

    /** 月度仓储费 = 在库件数（可售 + 预留 + 不可售）× 单件体积 × 当月费率 */
    static BigDecimal monthlyFee(BigDecimal onHand, FbaFeeCalculator.Dims dims, FbaSizeCategory category, List<EcomFbaStorageRate> rates, int month) {
        EcomFbaStorageRate rate = rates.stream()
                .filter(x -> x.getFeeType() == FbaStorageFeeType.MONTHLY && x.getSizeCategory() == category)
                .filter(x -> inMonth(month, x.getMonthFrom(), x.getMonthTo()))
                .findFirst().orElse(null);
        if (rate == null) {
            return null;
        }
        BigDecimal vol = FbaFeeCalculator.volume(dims, rate.getVolumeUnit() == FbaVolumeUnit.CBM);
        return onHand.multiply(vol).multiply(rate.getRate()).setScale(2, RoundingMode.HALF_UP);
    }

    /** 库龄分段的结束天数（最后一段不限） */
    static final int[] BUCKET_END = {90, 180, 270, 365, Integer.MAX_VALUE};

    /**
     * 超龄附加费 = Σ 各分段件数 × 单件体积 × 分段费率。费率按库龄段维护（如亚马逊的 241–270、271–300…天），与分段重叠的部分按天数占比折算
     * （假设分段内的库龄均匀分布），例如 181–270 天的分段只与 241–270 天的费率重叠 30 天 → 按费率的 30 / 90 计；
     * 最后一段（365 天以上）取覆盖 366 天的费率
     */
    static BigDecimal agedFee(BigDecimal[] buckets, FbaFeeCalculator.Dims dims, FbaSizeCategory category, List<EcomFbaStorageRate> rates) {
        BigDecimal total = BigDecimal.ZERO;
        boolean any = false;
        List<EcomFbaStorageRate> aged = rates.stream()
                .filter(x -> x.getFeeType() == FbaStorageFeeType.AGED && x.getSizeCategory() == category && x.getAgeFrom() != null).toList();
        for (int i = 0; i < buckets.length; i++) {
            if (buckets[i].signum() <= 0) {
                continue;
            }
            int from = BUCKET_START[i];
            int to = BUCKET_END[i];
            for (EcomFbaStorageRate rate : aged) {
                int rateTo = rate.getAgeTo() == null ? Integer.MAX_VALUE : rate.getAgeTo();
                BigDecimal share;
                if (to == Integer.MAX_VALUE) {
                    share = rate.getAgeFrom() <= from && rateTo >= from ? BigDecimal.ONE : BigDecimal.ZERO;
                } else {
                    int overlap = Math.min(to, rateTo) - Math.max(from, rate.getAgeFrom()) + 1;
                    share = overlap <= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(overlap).divide(BigDecimal.valueOf(to - from + 1L), 6, RoundingMode.HALF_UP);
                }
                if (share.signum() == 0) {
                    continue;
                }
                any = true;
                BigDecimal vol = FbaFeeCalculator.volume(dims, rate.getVolumeUnit() == FbaVolumeUnit.CBM);
                total = total.add(buckets[i].multiply(vol).multiply(rate.getRate()).multiply(share));
            }
        }
        return any ? total.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private static boolean inMonth(int month, Integer from, Integer to) {
        int f = from == null ? 1 : from;
        int t = to == null ? 12 : to;
        return f <= t ? month >= f && month <= t : month >= f || month <= t;
    }

    /**
     * 处理建议：超过 365 天有库存，或超龄（181 天以上）且最近 30 天没有销量 → 移除 / 弃置；
     * 冗余且有超龄库存 → 降价清仓；冗余 → 站外推广；否则健康
     */
    static FbaHealthAdvice advice(FbaViews.HealthRow r) {
        boolean excess = r.getExcessQty() != null && r.getExcessQty().signum() > 0;
        boolean aged = r.getAgedQty() != null && r.getAgedQty().signum() > 0;
        if (nz(r.getAge365Plus()).signum() > 0 || (aged && nz(r.getDailySales()).signum() == 0)) {
            return FbaHealthAdvice.REMOVE;
        }
        if (excess && aged) {
            return FbaHealthAdvice.PRICE_DOWN;
        }
        if (excess) {
            return FbaHealthAdvice.PROMOTE;
        }
        return FbaHealthAdvice.HEALTHY;
    }

    /** 签收记录列表（倒序） */
    public static List<Lot> lots(List<FbaViews.ReceiptLot> rows) {
        List<Lot> list = new ArrayList<>();
        for (FbaViews.ReceiptLot r : rows) {
            list.add(new Lot(r.getReceiveDate(), r.getQty()));
        }
        list.sort((a, b) -> b.date().compareTo(a.date()));
        return list;
    }

    static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
