package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.analytics.entity.EcomForecastEvent;
import com.lemon.erp.modules.ecom.analytics.entity.EcomStockSnapshot;
import com.lemon.erp.modules.ecom.analytics.mapper.EcomStockSnapshotMapper;
import com.lemon.erp.modules.ecom.analytics.service.EcomForecastService;
import com.lemon.erp.modules.ecom.analytics.service.EcomStockSnapshotService;
import com.lemon.erp.modules.ecom.dto.PlatformStockImportRow;
import com.lemon.erp.modules.ecom.dto.PurchasePlanRequest;
import com.lemon.erp.modules.ecom.dto.ReplenishSettingRequest;
import com.lemon.erp.modules.ecom.entity.EcomFeeMapping;
import com.lemon.erp.modules.ecom.mapper.EcomDemoMapper;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ReplenishScope;
import com.lemon.erp.modules.ecom.model.SettlementItemType;
import com.lemon.erp.modules.ecom.service.EcomFeeMappingService;
import com.lemon.erp.modules.ecom.service.EcomPlatformStockService;
import com.lemon.erp.modules.ecom.service.EcomPurchasePlanService;
import com.lemon.erp.modules.ecom.service.EcomReplenishSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 演示数据 · 库存与计划（order 70）：平台仓库存报告（与系统平台仓库存并列，少量差异）、120 天的每日库存快照（按出入库单的业务日期回推）、
 * 补货设置（SKU 级手工日均 / 周期、物料级采购周期、停止补货）、销量预测（按历史销量生成，含一个大促事件）、
 * 采购计划（草稿、已审核、已下单生成采购订单）、自定义费用映射
 */
@Component
@RequiredArgsConstructor
public class DemoPlanningContributor implements DemoContributor {

    private final EcomDemoMapper demoMapper;
    private final EcomStockSnapshotMapper snapshotMapper;
    private final EcomStockSnapshotService snapshotService;
    private final EcomPlatformStockService platformStockService;
    private final EcomReplenishSettingService settingService;
    private final EcomForecastService forecastService;
    private final EcomPurchasePlanService purchasePlanService;
    private final EcomFeeMappingService feeMappingService;
    private final DemoTx tx;

    @Override
    public int order() {
        return 70;
    }

    @Override
    public String key() {
        return "planning";
    }

    @Override
    public void generate(DemoContext ctx) {
        Map<String, Long> warehouses = ctx.get(DemoContributors.WAREHOUSE_BY_CODE);
        Map<String, Long> stores = ctx.get(DemoKeys.STORE_BY_CODE);
        Map<Long, Map<String, DemoListings.Item>> listings = ctx.get(DemoContributors.LISTINGS);
        Long localWh = ctx.get(DemoKeys.LOCAL_WAREHOUSE_ID);
        StockHistory history = new StockHistory(demoMapper.selectDailyStockChange(warehouses.values()));
        tx.run("snapshots", () -> snapshots(ctx, stores, listings, warehouses, localWh, history));
        tx.run("snapshotToday", () -> snapshotService.record(ctx.today()));
        tx.run("platformStock", () -> platformStock(ctx, stores, listings, warehouses, history));
        tx.run("replenishSettings", () -> settings(ctx, stores, listings));
        tx.run("forecastEvent", () -> forecastEvent(ctx, stores));
        tx.run("forecast", () -> forecastService.generate(null));
        purchasePlans(ctx, localWh);
        tx.run("feeMappings", this::feeMappings);
    }

    /** 历史库存：仓库 × 物料的每日变动累计 */
    static final class StockHistory {
        private final Map<String, TreeMap<LocalDate, BigDecimal>> cumulative = new HashMap<>();

        StockHistory(List<Map<String, Object>> rows) {
            Map<String, TreeMap<LocalDate, BigDecimal>> daily = new HashMap<>();
            for (Map<String, Object> r : rows) {
                Object date = r.get("bizDate");
                if (date == null) {
                    continue;
                }
                LocalDate d = date instanceof java.sql.Date sd ? sd.toLocalDate() : date instanceof LocalDate ld ? ld : LocalDate.parse(date.toString().substring(0, 10));
                daily.computeIfAbsent(r.get("warehouseId") + "@" + r.get("materialId"), k -> new TreeMap<>())
                        .merge(d, new BigDecimal(r.get("qty").toString()), BigDecimal::add);
            }
            daily.forEach((k, m) -> {
                TreeMap<LocalDate, BigDecimal> c = new TreeMap<>();
                BigDecimal sum = BigDecimal.ZERO;
                for (Map.Entry<LocalDate, BigDecimal> e : m.entrySet()) {
                    sum = sum.add(e.getValue());
                    c.put(e.getKey(), sum);
                }
                cumulative.put(k, c);
            });
        }

        /** 某天结束时的库存（不为负） */
        BigDecimal at(Long warehouseId, Long materialId, LocalDate date) {
            TreeMap<LocalDate, BigDecimal> c = cumulative.get(warehouseId + "@" + materialId);
            Map.Entry<LocalDate, BigDecimal> e = c == null ? null : c.floorEntry(date);
            return e == null || e.getValue().signum() < 0 ? BigDecimal.ZERO : e.getValue();
        }

        /** 在线商品能凑齐的件数：各物料 ⌊库存 ÷ 每件用量⌋ 的最小值 */
        BigDecimal sets(DemoListings.Item l, Long warehouseId, LocalDate date) {
            BigDecimal min = null;
            for (Map.Entry<Long, BigDecimal> e : l.perUnit().entrySet()) {
                BigDecimal n = at(warehouseId, e.getKey(), date).divide(e.getValue(), 0, RoundingMode.DOWN);
                min = min == null || n.compareTo(min) < 0 ? n : min;
            }
            return min == null ? BigDecimal.ZERO : min;
        }
    }

    private void snapshots(DemoContext ctx, Map<String, Long> stores, Map<Long, Map<String, DemoListings.Item>> listings,
                           Map<String, Long> warehouses, Long localWh, StockHistory history) {
        List<EcomStockSnapshot> rows = new ArrayList<>();
        for (DemoCatalog.Store d : DemoCatalog.STORES) {
            Long storeId = stores.get(d.code());
            Long platformWh = d.platformWh() == null ? null : warehouses.get(d.platformWh());
            for (DemoListings.Item l : listings.get(storeId).values()) {
                if (!l.mapped()) {
                    continue;
                }
                for (int i = ctx.days(); i >= 1; i--) {
                    LocalDate date = ctx.today().minusDays(i);
                    EcomStockSnapshot s = new EcomStockSnapshot();
                    s.setSnapshotDate(date);
                    s.setListingId(l.id());
                    s.setStoreId(storeId);
                    s.setFulfillment(l.fulfillment());
                    s.setPlatformQty(platformWh == null ? BigDecimal.ZERO : history.sets(l, platformWh, date));
                    s.setLocalQty(history.sets(l, localWh, date));
                    s.setSellableQty(l.fulfillment() == FulfillmentType.PLATFORM ? s.getPlatformQty() : s.getLocalQty());
                    rows.add(s);
                    if (rows.size() >= 500) {
                        snapshotMapper.insertBatch(rows);
                        rows = new ArrayList<>();
                    }
                }
            }
        }
        if (!rows.isEmpty()) {
            snapshotMapper.insertBatch(rows);
        }
    }

    /** 平台库存报告：可售与系统平台仓库存一致，少量 SKU 有盘亏 / 盘盈差异；入库中为在途货件 */
    private void platformStock(DemoContext ctx, Map<String, Long> stores, Map<Long, Map<String, DemoListings.Item>> listings,
                               Map<String, Long> warehouses, StockHistory history) {
        for (DemoCatalog.Store d : DemoCatalog.STORES) {
            if (d.platformWh() == null) {
                continue;
            }
            Long wh = warehouses.get(d.platformWh());
            for (DemoListings.Item l : listings.get(stores.get(d.code())).values()) {
                if (!l.mapped() || l.fulfillment() != FulfillmentType.PLATFORM) {
                    continue;
                }
                BigDecimal sys = history.sets(l, wh, ctx.today());
                BigDecimal diff = ctx.chance(0.15) ? BigDecimal.valueOf(-ctx.between(1, 4)) : ctx.chance(0.04) ? BigDecimal.valueOf(ctx.between(1, 2)) : BigDecimal.ZERO;
                PlatformStockImportRow r = new PlatformStockImportRow();
                r.setStoreCode(d.code());
                r.setSellerSku(l.sku());
                r.setPlatformItemId(l.platformItemId());
                r.setFulfillableQty(sys.add(diff).max(BigDecimal.ZERO));
                r.setInboundQty(BigDecimal.valueOf(l.popularity() >= 5 ? ctx.between(20, 120) : 0));
                r.setReservedQty(BigDecimal.valueOf(ctx.between(0, 6)));
                r.setUnfulfillableQty(BigDecimal.valueOf(ctx.chance(0.2) ? ctx.between(1, 3) : 0));
                r.setSnapshotDate(ctx.today());
                platformStockService.importRow(r);
            }
        }
    }

    private void settings(DemoContext ctx, Map<String, Long> stores, Map<Long, Map<String, DemoListings.Item>> listings) {
        Map<String, Long> materials = ctx.get(DemoKeys.MATERIAL_BY_CODE);
        List<DemoListings.Item> us = listings.get(stores.get("DEMO-AMZ-US")).values().stream()
                .filter(l -> l.mapped() && l.fulfillment() == FulfillmentType.PLATFORM).toList();
        if (us.size() >= 4) {
            settingService.save(new ReplenishSettingRequest(ReplenishScope.LISTING, us.get(0).id(), null, 50, 25, null, null, BigDecimal.valueOf(1.3),
                    false, "旺季备货：销量系数 1.3")); // i18n-ignore 演示数据存库
            settingService.save(new ReplenishSettingRequest(ReplenishScope.LISTING, us.get(1).id(), null, null, null, null, BigDecimal.valueOf(6), null,
                    false, "新品推广期，按运营预估日均 6 件")); // i18n-ignore 演示数据存库
            settingService.save(new ReplenishSettingRequest(ReplenishScope.LISTING, us.get(2).id(), null, 35, 10, null, null, null, false,
                    "改走海运，补货周期缩短")); // i18n-ignore 演示数据存库
            settingService.save(new ReplenishSettingRequest(ReplenishScope.LISTING, us.get(us.size() - 1).id(), null, null, null, null, null, null, true,
                    "准备清货，停止补货")); // i18n-ignore 演示数据存库
        }
        settingService.save(new ReplenishSettingRequest(ReplenishScope.MATERIAL, null, materials.get("DM-PT07"), null, null, 20, null, null, false,
                "供应商交期 20 天")); // i18n-ignore 演示数据存库
        settingService.save(new ReplenishSettingRequest(ReplenishScope.MATERIAL, null, materials.get("DM-OD02"), null, null, 25, null, null, false,
                "大件，整柜采购")); // i18n-ignore 演示数据存库
    }

    private void forecastEvent(DemoContext ctx, Map<String, Long> stores) {
        EcomForecastEvent e = new EcomForecastEvent();
        e.setName("黑五网一大促"); // i18n-ignore 演示数据存库
        e.setStartDate(ctx.today().plusDays(14));
        e.setEndDate(ctx.today().plusDays(20));
        e.setStoreId(stores.get("DEMO-AMZ-US"));
        e.setFactor(BigDecimal.valueOf(2.2));
        e.setRemark("参考去年同期，销量约为平日的 2.2 倍"); // i18n-ignore 演示数据存库
        forecastService.saveEvent(null, e);
    }

    /** 采购计划：已下单（生成采购订单）、已审核、草稿各一张 */
    private void purchasePlans(DemoContext ctx, Long localWh) {
        Map<String, Long> materials = ctx.get(DemoKeys.MATERIAL_BY_CODE);
        Map<String, Long> suppliers = ctx.get(DemoContributors.SUPPLIER_BY_CODE);
        String[][] groups = {{"DM-PB10", "DM-CB01", "DM-GAN65", "DM-EB01"}, {"DM-HM03", "DM-HM04", "DM-HM02", "DM-PT05", "DM-PT04"},
                {"DM-OD03", "DM-OD01", "DM-OD09", "DM-PT01"}};
        String[] remarks = {"补货建议：3C 爆款补货", "补货建议：家居与宠物补货", "补货建议：户外旺季备货"}; // i18n-ignore 演示数据存库
        for (int g = 0; g < groups.length; g++) {
            List<PurchasePlanRequest.Item> items = new ArrayList<>();
            for (String code : groups[g]) {
                DemoCatalog.Product p = DemoCatalog.product(code);
                DemoCatalog.Supplier s = DemoCatalog.SUPPLIERS.get(p.supplier());
                BigDecimal qty = BigDecimal.valueOf(p.popularity() * 30L);
                items.add(new PurchasePlanRequest.Item(materials.get(code), qty, qty, suppliers.get(s.code()),
                        BigDecimal.valueOf(p.cost() / 1.13).setScale(2, RoundingMode.HALF_UP), BigDecimal.valueOf(13),
                        ctx.today().plusDays(s.leadDays()), null, null));
            }
            int stage = g;
            String remark = remarks[g];
            tx.run("purchasePlan", () -> {
                Long id = purchasePlanService.create(new PurchasePlanRequest(ctx.today().minusDays(2L - stage), localWh, remark, items));
                if (stage <= 1) {
                    purchasePlanService.approve(id);
                }
                if (stage == 0) {
                    purchasePlanService.generateOrders(id);
                }
            });
        }
    }

    /** 自定义费用映射（内置映射之外，平台报告中新出现的费用名） */
    private void feeMappings() {
        Object[][] rows = {{EcomPlatform.TIKTOK, "Affiliate Commission", SettlementItemType.COMMISSION, "达人带货佣金"}, // i18n-ignore 演示数据存库
                {EcomPlatform.SHOPEE, "Service Fee", SettlementItemType.COMMISSION, "虾皮服务费（免运 / 返现计划）"}, // i18n-ignore 演示数据存库
                {EcomPlatform.AMAZON, "Vine Enrollment Fee", SettlementItemType.PROMOTION, "Vine 计划注册费"}}; // i18n-ignore 演示数据存库
        for (Object[] r : rows) {
            EcomPlatform platform = (EcomPlatform) r[0];
            String raw = (String) r[1];
            boolean exists = feeMappingService.page(1, 50, platform, null, raw).records().stream()
                    .anyMatch(m -> raw.equalsIgnoreCase(m.getRawType()));
            if (exists) {
                continue;
            }
            EcomFeeMapping m = new EcomFeeMapping();
            m.setPlatform(platform);
            m.setRawType(raw);
            m.setItemType((SettlementItemType) r[2]);
            m.setStatus(EnableStatus.ENABLED);
            m.setRemark((String) r[3]);
            feeMappingService.create(m);
        }
    }
}
