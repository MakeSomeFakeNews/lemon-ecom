package com.lemon.erp.modules.ecom.publish.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorSigns;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.connector.StoreCredentials;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 亚马逊 Selling Partner API 的刊登与定价：
 * <ul>
 *   <li>类目：Product Type Definitions API 2020-09-01 —— searchDefinitionsProductTypes（GET /definitions/2020-09-01/productTypes?marketplaceIds=&amp;keywords=）
 *       返回商品类型（Product Type，作为刊登的「类目」）；getDefinitionsProductType（GET /definitions/2020-09-01/productTypes/{type}?requirements=LISTING）
 *       返回 JSON Schema 的下载地址（schema.link.resource），下载后按 required 与 properties 列出属性（title、enum 可选值）；
 *       标题、五点、描述、品牌、条码、价格、库存、图片、变体关系由系统按刊登内容填写，不在属性中列出</li>
 *   <li>发布：Listings Items API 2021-08-01 putListingsItem（PUT /listings/2021-08-01/items/{sellerId}/{sku}?marketplaceIds=&amp;issueLocale=en_US，
 *       body = {productType, requirements: LISTING, attributes}）。多变体时先提交父体（parentage_level = parent、variation_theme），
 *       再逐个提交子体（child_parent_sku_relationship 指向父 SKU）。返回 status = ACCEPTED 为受理成功、INVALID 为失败（issues 中 severity = ERROR 的为原因）。
 *       ASIN 由亚马逊异步生成，发布成功时还没有，之后由「同步商品」补上</li>
 *   <li>下架：deleteListingsItem（DELETE 同一路径）</li>
 *   <li>改价：patchListingsItem（PATCH 同一路径，patches = [{op: replace, path: /attributes/purchasable_offer, value: [...]}]），
 *       productType 取在线商品附加信息中的 productType（本系统发布的会记录），没有时用 PRODUCT</li>
 *   <li>竞品价格：Product Pricing API 2022-05-01 getCompetitiveSummary 批量接口
 *       （POST /batches/products/pricing/2022-05-01/items/competitiveSummary，每批 20 个 ASIN，includedData = featuredBuyingOptions、lowestPricedOffers），
 *       featuredBuyingOptions 的第一个精选报价为购物车价（卖家 ID 等于本店时本店持有购物车），lowestPricedOffers 中去掉本店后最低的「商品价 + 运费」为竞品最低价</li>
 * </ul>
 * 授权复用店铺的渠道授权（region、marketplaceId、clientId、clientSecret、refreshToken），令牌由调用方先用 {@code AmazonSpConnector.ensureToken} 换取。
 * <p>
 * 说明：按亚马逊公开文档实现，尚未用真实卖家账号联调。需要真实账号联调确认：
 * <ul>
 *   <li>各商品类型的必填属性差异很大（如 item_type_keyword、recommended_browse_nodes、batteries_required 等），以 getDefinitionsProductType 返回的 Schema 为准，
 *       复杂结构的属性需要在属性中按 JSON 填写（以 [ 开头的值原样提交）</li>
 *   <li>variation_theme 的取值随商品类型不同（如 SIZE/COLOR、COLOR_NAME），没有填写时按变体属性名大写拼接，可能不被接受</li>
 *   <li>UPC 豁免需要先在卖家中心申请 GTIN 豁免，品牌备案后才能用 supplier_declared_has_product_identifier_exemption</li>
 *   <li>getCompetitiveSummary 需要 Pricing 角色；lowestPricedOffers 只返回前 20 个报价</li>
 * </ul>
 */
@Component
public class AmazonPublishConnector implements PublishConnector {

    static final String PLATFORM = "Amazon";
    static final String LISTINGS = "/listings/2021-08-01/items/";
    static final String DEFINITIONS = "/definitions/2020-09-01/productTypes";
    static final String COMPETITIVE = "/batches/products/pricing/2022-05-01/items/competitiveSummary";
    static final int PRICING_BATCH = 20;
    static final int MAX_ATTRIBUTES = 150;
    static final String DEFAULT_PRODUCT_TYPE = "PRODUCT";
    /** 在线商品附加信息中记录的商品类型 */
    public static final String EXTRA_PRODUCT_TYPE = "productType";
    private static final Map<String, String> ENDPOINTS = Map.of(
            "NA", "https://sellingpartnerapi-na.amazon.com",
            "EU", "https://sellingpartnerapi-eu.amazon.com",
            "FE", "https://sellingpartnerapi-fe.amazon.com");
    private static final Map<String, String> SANDBOX_ENDPOINTS = Map.of(
            "NA", "https://sandbox.sellingpartnerapi-na.amazon.com",
            "EU", "https://sandbox.sellingpartnerapi-eu.amazon.com",
            "FE", "https://sandbox.sellingpartnerapi-fe.amazon.com");
    private static final Map<String, String> AFN_CHANNELS = Map.of("NA", "AMAZON_NA", "EU", "AMAZON_EU", "FE", "AMAZON_JP");
    private static final Map<String, String> LANGUAGES = Map.ofEntries(Map.entry("US", "en_US"), Map.entry("CA", "en_CA"), Map.entry("MX", "es_MX"),
            Map.entry("UK", "en_GB"), Map.entry("GB", "en_GB"), Map.entry("DE", "de_DE"), Map.entry("FR", "fr_FR"), Map.entry("IT", "it_IT"),
            Map.entry("ES", "es_ES"), Map.entry("NL", "nl_NL"), Map.entry("SE", "sv_SE"), Map.entry("PL", "pl_PL"), Map.entry("JP", "ja_JP"),
            Map.entry("AU", "en_AU"), Map.entry("SG", "en_SG"), Map.entry("AE", "en_AE"), Map.entry("IN", "en_IN"), Map.entry("BR", "pt_BR"));
    /** 系统按刊登内容填写的属性，不在类目属性中列出 */
    static final Set<String> MANAGED = Set.of("item_name", "bullet_point", "product_description", "brand", "externally_assigned_product_identifier",
            "supplier_declared_has_product_identifier_exemption", "purchasable_offer", "fulfillment_availability", "main_product_image_locator",
            "condition_type", "child_parent_sku_relationship", "parentage_level", "variation_theme", "merchant_suggested_asin", "list_price");

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.AMAZON;
    }

    @Override
    public Set<PublishCapability> capabilities() {
        return EnumSet.allOf(PublishCapability.class);
    }

    @Override
    public String note() {
        return "ecom.text.pub.note.amazon";
    }

    // ---------------------------------------------------------------- 类目与属性

    @Override
    public List<PlatformCategory> categories(ConnectorContext ctx, String parentId, String keyword) {
        String url = endpoint(ctx) + DEFINITIONS + "?marketplaceIds=" + enc(marketplace(ctx))
                + (keyword == null || keyword.isBlank() ? "" : "&keywords=" + enc(keyword.trim()));
        JsonNode resp = HttpJson.get(url, headers(ctx), PLATFORM);
        List<PlatformCategory> out = new ArrayList<>();
        for (JsonNode t : resp.path("productTypes")) {
            String name = HttpJson.text(t, "name");
            if (name != null) {
                String display = HttpJson.text(t, "displayName");
                out.add(new PlatformCategory(name, display == null ? name : display + " (" + name + ")", null, true, null));
            }
        }
        return out;
    }

    @Override
    public List<PlatformAttribute> attributes(ConnectorContext ctx, String categoryId) {
        String url = endpoint(ctx) + DEFINITIONS + "/" + path(categoryId) + "?marketplaceIds=" + enc(marketplace(ctx))
                + "&requirements=LISTING&requirementsEnforced=ENFORCED&locale=DEFAULT";
        JsonNode def = HttpJson.get(url, headers(ctx), PLATFORM);
        String resource = HttpJson.text(def.path("schema").path("link"), "resource");
        if (resource == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "schema.link.resource missing");
        }
        return parseSchema(HttpJson.get(resource, Map.of(), PLATFORM));
    }

    /** JSON Schema → 属性：required 中的为必填（必填在前），enum 为可选值；系统填写的属性不列出 */
    static List<PlatformAttribute> parseSchema(JsonNode schema) {
        Set<String> required = new java.util.HashSet<>();
        for (JsonNode r : schema.path("required")) {
            required.add(r.asString());
        }
        List<PlatformAttribute> req = new ArrayList<>();
        List<PlatformAttribute> opt = new ArrayList<>();
        for (Map.Entry<String, JsonNode> e : schema.path("properties").properties()) {
            String key = e.getKey();
            if (MANAGED.contains(key) || key.startsWith("other_product_image_locator")) {
                continue;
            }
            JsonNode p = e.getValue();
            JsonNode value = p.path("items").path("properties").path("value");
            List<String> options = new ArrayList<>();
            for (JsonNode o : value.path("enum")) {
                options.add(o.asString());
            }
            String type = HttpJson.text(value, "type");
            String input = !options.isEmpty() ? PlatformAttribute.SELECT
                    : "number".equals(type) || "integer".equals(type) ? PlatformAttribute.NUMBER : PlatformAttribute.TEXT;
            String title = HttpJson.text(p, "title");
            PlatformAttribute a = new PlatformAttribute(key, title == null ? key : title, required.contains(key), input, options,
                    PublishSupport.cut(HttpJson.text(p, "description"), 300));
            (a.required() ? req : opt).add(a);
        }
        List<PlatformAttribute> out = new ArrayList<>(req);
        out.addAll(opt);
        return out.size() > MAX_ATTRIBUTES ? out.subList(0, MAX_ATTRIBUTES) : out;
    }

    // ---------------------------------------------------------------- 发布

    @Override
    public PublishResult publish(ConnectorContext ctx, PublishPayload p) {
        if (p.categoryId() == null) {
            throw new BizException("ecom.error.pub.categoryRequired");
        }
        String marketplace = marketplace(ctx);
        Map<String, Object> common = commonAttributes(p, marketplace);
        List<String> messages = new ArrayList<>();
        Map<String, Map<String, String>> extra = new LinkedHashMap<>();
        if (p.multiVariant()) {
            Map<String, Object> parent = new LinkedHashMap<>(common);
            parent.put("parentage_level", wrap("parent", marketplace));
            parent.put("variation_theme", List.of(Map.of("name", variationTheme(p))));
            String error = put(ctx, p.parentSku(), p.categoryId(), parent);
            if (error != null) {
                return PublishResult.failed(p.parentSku() + ": " + error);
            }
        }
        for (PublishSku sku : p.skus()) {
            Map<String, Object> attrs = new LinkedHashMap<>(common);
            attrs.putAll(skuAttributes(ctx, p, sku, marketplace));
            if (p.multiVariant()) {
                attrs.put("parentage_level", wrap("child", marketplace));
                attrs.put("child_parent_sku_relationship", List.of(Map.of("child_relationship_type", "variation", "parent_sku", p.parentSku(),
                        "marketplace_id", marketplace)));
                attrs.put("variation_theme", List.of(Map.of("name", variationTheme(p))));
                sku.variantAttrs().forEach((k, v) -> attrs.put(attributeKey(k), wrap(v, marketplace)));
            }
            String error = put(ctx, sku.sellerSku(), p.categoryId(), attrs);
            if (error != null) {
                messages.add(sku.sellerSku() + ": " + error);
            } else {
                extra.put(sku.sellerSku(), Map.of(EXTRA_PRODUCT_TYPE, p.categoryId()));
            }
        }
        if (!messages.isEmpty()) {
            return PublishResult.failed(String.join("; ", messages));
        }
        return PublishResult.success(null, null, Map.of(), extra, null);
    }

    /** 提交一个 SKU，受理成功返回 null，否则返回错误 */
    private String put(ConnectorContext ctx, String sku, String productType, Map<String, Object> attributes) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("productType", productType);
        body.put("requirements", "LISTING");
        body.put("attributes", attributes);
        JsonNode resp = HttpJson.putJson(listingUrl(ctx, sku) + "&issueLocale=en_US", headers(ctx), body, PLATFORM);
        return result(resp);
    }

    /** status = ACCEPTED 为成功（返回 null）；否则拼接 severity = ERROR 的 issues */
    static String result(JsonNode resp) {
        String status = HttpJson.text(resp, "status");
        List<String> errors = new ArrayList<>();
        for (JsonNode i : resp.path("issues")) {
            if (!"WARNING".equalsIgnoreCase(HttpJson.text(i, "severity")) && !"INFO".equalsIgnoreCase(HttpJson.text(i, "severity"))) {
                String attrs = i.path("attributeNames").isArray() && !i.path("attributeNames").isEmpty() ? " [" + i.path("attributeNames").get(0).asString() + "]" : "";
                errors.add(HttpJson.text(i, "code") + attrs + " " + HttpJson.text(i, "message"));
            }
        }
        if ("ACCEPTED".equalsIgnoreCase(status) && errors.isEmpty()) {
            return null;
        }
        if (errors.isEmpty()) {
            errors.add(String.valueOf(status));
        }
        return PublishSupport.cut(String.join("; ", errors), 1500);
    }

    /** 父体与子体共用的属性：标题、五点、描述、品牌、图片、自定义属性 */
    static Map<String, Object> commonAttributes(PublishPayload p, String marketplace) {
        String language = LANGUAGES.getOrDefault(p.marketplace() == null ? "US" : p.marketplace().toUpperCase(Locale.ROOT), "en_US");
        Map<String, Object> a = new LinkedHashMap<>();
        // 自定义属性先放，系统字段覆盖同名的
        p.attributes().forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                a.put(k, customValue(v, marketplace));
            }
        });
        a.put("item_name", text(p.title(), language, marketplace));
        if (!p.bullets().isEmpty()) {
            List<Map<String, Object>> bullets = new ArrayList<>();
            for (String b : p.bullets()) {
                if (b != null && !b.isBlank()) {
                    bullets.add(Map.of("value", b.trim(), "language_tag", language, "marketplace_id", marketplace));
                }
            }
            a.put("bullet_point", bullets);
        }
        if (p.description() != null && !p.description().isBlank()) {
            a.put("product_description", text(p.description(), language, marketplace));
        }
        if (p.brand() != null && !p.brand().isBlank()) {
            a.put("brand", text(p.brand(), language, marketplace));
        }
        if (!p.images().isEmpty()) {
            a.put("main_product_image_locator", List.of(Map.of("media_location", p.images().get(0), "marketplace_id", marketplace)));
            for (int i = 1; i < p.images().size() && i <= 8; i++) {
                a.put("other_product_image_locator_" + i, List.of(Map.of("media_location", p.images().get(i), "marketplace_id", marketplace)));
            }
        }
        a.put("condition_type", wrap("new_new", marketplace));
        if (p.upcExempt()) {
            a.put("supplier_declared_has_product_identifier_exemption", List.of(Map.of("value", true, "marketplace_id", marketplace)));
        }
        return a;
    }

    /** 子体（或单品）的属性：条码、价格、库存 / 配送渠道 */
    private static Map<String, Object> skuAttributes(ConnectorContext ctx, PublishPayload p, PublishSku sku, String marketplace) {
        Map<String, Object> a = new LinkedHashMap<>();
        if (!p.upcExempt() && sku.barcode() != null && !sku.barcode().isBlank()) {
            String type = p.barcodeType() == null ? "upc" : p.barcodeType().toLowerCase(Locale.ROOT);
            a.put("externally_assigned_product_identifier", List.of(Map.of("type", type, "value", sku.barcode().trim(), "marketplace_id", marketplace)));
        }
        if (sku.price() != null) {
            a.put("purchasable_offer", offer(sku.price(), p.currency(), marketplace));
        }
        Map<String, Object> fa = new LinkedHashMap<>();
        if (FulfillmentType.PLATFORM.name().equals(p.fulfillment())) {
            fa.put("fulfillment_channel_code", AFN_CHANNELS.getOrDefault(region(ctx), "AMAZON_NA"));
        } else {
            fa.put("fulfillment_channel_code", "DEFAULT");
            fa.put("quantity", sku.qty());
            if (p.handlingDays() != null) {
                fa.put("lead_time_to_ship_max_days", p.handlingDays());
            }
        }
        fa.put("marketplace_id", marketplace);
        a.put("fulfillment_availability", List.of(fa));
        return a;
    }

    static List<Map<String, Object>> offer(BigDecimal price, String currency, String marketplace) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("marketplace_id", marketplace);
        if (currency != null) {
            o.put("currency", currency);
        }
        o.put("our_price", List.of(Map.of("schedule", List.of(Map.of("value_with_tax", price.setScale(2, java.math.RoundingMode.HALF_UP))))));
        return List.of(o);
    }

    /** variation_theme：属性中填写的优先，否则按变体属性名大写拼接（如 COLOR/SIZE） */
    static String variationTheme(PublishPayload p) {
        String theme = p.attribute("variation_theme");
        if (theme != null) {
            return theme;
        }
        List<String> names = new ArrayList<>();
        for (PublishSku s : p.skus()) {
            for (String k : s.variantAttrs().keySet()) {
                String n = attributeKey(k).toUpperCase(Locale.ROOT);
                if (!names.contains(n)) {
                    names.add(n);
                }
            }
        }
        return names.isEmpty() ? "COLOR" : String.join("/", names);
    }

    /** 变体属性名 → 亚马逊属性名（小写、空格改下划线） */
    static String attributeKey(String name) {
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
    }

    private static Object customValue(String v, String marketplace) {
        if (PublishSupport.isJson(v)) {
            return PublishSupport.json(v);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (String s : PublishSupport.values(v)) {
            list.add(Map.of("value", s, "marketplace_id", marketplace));
        }
        return list;
    }

    private static List<Map<String, Object>> wrap(Object value, String marketplace) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("value", value);
        m.put("marketplace_id", marketplace);
        return List.of(m);
    }

    private static List<Map<String, Object>> text(String value, String language, String marketplace) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("value", value);
        m.put("language_tag", language);
        m.put("marketplace_id", marketplace);
        return List.of(m);
    }

    // ---------------------------------------------------------------- 下架与改价

    @Override
    public void offline(ConnectorContext ctx, PublishPayload payload, List<ListingRef> refs) {
        for (ListingRef r : refs) {
            HttpJson.request("DELETE", listingUrl(ctx, r.sellerSku()), headers(ctx), null, PLATFORM);
        }
        if (payload != null && payload.multiVariant() && payload.parentSku() != null) {
            HttpJson.request("DELETE", listingUrl(ctx, payload.parentSku()), headers(ctx), null, PLATFORM);
        }
    }

    @Override
    public List<PriceResult> updatePrices(ConnectorContext ctx, List<PriceUpdate> updates) {
        String marketplace = marketplace(ctx);
        List<PriceResult> out = new ArrayList<>();
        for (PriceUpdate u : updates) {
            try {
                String type = u.ref().extra(EXTRA_PRODUCT_TYPE);
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("productType", type == null ? DEFAULT_PRODUCT_TYPE : type);
                body.put("patches", List.of(Map.of("op", "replace", "path", "/attributes/purchasable_offer",
                        "value", offer(u.price(), u.currency(), marketplace))));
                String error = result(HttpJson.request("PATCH", listingUrl(ctx, u.sellerSku()) + "&issueLocale=en_US", headers(ctx), body, PLATFORM));
                out.add(error == null ? PriceResult.ok(u.sellerSku()) : PriceResult.fail(u.sellerSku(), error));
            } catch (BizException e) {
                out.add(PriceResult.fail(u.sellerSku(), e.getMessage()));
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- 竞品价格

    @Override
    public List<CompetitivePrice> competitivePrices(ConnectorContext ctx, List<ListingRef> refs) {
        String marketplace = marketplace(ctx);
        String me = sellerId(ctx);
        Map<String, List<ListingRef>> byAsin = new LinkedHashMap<>();
        for (ListingRef r : refs) {
            if (r.platformItemId() != null && !r.platformItemId().isBlank()) {
                byAsin.computeIfAbsent(r.platformItemId().trim(), k -> new ArrayList<>()).add(r);
            }
        }
        List<String> asins = new ArrayList<>(byAsin.keySet());
        List<CompetitivePrice> out = new ArrayList<>();
        for (int i = 0; i < asins.size(); i += PRICING_BATCH) {
            List<Map<String, Object>> requests = new ArrayList<>();
            for (String asin : asins.subList(i, Math.min(asins.size(), i + PRICING_BATCH))) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("asin", asin);
                r.put("marketplaceId", marketplace);
                r.put("includedData", List.of("featuredBuyingOptions", "lowestPricedOffers"));
                r.put("lowestPricedOffersInputs", List.of(Map.of("itemCondition", "New", "offerType", "Consumer")));
                r.put("method", "GET");
                r.put("uri", "/products/pricing/2022-05-01/items/competitiveSummary");
                requests.add(r);
            }
            JsonNode resp = HttpJson.postJson(endpoint(ctx) + COMPETITIVE, headers(ctx), Map.of("requests", requests), PLATFORM);
            for (JsonNode r : resp.path("responses")) {
                int code = r.path("status").path("statusCode").asInt(200);
                JsonNode body = r.path("body");
                String asin = HttpJson.text(body, "asin");
                if (code >= 300 || asin == null || !byAsin.containsKey(asin)) {
                    continue;
                }
                CompetitivePrice cp = parseSummary(body, me);
                for (ListingRef ref : byAsin.get(asin)) {
                    out.add(new CompetitivePrice(ref.sellerSku(), asin, cp.lowestPrice(), cp.lowestShipping(), cp.buyboxPrice(), cp.buyboxWinner(),
                            cp.offerCount(), cp.competitor(), cp.currency()));
                }
            }
        }
        return out;
    }

    /** 一个 ASIN 的竞争摘要 → 购物车价（含运费）、本店是否持有、去掉本店后的最低价 */
    static CompetitivePrice parseSummary(JsonNode body, String me) {
        BigDecimal buybox = null;
        boolean winner = false;
        String currency = null;
        for (JsonNode option : body.path("featuredBuyingOptions")) {
            JsonNode offer = option.path("segmentedFeaturedOffers").path(0);
            if (offer.isMissingNode()) {
                continue;
            }
            buybox = add(HttpJson.decimal(offer, "listingPrice", "amount"), shipping(offer));
            currency = HttpJson.text(offer.path("listingPrice"), "currencyCode");
            winner = me != null && me.equals(HttpJson.text(offer, "sellerId"));
            break;
        }
        BigDecimal lowest = null;
        BigDecimal lowestShipping = null;
        String competitor = null;
        int count = 0;
        for (JsonNode group : body.path("lowestPricedOffers")) {
            for (JsonNode offer : group.path("offers")) {
                count++;
                if (me != null && me.equals(HttpJson.text(offer, "sellerId"))) {
                    continue;
                }
                BigDecimal price = HttpJson.decimal(offer, "listingPrice", "amount");
                if (price == null) {
                    continue;
                }
                BigDecimal ship = shipping(offer);
                BigDecimal total = add(price, ship);
                if (lowest == null || total.compareTo(add(lowest, lowestShipping)) < 0) {
                    lowest = price;
                    lowestShipping = ship;
                    competitor = HttpJson.text(offer, "sellerId");
                    if (currency == null) {
                        currency = HttpJson.text(offer.path("listingPrice"), "currencyCode");
                    }
                }
            }
        }
        return new CompetitivePrice(null, HttpJson.text(body, "asin"), lowest, lowestShipping, buybox, winner, count == 0 ? null : count, competitor, currency);
    }

    private static BigDecimal shipping(JsonNode offer) {
        for (JsonNode s : offer.path("shippingOptions")) {
            BigDecimal v = HttpJson.decimal(s, "price", "amount");
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static BigDecimal add(BigDecimal a, BigDecimal b) {
        return a == null ? b : b == null ? a : a.add(b);
    }

    // ---------------------------------------------------------------- 地址与授权

    private static String listingUrl(ConnectorContext ctx, String sku) {
        return endpoint(ctx) + LISTINGS + path(sellerId(ctx)) + "/" + path(sku) + "?marketplaceIds=" + enc(marketplace(ctx));
    }

    private static Map<String, String> headers(ConnectorContext ctx) {
        Map<String, String> h = new HashMap<>();
        h.put("x-amz-access-token", ctx.credentials().require("accessToken"));
        h.put("Accept", "application/json");
        return h;
    }

    private static String marketplace(ConnectorContext ctx) {
        return ctx.credentials().require("marketplaceId");
    }

    private static String sellerId(ConnectorContext ctx) {
        String id = ctx.store() == null ? null : ctx.store().getSellerId();
        return id != null && !id.isBlank() ? id.trim() : ctx.credentials().require("sellingPartnerId");
    }

    private static String region(ConnectorContext ctx) {
        StoreCredentials c = ctx.credentials();
        return c.get("region") == null ? "NA" : c.get("region").toUpperCase(Locale.ROOT);
    }

    private static String endpoint(ConnectorContext ctx) {
        if (ctx.baseUrlOverride() != null) {
            return ctx.baseUrlOverride();
        }
        String endpoint = (ctx.sandbox() ? SANDBOX_ENDPOINTS : ENDPOINTS).get(region(ctx));
        if (endpoint == null) {
            throw new BizException("ecom.error.connectorRegionInvalid", region(ctx));
        }
        return endpoint;
    }

    private static String enc(String s) {
        return ConnectorSigns.enc(s);
    }

    private static String path(String s) {
        return ConnectorSigns.enc(s).replace("+", "%20");
    }
}
