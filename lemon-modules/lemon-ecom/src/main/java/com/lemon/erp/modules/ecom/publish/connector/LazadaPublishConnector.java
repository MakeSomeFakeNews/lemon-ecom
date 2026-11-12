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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Lazada Open Platform 的刊登与改价：
 * <ul>
 *   <li>类目：GET /category/tree/get（整棵树，按上级或关键字过滤）；属性：GET /category/attributes/get?primary_category_id=
 *       （is_mandatory 为必填，attribute_type = sku 的为 SKU 级属性（如 color_family、size），options 为可选值）</li>
 *   <li>图片：POST /image/migrate（把外部图片地址迁移到 Lazada 图片空间，payload 为 XML），取 data.image.url</li>
 *   <li>发布：POST /product/create（payload 为 JSON：PrimaryCategory、Images、Attributes（name、description、short_description、brand 等）、
 *       Skus.Sku[SellerSku、quantity、price、package_weight / length / width / height 与 SKU 级属性]），返回 item_id 与 sku_list</li>
 *   <li>改价：POST /product/price_quantity/update（XML，ItemId + SkuId + SellerSku + Price，每批 50 个）；下架：POST /product/deactivate</li>
 * </ul>
 * 授权复用店铺的渠道授权（appKey、appSecret、region、accessToken），签名与订单接口相同；令牌由调用方先用 {@code LazadaConnector.ensureToken} 刷新。
 * 包裹重量与尺寸取属性 / 其他参数 package_weight（kg，默认 0.5）、package_length / width / height（cm，默认 10）。
 * <p>
 * 说明：按开放平台公开文档实现，尚未用真实店铺联调。需要真实账号联调确认：
 * <ul>
 *   <li>/product/create 的 payload 在部分站点仍要求 XML（代码用 JSON）</li>
 *   <li>品牌需要是平台已登记的品牌名，没有品牌时填 No Brand</li>
 * </ul>
 */
@Component
public class LazadaPublishConnector implements PublishConnector {

    static final String PLATFORM = "Lazada";
    static final int PRICE_BATCH = 50;
    private static final Map<String, String> ENDPOINTS = Map.of(
            "SG", "https://api.lazada.sg/rest",
            "MY", "https://api.lazada.com.my/rest",
            "TH", "https://api.lazada.co.th/rest",
            "PH", "https://api.lazada.com.ph/rest",
            "VN", "https://api.lazada.vn/rest",
            "ID", "https://api.lazada.co.id/rest");

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.LAZADA;
    }

    @Override
    public Set<PublishCapability> capabilities() {
        return EnumSet.of(PublishCapability.CATEGORIES, PublishCapability.ATTRIBUTES, PublishCapability.PUBLISH, PublishCapability.OFFLINE,
                PublishCapability.PRICE);
    }

    // ---------------------------------------------------------------- 类目与属性

    @Override
    public List<PlatformCategory> categories(ConnectorContext ctx, String parentId, String keyword) {
        JsonNode data = get(ctx, "/category/tree/get", Map.of("language_code", "en_US")).path("data");
        List<PlatformCategory> out = new ArrayList<>();
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase(Locale.ROOT);
        walk(data, null, "", parentId == null || parentId.isBlank() ? null : parentId, kw, out);
        return out;
    }

    private static void walk(JsonNode nodes, String parent, String prefix, String wantedParent, String kw, List<PlatformCategory> out) {
        for (JsonNode c : nodes) {
            String id = HttpJson.text(c, "category_id");
            String name = String.valueOf(HttpJson.text(c, "name"));
            String path = prefix.isEmpty() ? name : prefix + " > " + name;
            boolean leaf = c.path("leaf").asBoolean(false);
            boolean match = kw != null ? leaf && name.toLowerCase(Locale.ROOT).contains(kw)
                    : wantedParent == null ? parent == null : wantedParent.equals(parent);
            if (match) {
                out.add(new PlatformCategory(id, name, parent, leaf, path));
            }
            if (c.path("children").isArray()) {
                walk(c.path("children"), id, path, wantedParent, kw, out);
            }
        }
    }

    @Override
    public List<PlatformAttribute> attributes(ConnectorContext ctx, String categoryId) {
        JsonNode data = get(ctx, "/category/attributes/get", Map.of("primary_category_id", categoryId, "language_code", "en_US")).path("data");
        List<PlatformAttribute> req = new ArrayList<>();
        List<PlatformAttribute> opt = new ArrayList<>();
        for (JsonNode a : data) {
            String key = HttpJson.text(a, "name");
            if (key == null || "name".equals(key) || "description".equals(key) || "short_description".equals(key) || "brand".equals(key)
                    || "SellerSku".equals(key) || "price".equals(key) || "quantity".equals(key)) {
                continue;
            }
            List<String> options = new ArrayList<>();
            for (JsonNode o : a.path("options")) {
                options.add(HttpJson.text(o, "name"));
            }
            String type = String.valueOf(HttpJson.text(a, "input_type"));
            String input = "multiSelect".equals(type) || "multiEnumInput".equals(type) ? PlatformAttribute.MULTI
                    : "numeric".equals(type) ? PlatformAttribute.NUMBER : options.isEmpty() ? PlatformAttribute.TEXT : PlatformAttribute.SELECT;
            boolean required = a.path("is_mandatory").asInt(0) == 1;
            String desc = "sku".equals(HttpJson.text(a, "attribute_type")) ? "SKU" : null;
            PlatformAttribute pa = new PlatformAttribute(key, ConnectorJson.firstNonBlank(HttpJson.text(a, "label"), key), required, input, options, desc);
            (required ? req : opt).add(pa);
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
        List<String> images = new ArrayList<>();
        for (String url : p.images()) {
            images.add(migrate(ctx, url));
        }
        Map<String, Object> attrs = new LinkedHashMap<>();
        p.attributes().forEach((k, v) -> {
            if (v != null && !v.isBlank() && !k.startsWith("package_")) {
                attrs.put(k, v.trim());
            }
        });
        attrs.put("name", p.title());
        attrs.put("description", TikTokPublishConnector.html(new PublishPayload(p.title(), List.of(), p.description(), List.of(), null, null, false, null,
                null, null, null, null, null, null, null, null, null, null, null, null)));
        if (!p.bullets().isEmpty()) {
            StringBuilder sb = new StringBuilder("<ul>");
            p.bullets().stream().filter(b -> b != null && !b.isBlank()).forEach(b -> sb.append("<li>").append(b.trim()).append("</li>"));
            attrs.put("short_description", sb.append("</ul>").toString());
        }
        attrs.put("brand", p.brand() == null || p.brand().isBlank() ? "No Brand" : p.brand());
        List<Map<String, Object>> skus = new ArrayList<>();
        for (PublishSku s : p.skus()) {
            Map<String, Object> sku = new LinkedHashMap<>();
            sku.put("SellerSku", s.sellerSku());
            sku.put("quantity", String.valueOf(s.qty()));
            sku.put("price", PublishSupport.money(s.price()));
            sku.put("package_weight", ConnectorJson.firstNonBlank(p.attribute("package_weight"), p.extra("weight"), "0.5"));
            sku.put("package_length", ConnectorJson.firstNonBlank(p.attribute("package_length"), "10"));
            sku.put("package_width", ConnectorJson.firstNonBlank(p.attribute("package_width"), "10"));
            sku.put("package_height", ConnectorJson.firstNonBlank(p.attribute("package_height"), "10"));
            s.variantAttrs().forEach(sku::put);
            if (!images.isEmpty()) {
                sku.put("Images", Map.of("Image", images));
            }
            skus.add(sku);
        }
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("PrimaryCategory", p.categoryId());
        if (!images.isEmpty()) {
            product.put("Images", Map.of("Image", images));
        }
        product.put("Attributes", attrs);
        product.put("Skus", Map.of("Sku", skus));
        String payload = HttpJson.MAPPER.writeValueAsString(Map.of("Request", Map.of("Product", product)));
        JsonNode data = post(ctx, "/product/create", Map.of("payload", payload)).path("data");
        String itemId = HttpJson.text(data, "item_id");
        if (itemId == null) {
            return PublishResult.failed("item_id missing");
        }
        Map<String, String> skuIds = new LinkedHashMap<>();
        for (JsonNode s : data.path("sku_list")) {
            skuIds.put(HttpJson.text(s, "seller_sku"), HttpJson.text(s, "sku_id"));
        }
        return PublishResult.success(itemId, null, skuIds, Map.of(), null);
    }

    private String migrate(ConnectorContext ctx, String url) {
        String payload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Request><Image><Url>" + xml(url) + "</Url></Image></Request>";
        String migrated = HttpJson.text(post(ctx, "/image/migrate", Map.of("payload", payload)).path("data").path("image"), "url");
        if (migrated == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "image url missing");
        }
        return migrated;
    }

    // ---------------------------------------------------------------- 下架与改价

    @Override
    public void offline(ConnectorContext ctx, PublishPayload payload, List<ListingRef> refs) {
        List<String> ids = refs.stream().map(ListingRef::productId).filter(id -> id != null && !id.isBlank()).distinct().toList();
        for (String id : ids) {
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Request><Product><ItemId>" + xml(id) + "</ItemId></Product></Request>";
            post(ctx, "/product/deactivate", Map.of("apiRequestBody", xml));
        }
    }

    @Override
    public List<PriceResult> updatePrices(ConnectorContext ctx, List<PriceUpdate> updates) {
        List<PriceResult> out = new ArrayList<>();
        for (int i = 0; i < updates.size(); i += PRICE_BATCH) {
            List<PriceUpdate> batch = updates.subList(i, Math.min(updates.size(), i + PRICE_BATCH));
            StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Request><Product><Skus>");
            for (PriceUpdate u : batch) {
                sb.append("<Sku>");
                if (u.ref().productId() != null) {
                    sb.append("<ItemId>").append(xml(u.ref().productId())).append("</ItemId>");
                }
                if (u.ref().variantId() != null) {
                    sb.append("<SkuId>").append(xml(u.ref().variantId())).append("</SkuId>");
                }
                sb.append("<SellerSku>").append(xml(u.sellerSku())).append("</SellerSku>");
                sb.append("<Price>").append(PublishSupport.money(u.price())).append("</Price>");
                sb.append("</Sku>");
            }
            sb.append("</Skus></Product></Request>");
            try {
                post(ctx, "/product/price_quantity/update", Map.of("payload", sb.toString()));
                batch.forEach(u -> out.add(PriceResult.ok(u.sellerSku())));
            } catch (BizException e) {
                batch.forEach(u -> out.add(PriceResult.fail(u.sellerSku(), e.getMessage())));
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- HTTP

    static String sign(String appSecret, String apiPath, Map<String, String> params) {
        return ConnectorSigns.hmacSha256Hex(appSecret, apiPath + ConnectorSigns.concatSorted(new TreeMap<>(params))).toUpperCase(Locale.ROOT);
    }

    private static TreeMap<String, String> signed(StoreCredentials c, String apiPath, Map<String, String> business) {
        TreeMap<String, String> p = new TreeMap<>(business);
        p.put("app_key", c.require("appKey"));
        p.put("timestamp", String.valueOf(System.currentTimeMillis()));
        p.put("sign_method", "sha256");
        p.put("access_token", c.require("accessToken"));
        p.put("sign", sign(c.require("appSecret"), apiPath, p));
        return p;
    }

    private static String base(ConnectorContext ctx) {
        if (ctx.baseUrlOverride() != null) {
            return ctx.baseUrlOverride();
        }
        String region = ctx.credentials().get("region") == null ? null : ctx.credentials().get("region").toUpperCase(Locale.ROOT);
        String url = region == null ? null : ENDPOINTS.get(region);
        if (url == null) {
            throw new BizException("ecom.error.connectorRegionUnsupported", PLATFORM, String.valueOf(region), String.join(" / ", ENDPOINTS.keySet()));
        }
        return url;
    }

    private static JsonNode get(ConnectorContext ctx, String path, Map<String, String> business) {
        return check(HttpJson.get(base(ctx) + path + "?" + ConnectorSigns.query(signed(ctx.credentials(), path, business)), Map.of(), PLATFORM));
    }

    private static JsonNode post(ConnectorContext ctx, String path, Map<String, String> business) {
        return check(HttpJson.postForm(base(ctx) + path, Map.of(), ConnectorSigns.query(signed(ctx.credentials(), path, business)), PLATFORM));
    }

    private static JsonNode check(JsonNode resp) {
        String code = HttpJson.text(resp, "code");
        if (code != null && !"0".equals(code)) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(code + ": " + HttpJson.text(resp, "message")
                    + (resp.has("detail") ? " " + resp.path("detail") : "")));
        }
        return resp;
    }

    private static String xml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
