package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.modules.ecom.entity.EcomBundleItem;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ListingComposeType;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.service.EcomListingService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示数据 · 在线商品（order 25）：各店铺的 listing 与本地物料配对（FBA / 海外仓 SKU、自发货 SKU、多件装、组合商品），
 * 同款不同色共用父商品（SPU / 变体视图），另有少量未配对的平台 SKU（SKU 配对中心演示，部分订单因此进入异常）和下架 / 信息不完整的商品
 */
@Component
@RequiredArgsConstructor
public class DemoListingContributor implements DemoContributor {

    /** 同款不同色：物料编码 → (父商品键, 变体属性) */
    private static final Map<String, String[]> VARIANTS = Map.of(
            "DM-CB01", new String[]{"CB", "Length: 1m"}, "DM-CB02", new String[]{"CB", "Length: 2m"},
            "DM-SC01", new String[]{"SC", "Color: Clear"}, "DM-SC02", new String[]{"SC", "Color: Matte Black"},
            "DM-HM04", new String[]{"HM04", "Color: Black"}, "DM-HM05", new String[]{"HM04", "Color: White"},
            "DM-OD04", new String[]{"OD04", "Color: Purple"}, "DM-OD05", new String[]{"OD04", "Color: Blue"});
    /** 品牌 → SKU 前缀 */
    private static final Map<String, String> PREFIX = Map.of("VOLTIX", "VX", "HOMELUX", "HL", "TRAILPRO", "TP", "PAWJOY", "PJ");

    private final EcomListingService listingService;
    private final WmsMaterialService materialService;

    @Override
    public int order() {
        return 25;
    }

    @Override
    public String key() {
        return "listings";
    }

    @Override
    public void generate(DemoContext ctx) {
        Map<String, Long> stores = ctx.get(DemoKeys.STORE_BY_CODE);
        Map<String, Long> materials = ctx.get(DemoKeys.MATERIAL_BY_CODE);
        Map<Long, WmsMaterial> mats = materialService.mapByIds(materials.values());
        Map<Long, Map<String, DemoListings.Item>> all = new LinkedHashMap<>();
        List<Long> listingIds = new ArrayList<>();
        for (DemoCatalog.Store d : DemoCatalog.STORES) {
            Long storeId = stores.get(d.code());
            Map<String, DemoListings.Item> items = new LinkedHashMap<>();
            List<DemoCatalog.Product> products = new ArrayList<>(DemoCatalog.PRODUCTS);
            Collections.shuffle(products, ctx.random());
            int count = switch (d.code()) {
                case "DEMO-AMZ-US" -> 40;
                case "DEMO-SHOPIFY" -> 30;
                case "DEMO-AMZ-DE" -> 28;
                case "DEMO-TT-US", "DEMO-AE" -> 25;
                case "DEMO-AMZ-JP" -> 22;
                case "DEMO-EBAY-US", "DEMO-SHP-MY" -> 20;
                default -> 18;
            };
            Map<String, String> parents = new HashMap<>();
            for (int i = 0; i < count; i++) {
                DemoCatalog.Product p = products.get(i);
                Long mid = materials.get(p.code());
                boolean platform = d.platformWh() != null && (d.platformShare() >= 0.7 || i % 5 < 3);
                FulfillmentType ful = platform ? FulfillmentType.PLATFORM : FulfillmentType.SELLER;
                String sku = sku(p, d, null);
                String[] variant = VARIANTS.get(p.code());
                String parent = variant == null ? null : parents.computeIfAbsent(variant[0], k -> itemId(d.platform(), ctx));
                EcomListing l = listing(storeId, sku, itemId(d.platform(), ctx), parent, p.en(), variant == null ? null : variant[1],
                        price(p.usd(), d), ful, mid, BigDecimal.ONE, mats.get(mid), d, ctx);
                // 少量商品下架 / 信息不完整（不参与订单）
                boolean inactive = i == count - 1 && d.platform() != EcomPlatform.AMAZON;
                if (inactive) {
                    l.setStatus(i % 2 == 0 ? ListingStatus.INACTIVE : ListingStatus.INCOMPLETE);
                }
                Long id = listingService.create(l);
                listingIds.add(id);
                if (!inactive) {
                    items.put(sku, item(id, storeId, l, mid, BigDecimal.ONE, null, p.weight(), p.popularity(), p.code()));
                }
                // 亚马逊：部分商品另有自发货 SKU（FBA 断货时跟卖自发货）
                if (d.platform() == EcomPlatform.AMAZON && i % 4 == 0) {
                    String fbm = sku(p, d, "M");
                    EcomListing m = listing(storeId, fbm, l.getPlatformItemId(), parent, p.en(), l.getVariant(), price(p.usd(), d),
                            FulfillmentType.SELLER, mid, BigDecimal.ONE, mats.get(mid), d, ctx);
                    m.setFnsku(null);
                    Long fid = listingService.create(m);
                    listingIds.add(fid);
                    items.put(fbm, item(fid, storeId, m, mid, BigDecimal.ONE, null, p.weight(), Math.max(1, p.popularity() / 3), p.code()));
                }
            }
            // 多件装：数据线 2 条装
            if (d.platform() == EcomPlatform.AMAZON || d.platform() == EcomPlatform.SHOPEE) {
                DemoCatalog.Product p = DemoCatalog.product("DM-CB01");
                Long mid = materials.get(p.code());
                String sku = sku(p, d, "2PK");
                FulfillmentType ful = d.platformWh() != null ? FulfillmentType.PLATFORM : FulfillmentType.SELLER;
                EcomListing l = listing(storeId, sku, itemId(d.platform(), ctx), null, p.en() + " (2 Pack)", null, price(p.usd() * 1.7, d),
                        ful, mid, BigDecimal.valueOf(2), mats.get(mid), d, ctx);
                Long id = listingService.create(l);
                listingIds.add(id);
                items.put(sku, item(id, storeId, l, mid, BigDecimal.valueOf(2), null, p.weight() * 2, 4, p.code()));
            }
            // 组合商品
            for (DemoCatalog.Bundle b : DemoCatalog.BUNDLES) {
                if (!bundleFor(d, b)) {
                    continue;
                }
                String sku = PREFIX.get(DemoCatalog.product(b.components().keySet().iterator().next()).brand()) + "-" + b.sku() + "-" + tag(d, null);
                FulfillmentType ful = d.platformWh() != null && d.platformShare() >= 0.7 ? FulfillmentType.PLATFORM : FulfillmentType.SELLER;
                List<EcomBundleItem> parts = new ArrayList<>();
                Map<Long, BigDecimal> components = new LinkedHashMap<>();
                double weight = 0;
                String first = null;
                for (Map.Entry<String, Integer> c : b.components().entrySet()) {
                    EcomBundleItem bi = new EcomBundleItem();
                    bi.setMaterialId(materials.get(c.getKey()));
                    bi.setQty(BigDecimal.valueOf(c.getValue()));
                    parts.add(bi);
                    components.put(bi.getMaterialId(), bi.getQty());
                    weight += DemoCatalog.product(c.getKey()).weight() * c.getValue();
                    first = first == null ? c.getKey() : first;
                }
                EcomListing l = listing(storeId, sku, itemId(d.platform(), ctx), null, b.en(), null, price(b.usd(), d), ful, null, BigDecimal.ONE,
                        mats.get(materials.get(first)), d, ctx);
                l.setComposeType(ListingComposeType.BUNDLE);
                l.setBundleItems(parts);
                Long id = listingService.create(l);
                listingIds.add(id);
                items.put(sku, item(id, storeId, l, null, BigDecimal.ONE, components, weight, 3, first));
            }
            all.put(storeId, items);
        }
        unpaired(ctx, stores, all);
        ctx.put(DemoContributors.LISTINGS, all);
        ctx.put(DemoKeys.LISTING_IDS, listingIds);
    }

    /** 未配对的平台 SKU（新上架的颜色 / 平台自动生成的 SKU），SKU 配对中心会按编码 / 标题推荐物料 */
    private void unpaired(DemoContext ctx, Map<String, Long> stores, Map<Long, Map<String, DemoListings.Item>> all) {
        String[][] rows = {
                {"DEMO-AMZ-US", "VX-PB10-US-PINK", "Voltix Slim Power Bank 10000mAh Pink", "25.99"},
                {"DEMO-AMZ-US", "HL-HM12-US", "Homelux Bamboo Cutting Board Set 3 Pcs", "27.99"},
                {"DEMO-SHP-MY", "PB10-MY-NEW", "Voltix Power Bank 10000mAh Fast Charging", "99.90"},
                {"DEMO-TEMU-US", "DM-OD03", "Sports Water Bottle 1L Tritan BPA Free", "13.99"},
                {"DEMO-AE", "1005006-PT04", "PawJoy Pet Deshedding Brush Self Cleaning", "9.99"},
                {"DEMO-TT-US", "TT-EB01-BLK", "Voltix Wireless Earbuds Pro Noise Cancelling", "42.99"},
                {"DEMO-EBAY-US", "EB-KB01", "Foldable Bluetooth Keyboard Tri-Fold", "33.99"},
                {"DEMO-SHOPIFY", "VX-RL01-18IN", "Voltix 18\" LED Ring Light Kit", "49.99"}};
        for (String[] r : rows) {
            Long storeId = stores.get(r[0]);
            DemoCatalog.Store d = DemoCatalog.store(r[0]);
            EcomListing l = new EcomListing();
            l.setStoreId(storeId);
            l.setSellerSku(r[1]);
            l.setPlatformItemId(itemId(d.platform(), ctx));
            l.setTitle(r[2]);
            l.setPrice(new BigDecimal(r[3]));
            l.setFulfillment(FulfillmentType.SELLER);
            l.setPlatformQty(BigDecimal.valueOf(ctx.between(20, 200)));
            l.setStatus(ListingStatus.ACTIVE);
            Long id = listingService.create(l);
            all.get(storeId).put(r[1], new DemoListings.Item(id, storeId, r[1], l.getPlatformItemId(), r[2], l.getPrice(), FulfillmentType.SELLER,
                    null, BigDecimal.ONE, null, 0.3, 0, null));
        }
    }

    private static boolean bundleFor(DemoCatalog.Store d, DemoCatalog.Bundle b) {
        return switch (b.sku()) {
            case "KIT-CHARGE" -> d.platform() == EcomPlatform.AMAZON || d.platform() == EcomPlatform.SHOPIFY;
            case "KIT-CAT" -> d.platform() == EcomPlatform.SHOPEE || d.platform() == EcomPlatform.TIKTOK || d.platform() == EcomPlatform.SHOPIFY;
            default -> d == DemoCatalog.STORES.get(0) || d.platform() == EcomPlatform.EBAY;
        };
    }

    private static EcomListing listing(Long storeId, String sku, String itemId, String parent, String title, String variant, BigDecimal price,
                                       FulfillmentType ful, Long materialId, BigDecimal packQty, WmsMaterial m, DemoCatalog.Store d, DemoContext ctx) {
        EcomListing l = new EcomListing();
        l.setStoreId(storeId);
        l.setSellerSku(sku);
        l.setPlatformItemId(itemId);
        l.setParentItemId(parent);
        l.setTitle(title);
        l.setVariant(variant);
        l.setPrice(price);
        l.setCurrency(d.currency());
        l.setFulfillment(ful);
        l.setMaterialId(materialId);
        l.setPackQty(packQty);
        l.setComposeType(ListingComposeType.SINGLE);
        l.setPlatformQty(BigDecimal.valueOf(ctx.between(30, 400)));
        l.setStatus(ListingStatus.ACTIVE);
        l.setImageUrl(m == null ? null : m.getImageUrl());
        if (d.platform() == EcomPlatform.AMAZON && ful == FulfillmentType.PLATFORM) {
            l.setFnsku("X00" + code(ctx, 7));
        }
        l.setUrl(url(d, itemId));
        return l;
    }

    private static DemoListings.Item item(Long id, Long storeId, EcomListing l, Long materialId, BigDecimal packQty, Map<Long, BigDecimal> components,
                                          double weight, int popularity, String productCode) {
        return new DemoListings.Item(id, storeId, l.getSellerSku(), l.getPlatformItemId(), l.getTitle(), l.getPrice(), l.getFulfillment(),
                materialId, packQty, components, weight, popularity, productCode);
    }

    private static String sku(DemoCatalog.Product p, DemoCatalog.Store d, String suffix) {
        return PREFIX.get(p.brand()) + "-" + p.code().substring(3) + "-" + tag(d, suffix);
    }

    private static String tag(DemoCatalog.Store d, String suffix) {
        String t = switch (d.platform()) {
            case AMAZON -> d.marketplace();
            case SHOPEE -> "SP" + d.marketplace();
            case TIKTOK -> "TT";
            case TEMU -> "TM";
            case ALIEXPRESS -> "AE";
            case SHOPIFY -> "WEB";
            case EBAY -> "EB";
            default -> "X";
        };
        return suffix == null ? t : t + "-" + suffix;
    }

    /** 当地价格：美元价 × 倍数，日元取整到 10（以 0 结尾的价格减 20），其他取 .99 / .90 */
    static BigDecimal price(double usd, DemoCatalog.Store d) {
        double v = usd * d.fx();
        return switch (d.currency()) {
            case DemoCatalog.JPY -> BigDecimal.valueOf(Math.round(v / 10) * 10 - 20L);
            case "MYR" -> BigDecimal.valueOf(Math.floor(v) + 0.9).setScale(2, RoundingMode.HALF_UP);
            default -> BigDecimal.valueOf(Math.floor(v) + 0.99).setScale(2, RoundingMode.HALF_UP);
        };
    }

    static String itemId(EcomPlatform platform, DemoContext ctx) {
        return switch (platform) {
            case AMAZON -> "B0" + code(ctx, 8);
            case SHOPEE -> String.valueOf(20000000000L + ctx.random().nextInt(999_999_999));
            case TIKTOK -> "17293" + String.format("%014d", Math.abs(ctx.random().nextLong()) % 100_000_000_000_000L);
            case TEMU -> "601" + String.format("%09d", ctx.random().nextInt(999_999_999));
            case ALIEXPRESS -> "100500" + String.format("%07d", ctx.random().nextInt(9_999_999));
            case SHOPIFY -> "8" + String.format("%012d", Math.abs(ctx.random().nextLong()) % 1_000_000_000_000L);
            case EBAY -> "3" + String.format("%011d", Math.abs(ctx.random().nextLong()) % 100_000_000_000L);
            default -> String.valueOf(ctx.random().nextInt(999_999_999));
        };
    }

    private static String url(DemoCatalog.Store d, String itemId) {
        return switch (d.code()) {
            case "DEMO-AMZ-US" -> "https://www.amazon.com/dp/" + itemId;
            case "DEMO-AMZ-DE" -> "https://www.amazon.de/dp/" + itemId;
            case "DEMO-AMZ-JP" -> "https://www.amazon.co.jp/dp/" + itemId;
            case "DEMO-EBAY-US" -> "https://www.ebay.com/itm/" + itemId;
            case "DEMO-AE" -> "https://www.aliexpress.com/item/" + itemId + ".html";
            default -> null;
        };
    }

    private static final String ALNUM = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";

    static String code(DemoContext ctx, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(ALNUM.charAt(ctx.random().nextInt(ALNUM.length())));
        }
        return sb.toString();
    }
}
