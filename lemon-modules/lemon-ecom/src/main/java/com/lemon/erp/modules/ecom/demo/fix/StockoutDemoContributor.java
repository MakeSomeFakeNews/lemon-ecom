package com.lemon.erp.modules.ecom.demo.fix;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.modules.ecom.analytics.service.EcomForecastService;
import com.lemon.erp.modules.ecom.analytics.service.EcomStockoutService;
import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.demo.DemoContributor;
import com.lemon.erp.modules.ecom.demo.DemoKeys;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 演示数据 · 断货日（order 171）：亚马逊美国 / 德国站销量最高的 3 个 SKU，在 10–60 天前销量最低的连续 5 天（像是 FBA 断货期间）
 * 把库存快照记为断货（可售 0），随后重新生成这些 SKU 的预测——销量预测详情中标出断货日并按前后正常日补偿，补货建议的加权日均排除断货天数
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockoutDemoContributor implements DemoContributor {

    static final int SPELL = 5;
    private static final String[] STORES = {"DEMO-AMZ-US", "DEMO-AMZ-DE"};

    private final EcomStoreService storeService;
    private final EcomListingMapper listingMapper;
    private final EcomStockoutService stockoutService;
    private final EcomForecastService forecastService;

    @Override
    public int order() {
        return 171;
    }

    @Override
    public String key() {
        return "stockout";
    }

    @Override
    public void generate(DemoContext ctx) {
        Map<String, Long> stores = ctx.get(DemoKeys.STORE_BY_CODE);
        LocalDate today = ctx.today();
        for (String code : STORES) {
            Long storeId = stores == null ? null : stores.get(code);
            EcomStore store = storeId == null ? storeService.findByCode(code) : storeService.get(storeId);
            if (store == null) {
                continue;
            }
            List<EcomListing> listings = listingMapper.selectList(Wrappers.<EcomListing>lambdaQuery().eq(EcomListing::getStoreId, store.getId())
                    .eq(EcomListing::getStatus, ListingStatus.ACTIVE).isNotNull(EcomListing::getMaterialId));
            // 最近 60 天销量最高的 3 个
            List<Candidate> ranked = new ArrayList<>();
            for (EcomListing l : listings) {
                Map<LocalDate, BigDecimal> sales = stockoutService.dailySales(l.getId(), today.minusDays(60), today);
                double total = sales.values().stream().mapToDouble(BigDecimal::doubleValue).sum();
                if (total > 0) {
                    ranked.add(new Candidate(l, sales, total));
                }
            }
            ranked.sort(Comparator.comparingDouble(Candidate::total).reversed());
            for (Candidate c : ranked.subList(0, Math.min(3, ranked.size()))) {
                List<LocalDate> spell = lowestWindow(c.sales(), today, 10, 60);
                stockoutService.markStockout(c.listing().getId(), store.getId(), c.listing().getFulfillment(), spell);
                try {
                    forecastService.regenerate(c.listing().getId());
                } catch (RuntimeException e) {
                    log.warn("Demo stockout forecast failed for {}: {}", c.listing().getSellerSku(), e.getMessage());
                }
            }
        }
    }

    record Candidate(EcomListing listing, Map<LocalDate, BigDecimal> sales, double total) {
    }

    /** [today − to, today − from] 内销量合计最低的连续 SPELL 天 */
    static List<LocalDate> lowestWindow(Map<LocalDate, BigDecimal> sales, LocalDate today, int from, int to) {
        LocalDate best = today.minusDays(from + SPELL);
        double bestSum = Double.MAX_VALUE;
        for (int ago = to; ago >= from + SPELL; ago--) {
            LocalDate start = today.minusDays(ago);
            double sum = 0;
            for (int i = 0; i < SPELL; i++) {
                BigDecimal v = sales.get(start.plusDays(i));
                sum += v == null ? 0 : v.doubleValue();
            }
            if (sum < bestSum) {
                bestSum = sum;
                best = start;
            }
        }
        List<LocalDate> out = new ArrayList<>();
        for (int i = 0; i < SPELL; i++) {
            out.add(best.plusDays(i));
        }
        return out;
    }
}
