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
 * 美客多 Mercado Libre 的刊登、改价与下架（Bearer accessToken，站点取授权信息 site，如 MLM / MLB）：
 * <ul>
 *   <li>类目：关键字 GET /sites/{site}/domain_discovery/search?q=（类目预测）；浏览 GET /sites/{site}/categories（顶级）与
 *       GET /categories/{id}（children_categories，没有下级的为叶子）；属性：GET /categories/{id}/attributes（tags.required 为必填，values 为可选值）</li>
 *   <li>发布：POST /items（title、category_id、price、currency_id、available_quantity、buying_mode、listing_type_id、condition、pictures、
 *       attributes（含 SELLER_SKU、GTIN / EMPTY_GTIN_REASON）、多变体时 variations[attribute_combinations、price、available_quantity、SELLER_SKU]），
 *       再 POST /items/{id}/description（plain_text）；返回刊登 ID、permalink 与变体 ID</li>
 *   <li>改价：PUT /items/{id}（price 或 variations[id、price]）；下架：PUT /items/{id}（status = paused）</li>
 * </ul>
 * 刊登类型取其他参数 listingTypeId（默认 gold_special），运费模式取其他参数 shippingMode（如 me2）。
 * <p>
 * 说明：按公开文档实现，尚未用真实账号联调。需要真实账号联调确认：
 * <ul>
 *   <li>部分站点 / 类目要求先匹配官方商品目录（catalog_product_id）才能刊登</li>
 *   <li>同一刊登下规格价格是否允许不同；UPC 豁免时 EMPTY_GTIN_REASON 的取值</li>
 * </ul>
 */
@Component
public class MercadoLibrePublishConnector implements PublishConnector {

    static final String PLATFORM = "Mercado Libre";
    static final String API = "https://api.mercadolibre.com";

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.MERCADO_LIBRE;
    }

    @Override
    public Set<PublishCapability> capabilities() {
        return EnumSet.of(PublishCapability.CATEGORIES, PublishCapability.ATTRIBUTES, PublishCapability.PUBLISH, PublishCapability.OFFLINE,
                PublishCapability.PRICE);
    }

    @Override
    public List<PlatformCategory> categories(ConnectorContext ctx, String parentId, String keyword) {
        String site = ctx.credentials().require("site").toUpperCase(Locale.ROOT);
        List<PlatformCategory> out = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            for (JsonNode c : get(ctx, "/sites/" + site + "/domain_discovery/search?limit=8&q=" + ConnectorSigns.enc(keyword.trim()))) {
                out.add(new PlatformCategory(HttpJson.text(c, "category_id"), HttpJson.text(c, "category_name"), null, true, HttpJson.text(c, "domain_name")));
            }
        } else if (parentId == null || parentId.isBlank()) {
            for (JsonNode c : get(ctx, "/sites/" + site + "/categories")) {
                out.add(new PlatformCategory(HttpJson.text(c, "id"), HttpJson.text(c, "name"), null, false, null));
            }
        } else {
            for (JsonNode c : get(ctx, "/categories/" + ConnectorSigns.enc(parentId)).path("children_categories")) {
                out.add(new PlatformCategory(HttpJson.text(c, "id"), HttpJson.text(c, "name"), parentId, isLeaf(ctx, HttpJson.text(c, "id")), null));
            }
        }
        return out;
    }

    private static boolean isLeaf(ConnectorContext ctx, String id) {
        try {
            JsonNode children = get(ctx, "/categories/" + ConnectorSigns.enc(id)).path("children_categories");
            return !children.isArray() || children.isEmpty();
        } catch (BizException e) {
            return false;
        }
    }

    @Override
    public List<PlatformAttribute> attributes(ConnectorContext ctx, String categoryId) {
        List<PlatformAttribute> req = new ArrayList<>();
        List<PlatformAttribute> opt = new ArrayList<>();
        for (JsonNode a : get(ctx, "/categories/" + ConnectorSigns.enc(categoryId) + "/attributes")) {
            JsonNode tags = a.path("tags");
            if (tags.path("read_only").asBoolean(false) || tags.path("hidden").asBoolean(false)) {
                continue;
            }
            List<String> options = new ArrayList<>();
            for (JsonNode v : a.path("values")) {
                options.add(HttpJson.text(v, "name"));
            }
            boolean required = tags.path("required").asBoolean(false) || tags.path("catalog_required").asBoolean(false);
            String type = String.valueOf(HttpJson.text(a, "value_type"));
            PlatformAttribute pa = new PlatformAttribute(HttpJson.text(a, "id"), HttpJson.text(a, "name"), required,
                    tags.path("multivalued").asBoolean(false) ? PlatformAttribute.MULTI : !options.isEmpty() ? PlatformAttribute.SELECT
                            : "number".equals(type) || "number_unit".equals(type) ? PlatformAttribute.NUMBER : PlatformAttribute.TEXT,
                    options, HttpJson.text(a, "hint"));
            (required ? req : opt).add(pa);
        }
        req.addAll(opt);
        return req;
    }

    @Override
    public PublishResult publish(ConnectorContext ctx, PublishPayload p) {
        if (p.categoryId() == null) {
            throw new BizException("ecom.error.pub.categoryRequired");
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", PublishSupport.cut(p.title(), 60));
        item.put("category_id", p.categoryId());
        PublishSku first = p.skus().get(0);
        item.put("price", first.price());
        item.put("currency_id", p.currency());
        item.put("available_quantity", p.skus().stream().mapToInt(PublishSku::qty).sum());
        item.put("buying_mode", "buy_it_now");
        item.put("listing_type_id", ConnectorJson.firstNonBlank(p.extra("listingTypeId"), "gold_special"));
        item.put("condition", "new");
        item.put("pictures", p.images().stream().map(u -> Map.of("source", u)).toList());
        if (p.extra("shippingMode") != null) {
            item.put("shipping", Map.of("mode", p.extra("shippingMode")));
        }
        List<Map<String, Object>> attrs = new ArrayList<>();
        p.attributes().forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                attrs.add(Map.of("id", k, "value_name", v.trim()));
            }
        });
        if (p.brand() != null && p.attribute("BRAND") == null) {
            attrs.add(Map.of("id", "BRAND", "value_name", p.brand()));
        }
        if (!p.multiVariant()) {
            attrs.add(Map.of("id", "SELLER_SKU", "value_name", first.sellerSku()));
            if (!p.upcExempt() && first.barcode() != null && !first.barcode().isBlank()) {
                attrs.add(Map.of("id", "GTIN", "value_name", first.barcode().trim()));
            }
        }
        if (p.upcExempt()) {
            attrs.add(Map.of("id", "EMPTY_GTIN_REASON", "value_id", ConnectorJson.firstNonBlank(p.extra("emptyGtinReason"), "17055160")));
        }
        item.put("attributes", attrs);
        if (p.multiVariant()) {
            List<Map<String, Object>> variations = new ArrayList<>();
            for (PublishSku s : p.skus()) {
                Map<String, Object> v = new LinkedHashMap<>();
                v.put("attribute_combinations", (s.variantAttrs().isEmpty() ? Map.of("Option", ConnectorJson.firstNonBlank(s.variantText(), s.sellerSku()))
                        : s.variantAttrs()).entrySet().stream().map(e -> Map.of("name", e.getKey(), "value_name", e.getValue())).toList());
                v.put("price", s.price());
                v.put("available_quantity", s.qty());
                v.put("attributes", List.of(Map.of("id", "SELLER_SKU", "value_name", s.sellerSku())));
                variations.add(v);
            }
            item.put("variations", variations);
        }
        JsonNode resp = HttpJson.postJson(base(ctx) + "/items", headers(ctx), item, PLATFORM);
        String itemId = HttpJson.text(resp, "id");
        if (itemId == null) {
            return PublishResult.failed(HttpJson.abbreviate(resp.toString()));
        }
        String text = description(p);
        if (!text.isBlank()) {
            HttpJson.postJson(base(ctx) + "/items/" + ConnectorSigns.enc(itemId) + "/description", headers(ctx), Map.of("plain_text", text), PLATFORM);
        }
        Map<String, String> skuIds = new LinkedHashMap<>();
        if (p.multiVariant()) {
            int i = 0;
            for (JsonNode v : resp.path("variations")) {
                if (i < p.skus().size()) {
                    skuIds.put(p.skus().get(i++).sellerSku(), HttpJson.text(v, "id"));
                }
            }
        } else {
            skuIds.put(first.sellerSku(), itemId);
        }
        return PublishResult.success(itemId, HttpJson.text(resp, "permalink"), skuIds, Map.of(), null);
    }

    private static String description(PublishPayload p) {
        StringBuilder sb = new StringBuilder();
        p.bullets().stream().filter(b -> b != null && !b.isBlank()).forEach(b -> sb.append("- ").append(b.trim()).append('\n'));
        if (p.description() != null) {
            sb.append(p.description());
        }
        return sb.toString();
    }

    @Override
    public void offline(ConnectorContext ctx, PublishPayload payload, List<ListingRef> refs) {
        refs.stream().map(ListingRef::productId).filter(id -> id != null && !id.isBlank()).distinct()
                .forEach(id -> HttpJson.putJson(base(ctx) + "/items/" + ConnectorSigns.enc(id), headers(ctx), Map.of("status", "paused"), PLATFORM));
    }

    @Override
    public List<PriceResult> updatePrices(ConnectorContext ctx, List<PriceUpdate> updates) {
        List<PriceResult> out = new ArrayList<>();
        Map<String, List<PriceUpdate>> byItem = new LinkedHashMap<>();
        for (PriceUpdate u : updates) {
            if (u.ref().productId() == null) {
                out.add(PriceResult.fail(u.sellerSku(), "item_id missing"));
            } else {
                byItem.computeIfAbsent(u.ref().productId(), k -> new ArrayList<>()).add(u);
            }
        }
        byItem.forEach((itemId, list) -> {
            try {
                Object body = list.get(0).ref().variantId() == null ? Map.of("price", list.get(0).price())
                        : Map.of("variations", list.stream().map(u -> Map.of("id", u.ref().variantId(), "price", u.price())).toList());
                HttpJson.putJson(base(ctx) + "/items/" + ConnectorSigns.enc(itemId), headers(ctx), body, PLATFORM);
                list.forEach(u -> out.add(PriceResult.ok(u.sellerSku())));
            } catch (BizException e) {
                list.forEach(u -> out.add(PriceResult.fail(u.sellerSku(), e.getMessage())));
            }
        });
        return out;
    }

    private static JsonNode get(ConnectorContext ctx, String path) {
        return HttpJson.get(base(ctx) + path, headers(ctx), PLATFORM);
    }

    private static String base(ConnectorContext ctx) {
        return PublishSupport.baseUrl(ctx, API, null);
    }

    private static Map<String, String> headers(ConnectorContext ctx) {
        return Map.of("Authorization", "Bearer " + ctx.credentials().require("accessToken"), "Accept", "application/json");
    }
}
