package com.lemon.erp.modules.ecom.publish.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorJson;
import com.lemon.erp.modules.ecom.connector.ConnectorSigns;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.connector.StoreCredentials;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 虾皮 Shopee Open Platform v2 的刊登与改价：
 * <ul>
 *   <li>类目：GET /api/v2/product/get_category（language = en，全量返回，按上级 ID 或关键字过滤）；
 *       属性：GET /api/v2/product/get_attributes?category_id=（attribute_list，is_mandatory 为必填，attribute_value_list 为可选值）</li>
 *   <li>图片：先把每张图片下载后上传到图片空间 POST /api/v2/media_space/upload_image（multipart，字段 image，商户级签名
 *       HMAC-SHA256(partner_key, partner_id + path + timestamp)），取 image_info.image_id</li>
 *   <li>发布：POST /api/v2/product/add_item（item_name、description、original_price、seller_stock、weight、image.image_id_list、category_id、
 *       attribute_list、brand、logistic_info、condition、days_to_ship）；多变体时再调 POST /api/v2/product/init_tier_variation
 *       建立规格（最多两层，按变体属性名分层）与 model（model_sku、original_price、seller_stock），之后新增规格用 add_model。
 *       下拉类属性要提交 value_id：发布时重新查询类目属性，按值的名称换成 value_id，找不到的按自定义值（value_id = 0）提交</li>
 *   <li>下架：POST /api/v2/product/unlist_item；改价：POST /api/v2/product/update_price（price_list 按 model_id，单品为 0，
 *       failure_list 中为失败的规格）</li>
 * </ul>
 * 授权复用店铺的渠道授权（partnerId、partnerKey、shopId、accessToken），令牌由调用方先用 {@code ShopeeConnector.ensureToken} 刷新。
 * 物流渠道 logistic_id 取运费模板（多个用逗号分隔），重量（kg）取属性 / 其他参数中的 weight（默认 0.5），品牌 ID 取其他参数 brandId（没有时按 0 + 品牌名称）。
 * <p>
 * 说明：按开放平台公开文档实现，尚未用真实店铺联调。需要真实账号联调确认：
 * <ul>
 *   <li>新版类目 / 属性接口（get_attribute_tree、get_brand_list）在部分站点替代了 get_attributes，返回结构不同</li>
 *   <li>upload_image 的返回结构（image_info.image_id 或 image_info_list）与图片尺寸要求（至少 500 × 500）</li>
 *   <li>init_tier_variation 的 tier_variation / model 字段名（新版为 standardise_tier_variation）</li>
 * </ul>
 */
@Component
public class ShopeePublishConnector implements PublishConnector {

    static final String PLATFORM = "Shopee";
    static final String PRODUCTION = "https://partner.shopeemobile.com";
    static final String SANDBOX = "https://partner.test-stable.shopeemobile.com";
    static final String CATEGORY = "/api/v2/product/get_category";
    static final String ATTRIBUTES = "/api/v2/product/get_attributes";
    static final String UPLOAD_IMAGE = "/api/v2/media_space/upload_image";
    static final String ADD_ITEM = "/api/v2/product/add_item";
    static final String INIT_TIER = "/api/v2/product/init_tier_variation";
    static final String UNLIST = "/api/v2/product/unlist_item";
    static final String UPDATE_PRICE = "/api/v2/product/update_price";
    static final String DEFAULT_WEIGHT = "0.5";

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.SHOPEE;
    }

    @Override
    public Set<PublishCapability> capabilities() {
        return EnumSet.of(PublishCapability.CATEGORIES, PublishCapability.ATTRIBUTES, PublishCapability.PUBLISH, PublishCapability.OFFLINE,
                PublishCapability.PRICE);
    }

    // ---------------------------------------------------------------- 类目与属性

    @Override
    public List<PlatformCategory> categories(ConnectorContext ctx, String parentId, String keyword) {
        JsonNode resp = get(ctx, CATEGORY, "language=en");
        Map<String, JsonNode> byId = new HashMap<>();
        for (JsonNode c : resp.path("category_list")) {
            byId.put(HttpJson.text(c, "category_id"), c);
        }
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase(Locale.ROOT);
        String parent = parentId == null || parentId.isBlank() ? "0" : parentId;
        List<PlatformCategory> out = new ArrayList<>();
        for (JsonNode c : resp.path("category_list")) {
            boolean leaf = !c.path("has_children").asBoolean(false);
            String name = name(c);
            if (kw != null ? leaf && name.toLowerCase(Locale.ROOT).contains(kw) : parent.equals(HttpJson.text(c, "parent_category_id"))) {
                String pid = HttpJson.text(c, "parent_category_id");
                out.add(new PlatformCategory(HttpJson.text(c, "category_id"), name, "0".equals(pid) ? null : pid, leaf, path(c, byId)));
            }
        }
        return out;
    }

    private static String name(JsonNode c) {
        return ConnectorJson.firstNonBlank(HttpJson.text(c, "display_category_name"), HttpJson.text(c, "original_category_name"),
                HttpJson.text(c, "category_id"));
    }

    private static String path(JsonNode c, Map<String, JsonNode> byId) {
        List<String> names = new ArrayList<>();
        JsonNode cur = c;
        for (int i = 0; cur != null && i < 10; i++) {
            names.add(0, name(cur));
            cur = byId.get(HttpJson.text(cur, "parent_category_id"));
        }
        return String.join(" > ", names);
    }

    @Override
    public List<PlatformAttribute> attributes(ConnectorContext ctx, String categoryId) {
        List<PlatformAttribute> req = new ArrayList<>();
        List<PlatformAttribute> opt = new ArrayList<>();
        for (JsonNode a : rawAttributes(ctx, categoryId)) {
            List<String> options = new ArrayList<>();
            for (JsonNode v : a.path("attribute_value_list")) {
                options.add(valueName(v));
            }
            String type = String.valueOf(HttpJson.text(a, "input_type"));
            String input = type.startsWith("MULTIPLE") ? PlatformAttribute.MULTI : !options.isEmpty() ? PlatformAttribute.SELECT
                    : "INT_TYPE".equals(HttpJson.text(a, "input_validation_type")) || "FLOAT_TYPE".equals(HttpJson.text(a, "input_validation_type"))
                    ? PlatformAttribute.NUMBER : PlatformAttribute.TEXT;
            PlatformAttribute pa = new PlatformAttribute(HttpJson.text(a, "attribute_id"),
                    ConnectorJson.firstNonBlank(HttpJson.text(a, "display_attribute_name"), HttpJson.text(a, "original_attribute_name")),
                    a.path("is_mandatory").asBoolean(false), input, options, HttpJson.text(a, "attribute_unit"));
            (pa.required() ? req : opt).add(pa);
        }
        req.addAll(opt);
        return req;
    }

    private JsonNode rawAttributes(ConnectorContext ctx, String categoryId) {
        return get(ctx, ATTRIBUTES, "category_id=" + ConnectorSigns.enc(categoryId) + "&language=en").path("attribute_list");
    }

    private static String valueName(JsonNode v) {
        return ConnectorJson.firstNonBlank(HttpJson.text(v, "display_value_name"), HttpJson.text(v, "original_value_name"), HttpJson.text(v, "value_id"));
    }

    // ---------------------------------------------------------------- 发布

    @Override
    public PublishResult publish(ConnectorContext ctx, PublishPayload p) {
        if (p.categoryId() == null) {
            throw new BizException("ecom.error.pub.categoryRequired");
        }
        List<Long> imageIds = new ArrayList<>();
        for (String url : p.images()) {
            imageIds.add(uploadImage(ctx, url));
        }
        PublishSku first = p.skus().get(0);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("item_name", p.title());
        body.put("description", description(p));
        body.put("original_price", first.price());
        body.put("seller_stock", List.of(Map.of("stock", p.skus().stream().mapToInt(PublishSku::qty).sum())));
        body.put("weight", new BigDecimal(ConnectorJson.firstNonBlank(p.attribute("weight"), p.extra("weight"), DEFAULT_WEIGHT)));
        if (!p.multiVariant()) {
            body.put("item_sku", first.sellerSku());
        }
        body.put("image", Map.of("image_id_list", imageIds.stream().map(String::valueOf).toList()));
        body.put("category_id", parseLong(p.categoryId()));
        List<Map<String, Object>> attrs = attributeList(ctx, p);
        if (!attrs.isEmpty()) {
            body.put("attribute_list", attrs);
        }
        Map<String, Object> brand = new LinkedHashMap<>();
        brand.put("brand_id", parseLong(ConnectorJson.firstNonBlank(p.extra("brandId"), "0")));
        brand.put("original_brand_name", p.brand() == null || p.brand().isBlank() ? "NoBrand" : p.brand());
        body.put("brand", brand);
        List<Map<String, Object>> logistics = new ArrayList<>();
        for (String id : PublishSupport.values(p.shippingTemplate())) {
            logistics.add(Map.of("logistic_id", parseLong(id), "enabled", true));
        }
        if (!logistics.isEmpty()) {
            body.put("logistic_info", logistics);
        }
        body.put("condition", "NEW");
        body.put("item_status", "NORMAL");
        if (p.handlingDays() != null) {
            body.put("pre_order", Map.of("is_pre_order", p.handlingDays() > 3, "days_to_ship", p.handlingDays()));
        }
        JsonNode resp = post(ctx, ADD_ITEM, body);
        String itemId = HttpJson.text(resp, "item_id");
        if (itemId == null) {
            return PublishResult.failed("item_id missing");
        }
        Map<String, String> skuIds = new LinkedHashMap<>();
        if (p.multiVariant()) {
            skuIds.putAll(initTiers(ctx, itemId, p));
        } else {
            skuIds.put(first.sellerSku(), itemId);
        }
        return PublishResult.success(itemId, null, skuIds, Map.of(), null);
    }

    private static String description(PublishPayload p) {
        StringBuilder sb = new StringBuilder();
        for (String b : p.bullets()) {
            if (b != null && !b.isBlank()) {
                sb.append("- ").append(b.trim()).append('\n');
            }
        }
        if (p.description() != null) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append(p.description());
        }
        return sb.isEmpty() ? p.title() : sb.toString();
    }

    /** 属性：键为 attribute_id，值的名称按类目属性换成 value_id（找不到为自定义值 0） */
    private List<Map<String, Object>> attributeList(ConnectorContext ctx, PublishPayload p) {
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Map<String, Long>> valueIds = new HashMap<>();
        boolean any = p.attributes().keySet().stream().anyMatch(k -> k.chars().allMatch(Character::isDigit));
        if (any) {
            for (JsonNode a : rawAttributes(ctx, p.categoryId())) {
                Map<String, Long> names = new HashMap<>();
                for (JsonNode v : a.path("attribute_value_list")) {
                    names.put(valueName(v).toLowerCase(Locale.ROOT), v.path("value_id").asLong(0));
                    String original = HttpJson.text(v, "original_value_name");
                    if (original != null) {
                        names.put(original.toLowerCase(Locale.ROOT), v.path("value_id").asLong(0));
                    }
                }
                valueIds.put(HttpJson.text(a, "attribute_id"), names);
            }
        }
        p.attributes().forEach((k, v) -> {
            if (v == null || v.isBlank() || !k.chars().allMatch(Character::isDigit)) {
                return;
            }
            List<Map<String, Object>> values = new ArrayList<>();
            for (String s : PublishSupport.values(v)) {
                long id = valueIds.getOrDefault(k, Map.of()).getOrDefault(s.toLowerCase(Locale.ROOT), 0L);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("value_id", id);
                if (id == 0) {
                    m.put("original_value_name", s);
                }
                values.add(m);
            }
            out.add(Map.of("attribute_id", Long.parseLong(k), "attribute_value_list", values));
        });
        return out;
    }

    /** 建立规格：按变体属性名分层（最多两层），返回 model_sku → model_id */
    private Map<String, String> initTiers(ConnectorContext ctx, String itemId, PublishPayload p) {
        List<String> names = new ArrayList<>();
        for (PublishSku s : p.skus()) {
            for (String k : s.variantAttrs().keySet()) {
                if (!names.contains(k) && names.size() < 2) {
                    names.add(k);
                }
            }
        }
        if (names.isEmpty()) {
            names.add("Option");
        }
        List<List<String>> options = new ArrayList<>();
        names.forEach(n -> options.add(new ArrayList<>()));
        List<Map<String, Object>> models = new ArrayList<>();
        for (PublishSku s : p.skus()) {
            List<Integer> index = new ArrayList<>();
            for (int i = 0; i < names.size(); i++) {
                String v = s.variantAttrs().isEmpty() ? ConnectorJson.firstNonBlank(s.variantText(), s.sellerSku()) : s.variantAttrs().get(names.get(i));
                v = v == null || v.isBlank() ? "-" : v.trim();
                int idx = options.get(i).indexOf(v);
                if (idx < 0) {
                    options.get(i).add(v);
                    idx = options.get(i).size() - 1;
                }
                index.add(idx);
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("tier_index", index);
            m.put("original_price", s.price());
            m.put("model_sku", s.sellerSku());
            m.put("seller_stock", List.of(Map.of("stock", s.qty())));
            models.add(m);
        }
        List<Map<String, Object>> tiers = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            tiers.add(Map.of("name", names.get(i), "option_list", options.get(i).stream().map(o -> Map.of("option", o)).toList()));
        }
        JsonNode resp = post(ctx, INIT_TIER, Map.of("item_id", Long.parseLong(itemId), "tier_variation", tiers, "model", models));
        Map<String, String> out = new LinkedHashMap<>();
        for (JsonNode m : resp.path("model")) {
            String sku = HttpJson.text(m, "model_sku");
            if (sku != null) {
                out.put(sku, HttpJson.text(m, "model_id"));
            }
        }
        return out;
    }

    private long uploadImage(ConnectorContext ctx, String url) {
        StoreCredentials c = ctx.credentials();
        String partnerId = c.require("partnerId");
        long ts = Instant.now().getEpochSecond();
        Map<String, String> q = new LinkedHashMap<>();
        q.put("partner_id", partnerId);
        q.put("timestamp", String.valueOf(ts));
        q.put("sign", ConnectorSigns.hmacSha256Hex(c.require("partnerKey"), partnerId + UPLOAD_IMAGE + ts));
        JsonNode resp = check(PublishSupport.multipart(base(ctx) + UPLOAD_IMAGE + "?" + ConnectorSigns.query(q), Map.of(), "image",
                PublishSupport.fileName(url), PublishSupport.download(url, PLATFORM), Map.of(), PLATFORM));
        String id = ConnectorJson.firstNonBlank(HttpJson.text(resp.path("image_info"), "image_id"),
                HttpJson.text(resp.path("image_info_list").path(0).path("image_info"), "image_id"));
        if (id == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "image_id missing");
        }
        return parseLong(id);
    }

    // ---------------------------------------------------------------- 下架与改价

    @Override
    public void offline(ConnectorContext ctx, PublishPayload payload, List<ListingRef> refs) {
        List<Map<String, Object>> items = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (ListingRef r : refs) {
            String id = r.productId();
            if (id != null && seen.add(id)) {
                items.add(Map.of("item_id", parseLong(id), "unlist", true));
            }
        }
        if (!items.isEmpty()) {
            JsonNode resp = post(ctx, UNLIST, Map.of("item_list", items));
            for (JsonNode f : resp.path("failure_list")) {
                throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(f.toString()));
            }
        }
    }

    @Override
    public List<PriceResult> updatePrices(ConnectorContext ctx, List<PriceUpdate> updates) {
        Map<String, List<PriceUpdate>> byItem = new LinkedHashMap<>();
        List<PriceResult> out = new ArrayList<>();
        for (PriceUpdate u : updates) {
            if (u.ref().productId() == null) {
                out.add(PriceResult.fail(u.sellerSku(), "item_id missing"));
            } else {
                byItem.computeIfAbsent(u.ref().productId(), k -> new ArrayList<>()).add(u);
            }
        }
        byItem.forEach((itemId, list) -> {
            try {
                List<Map<String, Object>> prices = new ArrayList<>();
                for (PriceUpdate u : list) {
                    String model = u.ref().variantId();
                    prices.add(Map.of("model_id", model == null ? 0L : parseLong(model), "original_price", u.price()));
                }
                JsonNode resp = post(ctx, UPDATE_PRICE, Map.of("item_id", parseLong(itemId), "price_list", prices));
                Map<String, String> failed = new HashMap<>();
                for (JsonNode f : resp.path("failure_list")) {
                    failed.put(HttpJson.text(f, "model_id"), HttpJson.text(f, "failed_reason"));
                }
                for (PriceUpdate u : list) {
                    String model = u.ref().variantId() == null ? "0" : u.ref().variantId();
                    out.add(failed.containsKey(model) ? PriceResult.fail(u.sellerSku(), failed.get(model)) : PriceResult.ok(u.sellerSku()));
                }
            } catch (BizException e) {
                list.forEach(u -> out.add(PriceResult.fail(u.sellerSku(), e.getMessage())));
            }
        });
        return out;
    }

    // ---------------------------------------------------------------- HTTP

    private static String base(ConnectorContext ctx) {
        return PublishSupport.baseUrl(ctx, PRODUCTION, SANDBOX);
    }

    private static String commonQuery(StoreCredentials c, String path) {
        String partnerId = c.require("partnerId");
        String token = c.require("accessToken");
        String shopId = c.require("shopId");
        long ts = Instant.now().getEpochSecond();
        Map<String, String> q = new LinkedHashMap<>();
        q.put("partner_id", partnerId);
        q.put("timestamp", String.valueOf(ts));
        q.put("access_token", token);
        q.put("shop_id", shopId);
        q.put("sign", ConnectorSigns.hmacSha256Hex(c.require("partnerKey"), partnerId + path + ts + token + shopId));
        return ConnectorSigns.query(q);
    }

    private static JsonNode get(ConnectorContext ctx, String path, String query) {
        String url = base(ctx) + path + "?" + commonQuery(ctx.credentials(), path) + (query == null || query.isEmpty() ? "" : "&" + query);
        return check(HttpJson.get(url, Map.of(), PLATFORM));
    }

    private static JsonNode post(ConnectorContext ctx, String path, Object body) {
        return check(HttpJson.postJson(base(ctx) + path + "?" + commonQuery(ctx.credentials(), path), Map.of(), body, PLATFORM));
    }

    private static JsonNode check(JsonNode resp) {
        String error = HttpJson.text(resp, "error");
        if (error != null && !error.isBlank()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(error + ": " + HttpJson.text(resp, "message")));
        }
        return resp.path("response");
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "invalid id: " + s);
        }
    }
}
