package com.lemon.erp.modules.ecom.publish.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.demo.DemoContributor;
import com.lemon.erp.modules.ecom.demo.DemoKeys;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.publish.connector.PublishResult;
import com.lemon.erp.modules.ecom.publish.dto.PublishRequests;
import com.lemon.erp.modules.ecom.publish.entity.EcomCompetitorPrice;
import com.lemon.erp.modules.ecom.publish.entity.EcomPriceChange;
import com.lemon.erp.modules.ecom.publish.entity.EcomPriceTemplate;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishDraft;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishItem;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishLog;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishTemplate;
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishVariant;
import com.lemon.erp.modules.ecom.publish.entity.EcomRepriceLog;
import com.lemon.erp.modules.ecom.publish.entity.EcomRepriceRule;
import com.lemon.erp.modules.ecom.publish.mapper.EcomCompetitorPriceMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPriceChangeMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPriceTemplateMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishItemMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishLogMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishTemplateMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomPublishVariantMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomRepriceLogMapper;
import com.lemon.erp.modules.ecom.publish.mapper.EcomRepriceRuleMapper;
import com.lemon.erp.modules.ecom.publish.model.CompetitorSource;
import com.lemon.erp.modules.ecom.publish.model.PriceAdjustMode;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeSource;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeStatus;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeType;
import com.lemon.erp.modules.ecom.publish.model.PublishAction;
import com.lemon.erp.modules.ecom.publish.model.PublishBarcodeType;
import com.lemon.erp.modules.ecom.publish.model.PublishStatus;
import com.lemon.erp.modules.ecom.publish.model.RepriceFloorMode;
import com.lemon.erp.modules.ecom.publish.model.RepriceLogStatus;
import com.lemon.erp.modules.ecom.publish.model.RepriceStrategy;
import com.lemon.erp.modules.ecom.publish.service.EcomPriceTemplateService;
import com.lemon.erp.modules.ecom.publish.service.EcomPublishDraftService;
import com.lemon.erp.modules.ecom.publish.service.EcomPublishTemplateService;
import com.lemon.erp.modules.ecom.publish.service.EcomPublishWorker;
import com.lemon.erp.modules.ecom.publish.service.EcomRepricerService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示数据 · 刊登与定价（order 110）：价格模板、各平台刊登模板；8 个刊登草稿分别刊登到 2 – 3 个店铺（成功并生成在线商品、失败带平台错误、
 * 已下架、草稿；不生成待发布 / 发布中，避免刊登队列拿演示授权外呼，演示店铺的外部调用另由 EcomPublishSupport.isDemoStore 拦截）及刊登日志；近 60 天的改价 / 改库存历史（批量、手工、自动调价，含推送失败与仅本地）；
 * 5 条自动调价规则与近 14 天的调价日志（已推送、建议、已忽略、失败、跳过）；在售商品近 14 天的竞品价格快照（亚马逊为接口拉取，其他为手工 / 导入）。
 * 基础数据（店铺、物料、在线商品）来自演示数据的基础部分，平台接口不实际调用，发布结果通过 {@link EcomPublishWorker#record} 写入
 */
@Component
@RequiredArgsConstructor
public class PublishDemoContributor implements DemoContributor {

    private static final String[] AMAZON_SELLERS = {"A2X9KQ7L3M1ZPE", "A1VC38T7YXB528", "A3JWKAKR8XB7XF", "AZ8NB4QF7M2TD", "A1P5WQ9R2LH3K8"};
    private static final String[] OTHER_COMPETITORS = {"HomeEase Official", "SmartLife Store", "GearUp Outlet", "Urban Nest", "TechMate Mall"};

    private final EcomStoreService storeService;
    private final WmsMaterialService materialService;
    private final EcomListingMapper listingMapper;
    private final EcomPriceTemplateService priceTemplateService;
    private final EcomPriceTemplateMapper priceTemplateMapper;
    private final EcomPublishTemplateService publishTemplateService;
    private final EcomPublishTemplateMapper publishTemplateMapper;
    private final EcomPublishDraftService draftService;
    private final EcomPublishWorker worker;
    private final EcomPublishItemMapper itemMapper;
    private final EcomPublishVariantMapper variantMapper;
    private final EcomPublishLogMapper logMapper;
    private final EcomPriceChangeMapper changeMapper;
    private final EcomRepricerService repricerService;
    private final EcomRepriceRuleMapper ruleMapper;
    private final EcomRepriceLogMapper repriceLogMapper;
    private final EcomCompetitorPriceMapper competitorMapper;

    @Override
    public int order() {
        return 110;
    }

    @Override
    public String key() {
        return "publish";
    }

    @Override
    public void generate(DemoContext ctx) {
        Map<String, EcomStore> stores = stores(ctx);
        if (stores.isEmpty()) {
            return;
        }
        Map<String, Long> priceTemplates = priceTemplates();
        Map<String, Long> publishTemplates = publishTemplates(priceTemplates);
        drafts(ctx, stores, publishTemplates, priceTemplates);
        Map<String, Long> rules = rules(stores, priceTemplates);
        List<EcomListing> listings = listingMapper.selectList(Wrappers.<EcomListing>lambdaQuery()
                .in(EcomListing::getStoreId, stores.values().stream().map(EcomStore::getId).toList())
                .eq(EcomListing::getStatus, ListingStatus.ACTIVE).isNotNull(EcomListing::getPrice).orderByAsc(EcomListing::getId));
        Map<Long, EcomStore> byId = new HashMap<>();
        stores.values().forEach(s -> byId.put(s.getId(), s));
        priceHistory(ctx, listings, byId, rules);
        competitors(ctx, listings, byId);
        repriceLogs(ctx, listings, byId, rules);
    }

    private Map<String, EcomStore> stores(DemoContext ctx) {
        Map<String, Long> byCode = ctx.get(DemoKeys.STORE_BY_CODE);
        Map<String, EcomStore> out = new LinkedHashMap<>();
        if (byCode != null) {
            byCode.forEach((code, id) -> out.put(code, storeService.get(id)));
        } else {
            storeService.options().stream().filter(s -> s.getCode().startsWith(DemoKeys.STORE_CODE_PREFIX)).forEach(s -> out.put(s.getCode(), s));
        }
        return out;
    }

    // ---------------------------------------------------------------- 模板

    private Map<String, Long> priceTemplates() {
        Map<String, Long> ids = new LinkedHashMap<>();
        ids.put("FBA_US", priceTemplate("FBA 美国 · 海运头程", EcomPlatform.AMAZON, 9, 1.2, 15, 6, 3.8, 22, 0.99, "海运头程约 9 元 / 件，FBA 配送费按标准尺寸 3.8 美元")); // i18n-ignore 演示数据存库
        ids.put("FBA_EU", priceTemplate("FBA 欧洲 · 空运头程", EcomPlatform.AMAZON, 18, 1.2, 15, 5, 3.2, 20, 0.99, "空运头程，含 VAT 前售价")); // i18n-ignore 演示数据存库
        ids.put("SHOPEE", priceTemplate("虾皮 · 自发货", EcomPlatform.SHOPEE, 0, 1, 8, 4, 0.5, 25, 0.90, "跨境 SLS 物流由买家承担大部分运费")); // i18n-ignore 演示数据存库
        ids.put("TIKTOK", priceTemplate("TikTok 美区 · 海外仓", EcomPlatform.TIKTOK, 10, 1, 8, 8, 0.3, 25, 0.99, "其他费率含达人佣金与广告")); // i18n-ignore 演示数据存库
        ids.put("SMALL", priceTemplate("独立站 / eBay · 小包直发", null, 6, 1, 13, 3, 0.3, 28, 0.99, "国际小包 6 元 / 件估算")); // i18n-ignore 演示数据存库
        return ids;
    }

    private Long priceTemplate(String name, EcomPlatform platform, double firstLeg, double other, double commission, double otherRate, double fixedFee,
                               double margin, double tail, String remark) {
        EcomPriceTemplate old = priceTemplateMapper.selectOne(Wrappers.<EcomPriceTemplate>lambdaQuery().eq(EcomPriceTemplate::getName, name));
        if (old != null) {
            return old.getId();
        }
        EcomPriceTemplate t = new EcomPriceTemplate();
        t.setName(name);
        t.setPlatform(platform);
        t.setFirstLegCost(BigDecimal.valueOf(firstLeg));
        t.setOtherCost(BigDecimal.valueOf(other));
        t.setCommissionRate(BigDecimal.valueOf(commission));
        t.setOtherRate(BigDecimal.valueOf(otherRate));
        t.setFixedFee(BigDecimal.valueOf(fixedFee));
        t.setTargetMargin(BigDecimal.valueOf(margin));
        t.setPriceTail(BigDecimal.valueOf(tail));
        t.setStatus(EnableStatus.ENABLED);
        t.setRemark(remark);
        return priceTemplateService.create(t);
    }

    private Map<String, Long> publishTemplates(Map<String, Long> pt) {
        Map<String, Long> ids = new LinkedHashMap<>();
        ids.put("AMZ_HOME", publishTemplate("亚马逊美国 · 家居收纳", EcomPlatform.AMAZON, "HOME_ORGANIZERS_AND_STORAGE", "Home Organizers And Storage", // i18n-ignore 演示数据存库
                Map.of("item_type_keyword", "vacuum-storage-bags", "country_of_origin", "CN", "recommended_browse_nodes", "3744171", "material", "Plastic"),
                null, null, 2, FulfillmentType.PLATFORM, pt.get("FBA_US"), Map.of()));
        ids.put("AMZ_3C", publishTemplate("亚马逊 · 3C 配件", EcomPlatform.AMAZON, "PORTABLE_ELECTRONIC_DEVICE_COVER", "Portable Electronic Device Cover", // i18n-ignore 演示数据存库
                Map.of("item_type_keyword", "cell-phone-basic-cases", "country_of_origin", "CN", "batteries_required", "false"),
                null, null, 2, FulfillmentType.PLATFORM, pt.get("FBA_US"), Map.of()));
        ids.put("AMZ_EU", publishTemplate("亚马逊欧洲 · 通用", EcomPlatform.AMAZON, "HOME", "Home", Map.of("country_of_origin", "CN"), // i18n-ignore 演示数据存库
                null, null, 3, FulfillmentType.PLATFORM, pt.get("FBA_EU"), Map.of()));
        ids.put("SHOPEE", publishTemplate("虾皮 · 家居日用", EcomPlatform.SHOPEE, "101205", "Home & Living > Storage & Organization", // i18n-ignore 演示数据存库
                Map.of("100134", "No Brand", "100037", "China", "weight", "0.6"), "28016", null, 3, FulfillmentType.SELLER, pt.get("SHOPEE"), Map.of()));
        ids.put("TIKTOK", publishTemplate("TikTok 美区 · 家居", EcomPlatform.TIKTOK, "601152", "Home Supplies > Home Organizers", // i18n-ignore 演示数据存库
                Map.of("100198", "China", "100347", "Plastic"), null, null, 2, FulfillmentType.SELLER, pt.get("TIKTOK"), Map.of("warehouseId", "7311234560001")));
        ids.put("EBAY", publishTemplate("eBay 美国 · 标准", EcomPlatform.EBAY, "20625", "Home & Garden > Household Supplies > Storage", // i18n-ignore 演示数据存库
                Map.of("Type", "Storage Bag", "Country/Region of Manufacture", "China"), "6196932000", "6196935000", 3, FulfillmentType.SELLER,
                pt.get("SMALL"), Map.of("paymentPolicyId", "6196931000", "merchantLocationKey", "US-LA-01")));
        ids.put("SHOPIFY", publishTemplate("独立站 · 默认", EcomPlatform.SHOPIFY, null, null, Map.of("productType", "Home & Kitchen", "tags", "new-arrival,bestseller"), // i18n-ignore 演示数据存库
                null, null, 2, FulfillmentType.SELLER, pt.get("SMALL"), Map.of("locationId", "68123456789")));
        return ids;
    }

    private Long publishTemplate(String name, EcomPlatform platform, String categoryId, String categoryName, Map<String, String> attrs, String shipping,
                                 String returns, Integer handlingDays, FulfillmentType fulfillment, Long priceTemplateId, Map<String, String> extra) {
        EcomPublishTemplate old = publishTemplateMapper.selectOne(Wrappers.<EcomPublishTemplate>lambdaQuery().eq(EcomPublishTemplate::getName, name));
        if (old != null) {
            return old.getId();
        }
        EcomPublishTemplate t = new EcomPublishTemplate();
        t.setName(name);
        t.setPlatform(platform);
        t.setCategoryId(categoryId);
        t.setCategoryName(categoryName);
        t.setAttributes(new LinkedHashMap<>(attrs));
        t.setShippingTemplate(shipping);
        t.setReturnPolicy(returns);
        t.setHandlingDays(handlingDays);
        t.setFulfillment(fulfillment);
        t.setPriceTemplateId(priceTemplateId);
        t.setExtra(new LinkedHashMap<>(extra));
        t.setStatus(EnableStatus.ENABLED);
        return publishTemplateService.create(t);
    }

    // ---------------------------------------------------------------- 刊登草稿与记录

    /** 一个店铺的刊登结果：状态、模板键、失败原因 / 平台商品 ID */
    private record Target(String store, PublishStatus status, String template, String detail) {
    }

    private record DraftDef(String material, String title, String[] bullets, String description, List<Target> targets) {
    }

    private void drafts(DemoContext ctx, Map<String, EcomStore> stores, Map<String, Long> templates, Map<String, Long> priceTemplates) {
        List<DraftDef> defs = List.of(
                new DraftDef("DM-HM02", "Homelux Vacuum Storage Bags 8 Pack with Hand Pump, Space Saver Bags for Comforters, Blankets and Clothes",
                        new String[]{"SAVE 80% SPACE: Jumbo, large and medium sizes fit comforters, pillows and seasonal clothes",
                                "DOUBLE-ZIP SEAL: Leak-proof valve keeps air out for months", "TRAVEL HAND PUMP INCLUDED: No vacuum cleaner needed",
                                "THICK & REUSABLE: 0.11mm PA+PE material resists punctures", "WORRY-FREE: 2-year warranty and friendly support"},
                        "Keep your closet tidy and your clothes fresh. Each pack includes 8 reusable vacuum bags and a hand pump.",
                        List.of(new Target("DEMO-AMZ-US", PublishStatus.SUCCESS, "AMZ_HOME", null),
                                new Target("DEMO-SHP-MY", PublishStatus.SUCCESS, "SHOPEE", "2389104857"),
                                new Target("DEMO-TT-US", PublishStatus.FAILED, "TIKTOK",
                                        "TikTok Shop 接口调用失败：12052048: The category requires qualification documents. Please submit the documents in Seller Center first."))), // i18n-ignore 演示数据存库
                new DraftDef("DM-PB20", "Voltix Portable Charger 20000mAh PD 22.5W Fast Charging Power Bank with USB-C Input and Output",
                        new String[]{"20000mAh HIGH CAPACITY: Charges an iPhone 15 about 4 times", "22.5W PD FAST CHARGE: 0-50% in 30 minutes",
                                "3 OUTPUTS: Charge three devices at the same time", "LED DISPLAY: Shows the exact remaining power", "AIRLINE APPROVED: 74Wh, safe to carry on"},
                        "A slim 20000mAh power bank with PD fast charging, built for travel and daily commuting.",
                        List.of(new Target("DEMO-AMZ-US", PublishStatus.FAILED, "AMZ_3C",
                                        "Amazon 接口调用失败：VX-PB20-US: 8541 [externally_assigned_product_identifier] The UPC you provided matches an existing ASIN with conflicting attributes; 90220 [batteries_required] 'batteries_required' is required but not supplied."), // i18n-ignore 演示数据存库
                                new Target("DEMO-AMZ-DE", PublishStatus.FAILED, "AMZ_EU",
                                        "Amazon 接口调用失败：VX-PB20-DE: 90220 [item_type_keyword] 'item_type_keyword' is required but not supplied."), // i18n-ignore 演示数据存库
                                new Target("DEMO-EBAY-US", PublishStatus.SUCCESS, "EBAY", "395712864021"))),
                new DraftDef("DM-OD02", "TrailPro Folding Camping Chair, Portable Lightweight Lawn Chair with Cup Holder, Supports 265 lbs",
                        new String[]{"HEAVY DUTY: Steel frame supports up to 265 lbs", "COMFORT: Padded seat with high backrest", "PORTABLE: Folds into a carry bag with strap",
                                "CUP HOLDER & SIDE POCKET: Keep drinks and phones within reach", "EASY SETUP: Opens in seconds, no assembly required"},
                        "Relax anywhere with the TrailPro folding chair, perfect for camping, fishing, beach and backyard.",
                        List.of(new Target("DEMO-AMZ-US", PublishStatus.DRAFT, "AMZ_HOME", null),
                                new Target("DEMO-SHOPIFY", PublishStatus.SUCCESS, "SHOPIFY", "8123456789012"),
                                new Target("DEMO-EBAY-US", PublishStatus.FAILED, "EBAY",
                                        "eBay 接口调用失败：HTTP 400 {\"errors\":[{\"errorId\":25007,\"message\":\"Please add at least one valid shipping service option to your listing.\"}]}"))), // i18n-ignore 演示数据存库
                new DraftDef("DM-PT07", "PawJoy Automatic Cat Feeder 4L with App Control, Timed Pet Food Dispenser for Cats and Dogs",
                        new String[]{"APP CONTROL: Schedule up to 10 meals a day from your phone", "4L CAPACITY: Feeds one cat for about 15 days",
                                "DUAL POWER: Adapter plus battery backup", "VOICE RECORDER: Call your pet to eat with your own voice", "EASY TO CLEAN: Detachable bowl and tank"},
                        "Never miss a meal again. The PawJoy smart feeder keeps your pet on a healthy schedule.",
                        List.of(new Target("DEMO-TT-US", PublishStatus.SUCCESS, "TIKTOK", "1729384756102938"),
                                new Target("DEMO-SHOPIFY", PublishStatus.DRAFT, "SHOPIFY", null),
                                new Target("DEMO-SHP-SG", PublishStatus.FAILED, "SHOPEE",
                                        "Shopee 接口调用失败：error_param: logistic_info is invalid, the channel 28016 is not enabled for this shop"))), // i18n-ignore 演示数据存库
                new DraftDef("DM-HM10", "Homelux Electric Coffee Grinder with Stainless Steel Blades for Beans, Spices and Nuts",
                        new String[]{"POWERFUL 200W MOTOR: Grinds beans in seconds", "STAINLESS STEEL BLADES", "ONE-TOUCH OPERATION", "SAFETY LID LOCK", "COMPACT DESIGN"},
                        "Freshly ground coffee every morning with the Homelux electric grinder.",
                        List.of(new Target("DEMO-AMZ-JP", PublishStatus.DRAFT, null, null),
                                new Target("DEMO-AMZ-DE", PublishStatus.OFFLINE, "AMZ_EU", null))),
                new DraftDef("DM-OD06", "TrailPro Carbon Fiber Trekking Poles 2 Pack, Collapsible Hiking Sticks with Quick Lock",
                        new String[]{"ULTRALIGHT CARBON: Only 7.6 oz per pole", "QUICK LOCK: Adjust from 24\" to 54\" in seconds", "CORK GRIPS: Absorb sweat and reduce fatigue",
                                "ALL-TERRAIN TIPS: Rubber and mud baskets included", "COMPACT: Folds to fit any backpack"},
                        "Hike farther with less strain on your knees.",
                        List.of(new Target("DEMO-EBAY-US", PublishStatus.SUCCESS, "EBAY", "395712864388"),
                                new Target("DEMO-SHOPIFY", PublishStatus.SUCCESS, "SHOPIFY", "8123456789345"),
                                new Target("DEMO-TEMU-US", PublishStatus.DRAFT, null, null))),
                new DraftDef("DM-KB01", "Voltix Foldable Bluetooth Keyboard, Multi-Device Pocket Keyboard for iPad, iPhone and Android",
                        new String[]{"FOLDABLE: Fits in a pocket", "3 DEVICES: Switch with one key", "LONG BATTERY: 60 hours per charge", "UNIVERSAL", "SILENT KEYS"},
                        "Type anywhere with a full-size feel.",
                        List.of(new Target("DEMO-AMZ-US", PublishStatus.DRAFT, "AMZ_3C", null),
                                new Target("DEMO-TT-US", PublishStatus.DRAFT, "TIKTOK", null))),
                new DraftDef("DM-HM06", "Homelux Essential Oil Diffuser 300ml, Ultrasonic Aroma Humidifier with 7 Color Lights",
                        new String[]{"300ML TANK: Runs up to 10 hours", "WHISPER QUIET", "7 AMBIENT LIGHTS", "AUTO SHUT-OFF", "WOOD GRAIN DESIGN"},
                        "Create a calm and cozy atmosphere in any room.",
                        List.of(new Target("DEMO-SHP-MY", PublishStatus.FAILED, "SHOPEE",
                                        "Shopee 接口调用失败：error_image: image size must be at least 500x500"), // i18n-ignore 演示数据存库
                                new Target("DEMO-SHP-SG", PublishStatus.SUCCESS, "SHOPEE", "2389105511"))));
        Map<String, Long> materials = ctx.get(DemoKeys.MATERIAL_BY_CODE);
        Map<String, WmsMaterial> byCode = materialService.mapByCodes(defs.stream().map(DraftDef::material).toList());
        LocalDateTime now = LocalDateTime.now().withNano(0);
        int index = 0;
        for (DraftDef def : defs) {
            index++;
            WmsMaterial m = byCode.get(def.material());
            Long materialId = m != null ? m.getId() : materials == null ? null : materials.get(def.material());
            List<Target> targets = def.targets().stream().filter(t -> stores.containsKey(t.store())).toList();
            if (materialId == null || targets.isEmpty()) {
                continue;
            }
            Map<String, Long> templateIds = new HashMap<>();
            for (Target t : targets) {
                if (t.template() != null) {
                    templateIds.putIfAbsent(stores.get(t.store()).getPlatform().name(), templates.get(t.template()));
                }
            }
            // 同一平台多个店铺共用一个模板（取第一个）
            Long draftId = draftService.createDraft(new PublishRequests.CreateDraft(materialId, def.title(),
                    targets.stream().map(t -> stores.get(t.store()).getId()).toList(), templateIds, null));
            EcomPublishDraft d = draftService.getDraft(draftId);
            d.setBullets(List.of(def.bullets()));
            d.setDescription(def.description());
            d.setBarcodeType(PublishBarcodeType.UPC);
            d.setUpcExempt(index % 3 == 0 ? 1 : 0);
            d.setExemptionNote(index % 3 == 0 ? "Brand Registry GTIN exemption approved" : null);
            draftService.updateDraft(draftId, d);
            draftService.applyMaster(draftId, null);
            LocalDateTime created = now.minusDays(30L - index * 3L).minusHours(ctx.between(1, 8));
            for (EcomPublishItem item : d.getItems()) {
                Target t = targets.stream().filter(x -> stores.get(x.store()).getId().equals(item.getStoreId())).findFirst().orElse(null);
                if (t == null) {
                    continue;
                }
                EcomStore store = stores.get(t.store());
                String sku = (m != null ? m.getCode() : def.material()).replace(DemoKeys.MATERIAL_CODE_PREFIX, "") + "-" + suffix(t.store());
                variantMapper.update(Wrappers.<EcomPublishVariant>lambdaUpdate().set(EcomPublishVariant::getSellerSku, sku)
                        .set(EcomPublishVariant::getQty, store.getPlatform() == EcomPlatform.AMAZON ? 0 : ctx.between(30, 200))
                        .eq(EcomPublishVariant::getItemId, item.getId()));
                itemMapper.update(Wrappers.<EcomPublishItem>lambdaUpdate().set(EcomPublishItem::getCreateTime, created)
                        .eq(EcomPublishItem::getId, item.getId()));
                applyStatus(ctx, item.getId(), store, t, created);
            }
        }
    }

    /** 演示店铺编码去掉 DEMO- 前缀（取后 6 位）作为平台 SKU 的后缀 */
    private static String suffix(String storeCode) {
        String code = storeCode.replace(DemoKeys.STORE_CODE_PREFIX, "");
        return code.length() > 6 ? code.substring(code.length() - 6) : code;
    }

    private void applyStatus(DemoContext ctx, Long itemId, EcomStore store, Target t, LocalDateTime created) {
        LocalDateTime submit = created.plusDays(ctx.between(0, 2)).plusHours(ctx.between(1, 6));
        switch (t.status()) {
            case DRAFT -> {
                // 保持草稿
            }
            case FAILED -> {
                log(itemId, store, PublishAction.SUBMIT, true, null, submit);
                worker.record(itemId, PublishResult.failed(t.detail()), PublishAction.PUBLISH);
                // 第一次失败后修改重试，仍然失败
                itemMapper.update(Wrappers.<EcomPublishItem>lambdaUpdate().set(EcomPublishItem::getSubmitTime, submit.plusDays(1))
                        .set(EcomPublishItem::getRetryCount, 1).eq(EcomPublishItem::getId, itemId));
                log(itemId, store, PublishAction.SUBMIT, true, Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.retry"), submit.plusDays(1));
                shiftLogs(itemId, submit.plusDays(1).plusMinutes(1));
            }
            case SUCCESS, OFFLINE -> {
                log(itemId, store, PublishAction.SUBMIT, true, null, submit);
                EcomPublishItem item = draftService.getItem(itemId);
                Map<String, String> skuIds = new LinkedHashMap<>();
                Map<String, Map<String, String>> extra = new LinkedHashMap<>();
                String platformId = t.detail();
                String url = null;
                for (EcomPublishVariant v : item.getVariants()) {
                    switch (store.getPlatform()) {
                        case AMAZON -> extra.put(v.getSellerSku(), Map.of("productType", String.valueOf(item.getCategoryId())));
                        case EBAY -> {
                            skuIds.put(v.getSellerSku(), v.getSellerSku());
                            extra.put(v.getSellerSku(), Map.of("offerId", "8" + Math.abs((long) v.getSellerSku().hashCode())));
                            url = "https://www.ebay.com/itm/" + platformId;
                        }
                        case SHOPIFY -> {
                            String variantId = "4" + Math.abs((long) v.getSellerSku().hashCode());
                            skuIds.put(v.getSellerSku(), variantId);
                            extra.put(v.getSellerSku(), Map.of("variantId", variantId));
                        }
                        default -> skuIds.put(v.getSellerSku(), platformId);
                    }
                }
                worker.record(itemId, PublishResult.success(platformId, url, skuIds, extra, null), PublishAction.PUBLISH);
                LocalDateTime published = submit.plusMinutes(ctx.between(2, 20));
                itemMapper.update(Wrappers.<EcomPublishItem>lambdaUpdate().set(EcomPublishItem::getSubmitTime, submit)
                        .set(EcomPublishItem::getPublishTime, published).eq(EcomPublishItem::getId, itemId));
                shiftLogs(itemId, published);
                if (t.status() == PublishStatus.OFFLINE) {
                    LocalDateTime off = published.plusDays(ctx.between(5, 12));
                    itemMapper.update(Wrappers.<EcomPublishItem>lambdaUpdate().set(EcomPublishItem::getStatus, PublishStatus.OFFLINE)
                            .set(EcomPublishItem::getOfflineTime, off).eq(EcomPublishItem::getId, itemId));
                    List<Long> listingIds = draftService.getItem(itemId).getVariants().stream().map(EcomPublishVariant::getListingId)
                            .filter(java.util.Objects::nonNull).toList();
                    if (!listingIds.isEmpty()) {
                        listingMapper.update(Wrappers.<EcomListing>lambdaUpdate().set(EcomListing::getStatus, ListingStatus.INACTIVE)
                                .in(EcomListing::getId, listingIds));
                    }
                    log(itemId, store, PublishAction.OFFLINE, true, null, off);
                }
            }
            default -> {
            }
        }
    }

    private void log(Long itemId, EcomStore store, PublishAction action, boolean success, String message, LocalDateTime time) {
        EcomPublishLog l = new EcomPublishLog();
        l.setItemId(itemId);
        l.setStoreId(store.getId());
        l.setAction(action);
        l.setSuccess(success ? 1 : 0);
        l.setMessage(message);
        l.setCreateTime(time);
        l.setUpdateTime(time);
        logMapper.insert(l);
    }

    /** 把发布结果日志（PUBLISH 动作，写入时为当前时间）改到演示时间 */
    private void shiftLogs(Long itemId, LocalDateTime time) {
        logMapper.update(Wrappers.<EcomPublishLog>lambdaUpdate().set(EcomPublishLog::getCreateTime, time).eq(EcomPublishLog::getItemId, itemId)
                .eq(EcomPublishLog::getAction, PublishAction.PUBLISH));
        changeMapper.update(Wrappers.<EcomPriceChange>lambdaUpdate().set(EcomPriceChange::getCreateTime, time)
                .eq(EcomPriceChange::getSource, PriceChangeSource.PUBLISH).ge(EcomPriceChange::getCreateTime, LocalDateTime.now().minusMinutes(5))
                .in(EcomPriceChange::getListingId, listingIdsOf(itemId)));
    }

    private List<Long> listingIdsOf(Long itemId) {
        List<Long> ids = variantMapper.selectList(Wrappers.<EcomPublishVariant>lambdaQuery().select(EcomPublishVariant::getListingId)
                .eq(EcomPublishVariant::getItemId, itemId)).stream().filter(java.util.Objects::nonNull).map(EcomPublishVariant::getListingId)
                .filter(java.util.Objects::nonNull).toList();
        return ids.isEmpty() ? List.of(-1L) : ids;
    }

    // ---------------------------------------------------------------- 改价历史

    private void priceHistory(DemoContext ctx, List<EcomListing> listings, Map<Long, EcomStore> stores, Map<String, Long> rules) {
        List<EcomPriceChange> rows = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        int n = 0;
        for (EcomListing l : listings) {
            if (n++ >= 48) {
                break;
            }
            EcomStore store = stores.get(l.getStoreId());
            boolean local = store.getPlatform() == EcomPlatform.TEMU || store.getPlatform() == EcomPlatform.ALIEXPRESS;
            int changes = ctx.between(2, 4);
            BigDecimal price = l.getPrice();
            List<BigDecimal> prices = new ArrayList<>();
            prices.add(price);
            for (int i = 1; i <= changes; i++) {
                price = price.multiply(BigDecimal.valueOf(1 + (ctx.random().nextDouble() - 0.45) * 0.12)).setScale(2, RoundingMode.HALF_UP);
                prices.add(0, price);
            }
            for (int i = 1; i < prices.size(); i++) {
                LocalDateTime t = now.minusDays(60L - (60L * i / prices.size())).minusHours(ctx.between(0, 10));
                PriceChangeSource source = ctx.chance(0.5) ? PriceChangeSource.BATCH : ctx.chance(0.6) ? PriceChangeSource.REPRICER : PriceChangeSource.MANUAL;
                PriceChangeStatus status = local ? PriceChangeStatus.LOCAL : ctx.chance(0.08) ? PriceChangeStatus.FAILED : PriceChangeStatus.SUCCESS;
                String message = status == PriceChangeStatus.LOCAL ? Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.priceLocalOnly")
                        : status == PriceChangeStatus.FAILED ? failure(store.getPlatform()) : null;
                Long ruleId = source == PriceChangeSource.REPRICER ? ruleFor(store, rules) : null;
                if (source == PriceChangeSource.REPRICER && ruleId == null) {
                    source = PriceChangeSource.BATCH;
                }
                rows.add(change(t.format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHH")) + "001", l, store, PriceChangeType.PRICE,
                        prices.get(i - 1), prices.get(i), source, status, message, ruleId, t));
            }
            if (l.getFulfillment() == FulfillmentType.SELLER && l.getPlatformQty() != null && ctx.chance(0.4)) {
                LocalDateTime t = now.minusDays(ctx.between(1, 20));
                BigDecimal qty = l.getPlatformQty();
                rows.add(change(t.format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHH")) + "002", l, store, PriceChangeType.STOCK,
                        qty.add(BigDecimal.valueOf(ctx.between(-20, 40))).max(BigDecimal.ZERO), qty, PriceChangeSource.BATCH,
                        local ? PriceChangeStatus.LOCAL : PriceChangeStatus.SUCCESS, null, null, t));
            }
        }
        for (int i = 0; i < rows.size(); i += 200) {
            changeMapper.insertBatch(rows.subList(i, Math.min(rows.size(), i + 200)));
        }
    }

    private static String failure(EcomPlatform p) {
        return switch (p) {
            case AMAZON -> "Amazon 接口调用失败：8572 [purchasable_offer] The price is below the minimum allowed price for this ASIN (potential pricing error)."; // i18n-ignore 演示数据存库
            case SHOPEE -> "Shopee 接口调用失败：error_price: The price must be within the range of the platform price limit."; // i18n-ignore 演示数据存库
            case TIKTOK -> "TikTok Shop 接口调用失败：12019022: The product is under review and its price cannot be updated."; // i18n-ignore 演示数据存库
            case EBAY -> "eBay 接口调用失败：[{\"errorId\":25020,\"message\":\"Invalid price.\"}]"; // i18n-ignore 演示数据存库
            default -> "Request timed out";
        };
    }

    private static EcomPriceChange change(String batchNo, EcomListing l, EcomStore store, PriceChangeType type, BigDecimal old, BigDecimal value,
                                          PriceChangeSource source, PriceChangeStatus status, String message, Long ruleId, LocalDateTime time) {
        EcomPriceChange c = new EcomPriceChange();
        c.setBatchNo(batchNo);
        c.setListingId(l.getId());
        c.setStoreId(store.getId());
        c.setSellerSku(l.getSellerSku());
        c.setChangeType(type);
        c.setOldValue(old);
        c.setNewValue(value);
        c.setCurrency(type == PriceChangeType.PRICE ? l.getCurrency() : null);
        c.setSource(source);
        c.setStatus(status);
        c.setMessage(message);
        c.setRuleId(ruleId);
        c.setCreateTime(time);
        return c;
    }

    // ---------------------------------------------------------------- 调价规则与日志

    private Map<String, Long> rules(Map<String, EcomStore> stores, Map<String, Long> pt) {
        Map<String, Long> ids = new LinkedHashMap<>();
        rule(ids, "AMZ_US", "亚马逊美国 · 跟随购物车", ids(stores, "DEMO-AMZ-US"), RepriceStrategy.MATCH_BUYBOX, null, null, null, RepriceFloorMode.COST_MARGIN, // i18n-ignore 演示数据存库
                null, null, 8, pt.get("FBA_US"), null, null, 60, 0, EnableStatus.ENABLED, "底价 = 成本 + 8% 利润，持有购物车时不调整"); // i18n-ignore 演示数据存库
        rule(ids, "AMZ_EU", "亚马逊欧洲 · 低于最低价 0.1", ids(stores, "DEMO-AMZ-DE"), RepriceStrategy.BEAT_LOWEST, PriceAdjustMode.AMOUNT, 0.1, null, // i18n-ignore 演示数据存库
                RepriceFloorMode.COST_MARGIN, null, null, 5, pt.get("FBA_EU"), null, null, 120, 1, EnableStatus.ENABLED, "先观察建议，确认后再推送"); // i18n-ignore 演示数据存库
        rule(ids, "TIKTOK", "TikTok · 目标利润率 25%", ids(stores, "DEMO-TT-US"), RepriceStrategy.TARGET_MARGIN, null, null, 25, RepriceFloorMode.FIXED, // i18n-ignore 演示数据存库
                5.0, null, null, pt.get("TIKTOK"), null, null, 240, 1, EnableStatus.ENABLED, null);
        rule(ids, "SHOPEE", "虾皮 · 按库存调价", ids(stores, "DEMO-SHP-MY", "DEMO-SHP-SG"), RepriceStrategy.BY_STOCK, PriceAdjustMode.PERCENT, 5, null, // i18n-ignore 演示数据存库
                RepriceFloorMode.FIXED, 3.0, 300.0, null, null, 60, 15, 720, 0, EnableStatus.ENABLED, "可售超过 60 天降 5%，少于 15 天涨 5%"); // i18n-ignore 演示数据存库
        rule(ids, "EBAY", "eBay · 跟随最低价", ids(stores, "DEMO-EBAY-US"), RepriceStrategy.MATCH_LOWEST, null, null, null, RepriceFloorMode.COST_MARGIN, // i18n-ignore 演示数据存库
                null, null, 10, pt.get("SMALL"), null, null, 360, 1, EnableStatus.DISABLED, "竞品价格靠导入，暂停中"); // i18n-ignore 演示数据存库
        return ids;
    }

    private static String ids(Map<String, EcomStore> stores, String... codes) {
        List<String> out = new ArrayList<>();
        for (String c : codes) {
            if (stores.containsKey(c)) {
                out.add(String.valueOf(stores.get(c).getId()));
            }
        }
        return out.isEmpty() ? null : String.join(",", out);
    }

    private void rule(Map<String, Long> ids, String key, String name, String storeIds, RepriceStrategy strategy, PriceAdjustMode mode, Number value,
                      Number targetMargin, RepriceFloorMode floor, Double minPrice, Double maxPrice, Number minMargin, Long priceTemplateId,
                      Integer highDays, Integer lowDays, int interval, int suggestOnly, EnableStatus status, String remark) {
        if (storeIds == null) {
            return;
        }
        EcomRepriceRule old = ruleMapper.selectOne(Wrappers.<EcomRepriceRule>lambdaQuery().eq(EcomRepriceRule::getName, name));
        if (old != null) {
            ids.put(key, old.getId());
            return;
        }
        EcomRepriceRule r = new EcomRepriceRule();
        r.setName(name);
        r.setStoreIds(storeIds);
        r.setStrategy(strategy);
        r.setAdjustMode(mode);
        r.setAdjustValue(value == null ? null : BigDecimal.valueOf(value.doubleValue()));
        r.setTargetMargin(targetMargin == null ? null : BigDecimal.valueOf(targetMargin.doubleValue()));
        r.setFloorMode(floor);
        r.setMinPrice(minPrice == null ? null : BigDecimal.valueOf(minPrice));
        r.setMaxPrice(maxPrice == null ? null : BigDecimal.valueOf(maxPrice));
        r.setMinMargin(minMargin == null ? null : BigDecimal.valueOf(minMargin.doubleValue()));
        r.setPriceTemplateId(priceTemplateId);
        r.setStockHighDays(highDays);
        r.setStockLowDays(lowDays);
        r.setIntervalMinutes(interval);
        r.setSuggestOnly(suggestOnly);
        r.setStatus(status);
        r.setRemark(remark);
        Long id = repricerService.create(r);
        ids.put(key, id);
    }

    private static Long ruleFor(EcomStore store, Map<String, Long> rules) {
        return switch (store.getPlatform()) {
            case AMAZON -> "USD".equals(store.getCurrency()) ? rules.get("AMZ_US") : rules.get("AMZ_EU");
            case TIKTOK -> rules.get("TIKTOK");
            case SHOPEE -> rules.get("SHOPEE");
            case EBAY -> rules.get("EBAY");
            default -> null;
        };
    }

    private void repriceLogs(DemoContext ctx, List<EcomListing> listings, Map<Long, EcomStore> stores, Map<String, Long> rules) {
        List<EcomRepriceLog> logs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Map<Long, Integer> perRule = new HashMap<>();
        for (EcomListing l : listings) {
            EcomStore store = stores.get(l.getStoreId());
            Long ruleId = ruleFor(store, rules);
            if (ruleId == null || ruleId.equals(rules.get("EBAY")) || perRule.merge(ruleId, 1, Integer::sum) > 12) {
                continue;
            }
            boolean suggestOnly = ruleId.equals(rules.get("AMZ_EU")) || ruleId.equals(rules.get("TIKTOK"));
            BigDecimal price = l.getPrice();
            for (int day = 13; day >= 0; day -= ctx.between(1, 3)) {
                LocalDateTime t = now.minusDays(day).withMinute(0).withSecond(0).minusHours(ctx.between(0, 12));
                BigDecimal lowest = price.multiply(BigDecimal.valueOf(0.9 + ctx.random().nextDouble() * 0.15)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal buybox = lowest.min(price).add(BigDecimal.valueOf(ctx.between(0, 30) / 100.0)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal floor = price.multiply(BigDecimal.valueOf(0.82)).setScale(2, RoundingMode.HALF_UP);
                EcomRepriceLog g = new EcomRepriceLog();
                g.setRuleId(ruleId);
                g.setListingId(l.getId());
                g.setStoreId(store.getId());
                g.setSellerSku(l.getSellerSku());
                g.setOldPrice(price);
                g.setCurrency(l.getCurrency());
                g.setRunTime(t);
                g.setFloorPrice(floor);
                if (ctx.chance(0.1)) {
                    g.setStatus(RepriceLogStatus.SKIPPED);
                    g.setReason(Messages.get(Messages.DEFAULT_LOCALE, ruleId.equals(rules.get("TIKTOK")) ? "ecom.text.pub.reprice.noCost"
                            : "ecom.text.pub.reprice.noCompetitor"));
                    logs.add(g);
                    continue;
                }
                BigDecimal next;
                String reason;
                if (ruleId.equals(rules.get("AMZ_US"))) {
                    next = buybox.max(floor);
                    g.setBuyboxPrice(buybox);
                    g.setLowestPrice(lowest);
                    reason = next.compareTo(buybox) == 0 ? Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.reprice.matchBuybox", buybox.toPlainString())
                            : Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.reprice.matchBuyboxFloor", buybox.toPlainString(), next.toPlainString());
                } else if (ruleId.equals(rules.get("AMZ_EU"))) {
                    next = lowest.subtract(new BigDecimal("0.10")).max(floor);
                    g.setLowestPrice(lowest);
                    g.setBuyboxPrice(buybox);
                    reason = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.reprice.beatLowest", lowest.toPlainString(), "0.1");
                } else if (ruleId.equals(rules.get("TIKTOK"))) {
                    next = price.multiply(BigDecimal.valueOf(0.96 + ctx.random().nextDouble() * 0.1)).setScale(2, RoundingMode.HALF_UP);
                    reason = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.reprice.targetMargin", next.toPlainString());
                } else {
                    boolean high = ctx.chance(0.6);
                    next = price.multiply(BigDecimal.valueOf(high ? 0.95 : 1.05)).setScale(2, RoundingMode.HALF_UP);
                    reason = Messages.get(Messages.DEFAULT_LOCALE, high ? "ecom.text.pub.reprice.stockHigh" : "ecom.text.pub.reprice.stockLow",
                            String.valueOf(high ? ctx.between(61, 140) : ctx.between(4, 14)), "5%");
                }
                if (next.compareTo(price) == 0) {
                    continue;
                }
                g.setNewPrice(next);
                g.setReason(reason);
                if (suggestOnly) {
                    g.setStatus(day <= 1 ? RepriceLogStatus.SUGGESTED : ctx.chance(0.5) ? RepriceLogStatus.PUSHED : RepriceLogStatus.IGNORED);
                } else {
                    g.setStatus(ctx.chance(0.08) ? RepriceLogStatus.FAILED : RepriceLogStatus.PUSHED);
                }
                if (g.getStatus() == RepriceLogStatus.FAILED) {
                    g.setMessage(failure(store.getPlatform()));
                }
                if (g.getStatus() == RepriceLogStatus.PUSHED) {
                    price = next;
                }
                logs.add(g);
            }
        }
        for (int i = 0; i < logs.size(); i += 200) {
            repriceLogMapper.insertBatch(logs.subList(i, Math.min(logs.size(), i + 200)));
        }
        rules.forEach((key, id) -> {
            EcomRepriceRule r = ruleMapper.selectById(id);
            if (r != null && r.getStatus() == EnableStatus.ENABLED) {
                long checked = logs.stream().filter(g -> g.getRuleId().equals(id)).count();
                ruleMapper.update(Wrappers.<EcomRepriceRule>lambdaUpdate().set(EcomRepriceRule::getLastRunTime, now.minusMinutes(ctx.between(5, 50)))
                        .set(EcomRepriceRule::getLastRunMessage, Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.pub.reprice.summary",
                                Math.max(checked, 8), Math.min(checked, 5), r.getSuggestOnly() == 1 ? Math.min(checked, 5) : 0,
                                r.getSuggestOnly() == 1 ? 0 : Math.min(checked, 5), 0, 1))
                        .eq(EcomRepriceRule::getId, id));
            }
        });
    }

    // ---------------------------------------------------------------- 竞品价格

    private void competitors(DemoContext ctx, List<EcomListing> listings, Map<Long, EcomStore> stores) {
        List<EcomCompetitorPrice> rows = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Map<Long, Integer> perStore = new HashMap<>();
        for (EcomListing l : listings) {
            EcomStore store = stores.get(l.getStoreId());
            if (store.getPlatform() == EcomPlatform.TEMU || store.getPlatform() == EcomPlatform.ALIEXPRESS
                    || perStore.merge(store.getId(), 1, Integer::sum) > 10) {
                continue;
            }
            boolean amazon = store.getPlatform() == EcomPlatform.AMAZON;
            CompetitorSource source = amazon ? CompetitorSource.API : store.getPlatform() == EcomPlatform.EBAY ? CompetitorSource.IMPORT : CompetitorSource.MANUAL;
            int step = amazon ? 1 : 2;
            for (int day = 13; day >= 0; day -= step) {
                BigDecimal lowest = l.getPrice().multiply(BigDecimal.valueOf(0.88 + ctx.random().nextDouble() * 0.18)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal shipping = amazon || ctx.chance(0.5) ? BigDecimal.ZERO : BigDecimal.valueOf(ctx.between(99, 399) / 100.0);
                BigDecimal total = lowest.add(shipping);
                boolean winner = l.getPrice().compareTo(total) <= 0 && ctx.chance(0.7);
                EcomCompetitorPrice p = new EcomCompetitorPrice();
                p.setListingId(l.getId());
                p.setStoreId(store.getId());
                p.setSellerSku(l.getSellerSku());
                p.setPlatformItemId(l.getPlatformItemId());
                p.setSource(source);
                p.setLowestPrice(lowest);
                p.setLowestShipping(shipping);
                p.setBuyboxPrice(amazon || store.getPlatform() == EcomPlatform.EBAY ? (winner ? l.getPrice() : total.min(l.getPrice())) : null);
                p.setBuyboxWinner(winner ? 1 : 0);
                p.setOfferCount(ctx.between(2, 14));
                p.setCompetitor(amazon ? ctx.pick(AMAZON_SELLERS) : ctx.pick(OTHER_COMPETITORS));
                p.setCurrency(l.getCurrency());
                p.setSnapshotTime(day == 0 ? now.minusMinutes(ctx.between(10, 90)) : now.minusDays(day).withHour(ctx.between(8, 20)).withMinute(0).withSecond(0));
                p.setRemark(source == CompetitorSource.MANUAL && ctx.chance(0.3) ? "前台搜索同款最低价" : null); // i18n-ignore 演示数据存库
                rows.add(p);
            }
        }
        for (int i = 0; i < rows.size(); i += 200) {
            competitorMapper.insertBatch(rows.subList(i, Math.min(rows.size(), i + 200)));
        }
    }
}
