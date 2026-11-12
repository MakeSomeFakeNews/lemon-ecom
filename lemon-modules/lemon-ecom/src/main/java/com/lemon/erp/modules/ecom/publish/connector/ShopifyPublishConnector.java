package com.lemon.erp.modules.ecom.publish.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorJson;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shopify Admin GraphQL API（2025-01）的刊登与改价。Shopify 没有平台类目（商品类型、标签在属性中填写 productType、tags）：
 * <ul>
 *   <li>发布：productSet（synchronous = true，一次带上选项 productOptions、变体 variants（价格、条码、inventoryItem.sku、
 *       有默认地点 locationId 时带 inventoryQuantities）与图片 files），相当于 productCreate + productVariantsBulkCreate，返回商品 ID 与变体 ID</li>
 *   <li>改价：productVariantsBulkUpdate（按商品分组，变体 ID 取在线商品的平台商品 ID / 附加信息 variantId）</li>
 *   <li>下架：productUpdate 把商品状态改为 DRAFT（从销售渠道下架，不删除）</li>
 * </ul>
 * 授权复用店铺的渠道授权（shopDomain、adminAccessToken，权限需含 write_products、write_inventory），地址 https://{shopDomain}/admin/api/2025-01/graphql.json。
 * <p>
 * 说明：按 Shopify 开发者文档实现，尚未用真实商店联调。需要真实账号联调确认：
 * <ul>
 *   <li>productSet 在 2024-04 之前的版本不可用；inventoryQuantities 的 name 取 available</li>
 *   <li>图片 originalSource 必须是 Shopify 能访问的公网地址，图片处理是异步的</li>
 * </ul>
 */
@Component
public class ShopifyPublishConnector implements PublishConnector {

    static final String PLATFORM = "Shopify";
    static final String API_VERSION = "2025-01";
    static final String DEFAULT_OPTION = "Title";
    static final String DEFAULT_VALUE = "Default Title";

    static final String PRODUCT_SET = """
            mutation productSet($input: ProductSetInput!, $synchronous: Boolean!) {
              productSet(synchronous: $synchronous, input: $input) {
                product { id handle onlineStoreUrl variants(first: 100) { nodes { id sku inventoryItem { id } } } }
                userErrors { field message }
              }
            }""";
    static final String VARIANTS_UPDATE = """
            mutation productVariantsBulkUpdate($productId: ID!, $variants: [ProductVariantsBulkInput!]!) {
              productVariantsBulkUpdate(productId: $productId, variants: $variants) {
                productVariants { id price }
                userErrors { field message }
              }
            }""";
    static final String PRODUCT_UPDATE = """
            mutation productUpdate($product: ProductUpdateInput!) {
              productUpdate(product: $product) { product { id status } userErrors { field message } }
            }""";

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.SHOPIFY;
    }

    @Override
    public Set<PublishCapability> capabilities() {
        return EnumSet.of(PublishCapability.PUBLISH, PublishCapability.OFFLINE, PublishCapability.PRICE);
    }

    @Override
    public int minImages() {
        return 0;
    }

    @Override
    public String note() {
        return "ecom.text.pub.note.shopify";
    }

    @Override
    public PublishResult publish(ConnectorContext ctx, PublishPayload p) {
        List<String> optionNames = new ArrayList<>();
        for (PublishSku s : p.skus()) {
            s.variantAttrs().keySet().forEach(k -> {
                if (!optionNames.contains(k) && optionNames.size() < 3) {
                    optionNames.add(k);
                }
            });
        }
        boolean single = !p.multiVariant() || optionNames.isEmpty() && p.skus().size() == 1;
        if (p.multiVariant() && optionNames.isEmpty()) {
            optionNames.add("Option");
        }
        if (single) {
            optionNames.clear();
            optionNames.add(DEFAULT_OPTION);
        }
        Map<String, List<String>> optionValues = new LinkedHashMap<>();
        optionNames.forEach(n -> optionValues.put(n, new ArrayList<>()));
        String location = ConnectorJson.firstNonBlank(p.extra("locationId"), ctx.credentials().get("locationId"));
        List<Map<String, Object>> variants = new ArrayList<>();
        for (PublishSku s : p.skus()) {
            List<Map<String, Object>> values = new ArrayList<>();
            for (String n : optionNames) {
                String v = single ? DEFAULT_VALUE : s.variantAttrs().isEmpty() ? ConnectorJson.firstNonBlank(s.variantText(), s.sellerSku())
                        : ConnectorJson.firstNonBlank(s.variantAttrs().get(n), "-");
                if (!optionValues.get(n).contains(v)) {
                    optionValues.get(n).add(v);
                }
                values.add(Map.of("optionName", n, "name", v));
            }
            Map<String, Object> variant = new LinkedHashMap<>();
            variant.put("optionValues", values);
            variant.put("price", PublishSupport.money(s.price()));
            variant.put("inventoryItem", Map.of("sku", s.sellerSku(), "tracked", true));
            if (s.barcode() != null && !s.barcode().isBlank()) {
                variant.put("barcode", s.barcode().trim());
            }
            if (location != null) {
                variant.put("inventoryQuantities", List.of(Map.of("locationId", gid("Location", location), "name", "available", "quantity", s.qty())));
            }
            variants.add(variant);
        }
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("title", p.title());
        input.put("descriptionHtml", TikTokPublishConnector.html(p));
        if (p.brand() != null && !p.brand().isBlank()) {
            input.put("vendor", p.brand());
        }
        String productType = ConnectorJson.firstNonBlank(p.attribute("productType"), p.categoryId());
        if (productType != null) {
            input.put("productType", productType);
        }
        if (p.attribute("tags") != null) {
            input.put("tags", PublishSupport.values(p.attribute("tags")));
        }
        input.put("status", "ACTIVE");
        input.put("productOptions", optionValues.entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "values", e.getValue().stream().map(v -> Map.of("name", v)).toList())).toList());
        input.put("variants", variants);
        if (!p.images().isEmpty()) {
            input.put("files", p.images().stream().map(u -> Map.of("originalSource", u, "contentType", "IMAGE")).toList());
        }
        JsonNode payload = call(ctx, PRODUCT_SET, Map.of("input", input, "synchronous", true)).path("productSet");
        JsonNode product = payload.path("product");
        String productId = ShopifyIds.numeric(HttpJson.text(product, "id"));
        if (productId == null) {
            return PublishResult.failed("product id missing");
        }
        Map<String, String> skuIds = new LinkedHashMap<>();
        Map<String, Map<String, String>> extra = new LinkedHashMap<>();
        for (JsonNode v : product.path("variants").path("nodes")) {
            String sku = HttpJson.text(v, "sku");
            String variantId = ShopifyIds.numeric(HttpJson.text(v, "id"));
            if (sku != null) {
                skuIds.put(sku, variantId);
                Map<String, String> e = new LinkedHashMap<>();
                e.put("variantId", variantId);
                String inventoryItem = ShopifyIds.numeric(HttpJson.text(v.path("inventoryItem"), "id"));
                if (inventoryItem != null) {
                    e.put("inventoryItemId", inventoryItem);
                }
                extra.put(sku, e);
            }
        }
        return PublishResult.success(productId, HttpJson.text(product, "onlineStoreUrl"), skuIds, extra, null);
    }

    @Override
    public void offline(ConnectorContext ctx, PublishPayload payload, List<ListingRef> refs) {
        List<String> ids = refs.stream().map(ListingRef::productId).filter(id -> id != null && !id.isBlank()).distinct().toList();
        for (String id : ids) {
            call(ctx, PRODUCT_UPDATE, Map.of("product", Map.of("id", gid("Product", id), "status", "DRAFT"))).path("productUpdate");
        }
    }

    @Override
    public List<PriceResult> updatePrices(ConnectorContext ctx, List<PriceUpdate> updates) {
        Map<String, List<PriceUpdate>> byProduct = new LinkedHashMap<>();
        List<PriceResult> out = new ArrayList<>();
        for (PriceUpdate u : updates) {
            if (u.ref().parentItemId() == null || variantId(u.ref()) == null) {
                out.add(PriceResult.fail(u.sellerSku(), "product / variant id missing"));
            } else {
                byProduct.computeIfAbsent(u.ref().parentItemId(), k -> new ArrayList<>()).add(u);
            }
        }
        byProduct.forEach((productId, list) -> {
            try {
                List<Map<String, Object>> variants = list.stream()
                        .map(u -> Map.<String, Object>of("id", gid("ProductVariant", variantId(u.ref())), "price", PublishSupport.money(u.price()))).toList();
                call(ctx, VARIANTS_UPDATE, Map.of("productId", gid("Product", productId), "variants", variants));
                list.forEach(u -> out.add(PriceResult.ok(u.sellerSku())));
            } catch (BizException e) {
                list.forEach(u -> out.add(PriceResult.fail(u.sellerSku(), e.getMessage())));
            }
        });
        return out;
    }

    private static String variantId(ListingRef ref) {
        return ConnectorJson.firstNonBlank(ref.extra("variantId"), ref.platformItemId());
    }

    // ---------------------------------------------------------------- 调用

    private static String url(ConnectorContext ctx) {
        String base = ctx.baseUrlOverride();
        if (base == null) {
            String domain = ctx.credentials().require("shopDomain").toLowerCase().replaceFirst("^https?://", "");
            int slash = domain.indexOf('/');
            if (slash >= 0) {
                domain = domain.substring(0, slash);
            }
            if (!domain.contains(".")) {
                domain = domain + ".myshopify.com";
            }
            base = "https://" + domain;
        }
        return base + "/admin/api/" + API_VERSION + "/graphql.json";
    }

    /** 调用 GraphQL：errors 或任一 mutation 的 userErrors 不为空时抛出异常，返回 data */
    private static JsonNode call(ConnectorContext ctx, String query, Map<String, Object> variables) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("variables", variables);
        JsonNode resp = HttpJson.postJson(url(ctx), Map.of("X-Shopify-Access-Token", ctx.credentials().require("adminAccessToken"),
                "Accept", "application/json"), body, PLATFORM);
        JsonNode errors = resp.path("errors");
        if ((errors.isArray() && !errors.isEmpty()) || errors.isString() || errors.isObject()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(errors.toString()));
        }
        JsonNode data = resp.path("data");
        for (Map.Entry<String, JsonNode> e : data.properties()) {
            JsonNode ue = e.getValue().path("userErrors");
            if (ue.isArray() && !ue.isEmpty()) {
                throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(ue.toString()));
            }
        }
        return data;
    }

    private static String gid(String type, String id) {
        return id.startsWith("gid://") ? id : "gid://shopify/" + type + "/" + id;
    }

    /** gid 与数字 ID 的换算 */
    static final class ShopifyIds {
        private ShopifyIds() {
        }

        static String numeric(String gid) {
            if (gid == null) {
                return null;
            }
            String s = gid;
            int q = s.indexOf('?');
            if (q >= 0) {
                s = s.substring(0, q);
            }
            int slash = s.lastIndexOf('/');
            return slash >= 0 ? s.substring(slash + 1) : s;
        }
    }
}
