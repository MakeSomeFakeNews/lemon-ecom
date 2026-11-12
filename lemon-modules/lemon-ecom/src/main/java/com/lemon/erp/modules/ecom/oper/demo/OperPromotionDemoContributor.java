package com.lemon.erp.modules.ecom.oper.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.demo.DemoContributor;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.oper.entity.EcomPromotion;
import com.lemon.erp.modules.ecom.oper.entity.EcomPromotionItem;
import com.lemon.erp.modules.ecom.oper.mapper.EcomOperReportMapper;
import com.lemon.erp.modules.ecom.oper.mapper.EcomPromotionMapper;
import com.lemon.erp.modules.ecom.oper.model.EcomPromoPushStatus;
import com.lemon.erp.modules.ecom.oper.service.EcomPromotionService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 演示数据 · 促销（order 152）：各店铺按平台安排不同类型、不同状态的促销——已结束（放在参与 SKU 销量最高的一段时间，效果分析能看到明显增长）、 // i18n-ignore 演示数据存库
 * 进行中、未开始、草稿、已取消；虾皮折扣活动标记为已推送（平台活动 ID 为演示值），店铺满减不登记 SKU（按全店统计）。 // i18n-ignore 演示数据存库
 * 活动费用按平台常见的收费填写（秒杀费、优惠券费） // i18n-ignore 演示数据存库
 */
@Component
@RequiredArgsConstructor
public class OperPromotionDemoContributor implements DemoContributor {

    static final String COUPON = "COUPON";
    static final String LIGHTNING_DEAL = "LIGHTNING_DEAL";
    static final String PRIME_DISCOUNT = "PRIME_DISCOUNT";
    static final String SHOPEE_DISCOUNT = "SHOPEE_DISCOUNT";
    static final String TIKTOK_FLASH_SALE = "TIKTOK_FLASH_SALE";
    static final String FULL_REDUCTION = "FULL_REDUCTION";
    static final String PRICE_DISCOUNT = "PRICE_DISCOUNT";

    private final EcomStoreService storeService;
    private final EcomPromotionService promotionService;
    private final EcomPromotionMapper promotionMapper;
    private final EcomOperReportMapper reportMapper;
    private final ExchangeRateProvider exchangeRateProvider;

    @Override
    public int order() {
        return 152;
    }

    @Override
    public String key() {
        return "oper-promotion";
    }

    @Override
    public void generate(DemoContext ctx) {
        Map<String, EcomStore> stores = OperDemoSupport.stores(ctx, storeService);
        LocalDate today = ctx.today();
        for (EcomStore s : stores.values()) {
            switch (s.getCode()) {
                case OperDemoSupport.AMZ_US -> {
                    past(ctx, s, "黑五预热 Lightning Deal", LIGHTNING_DEAL, 2, 20, 150, 3); // i18n-ignore 演示数据存库
                    past(ctx, s, "Prime 会员日折扣", PRIME_DISCOUNT, 4, 15, 0, 2); // i18n-ignore 演示数据存库
                    ongoing(s, "11 月优惠券 15% off", COUPON, today.minusDays(3), today.plusDays(4), 15, 60, 3); // i18n-ignore 演示数据存库
                    upcoming(s, "黑色星期五 Lightning Deal", LIGHTNING_DEAL, today.plusDays(14), 2, 25, 300, 3, true); // i18n-ignore 演示数据存库
                    upcoming(s, "网络星期一 Prime 专享", PRIME_DISCOUNT, today.plusDays(18), 2, 20, 0, 2, false); // i18n-ignore 演示数据存库
                }
                case OperDemoSupport.AMZ_DE -> {
                    past(ctx, s, "Herbst-Coupon 10%", COUPON, 7, 10, 40, 2);
                    upcoming(s, "Black Friday Blitzangebot", LIGHTNING_DEAL, today.plusDays(13), 2, 20, 120, 2, true);
                    cancelled(s, "Oktober Prime Rabatt", PRIME_DISCOUNT, today.minusDays(25), 3, 2);
                }
                case OperDemoSupport.AMZ_JP -> {
                    past(ctx, s, "タイムセール", LIGHTNING_DEAL, 1, 20, 9000, 2);
                    ongoing(s, "クーポン 10%OFF", COUPON, today.minusDays(1), today.plusDays(6), 10, 3000, 2);
                }
                case OperDemoSupport.SHP_MY -> {
                    Long id = past(ctx, s, "10.10 Mega Sale 折扣", SHOPEE_DISCOUNT, 3, 18, 0, 3); // i18n-ignore 演示数据存库
                    pushed(id, "60" + (100000 + s.getId()));
                    Long id2 = upcoming(s, "11.11 大促折扣活动", SHOPEE_DISCOUNT, today.plusDays(2), 3, 22, 0, 3, true); // i18n-ignore 演示数据存库
                    pushed(id2, "60" + (200000 + s.getId()));
                }
                case OperDemoSupport.SHP_SG -> past(ctx, s, "Payday Sale", SHOPEE_DISCOUNT, 3, 12, 0, 2);
                case OperDemoSupport.TT_US -> {
                    past(ctx, s, "Flash Sale 限时闪购", TIKTOK_FLASH_SALE, 2, 30, 0, 2); // i18n-ignore 演示数据存库
                    upcoming(s, "Live 直播间闪购", TIKTOK_FLASH_SALE, today.plusDays(6), 1, 35, 0, 2, false); // i18n-ignore 演示数据存库
                }
                case OperDemoSupport.SHOPIFY -> {
                    storeWide(s, "满 $99 减 $15", today.minusDays(40), 7, BigDecimal.valueOf(99), BigDecimal.valueOf(15)); // i18n-ignore 演示数据存库
                    storeWide(s, "满 $59 减 $8", today.minusDays(2), 7, BigDecimal.valueOf(59), BigDecimal.valueOf(8)); // i18n-ignore 演示数据存库
                }
                case OperDemoSupport.EBAY_US -> upcoming(s, "Weekend Markdown", PRICE_DISCOUNT, today.plusDays(9), 3, 10, 0, 2, false);
                case OperDemoSupport.TEMU_US -> cancelled(s, "Temu 秒杀报名（未通过）", LIGHTNING_DEAL, today.minusDays(12), 2, 1); // i18n-ignore 演示数据存库
                default -> {
                }
            }
        }
    }

    /**
     * 已结束的活动：放在参与 SKU 最近 100 天里销量最高的一段（前后各留同等天数），已生效 // i18n-ignore 演示数据存库
     */
    private Long past(DemoContext ctx, EcomStore s, String name, String type, int days, double discount, double fee, int skuCount) {
        LocalDate today = ctx.today();
        List<Map<String, Object>> top = topSkus(s, today, skuCount);
        LocalDate start = bestWindow(s, top, today, days);
        return create(s, name, type, start.atTime(0, 0), start.plusDays(days).atTime(0, 0).minusSeconds(1), discount, fee, top, true);
    }

    private Long ongoing(EcomStore s, String name, String type, LocalDate start, LocalDate end, double discount, double fee, int skuCount) {
        return create(s, name, type, start.atTime(0, 0), end.atTime(23, 59, 59), discount, fee, topSkus(s, LocalDate.now(), skuCount), true);
    }

    private Long upcoming(EcomStore s, String name, String type, LocalDate start, int days, double discount, double fee, int skuCount, boolean active) {
        return create(s, name, type, start.atTime(0, 0), start.plusDays(days).atTime(23, 59, 59), discount, fee, topSkus(s, LocalDate.now(), skuCount), active);
    }

    private void cancelled(EcomStore s, String name, String type, LocalDate start, int days, int skuCount) {
        Long id = create(s, name, type, start.atTime(0, 0), start.plusDays(days).atTime(23, 59, 59), 15, 0, topSkus(s, LocalDate.now(), skuCount), false);
        promotionService.cancel(id);
    }

    private void storeWide(EcomStore s, String name, LocalDate start, int days, BigDecimal threshold, BigDecimal reduce) {
        EcomPromotion p = base(s, name, FULL_REDUCTION, start.atTime(0, 0), start.plusDays(days).atTime(23, 59, 59), 0);
        p.setThresholdAmount(threshold);
        p.setReduceAmount(reduce);
        p.setItems(List.of());
        Long id = promotionService.create(p);
        promotionService.confirm(id);
    }

    private Long create(EcomStore s, String name, String type, LocalDateTime start, LocalDateTime end, double discount, double fee,
                        List<Map<String, Object>> skus, boolean active) {
        EcomPromotion p = base(s, name, type, start, end, fee);
        p.setDiscountPercent(BigDecimal.valueOf(discount));
        List<EcomPromotionItem> items = new ArrayList<>();
        for (Map<String, Object> r : skus) {
            EcomPromotionItem it = new EcomPromotionItem();
            it.setSellerSku(String.valueOf(r.get("sellerSku")));
            it.setTitle(Objects.toString(r.get("title"), null));
            it.setPromoQty(50 + items.size() * 30);
            it.setLimitPerBuyer(type.equals(LIGHTNING_DEAL) || type.equals(TIKTOK_FLASH_SALE) ? 2 : null);
            items.add(it);
        }
        p.setItems(items);
        Long id = promotionService.create(p);
        if (active) {
            promotionService.confirm(id);
        }
        return id;
    }

    private EcomPromotion base(EcomStore s, String name, String type, LocalDateTime start, LocalDateTime end, double fee) {
        EcomPromotion p = new EcomPromotion();
        p.setName(name);
        p.setStoreId(s.getId());
        p.setPromoType(type);
        p.setStartTime(start);
        p.setEndTime(end);
        // 没有维护汇率时不填费用（活动费用要折算本位币）
        boolean hasRate = exchangeRateProvider.rate(s.getCurrency(), start.toLocalDate()) != null;
        p.setFeeAmount(hasRate ? BigDecimal.valueOf(fee) : BigDecimal.ZERO);
        p.setOwnerId(s.getOwnerId());
        p.setRemark("演示促销"); // i18n-ignore 演示数据存库
        return p;
    }

    private void pushed(Long id, String platformId) {
        promotionMapper.update(Wrappers.<EcomPromotion>lambdaUpdate().set(EcomPromotion::getPushStatus, EcomPromoPushStatus.PUSHED)
                .set(EcomPromotion::getPlatformPromoId, platformId).set(EcomPromotion::getPushTime, LocalDateTime.now().minusDays(1))
                .eq(EcomPromotion::getId, id));
    }

    private List<Map<String, Object>> topSkus(EcomStore s, LocalDate today, int n) {
        return reportMapper.selectTopSkus(s.getId(), today.minusDays(100).atStartOfDay(), today.plusDays(1).atStartOfDay(), n);
    }

    /** 参与 SKU 合计销量最高的连续 days 天（前后各留 days 天，且已经结束），没有销量时取 30 天前 */ // i18n-ignore 演示数据存库
    private LocalDate bestWindow(EcomStore s, List<Map<String, Object>> top, LocalDate today, int days) {
        LocalDate fallback = today.minusDays(30L + days);
        if (top.isEmpty()) {
            return fallback;
        }
        LocalDate begin = today.minusDays(100);
        TreeMap<LocalDate, BigDecimal> daily = new TreeMap<>();
        for (Map<String, Object> r : promotionMapper.selectSkuDaily(s.getId(), top.stream().map(x -> String.valueOf(x.get("sellerSku"))).toList(),
                begin.atStartOfDay(), today.atStartOfDay())) {
            Object d = r.get("day");
            LocalDate day = d instanceof java.sql.Date sd ? sd.toLocalDate() : d instanceof LocalDate ld ? ld : LocalDate.parse(String.valueOf(d).substring(0, 10));
            daily.put(day, new BigDecimal(String.valueOf(r.get("qty"))));
        }
        LocalDate best = fallback;
        BigDecimal bestSum = BigDecimal.valueOf(-1);
        for (LocalDate start = begin.plusDays(days); !start.plusDays(2L * days).isAfter(today.minusDays(1)); start = start.plusDays(1)) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = 0; i < days; i++) {
                sum = sum.add(daily.getOrDefault(start.plusDays(i), BigDecimal.ZERO));
            }
            if (sum.compareTo(bestSum) > 0) {
                bestSum = sum;
                best = start;
            }
        }
        return best;
    }
}
