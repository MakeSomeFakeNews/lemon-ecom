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

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * TikTok Shop Product API 202309 的刊登与改价（https://partner.tiktokshop.com/docv2）：
 * <ul>
 *   <li>类目：GET /product/202309/categories（全量，按上级 ID 或关键字过滤，is_leaf 为叶子类目）；
 *       属性：GET /product/202309/categories/{category_id}/attributes（type = PRODUCT_PROPERTY 的为商品属性，SALES_PROPERTY 为销售属性即变体，
 *       is_requried 为必填（平台字段拼写如此），values 为可选值）</li>
 *   <li>图片：POST /product/202309/images/upload（multipart，字段 data、use_case = MAIN_IMAGE，multipart 请求的签名不含请求体），取 data.uri</li>
 *   <li>发布：POST /product/202309/products（title、description、category_id、main_images、skus[seller_sku、price、inventory、sales_attributes]、
 *       product_attributes、package_weight），返回 data.product_id 与 skus[].id</li>
 *   <li>改价：POST /product/202309/products/{product_id}/prices/update（skus[id、price]）；下架：POST /product/202309/products/deactivate</li>
 * </ul>
 * 授权复用店铺的渠道授权（appKey、appSecret、accessToken、shopCipher），签名与订单接口相同；库存仓库 ID 取在线商品附加信息 / 授权信息 warehouseId。
 * 商品属性按名称提交（values[].name），可选值能匹配到时同时带上 ID。包裹重量（kg）取属性 / 其他参数 weight（默认 0.5）。
 * <p>
 * 说明：按 Partner Center 公开文档实现，尚未用真实店铺联调。需要真实账号联调确认：
 * <ul>
 *   <li>美区与东南亚的类目树不同，部分类目需要资质（permission_statuses）</li>
 *   <li>品牌 brand_id 需要先在平台登记，未填写时不带品牌</li>
 *   <li>商品创建后进入平台审核（status = PENDING），审核结果要在卖家中心或通过商品详情接口查看</li>
 * </ul>
 */
@Component
public class TikTokPublishConnector implements PublishConnector {

    static final String PLATFORM = "TikTok Shop";
    static final String API_BASE = "https://open-api.tiktokglobalshop.com";
    static final String CATEGORIES = "/product/202309/categories";
    static final String UPLOAD = "/product/202309/images/upload";
    static final String PRODUCTS = "/product/202309/products";
    static final String DEACTIVATE = "/product/202309/products/deactivate";
    static final String DEFAULT_WEIGHT = "0.5";

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.TIKTOK;
    }

    @Override
    public Set<PublishCapability> capabilities() {
        return EnumSet.of(PublishCapability.CATEGORIES, PublishCapability.ATTRIBUTES, PublishCapability.PUBLISH, PublishCapability.OFFLINE,
                PublishCapability.PRICE);
    }

    @Override
    public String note() {
        return "ecom.text.pub.note.tiktok";
    }

    // ---------------------------------------------------------------- 类目与属性

    @Override
    public List<PlatformCategory> categories(ConnectorContext ctx, String parentId, String keyword) {
        JsonNode data = call(ctx, "GET", CATEGORIES, new LinkedHashMap<>(), null);
        Map<String, JsonNode> byId = new HashMap<>();
        for (JsonNode c : data.path("categories")) {
            byId.put(HttpJson.text(c, "id"), c);
        }
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase(Locale.ROOT);
        String parent = parentId == null || parentId.isBlank() ? "0" : parentId;
        List<PlatformCategory> out = new ArrayList<>();
        for (JsonNode c : data.path("categories")) {
            boolean leaf = c.path("is_leaf").asBoolean(false);
            String name = String.valueOf(HttpJson.text(c, "local_name"));
            String pid = ConnectorJson.firstNonBlank(HttpJson.text(c, "parent_id"), "0");
            if (kw != null ? leaf && name.toLowerCase(Locale.ROOT).contains(kw) : parent.equals(pid)) {
                out.add(new PlatformCategory(HttpJson.text(c, "id"), name, "0".equals(pid) ? null : pid, leaf, path(c, byId)));
            }
        }
        return out;
    }

    private static String path(JsonNode c, Map<String, JsonNode> byId) {
        List<String> names = new ArrayList<>();
        JsonNode cur = c;
        for (int i = 0; cur != null && i < 10; i++) {
            names.add(0, String.valueOf(HttpJson.text(cur, "local_name")));
            cur = byId.get(HttpJson.text(cur, "parent_id"));
        }
        return String.join(" > ", names);
    }

    @Override
    public List<PlatformAttribute> attributes(ConnectorContext ctx, String categoryId) {
        List<PlatformAttribute> req = new ArrayList<>();
        List<PlatformAttribute> opt = new ArrayList<>();
        for (JsonNode a : rawAttributes(ctx, categoryId)) {
            if ("SALES_PROPERTY".equals(HttpJson.text(a, "type"))) {
                continue;
            }
            List<String> options = new ArrayList<>();
            for (JsonNode v : a.path("values")) {
                options.add(HttpJson.text(v, "name"));
            }
            boolean required = a.path("is_requried").asBoolean(false) || a.path("is_required").asBoolean(false);
            String input = a.path("is_multiple_selection").asBoolean(false) ? PlatformAttribute.MULTI
                    : options.isEmpty() ? PlatformAttribute.TEXT : PlatformAttribute.SELECT;
            PlatformAttribute pa = new PlatformAttribute(HttpJson.text(a, "id"), HttpJson.text(a, "name"), required, input, options, null);
            (required ? req : opt).add(pa);
        }
        req.addAll(opt);
        return req;
    }

    private JsonNode rawAttributes(ConnectorContext ctx, String categoryId) {
        return call(ctx, "GET", CATEGORIES + "/" + ConnectorSigns.enc(categoryId) + "/attributes", new LinkedHashMap<>(), null).path("attributes");
    }

    // ---------------------------------------------------------------- 发布

    @Override
    public PublishResult publish(ConnectorContext ctx, PublishPayload p) {
        if (p.categoryId() == null) {
            throw new BizException("ecom.error.pub.categoryRequired");
        }
        List<Map<String, Object>> images = new ArrayList<>();
        for (String url : p.images()) {
            images.add(Map.of("uri", uploadImage(ctx, url)));
        }
        String warehouse = ConnectorJson.firstNonBlank(p.extra("warehouseId"), ctx.credentials().get("warehouseId"));
        List<Map<String, Object>> skus = new ArrayList<>();
        for (PublishSku s : p.skus()) {
            Map<String, Object> sku = new LinkedHashMap<>();
            sku.put("seller_sku", s.sellerSku());
            sku.put("price", Map.of("amount", PublishSupport.money(s.price()), "currency", p.currency()));
            Map<String, Object> inv = new LinkedHashMap<>();
            if (warehouse != null) {
                inv.put("warehouse_id", warehouse);
            }
            inv.put("quantity", s.qty());
            sku.put("inventory", List.of(inv));
            if (p.multiVariant()) {
                List<Map<String, Object>> sales = new ArrayList<>();
                if (s.variantAttrs().isEmpty()) {
                    sales.add(Map.of("name", "Option", "value_name", ConnectorJson.firstNonBlank(s.variantText(), s.sellerSku())));
                } else {
                    s.variantAttrs().forEach((k, v) -> sales.add(Map.of("name", k, "value_name", v)));
                }
                sku.put("sales_attributes", sales);
            }
            if (!p.upcExempt() && s.barcode() != null && !s.barcode().isBlank()) {
                sku.put("identifier_code", Map.of("code", s.barcode().trim(), "type", p.barcodeType() == null ? "UPC" : p.barcodeType()));
            }
            skus.add(sku);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", p.title());
        body.put("description", html(p));
        body.put("category_id", p.categoryId());
        if (p.extra("brandId") != null) {
            body.put("brand_id", p.extra("brandId"));
        }
        body.put("main_images", images);
        body.put("skus", skus);
        List<Map<String, Object>> attrs = productAttributes(ctx, p);
        if (!attrs.isEmpty()) {
            body.put("product_attributes", attrs);
        }
        body.put("package_weight", Map.of("value", ConnectorJson.firstNonBlank(p.attribute("weight"), p.extra("weight"), DEFAULT_WEIGHT),
                "unit", "KILOGRAM"));
        JsonNode data = call(ctx, "POST", PRODUCTS, new LinkedHashMap<>(), body);
        String productId = HttpJson.text(data, "product_id");
        if (productId == null) {
            return PublishResult.failed("product_id missing");
        }
        Map<String, String> skuIds = new LinkedHashMap<>();
        Map<String, Map<String, String>> extra = new LinkedHashMap<>();
        for (JsonNode s : data.path("skus")) {
            String sellerSku = HttpJson.text(s, "seller_sku");
            if (sellerSku != null) {
                skuIds.put(sellerSku, HttpJson.text(s, "id"));
                if (warehouse != null) {
                    extra.put(sellerSku, Map.of("warehouseId", warehouse));
                }
            }
        }
        return PublishResult.success(productId, null, skuIds, extra, null);
    }

    /** 五点描述转为列表放在描述前面（description 为 HTML） */
    static String html(PublishPayload p) {
        StringBuilder sb = new StringBuilder();
        List<String> bullets = p.bullets().stream().filter(b -> b != null && !b.isBlank()).toList();
        if (!bullets.isEmpty()) {
            sb.append("<ul>");
            bullets.forEach(b -> sb.append("<li>").append(escape(b.trim())).append("</li>"));
            sb.append("</ul>");
        }
        if (p.description() != null && !p.description().isBlank()) {
            String d = p.description().trim();
            sb.append(d.startsWith("<") ? d : "<p>" + escape(d).replace("\n", "<br/>") + "</p>");
        }
        return sb.isEmpty() ? "<p>" + escape(p.title()) + "</p>" : sb.toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 商品属性：键为属性 ID，值按名称匹配可选值的 ID */
    private List<Map<String, Object>> productAttributes(ConnectorContext ctx, PublishPayload p) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (p.attributes().keySet().stream().noneMatch(k -> k.chars().allMatch(Character::isDigit))) {
            return out;
        }
        Map<String, Map<String, String>> valueIds = new HashMap<>();
        for (JsonNode a : rawAttributes(ctx, p.categoryId())) {
            Map<String, String> names = new HashMap<>();
            for (JsonNode v : a.path("values")) {
                names.put(String.valueOf(HttpJson.text(v, "name")).toLowerCase(Locale.ROOT), HttpJson.text(v, "id"));
            }
            valueIds.put(HttpJson.text(a, "id"), names);
        }
        p.attributes().forEach((k, v) -> {
            if (v == null || v.isBlank() || !k.chars().allMatch(Character::isDigit)) {
                return;
            }
            List<Map<String, Object>> values = new ArrayList<>();
            for (String s : PublishSupport.values(v)) {
                Map<String, Object> m = new LinkedHashMap<>();
                String id = valueIds.getOrDefault(k, Map.of()).get(s.toLowerCase(Locale.ROOT));
                if (id != null) {
                    m.put("id", id);
                }
                m.put("name", s);
                values.add(m);
            }
            out.add(Map.of("id", k, "values", values));
        });
        return out;
    }

    private String uploadImage(ConnectorContext ctx, String url) {
        StoreCredentials c = ctx.credentials();
        Map<String, String> q = new LinkedHashMap<>();
        q.put("app_key", c.require("appKey"));
        q.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));
        // multipart 请求的签名不含请求体
        q.put("sign", sign(c.require("appSecret"), UPLOAD, q, null));
        JsonNode resp = PublishSupport.multipart(base(ctx) + UPLOAD + "?" + ConnectorSigns.query(q), Map.of("x-tts-access-token", c.require("accessToken")),
                "data", PublishSupport.fileName(url), PublishSupport.download(url, PLATFORM), Map.of("use_case", "MAIN_IMAGE"), PLATFORM);
        JsonNode data = check(resp);
        String uri = HttpJson.text(data, "uri");
        if (uri == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "uri missing");
        }
        return uri;
    }

    // ---------------------------------------------------------------- 下架与改价

    @Override
    public void offline(ConnectorContext ctx, PublishPayload payload, List<ListingRef> refs) {
        List<String> ids = refs.stream().map(ListingRef::productId).filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (!ids.isEmpty()) {
            call(ctx, "POST", DEACTIVATE, new LinkedHashMap<>(), Map.of("product_ids", ids));
        }
    }

    @Override
    public List<PriceResult> updatePrices(ConnectorContext ctx, List<PriceUpdate> updates) {
        Map<String, List<PriceUpdate>> byProduct = new LinkedHashMap<>();
        List<PriceResult> out = new ArrayList<>();
        for (PriceUpdate u : updates) {
            if (u.ref().productId() == null || u.ref().platformItemId() == null) {
                out.add(PriceResult.fail(u.sellerSku(), "product_id / sku_id missing"));
            } else {
                byProduct.computeIfAbsent(u.ref().productId(), k -> new ArrayList<>()).add(u);
            }
        }
        byProduct.forEach((productId, list) -> {
            try {
                List<Map<String, Object>> skus = new ArrayList<>();
                for (PriceUpdate u : list) {
                    skus.add(Map.of("id", u.ref().platformItemId(), "price", Map.of("amount", PublishSupport.money(u.price()), "currency", u.currency())));
                }
                call(ctx, "POST", PRODUCTS + "/" + ConnectorSigns.enc(productId) + "/prices/update", new LinkedHashMap<>(), Map.of("skus", skus));
                list.forEach(u -> out.add(PriceResult.ok(u.sellerSku())));
            } catch (BizException e) {
                list.forEach(u -> out.add(PriceResult.fail(u.sellerSku(), e.getMessage())));
            }
        });
        return out;
    }

    // ---------------------------------------------------------------- HTTP

    /** 签名：app_secret + path + 排序后的 k+v（不含 sign、access_token）+ body + app_secret，HMAC-SHA256(app_secret) 小写十六进制 */
    static String sign(String appSecret, String path, Map<String, String> query, String body) {
        TreeMap<String, String> sorted = new TreeMap<>(query);
        sorted.remove("sign");
        sorted.remove("access_token");
        StringBuilder sb = new StringBuilder(appSecret).append(path);
        sorted.forEach((k, v) -> sb.append(k).append(v));
        if (body != null) {
            sb.append(body);
        }
        sb.append(appSecret);
        return ConnectorSigns.hmacSha256Hex(appSecret, sb.toString());
    }

    private static String base(ConnectorContext ctx) {
        return PublishSupport.baseUrl(ctx, API_BASE, API_BASE);
    }

    private static JsonNode call(ConnectorContext ctx, String method, String path, Map<String, String> query, Object body) {
        StoreCredentials c = ctx.credentials();
        Map<String, String> q = new LinkedHashMap<>(query);
        q.put("app_key", c.require("appKey"));
        q.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));
        q.put("shop_cipher", c.require("shopCipher"));
        String json = body == null ? null : HttpJson.MAPPER.writeValueAsString(body);
        q.put("sign", sign(c.require("appSecret"), path, q, json));
        String url = base(ctx) + path + "?" + ConnectorSigns.query(q);
        Map<String, String> headers = Map.of("x-tts-access-token", c.require("accessToken"), "Content-Type", "application/json");
        JsonNode resp = json == null && "GET".equals(method)
                ? HttpJson.get(url, headers, PLATFORM)
                : HttpJson.requestRaw(method, url, headers, json, "application/json", PLATFORM);
        return check(resp);
    }

    private static JsonNode check(JsonNode resp) {
        if (resp.path("code").asLong(-1) != 0) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM,
                    HttpJson.abbreviate(HttpJson.text(resp, "code") + ": " + HttpJson.text(resp, "message")));
        }
        return resp.path("data");
    }
}
