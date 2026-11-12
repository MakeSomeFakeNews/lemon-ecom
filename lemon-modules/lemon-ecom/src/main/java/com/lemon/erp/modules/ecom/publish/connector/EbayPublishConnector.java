package com.lemon.erp.modules.ecom.publish.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorJson;
import com.lemon.erp.modules.ecom.connector.ConnectorSigns;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * eBay 的刊登与改价（RESTful Sell / Commerce API）：
 * <ul>
 *   <li>类目：Taxonomy API —— get_default_category_tree_id 取站点的类目树，按关键字 get_category_suggestions 搜索，
 *       按上级 get_category_subtree 浏览；属性：get_item_aspects_for_category（aspectConstraint.aspectRequired 为必填，aspectValues 为可选值）</li>
 *   <li>发布：Inventory API —— 每个 SKU 先 PUT /sell/inventory/v1/inventory_item/{sku}（商品信息、条码、图片、库存）；多变体时再
 *       PUT /sell/inventory/v1/inventory_item_group/{父 SKU}（variesBy 规格）；然后每个 SKU POST /sell/inventory/v1/offer
 *       （站点、一口价、类目、listingPolicies：运费 fulfillmentPolicyId = 运费模板、退货 returnPolicyId = 退货政策、付款 paymentPolicyId、
 *       merchantLocationKey 取其他参数），最后发布：单品 POST /offer/{offerId}/publish，多变体 POST /offer/publish_by_inventory_item_group，返回 listingId</li>
 *   <li>改价：POST /sell/inventory/v1/bulk_update_price_quantity（每批 25 个，offerId 取在线商品附加信息，没有时按 SKU 查询 GET /offer?sku=）；
 *       下架：POST /offer/{offerId}/withdraw（多变体 withdraw_by_inventory_item_group）</li>
 * </ul>
 * 授权复用店铺的渠道授权（OAuth 用户令牌 accessToken，作用域需含 sell.inventory），令牌由调用方先用 {@code EbayConnector.ensureToken} 刷新。
 * 站点（marketplaceId，如 EBAY_US）取其他参数 / 授权信息 marketplaceId，没有时按店铺站点推算。
 * <p>
 * 说明：按 eBay 开发者文档实现，尚未用真实卖家账号联调。需要真实账号联调确认：
 * <ul>
 *   <li>卖家需要先开通 Business Policies 并在后台建好运费 / 退货 / 付款政策，以及 inventory location（merchantLocationKey）</li>
 *   <li>同一 SKU 已有 offer 时 createOffer 返回错误 25002，需要改用 updateOffer（代码按已有 offer 查询后继续发布）</li>
 * </ul>
 */
@Component
public class EbayPublishConnector implements PublishConnector {

    static final String PLATFORM = "eBay";
    static final String PRODUCTION = "https://api.ebay.com";
    static final String SANDBOX = "https://api.sandbox.ebay.com";
    static final String TAXONOMY = "/commerce/taxonomy/v1";
    static final String INVENTORY = "/sell/inventory/v1";
    static final int PRICE_BATCH = 25;
    public static final String EXTRA_OFFER_ID = "offerId";
    private static final Map<String, String> MARKETPLACES = Map.ofEntries(Map.entry("US", "EBAY_US"), Map.entry("UK", "EBAY_GB"),
            Map.entry("GB", "EBAY_GB"), Map.entry("DE", "EBAY_DE"), Map.entry("FR", "EBAY_FR"), Map.entry("IT", "EBAY_IT"), Map.entry("ES", "EBAY_ES"),
            Map.entry("AU", "EBAY_AU"), Map.entry("CA", "EBAY_CA"), Map.entry("AT", "EBAY_AT"), Map.entry("CH", "EBAY_CH"), Map.entry("NL", "EBAY_NL"));

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.EBAY;
    }

    @Override
    public Set<PublishCapability> capabilities() {
        return EnumSet.of(PublishCapability.CATEGORIES, PublishCapability.ATTRIBUTES, PublishCapability.PUBLISH, PublishCapability.OFFLINE,
                PublishCapability.PRICE);
    }

    @Override
    public String note() {
        return "ecom.text.pub.note.ebay";
    }

    // ---------------------------------------------------------------- 类目与属性

    @Override
    public List<PlatformCategory> categories(ConnectorContext ctx, String parentId, String keyword) {
        String tree = treeId(ctx);
        List<PlatformCategory> out = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            JsonNode resp = HttpJson.get(base(ctx) + TAXONOMY + "/category_tree/" + tree + "/get_category_suggestions?q=" + ConnectorSigns.enc(keyword.trim()),
                    headers(ctx), PLATFORM);
            for (JsonNode s : resp.path("categorySuggestions")) {
                JsonNode c = s.path("category");
                List<String> names = new ArrayList<>();
                for (JsonNode a : s.path("categoryTreeNodeAncestors")) {
                    names.add(0, HttpJson.text(a, "categoryName"));
                }
                names.add(HttpJson.text(c, "categoryName"));
                out.add(new PlatformCategory(HttpJson.text(c, "categoryId"), HttpJson.text(c, "categoryName"), null, true, String.join(" > ", names)));
            }
        } else if (parentId != null && !parentId.isBlank()) {
            JsonNode resp = HttpJson.get(base(ctx) + TAXONOMY + "/category_tree/" + tree + "/get_category_subtree?category_id=" + ConnectorSigns.enc(parentId),
                    headers(ctx), PLATFORM);
            for (JsonNode n : resp.path("categorySubtreeNode").path("childCategoryTreeNodes")) {
                JsonNode c = n.path("category");
                out.add(new PlatformCategory(HttpJson.text(c, "categoryId"), HttpJson.text(c, "categoryName"), parentId,
                        n.path("leafCategoryTreeNode").asBoolean(false), null));
            }
        }
        return out;
    }

    @Override
    public List<PlatformAttribute> attributes(ConnectorContext ctx, String categoryId) {
        JsonNode resp = HttpJson.get(base(ctx) + TAXONOMY + "/category_tree/" + treeId(ctx) + "/get_item_aspects_for_category?category_id="
                + ConnectorSigns.enc(categoryId), headers(ctx), PLATFORM);
        List<PlatformAttribute> req = new ArrayList<>();
        List<PlatformAttribute> opt = new ArrayList<>();
        for (JsonNode a : resp.path("aspects")) {
            JsonNode c = a.path("aspectConstraint");
            List<String> options = new ArrayList<>();
            for (JsonNode v : a.path("aspectValues")) {
                options.add(HttpJson.text(v, "localizedValue"));
            }
            boolean multi = "MULTI".equals(HttpJson.text(c, "itemToAspectCardinality"));
            boolean free = "FREE_TEXT".equals(HttpJson.text(c, "aspectMode"));
            String input = multi ? PlatformAttribute.MULTI : options.isEmpty() || free ? PlatformAttribute.TEXT : PlatformAttribute.SELECT;
            String name = HttpJson.text(a, "localizedAspectName");
            PlatformAttribute pa = new PlatformAttribute(name, name, c.path("aspectRequired").asBoolean(false), input,
                    options.size() > 200 ? options.subList(0, 200) : options, free && !options.isEmpty() ? "FREE_TEXT" : null);
            (pa.required() ? req : opt).add(pa);
        }
        req.addAll(opt);
        return req;
    }

    private String treeId(ConnectorContext ctx) {
        JsonNode resp = HttpJson.get(base(ctx) + TAXONOMY + "/get_default_category_tree_id?marketplace_id=" + marketplace(ctx, null), headers(ctx), PLATFORM);
        String id = HttpJson.text(resp, "categoryTreeId");
        if (id == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "categoryTreeId missing");
        }
        return id;
    }

    // ---------------------------------------------------------------- 发布

    @Override
    public PublishResult publish(ConnectorContext ctx, PublishPayload p) {
        if (p.categoryId() == null) {
            throw new BizException("ecom.error.pub.categoryRequired");
        }
        String marketplace = marketplace(ctx, p);
        Map<String, List<String>> aspects = new LinkedHashMap<>();
        p.attributes().forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                aspects.put(k, PublishSupport.values(v));
            }
        });
        if (p.brand() != null && !p.brand().isBlank()) {
            aspects.putIfAbsent("Brand", List.of(p.brand()));
        }
        for (PublishSku s : p.skus()) {
            Map<String, List<String>> a = new LinkedHashMap<>(aspects);
            s.variantAttrs().forEach((k, v) -> a.put(k, List.of(v)));
            Map<String, Object> product = new LinkedHashMap<>();
            product.put("title", PublishSupport.cut(p.title(), 80));
            product.put("description", TikTokPublishConnector.html(p));
            product.put("aspects", a);
            if (p.brand() != null) {
                product.put("brand", p.brand());
            }
            if (!p.upcExempt() && s.barcode() != null && !s.barcode().isBlank()) {
                String key = "EAN".equals(p.barcodeType()) ? "ean" : "ISBN".equals(p.barcodeType()) ? "isbn" : "upc";
                product.put(key, List.of(s.barcode().trim()));
            }
            product.put("imageUrls", p.images());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("availability", Map.of("shipToLocationAvailability", Map.of("quantity", s.qty())));
            item.put("condition", "NEW");
            item.put("product", product);
            HttpJson.putJson(base(ctx) + INVENTORY + "/inventory_item/" + path(s.sellerSku()), headers(ctx), item, PLATFORM);
        }
        if (p.multiVariant()) {
            Map<String, List<String>> specs = new LinkedHashMap<>();
            for (PublishSku s : p.skus()) {
                s.variantAttrs().forEach((k, v) -> {
                    List<String> vals = specs.computeIfAbsent(k, x -> new ArrayList<>());
                    if (!vals.contains(v)) {
                        vals.add(v);
                    }
                });
            }
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("title", PublishSupport.cut(p.title(), 80));
            group.put("description", TikTokPublishConnector.html(p));
            group.put("imageUrls", p.images());
            group.put("aspects", aspects);
            group.put("variantSKUs", p.skus().stream().map(PublishSku::sellerSku).toList());
            group.put("variesBy", Map.of("specifications", specs.entrySet().stream().map(e -> Map.of("name", e.getKey(), "values", e.getValue())).toList()));
            HttpJson.putJson(base(ctx) + INVENTORY + "/inventory_item_group/" + path(p.parentSku()), headers(ctx), group, PLATFORM);
        }
        Map<String, Map<String, String>> extra = new LinkedHashMap<>();
        Map<String, String> skuIds = new LinkedHashMap<>();
        String firstOffer = null;
        for (PublishSku s : p.skus()) {
            String offerId = createOffer(ctx, p, s, marketplace);
            firstOffer = firstOffer == null ? offerId : firstOffer;
            extra.put(s.sellerSku(), Map.of(EXTRA_OFFER_ID, offerId));
            skuIds.put(s.sellerSku(), s.sellerSku());
        }
        JsonNode published = p.multiVariant()
                ? HttpJson.postJson(base(ctx) + INVENTORY + "/offer/publish_by_inventory_item_group", headers(ctx),
                Map.of("inventoryItemGroupKey", p.parentSku(), "marketplaceId", marketplace), PLATFORM)
                : HttpJson.postJson(base(ctx) + INVENTORY + "/offer/" + path(firstOffer) + "/publish", headers(ctx), Map.of(), PLATFORM);
        String listingId = HttpJson.text(published, "listingId");
        return PublishResult.success(listingId, listingId == null ? null : "https://www.ebay.com/itm/" + listingId, skuIds, extra, null);
    }

    private String createOffer(ConnectorContext ctx, PublishPayload p, PublishSku s, String marketplace) {
        Map<String, Object> offer = new LinkedHashMap<>();
        offer.put("sku", s.sellerSku());
        offer.put("marketplaceId", marketplace);
        offer.put("format", "FIXED_PRICE");
        offer.put("availableQuantity", s.qty());
        offer.put("categoryId", p.categoryId());
        offer.put("listingDescription", TikTokPublishConnector.html(p));
        Map<String, Object> policies = new LinkedHashMap<>();
        if (p.shippingTemplate() != null) {
            policies.put("fulfillmentPolicyId", p.shippingTemplate());
        }
        if (p.returnPolicy() != null) {
            policies.put("returnPolicyId", p.returnPolicy());
        }
        if (p.extra("paymentPolicyId") != null) {
            policies.put("paymentPolicyId", p.extra("paymentPolicyId"));
        }
        offer.put("listingPolicies", policies);
        offer.put("pricingSummary", Map.of("price", Map.of("value", PublishSupport.money(s.price()), "currency", p.currency())));
        if (p.extra("merchantLocationKey") != null) {
            offer.put("merchantLocationKey", p.extra("merchantLocationKey"));
        }
        try {
            String id = HttpJson.text(HttpJson.postJson(base(ctx) + INVENTORY + "/offer", headers(ctx), offer, PLATFORM), "offerId");
            if (id != null) {
                return id;
            }
        } catch (BizException e) {
            // 已有 offer（错误 25002）时按 SKU 查询沿用
            String existing = findOffer(ctx, s.sellerSku(), marketplace);
            if (existing == null) {
                throw e;
            }
            return existing;
        }
        throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "offerId missing");
    }

    private String findOffer(ConnectorContext ctx, String sku, String marketplace) {
        try {
            JsonNode resp = HttpJson.get(base(ctx) + INVENTORY + "/offer?sku=" + ConnectorSigns.enc(sku) + "&marketplace_id=" + marketplace, headers(ctx), PLATFORM);
            return HttpJson.text(resp.path("offers").path(0), "offerId");
        } catch (BizException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- 下架与改价

    @Override
    public void offline(ConnectorContext ctx, PublishPayload payload, List<ListingRef> refs) {
        if (payload != null && payload.multiVariant() && payload.parentSku() != null) {
            HttpJson.postJson(base(ctx) + INVENTORY + "/offer/withdraw_by_inventory_item_group", headers(ctx),
                    Map.of("inventoryItemGroupKey", payload.parentSku(), "marketplaceId", marketplace(ctx, payload)), PLATFORM);
            return;
        }
        for (ListingRef r : refs) {
            String offerId = ConnectorJson.firstNonBlank(r.extra(EXTRA_OFFER_ID), findOffer(ctx, r.sellerSku(), marketplace(ctx, payload)));
            if (offerId == null) {
                throw new BizException("ecom.error.connectorCallFailed", PLATFORM, r.sellerSku() + ": offerId missing");
            }
            HttpJson.postJson(base(ctx) + INVENTORY + "/offer/" + path(offerId) + "/withdraw", headers(ctx), Map.of(), PLATFORM);
        }
    }

    @Override
    public List<PriceResult> updatePrices(ConnectorContext ctx, List<PriceUpdate> updates) {
        String marketplace = marketplace(ctx, null);
        List<PriceResult> out = new ArrayList<>();
        List<Map<String, Object>> requests = new ArrayList<>();
        List<PriceUpdate> batch = new ArrayList<>();
        for (PriceUpdate u : updates) {
            String offerId = ConnectorJson.firstNonBlank(u.ref().extra(EXTRA_OFFER_ID), findOffer(ctx, u.sellerSku(), marketplace));
            if (offerId == null) {
                out.add(PriceResult.fail(u.sellerSku(), "offerId missing"));
                continue;
            }
            requests.add(Map.of("sku", u.sellerSku(), "offers", List.of(Map.of("offerId", offerId,
                    "price", Map.of("value", PublishSupport.money(u.price()), "currency", u.currency())))));
            batch.add(u);
            if (requests.size() == PRICE_BATCH) {
                out.addAll(sendPrices(ctx, requests, batch));
                requests = new ArrayList<>();
                batch = new ArrayList<>();
            }
        }
        if (!requests.isEmpty()) {
            out.addAll(sendPrices(ctx, requests, batch));
        }
        return out;
    }

    private List<PriceResult> sendPrices(ConnectorContext ctx, List<Map<String, Object>> requests, List<PriceUpdate> batch) {
        List<PriceResult> out = new ArrayList<>();
        try {
            JsonNode resp = HttpJson.postJson(base(ctx) + INVENTORY + "/bulk_update_price_quantity", headers(ctx), Map.of("requests", requests), PLATFORM);
            Map<String, String> errors = new LinkedHashMap<>();
            for (JsonNode r : resp.path("responses")) {
                int code = r.path("statusCode").asInt(200);
                if (code >= 300) {
                    errors.put(HttpJson.text(r, "sku"), HttpJson.abbreviate(r.path("errors").toString()));
                }
            }
            for (PriceUpdate u : batch) {
                out.add(errors.containsKey(u.sellerSku()) ? PriceResult.fail(u.sellerSku(), errors.get(u.sellerSku())) : PriceResult.ok(u.sellerSku()));
            }
        } catch (BizException e) {
            batch.forEach(u -> out.add(PriceResult.fail(u.sellerSku(), e.getMessage())));
        }
        return out;
    }

    // ---------------------------------------------------------------- HTTP

    private static String marketplace(ConnectorContext ctx, PublishPayload p) {
        String m = ConnectorJson.firstNonBlank(p == null ? null : p.extra("marketplaceId"), ctx.credentials().get("marketplaceId"));
        if (m != null) {
            return m;
        }
        String site = ctx.store() == null || ctx.store().getMarketplace() == null ? "US" : ctx.store().getMarketplace().toUpperCase(Locale.ROOT);
        return MARKETPLACES.getOrDefault(site, "EBAY_US");
    }

    private static String base(ConnectorContext ctx) {
        return PublishSupport.baseUrl(ctx, PRODUCTION, SANDBOX);
    }

    private static Map<String, String> headers(ConnectorContext ctx) {
        return Map.of("Authorization", "Bearer " + ctx.credentials().require("accessToken"), "Accept", "application/json",
                "Content-Language", "en-US");
    }

    private static String path(String s) {
        return ConnectorSigns.enc(s).replace("+", "%20");
    }
}
