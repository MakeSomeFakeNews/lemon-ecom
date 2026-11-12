package com.lemon.erp.modules.ecom.automation.trigger;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.dto.PurchaseSuggestRow;
import com.lemon.erp.modules.ecom.dto.ReplenishRow;
import com.lemon.erp.modules.ecom.dto.ReplenishWeights;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.service.EcomReplenishService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 库存触发器（每日扫描，复用补货建议的计算）：
 * <ul>
 *   <li>断货风险：平台仓补货建议中有销量的在线商品，字段「可售天数」= (平台仓库存 + 在途) ÷ 日均，阈值在条件中设置（如 &lt; 15）；</li>
 *   <li>低于安全库存：本地仓采购建议中本地库存低于安全库存（日均 × 采购周期 + 物料安全库存）或物料安全库存的物料。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class StockTriggerSource implements AutoTriggerSource {

    private final EcomReplenishService replenishService;
    private final EcomStoreService storeService;

    @Override
    public Map<AutoTrigger, List<AutoField>> fields() {
        Map<AutoTrigger, List<AutoField>> map = new EnumMap<>(AutoTrigger.class);
        List<AutoField> risk = new ArrayList<>(List.of(AutoField.text("sku", "DM-EARBUD-BK"), AutoField.text("title", "Wireless Earbuds")));
        risk.addAll(List.of(AutoField.store()));
        risk.addAll(List.of(AutoField.text("materialCode", "DM-EARBUD-BK"), AutoField.text("materialName", "Wireless Earbuds Black"),
                AutoField.number("coverDays", new BigDecimal("12.5")), AutoField.number("dailySales", new BigDecimal("8.2")),
                AutoField.number("platformQty", 80), AutoField.number("inTransitQty", 22), AutoField.number("leadDays", 30),
                AutoField.number("suggestQty", 260), AutoField.text("stockoutDate", "2026-11-25"),
                AutoField.enumOf("level", "ecom_replenish_level", "URGENT")));
        map.put(AutoTrigger.STOCKOUT_RISK, risk);
        map.put(AutoTrigger.STOCK_LOW, List.of(AutoField.text("materialCode", "DM-EARBUD-BK"), AutoField.text("materialName", "Wireless Earbuds Black"),
                AutoField.number("localQty", 40), AutoField.number("safetyStock", 50), AutoField.number("safetyQty", 300),
                AutoField.number("gapQty", 260), AutoField.number("coverDays", new BigDecimal("4.8")), AutoField.number("dailySales", new BigDecimal("8.2")),
                AutoField.number("purchaseTransitQty", 0), AutoField.number("suggestQty", 400), AutoField.text("supplierName", "Shenzhen Audio Co."),
                AutoField.text("skus", "DM-EARBUD-BK"), AutoField.enumOf("level", "ecom_replenish_level", "URGENT")));
        return map;
    }

    @Override
    public List<AutoEvent> recent(AutoTrigger trigger, int limit) {
        return scan(trigger).stream().limit(limit).toList();
    }

    @Override
    public List<AutoEvent> scan(AutoTrigger trigger) {
        return trigger == AutoTrigger.STOCKOUT_RISK ? risks() : lows();
    }

    private List<AutoEvent> risks() {
        Map<Long, EcomStore> stores = storeService.all().stream().collect(Collectors.toMap(EcomStore::getId, Function.identity(), (a, b) -> a));
        List<AutoEvent> out = new ArrayList<>();
        for (ReplenishRow r : replenishService.rows(null, null, null)) {
            if (r.isDisabled() || r.getCoverDays() == null || r.getDailySales() == null || r.getDailySales().signum() <= 0) {
                continue;
            }
            EcomStore s = stores.get(r.getStoreId());
            out.add(AutoEvent.builder(AutoTrigger.STOCKOUT_RISK, "LISTING:" + r.getListingId()).label(r.getSellerSku())
                    .store(r.getStoreId(), r.getStoreName(), r.getPlatform(), s == null ? null : s.getMarketplace())
                    .skus(List.of(r.getSellerSku()))
                    .field("sku", r.getSellerSku())
                    .field("title", r.getTitle())
                    .field("materialCode", r.getMaterialCode())
                    .field("materialName", r.getMaterialName())
                    .field("coverDays", r.getCoverDays())
                    .field("dailySales", r.getDailySales())
                    .field("platformQty", r.getPlatformQty())
                    .field("inTransitQty", r.getInTransitQty())
                    .field("leadDays", r.getLeadDays())
                    .field("suggestQty", r.getSuggestQty())
                    .field("stockoutDate", r.getStockoutDate() == null ? null : r.getStockoutDate().toString())
                    .field("level", r.getLevel() == null ? null : r.getLevel().name())
                    .ref(AutoRef.LISTING, r.getListingId())
                    .ref(AutoRef.MATERIAL, r.getMaterialId())
                    .link("/ecom/replenish" + (r.getLevel() == null ? "" : "?level=" + r.getLevel().name()))
                    .build());
        }
        return out;
    }

    private List<AutoEvent> lows() {
        List<AutoEvent> out = new ArrayList<>();
        for (PurchaseSuggestRow r : replenishService.purchaseRows(null, null, ReplenishWeights.DEFAULT)) {
            BigDecimal local = Decimals.nz(r.getLocalQty());
            BigDecimal safety = Decimals.nz(r.getLocalSafetyQty()).max(Decimals.nz(r.getSafetyStock()));
            if (r.isDisabled() || safety.signum() <= 0 || local.compareTo(safety) >= 0) {
                continue;
            }
            List<String> skus = r.getDemands().stream().map(PurchaseSuggestRow.Demand::sellerSku).distinct().toList();
            out.add(AutoEvent.builder(AutoTrigger.STOCK_LOW, "MATERIAL:" + r.getMaterialId()).label(r.getMaterialCode() + " " + r.getMaterialName())
                    .skus(skus)
                    .field("materialCode", r.getMaterialCode())
                    .field("materialName", r.getMaterialName())
                    .field("localQty", local)
                    .field("safetyStock", r.getSafetyStock())
                    .field("safetyQty", safety)
                    .field("gapQty", safety.subtract(local))
                    .field("coverDays", r.getLocalCoverDays())
                    .field("dailySales", r.getDailySales())
                    .field("purchaseTransitQty", r.getPurchaseTransitQty())
                    .field("suggestQty", r.getSuggestQty())
                    .field("supplierName", r.getSupplierName())
                    .field("skus", skus)
                    .field("level", r.getLevel() == null ? null : r.getLevel().name())
                    .ref(AutoRef.MATERIAL, r.getMaterialId())
                    .link("/ecom/replenish?tab=purchase")
                    .build());
        }
        return out;
    }
}
