package com.lemon.erp.modules.ecom.automation.trigger;

import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoScanMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.publish.service.EcomCompetitorPriceService;
import com.lemon.erp.modules.ecom.service.EcomListingService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 在线商品触发器（每日扫描）：
 * <ul>
 *   <li>退货率异常：近 30 天各店铺各 SKU 的退货退款件数 ÷ 下单件数 × 100（有退货的 SKU），阈值在条件中设置（可同时限定下单件数，避免小样本）；</li>
 *   <li>价格被竞品低于：在售商品最新的竞品价格（72 小时内的快照）低于本店售价的，字段「低于本店（%）」= (售价 − 竞品最低价) ÷ 售价 × 100。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ListingTriggerSource implements AutoTriggerSource {

    static final int RETURN_DAYS = 30;

    private final EcomAutoScanMapper scanMapper;
    private final EcomStoreService storeService;
    private final EcomListingService listingService;

    @Override
    public Map<AutoTrigger, List<AutoField>> fields() {
        Map<AutoTrigger, List<AutoField>> map = new EnumMap<>(AutoTrigger.class);
        List<AutoField> ret = new ArrayList<>(List.of(AutoField.text("sku", "DM-EARBUD-BK"), AutoField.text("title", "Wireless Earbuds")));
        ret.addAll(List.of(AutoField.store()));
        ret.addAll(List.of(AutoField.number("orderedQty", 120), AutoField.number("returnedQty", 14), AutoField.number("returnRate", new BigDecimal("11.67")),
                AutoField.number("orders", 110), AutoField.number("returns", 13)));
        map.put(AutoTrigger.RETURN_RATE_HIGH, ret);
        List<AutoField> price = new ArrayList<>(List.of(AutoField.text("sku", "DM-EARBUD-BK"), AutoField.text("title", "Wireless Earbuds"),
                AutoField.text("itemId", "B0DEMO1234")));
        price.addAll(List.of(AutoField.store()));
        price.addAll(List.of(AutoField.number("price", new BigDecimal("29.99")), AutoField.number("competitorPrice", new BigDecimal("26.99")),
                AutoField.number("gap", new BigDecimal("3.00")), AutoField.number("gapPercent", new BigDecimal("10.00")),
                AutoField.number("buyboxPrice", new BigDecimal("27.49")), AutoField.bool("buyboxWinner", false), AutoField.number("offerCount", 5),
                AutoField.text("competitor", "AudioPro Store"), AutoField.text("currency", "USD")));
        map.put(AutoTrigger.COMPETITOR_PRICE_LOWER, price);
        return map;
    }

    @Override
    public List<AutoEvent> recent(AutoTrigger trigger, int limit) {
        return scan(trigger).stream().limit(limit).toList();
    }

    @Override
    public List<AutoEvent> scan(AutoTrigger trigger) {
        return trigger == AutoTrigger.RETURN_RATE_HIGH ? returns() : competitors();
    }

    private List<AutoEvent> returns() {
        LocalDateTime since = LocalDateTime.now().minusDays(RETURN_DAYS);
        Map<String, Map<String, Object>> ordered = new HashMap<>();
        for (Map<String, Object> r : scanMapper.selectSkuOrdered(since)) {
            ordered.put(AutoRows.id(r, "storeId") + "|" + AutoRows.str(r, "sku"), r);
        }
        Map<Long, EcomStore> stores = storeService.all().stream().collect(Collectors.toMap(EcomStore::getId, Function.identity(), (a, b) -> a));
        Map<Long, List<Map<String, Object>>> byStore = new LinkedHashMap<>();
        for (Map<String, Object> r : scanMapper.selectSkuReturned(since)) {
            if (ordered.containsKey(AutoRows.id(r, "storeId") + "|" + AutoRows.str(r, "sku"))) {
                byStore.computeIfAbsent(AutoRows.id(r, "storeId"), k -> new ArrayList<>()).add(r);
            }
        }
        List<AutoEvent> out = new ArrayList<>();
        byStore.forEach((storeId, rows) -> {
            EcomStore s = stores.get(storeId);
            Map<String, EcomListing> listings = listingService.byStoreSkus(storeId, rows.stream().map(r -> AutoRows.str(r, "sku")).toList());
            for (Map<String, Object> r : rows) {
                String sku = AutoRows.str(r, "sku");
                Map<String, Object> o = ordered.get(storeId + "|" + sku);
                BigDecimal orderedQty = AutoRows.num(o, "qty");
                BigDecimal returnedQty = AutoRows.num(r, "qty");
                EcomListing l = listings.get(sku);
                out.add(AutoEvent.builder(AutoTrigger.RETURN_RATE_HIGH, "SKU:" + storeId + ":" + sku).label(sku)
                        .store(storeId, s == null ? null : s.getName(), s == null ? null : s.getPlatform(), s == null ? null : s.getMarketplace())
                        .skus(List.of(sku))
                        .field("sku", sku)
                        .field("title", l == null ? null : l.getTitle())
                        .field("orderedQty", orderedQty)
                        .field("returnedQty", returnedQty)
                        .field("returnRate", AutoRows.percent(returnedQty, orderedQty))
                        .field("orders", AutoRows.integer(o, "orders"))
                        .field("returns", AutoRows.integer(r, "returns"))
                        .ref(AutoRef.LISTING, l == null ? null : l.getId())
                        .ref(AutoRef.MATERIAL, l != null && l.getMaterialId() != null ? l.getMaterialId() : AutoRows.id(r, "materialId"))
                        .link("/ecom/return")
                        .build());
            }
        });
        return out;
    }

    private List<AutoEvent> competitors() {
        List<AutoEvent> out = new ArrayList<>();
        LocalDateTime since = LocalDateTime.now().minusHours(EcomCompetitorPriceService.FRESH_HOURS);
        for (Map<String, Object> r : scanMapper.selectCompetitorLower(since)) {
            Long listingId = AutoRows.id(r, "listingId");
            String sku = AutoRows.str(r, "sku");
            BigDecimal price = AutoRows.num(r, "price");
            BigDecimal comp = AutoRows.num(r, "competitorPrice");
            BigDecimal gap = price.subtract(comp);
            out.add(AutoEvent.builder(AutoTrigger.COMPETITOR_PRICE_LOWER, "LISTING:" + listingId).label(sku)
                    .store(AutoRows.id(r, "storeId"), AutoRows.str(r, "storeName"), AutoRows.platform(r, "platform"), AutoRows.str(r, "site"))
                    .skus(List.of(sku))
                    .field("sku", sku)
                    .field("title", AutoRows.str(r, "title"))
                    .field("itemId", AutoRows.str(r, "itemId"))
                    .field("price", AutoRows.scale(price, 2))
                    .field("competitorPrice", AutoRows.scale(comp, 2))
                    .field("gap", AutoRows.scale(gap, 2))
                    .field("gapPercent", AutoRows.percent(gap, price))
                    .field("buyboxPrice", AutoRows.scale(AutoRows.num(r, "buyboxPrice"), 2))
                    .field("buyboxWinner", AutoRows.flag(r, "buyboxWinner"))
                    .field("offerCount", AutoRows.integer(r, "offerCount"))
                    .field("competitor", AutoRows.str(r, "competitor"))
                    .field("currency", AutoRows.str(r, "currency"))
                    .ref(AutoRef.LISTING, listingId)
                    .ref(AutoRef.MATERIAL, AutoRows.id(r, "materialId"))
                    .link("/ecom/competitor-price")
                    .build());
        }
        return out;
    }
}
