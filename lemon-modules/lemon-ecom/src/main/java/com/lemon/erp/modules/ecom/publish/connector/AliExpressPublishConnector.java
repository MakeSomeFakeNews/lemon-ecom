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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 速卖通开放平台（TOP 协议，与订单连接器相同的网关与 MD5 签名）的刊登与改价：
 * <ul>
 *   <li>类目：aliexpress.solution.seller.category.tree.query（category_id 为上级，0 为顶级，filter_no_permission = true 只列有权限的；
 *       is_leaf_category 为叶子，名称取 multi_language_names 中的英文）</li>
 *   <li>属性：aliexpress.solution.product.schema.get（aliexpress_category_id）返回该类目商品的 JSON Schema，
 *       按 properties.category_attributes 列出属性（required 为必填，oneOf 的 title 为可选值）</li>
 *   <li>发布：aliexpress.solution.product.post（post_product_request：category_id、subject_list、description_multi_language_list、
 *       main_image_urls_list、attribute_list、sku_info_list[sku_code、price、inventory、sku_attributes_list]、shipping_template_id、
 *       service_template_id、shipping_preparation_time、package 尺寸重量、product_unit），返回 product_id，之后进入平台审核</li>
 *   <li>改价：aliexpress.solution.batch.product.price.update（按商品分组，sku_code 定位，每次最多 20 个商品）；
 *       下架：aliexpress.offline.redefining.offlineaeproduct（product_ids 分号分隔）</li>
 * </ul>
 * 运费模板 ID 取运费模板，服务模板 ID 取退货政策（为空用 0 默认模板），包装重量 / 尺寸取属性或其他参数 gross_weight（kg）、
 * package_length / width / height（cm），计量单位取其他参数 product_unit（默认 100000015 件）。授权复用店铺的 appKey、appSecret、accessToken。
 * <p>
 * 说明：按开放平台公开文档实现，尚未用真实店铺联调。需要真实账号联调确认：
 * <ul>
 *   <li>新版开放平台（api-sg.aliexpress.com，HMAC-SHA256）中这些接口的名称与参数是否一致</li>
 *   <li>product.schema.get 返回的 schema 结构（属性在 category_attributes 下、可选值为 oneOf）与属性提交的名称 / ID 方式</li>
 *   <li>sku_attributes_list 需要用平台的 SKU 属性（颜色、尺码等的属性 ID 与值 ID），自定义值的写法</li>
 *   <li>商品审核结果需要在卖家后台或商品详情接口查看</li>
 * </ul>
 */
@Component
public class AliExpressPublishConnector implements PublishConnector {

    static final String PLATFORM = "AliExpress";
    static final String GATEWAY = "https://gw.api.taobao.com/router/rest";
    static final int PRICE_BATCH = 20;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId GATEWAY_ZONE = ZoneId.of("Asia/Shanghai");

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.ALIEXPRESS;
    }

    @Override
    public Set<PublishCapability> capabilities() {
        return EnumSet.of(PublishCapability.CATEGORIES, PublishCapability.ATTRIBUTES, PublishCapability.PUBLISH, PublishCapability.OFFLINE,
                PublishCapability.PRICE);
    }

    @Override
    public String note() {
        return "ecom.text.pub.note.aliexpress";
    }

    // ---------------------------------------------------------------- 类目与属性

    @Override
    public List<PlatformCategory> categories(ConnectorContext ctx, String parentId, String keyword) {
        String parent = parentId == null || parentId.isBlank() ? "0" : parentId;
        JsonNode resp = call(ctx, "aliexpress.solution.seller.category.tree.query", Map.of("category_id", parent, "filter_no_permission", "true"))
                .path("aliexpress_solution_seller_category_tree_query_response");
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase();
        List<PlatformCategory> out = new ArrayList<>();
        for (JsonNode c : resp.path("children_category_list").path("category_info")) {
            String name = englishName(HttpJson.text(c, "multi_language_names"));
            if (name == null) {
                name = HttpJson.text(c, "children_category_id");
            }
            if (kw != null && !name.toLowerCase().contains(kw)) {
                continue;
            }
            out.add(new PlatformCategory(HttpJson.text(c, "children_category_id"), name, "0".equals(parent) ? null : parent,
                    c.path("is_leaf_category").asBoolean(false), null));
        }
        return out;
    }

    /** multi_language_names 是 JSON 串（{"en":"…","zh":"…"}），取英文，其次第一个 */
    static String englishName(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode n = HttpJson.MAPPER.readTree(json);
            String en = HttpJson.text(n, "en");
            if (en != null) {
                return en;
            }
            for (Map.Entry<String, JsonNode> e : n.properties()) {
                return e.getValue().asString();
            }
            return null;
        } catch (RuntimeException e) {
            return json;
        }
    }

    @Override
    public List<PlatformAttribute> attributes(ConnectorContext ctx, String categoryId) {
        JsonNode result = call(ctx, "aliexpress.solution.product.schema.get", Map.of("aliexpress_category_id", categoryId))
                .path("aliexpress_solution_product_schema_get_response").path("result");
        String schema = HttpJson.text(result, "schema");
        if (schema == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(result.toString()));
        }
        return parseSchema(HttpJson.MAPPER.readTree(schema));
    }

    /** schema.properties.category_attributes：required 为必填，oneOf[].title 为可选值 */
    static List<PlatformAttribute> parseSchema(JsonNode schema) {
        JsonNode attrs = schema.path("properties").path("category_attributes");
        Set<String> required = new HashSet<>();
        for (JsonNode r : attrs.path("required")) {
            required.add(r.asString());
        }
        List<PlatformAttribute> req = new ArrayList<>();
        List<PlatformAttribute> opt = new ArrayList<>();
        for (Map.Entry<String, JsonNode> e : attrs.path("properties").properties()) {
            JsonNode p = e.getValue();
            List<String> options = new ArrayList<>();
            JsonNode value = p.path("properties").path("value");
            for (JsonNode o : value.path("oneOf")) {
                String t = ConnectorJson.firstNonBlank(HttpJson.text(o, "title"), HttpJson.text(o, "const"));
                if (t != null) {
                    options.add(t);
                }
            }
            boolean multi = "array".equals(HttpJson.text(p, "type"));
            PlatformAttribute a = new PlatformAttribute(e.getKey(), ConnectorJson.firstNonBlank(HttpJson.text(p, "title"), e.getKey()),
                    required.contains(e.getKey()), multi ? PlatformAttribute.MULTI : options.isEmpty() ? PlatformAttribute.TEXT : PlatformAttribute.SELECT,
                    options, null);
            (a.required() ? req : opt).add(a);
        }
        req.addAll(opt);
        return req;
    }

    // ---------------------------------------------------------------- 发布

    @Override
    public PublishResult publish(ConnectorContext ctx, PublishPayload p) {
        if (p.categoryId() == null) {
            throw new BizException("ecom.error.pub.categoryRequired");
        }
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("category_id", Long.parseLong(p.categoryId().trim()));
        req.put("language", "en_US");
        req.put("subject_list", List.of(Map.of("locale", "en_US", "value", PublishSupport.cut(p.title(), 128))));
        req.put("description_multi_language_list", List.of(Map.of("locale", "en_US",
                "module_list", List.of(Map.of("type", "html", "html", Map.of("content", TikTokPublishConnector.html(p)))))));
        req.put("main_image_urls_list", p.images().size() > 6 ? p.images().subList(0, 6) : p.images());
        if (p.brand() != null && !p.brand().isBlank()) {
            req.put("brand_name", p.brand());
        }
        List<Map<String, Object>> attrs = new ArrayList<>();
        p.attributes().forEach((k, v) -> {
            if (v != null && !v.isBlank() && !k.startsWith("package_") && !"gross_weight".equals(k)) {
                for (String s : PublishSupport.values(v)) {
                    attrs.add(Map.of("attribute_name", k, "attribute_value", s));
                }
            }
        });
        if (!attrs.isEmpty()) {
            req.put("attribute_list", attrs);
        }
        List<Map<String, Object>> skus = new ArrayList<>();
        for (PublishSku s : p.skus()) {
            Map<String, Object> sku = new LinkedHashMap<>();
            sku.put("sku_code", s.sellerSku());
            sku.put("price", PublishSupport.money(s.price()));
            sku.put("inventory", s.qty());
            if (!s.variantAttrs().isEmpty()) {
                sku.put("sku_attributes_list", s.variantAttrs().entrySet().stream()
                        .map(e -> Map.of("sku_attribute_name", e.getKey(), "sku_attribute_value", e.getValue())).toList());
            }
            skus.add(sku);
        }
        req.put("sku_info_list", skus);
        if (p.shippingTemplate() != null) {
            req.put("shipping_template_id", Long.parseLong(PublishSupport.values(p.shippingTemplate()).get(0)));
        }
        req.put("service_template_id", p.returnPolicy() == null ? 0 : Long.parseLong(p.returnPolicy().trim()));
        if (p.handlingDays() != null) {
            req.put("shipping_preparation_time", p.handlingDays());
        }
        req.put("product_unit", Long.parseLong(ConnectorJson.firstNonBlank(p.extra("product_unit"), "100000015")));
        req.put("package_type", false);
        req.put("gross_weight", ConnectorJson.firstNonBlank(p.attribute("gross_weight"), p.extra("weight"), "0.5"));
        req.put("package_length", Integer.parseInt(ConnectorJson.firstNonBlank(p.attribute("package_length"), "10")));
        req.put("package_width", Integer.parseInt(ConnectorJson.firstNonBlank(p.attribute("package_width"), "10")));
        req.put("package_height", Integer.parseInt(ConnectorJson.firstNonBlank(p.attribute("package_height"), "10")));
        JsonNode result = call(ctx, "aliexpress.solution.product.post", Map.of("post_product_request", HttpJson.MAPPER.writeValueAsString(req)))
                .path("aliexpress_solution_product_post_response").path("result");
        String productId = HttpJson.text(result, "product_id");
        if (productId == null || !result.path("success").asBoolean(true)) {
            return PublishResult.failed(HttpJson.abbreviate(result.toString()));
        }
        Map<String, String> skuIds = new LinkedHashMap<>();
        p.skus().forEach(s -> skuIds.put(s.sellerSku(), productId));
        return PublishResult.success(productId, "https://www.aliexpress.com/item/" + productId + ".html", skuIds, Map.of(), null);
    }

    // ---------------------------------------------------------------- 下架与改价

    @Override
    public void offline(ConnectorContext ctx, PublishPayload payload, List<ListingRef> refs) {
        List<String> ids = refs.stream().map(ListingRef::productId).filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        JsonNode result = call(ctx, "aliexpress.offline.redefining.offlineaeproduct", Map.of("product_ids", String.join(";", ids)))
                .path("aliexpress_offline_redefining_offlineaeproduct_response").path("result");
        if (!result.path("success").asBoolean(false)) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(result.toString()));
        }
    }

    @Override
    public List<PriceResult> updatePrices(ConnectorContext ctx, List<PriceUpdate> updates) {
        Map<String, List<PriceUpdate>> byProduct = new LinkedHashMap<>();
        List<PriceResult> out = new ArrayList<>();
        for (PriceUpdate u : updates) {
            if (u.ref().productId() == null) {
                out.add(PriceResult.fail(u.sellerSku(), "product_id missing"));
            } else {
                byProduct.computeIfAbsent(u.ref().productId(), k -> new ArrayList<>()).add(u);
            }
        }
        List<String> productIds = new ArrayList<>(byProduct.keySet());
        for (int i = 0; i < productIds.size(); i += PRICE_BATCH) {
            List<String> batch = productIds.subList(i, Math.min(productIds.size(), i + PRICE_BATCH));
            List<Map<String, Object>> products = new ArrayList<>();
            List<PriceUpdate> all = new ArrayList<>();
            for (String id : batch) {
                List<PriceUpdate> list = byProduct.get(id);
                all.addAll(list);
                products.add(Map.of("product_id", Long.parseLong(id), "multiple_sku_update_list",
                        list.stream().map(u -> Map.of("sku_code", u.sellerSku(), "price", PublishSupport.money(u.price()))).toList()));
            }
            try {
                JsonNode resp = call(ctx, "aliexpress.solution.batch.product.price.update",
                        Map.of("mutiple_product_update_list", HttpJson.MAPPER.writeValueAsString(products)))
                        .path("aliexpress_solution_batch_product_price_update_response");
                if (resp.path("update_success").asBoolean(false)) {
                    all.forEach(u -> out.add(PriceResult.ok(u.sellerSku())));
                } else {
                    String msg = HttpJson.abbreviate(HttpJson.text(resp, "update_error_code") + " " + HttpJson.text(resp, "update_error_message"));
                    all.forEach(u -> out.add(PriceResult.fail(u.sellerSku(), msg)));
                }
            } catch (BizException e) {
                all.forEach(u -> out.add(PriceResult.fail(u.sellerSku(), e.getMessage())));
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- 协议

    /** TOP 协议：系统参数 + 业务参数，MD5(secret + 排序后的 k+v + secret) 大写，error_response 转为异常 */
    static JsonNode call(ConnectorContext ctx, String method, Map<String, String> business) {
        StoreCredentials c = ctx.credentials();
        TreeMap<String, String> params = new TreeMap<>(business);
        params.put("method", method);
        params.put("app_key", c.require("appKey"));
        params.put("session", c.require("accessToken"));
        params.put("timestamp", LocalDateTime.now(GATEWAY_ZONE).format(TS));
        params.put("format", "json");
        params.put("v", "2.0");
        params.put("sign_method", "md5");
        params.put("sign", ConnectorSigns.md5Upper(c.require("appSecret") + ConnectorSigns.concatSorted(params) + c.require("appSecret")));
        StringBuilder form = new StringBuilder();
        params.forEach((k, v) -> {
            if (!form.isEmpty()) {
                form.append('&');
            }
            form.append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        JsonNode resp = HttpJson.postForm(PublishSupport.baseUrl(ctx, GATEWAY, null), Map.of(), form.toString(), PLATFORM);
        JsonNode error = resp.path("error_response");
        if (!error.isMissingNode()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(error.toString()));
        }
        return resp;
    }

}
