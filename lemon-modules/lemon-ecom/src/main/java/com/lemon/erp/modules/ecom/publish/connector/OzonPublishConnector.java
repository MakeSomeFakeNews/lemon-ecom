package com.lemon.erp.modules.ecom.publish.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorJson;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.connector.StoreCredentials;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Ozon Seller API 的刊登与改价（请求头 Client-Id、Api-Key，全部 POST JSON）：
 * <ul>
 *   <li>类目：POST /v1/description-category/tree（类目 → 商品类型，商品类型为叶子，ID 记为「类目 ID:类型 ID」）；
 *       属性：POST /v1/description-category/attribute（is_required 为必填；字典型属性的取值另由 /v1/description-category/attribute/values 查询，这里按文字提交）</li>
 *   <li>发布：POST /v3/product/import（items：offer_id、name、description_category_id、type_id、price、currency_code、vat、images、primary_image、
 *       重量 / 尺寸、attributes[id、values[value]]，属性 4191 为描述），返回 task_id，平台异步处理，刊登记录保持「发布中」；
 *       定时任务查询 POST /v1/product/import/info（status = imported 成功并带回 product_id，failed 为失败，errors 为原因）</li>
 *   <li>改价：POST /v1/product/import/prices（每批 1000 个）；下架：POST /v1/product/archive（product_id）</li>
 * </ul>
 * 重量（g）、尺寸（mm）取属性或其他参数 weight、depth、width、height（默认 500 g、100 mm），增值税率取其他参数 vat（默认 0）。库存另经订单连接器回传。
 * <p>
 * 说明：按公开文档实现，尚未用真实账号联调。需要真实账号联调确认：
 * <ul>
 *   <li>必填属性（品牌 85、型号名称 9048 等）与字典值需要提交 dictionary_value_id 的情况</li>
 *   <li>/v3/product/import 的 currency_code 与店铺结算币种（RUB / CNY）一致，description 字段与属性 4191 的关系</li>
 * </ul>
 */
@Component
public class OzonPublishConnector implements PublishConnector {

    static final String PLATFORM = "Ozon";
    static final String GATEWAY = "https://api-seller.ozon.ru";
    static final int BATCH = 1000;
    /** 描述属性 */
    static final long DESCRIPTION_ATTRIBUTE = 4191;

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.OZON;
    }

    @Override
    public Set<PublishCapability> capabilities() {
        return EnumSet.of(PublishCapability.CATEGORIES, PublishCapability.ATTRIBUTES, PublishCapability.PUBLISH, PublishCapability.OFFLINE,
                PublishCapability.PRICE);
    }

    @Override
    public String note() {
        return "ecom.text.pub.note.ozon";
    }

    @Override
    public List<PlatformCategory> categories(ConnectorContext ctx, String parentId, String keyword) {
        JsonNode result = post(ctx, "/v1/description-category/tree", Map.of("language", "DEFAULT")).path("result");
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase(Locale.ROOT);
        List<PlatformCategory> out = new ArrayList<>();
        walk(result, null, null, "", parentId == null || parentId.isBlank() ? null : parentId, kw, out);
        return out;
    }

    private static void walk(JsonNode nodes, String parent, String categoryId, String prefix, String wanted, String kw, List<PlatformCategory> out) {
        for (JsonNode n : nodes) {
            String typeId = HttpJson.text(n, "type_id");
            boolean type = typeId != null && !"0".equals(typeId);
            String cat = type ? categoryId : HttpJson.text(n, "description_category_id");
            String id = type ? categoryId + ":" + typeId : cat;
            String name = String.valueOf(type ? HttpJson.text(n, "type_name") : HttpJson.text(n, "category_name"));
            String path = prefix.isEmpty() ? name : prefix + " > " + name;
            boolean match = kw != null ? type && name.toLowerCase(Locale.ROOT).contains(kw) : wanted == null ? parent == null : wanted.equals(parent);
            if (match) {
                out.add(new PlatformCategory(id, name, parent, type, path));
            }
            if (n.path("children").isArray()) {
                walk(n.path("children"), id, cat, path, wanted, kw, out);
            }
        }
    }

    @Override
    public List<PlatformAttribute> attributes(ConnectorContext ctx, String categoryId) {
        long[] ids = parse(categoryId);
        JsonNode result = post(ctx, "/v1/description-category/attribute", Map.of("description_category_id", ids[0], "type_id", ids[1],
                "language", "DEFAULT")).path("result");
        List<PlatformAttribute> req = new ArrayList<>();
        List<PlatformAttribute> opt = new ArrayList<>();
        for (JsonNode a : result) {
            if (a.path("id").asLong() == DESCRIPTION_ATTRIBUTE) {
                continue;
            }
            boolean required = a.path("is_required").asBoolean(false);
            String type = String.valueOf(HttpJson.text(a, "type"));
            PlatformAttribute pa = new PlatformAttribute(HttpJson.text(a, "id"), HttpJson.text(a, "name"), required,
                    a.path("is_collection").asBoolean(false) ? PlatformAttribute.MULTI
                            : "Integer".equals(type) || "Decimal".equals(type) ? PlatformAttribute.NUMBER : PlatformAttribute.TEXT,
                    List.of(), PublishSupport.cut(HttpJson.text(a, "description"), 300));
            (required ? req : opt).add(pa);
        }
        req.addAll(opt);
        return req;
    }

    /** 「类目 ID:类型 ID」 */
    static long[] parse(String categoryId) {
        String[] parts = categoryId == null ? new String[0] : categoryId.split(":");
        if (parts.length != 2) {
            throw new BizException("ecom.error.pub.ozonCategoryInvalid", String.valueOf(categoryId));
        }
        return new long[]{Long.parseLong(parts[0].trim()), Long.parseLong(parts[1].trim())};
    }

    @Override
    public PublishResult publish(ConnectorContext ctx, PublishPayload p) {
        long[] ids = parse(p.categoryId());
        List<Map<String, Object>> items = new ArrayList<>();
        for (PublishSku s : p.skus()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("offer_id", s.sellerSku());
            item.put("name", p.multiVariant() && s.variantText() != null ? p.title() + " " + s.variantText() : p.title());
            item.put("description_category_id", ids[0]);
            item.put("type_id", ids[1]);
            item.put("price", PublishSupport.money(s.price()));
            item.put("old_price", "0");
            if (p.currency() != null) {
                item.put("currency_code", p.currency());
            }
            item.put("vat", ConnectorJson.firstNonBlank(p.extra("vat"), "0"));
            if (!p.images().isEmpty()) {
                item.put("primary_image", p.images().get(0));
                item.put("images", p.images().subList(1, p.images().size()));
            }
            if (!p.upcExempt() && s.barcode() != null && !s.barcode().isBlank()) {
                item.put("barcode", s.barcode().trim());
            }
            item.put("weight", Integer.parseInt(ConnectorJson.firstNonBlank(p.attribute("weight"), p.extra("weight"), "500")));
            item.put("weight_unit", "g");
            item.put("depth", Integer.parseInt(ConnectorJson.firstNonBlank(p.attribute("depth"), "100")));
            item.put("width", Integer.parseInt(ConnectorJson.firstNonBlank(p.attribute("width"), "100")));
            item.put("height", Integer.parseInt(ConnectorJson.firstNonBlank(p.attribute("height"), "100")));
            item.put("dimension_unit", "mm");
            List<Map<String, Object>> attrs = new ArrayList<>();
            p.attributes().forEach((k, v) -> {
                if (v != null && !v.isBlank() && k.chars().allMatch(Character::isDigit)) {
                    attrs.add(Map.of("id", Long.parseLong(k), "complex_id", 0, "values", PublishSupport.values(v).stream().map(x -> Map.of("value", x)).toList()));
                }
            });
            String desc = TikTokPublishConnector.html(p);
            attrs.add(Map.of("id", DESCRIPTION_ATTRIBUTE, "complex_id", 0, "values", List.of(Map.of("value", desc))));
            item.put("attributes", attrs);
            items.add(item);
        }
        JsonNode result = post(ctx, "/v3/product/import", Map.of("items", items)).path("result");
        String taskId = HttpJson.text(result, "task_id");
        if (taskId == null) {
            return PublishResult.failed(HttpJson.abbreviate(result.toString()));
        }
        return PublishResult.pending(taskId, "task_id " + taskId);
    }

    @Override
    public PublishResult checkPublish(ConnectorContext ctx, PublishPayload p, String ref) {
        JsonNode result = post(ctx, "/v1/product/import/info", Map.of("task_id", Long.parseLong(ref))).path("result");
        return importResult(result, ref);
    }

    /** 导入任务结果：全部 imported 为成功；任一 failed 为失败；否则仍在处理 */
    static PublishResult importResult(JsonNode result, String ref) {
        Map<String, String> skuIds = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        boolean pending = false;
        for (JsonNode i : result.path("items")) {
            String status = String.valueOf(HttpJson.text(i, "status"));
            String offer = HttpJson.text(i, "offer_id");
            if ("imported".equals(status)) {
                skuIds.put(offer, HttpJson.text(i, "product_id"));
            } else if ("failed".equals(status)) {
                List<String> msgs = new ArrayList<>();
                for (JsonNode e : i.path("errors")) {
                    msgs.add(ConnectorJson.firstNonBlank(HttpJson.text(e, "code"), "") + " " + ConnectorJson.firstNonBlank(HttpJson.text(e, "message"),
                            HttpJson.text(e, "description"), ""));
                }
                errors.add(offer + ": " + String.join(", ", msgs));
            } else {
                pending = true;
            }
        }
        if (!errors.isEmpty()) {
            return PublishResult.failed(PublishSupport.cut(String.join("; ", errors), 1500));
        }
        if (pending || skuIds.isEmpty()) {
            return PublishResult.pending(ref, "pending");
        }
        // 在线商品约定：平台商品 ID 为 Ozon SKU（同步商品时补上），父 ID 为 product_id
        Map<String, Map<String, String>> extra = new HashMap<>();
        skuIds.forEach((sku, productId) -> extra.put(sku, Map.of("productId", String.valueOf(productId))));
        return PublishResult.success(null, null, Map.of(), extra, null);
    }

    @Override
    public void offline(ConnectorContext ctx, PublishPayload payload, List<ListingRef> refs) {
        List<Long> ids = new ArrayList<>();
        for (ListingRef r : refs) {
            String id = ConnectorJson.firstNonBlank(r.extra("productId"), r.parentItemId());
            if (id != null) {
                ids.add(Long.parseLong(id));
            }
        }
        if (!ids.isEmpty()) {
            post(ctx, "/v1/product/archive", Map.of("product_id", ids));
        }
    }

    @Override
    public List<PriceResult> updatePrices(ConnectorContext ctx, List<PriceUpdate> updates) {
        List<PriceResult> out = new ArrayList<>();
        for (int i = 0; i < updates.size(); i += BATCH) {
            List<PriceUpdate> batch = updates.subList(i, Math.min(updates.size(), i + BATCH));
            List<Map<String, Object>> prices = new ArrayList<>();
            for (PriceUpdate u : batch) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("offer_id", u.sellerSku());
                p.put("price", PublishSupport.money(u.price()));
                p.put("old_price", "0");
                if (u.currency() != null) {
                    p.put("currency_code", u.currency());
                }
                prices.add(p);
            }
            try {
                JsonNode resp = post(ctx, "/v1/product/import/prices", Map.of("prices", prices));
                Map<String, String> failed = new HashMap<>();
                for (JsonNode r : resp.path("result")) {
                    if (!r.path("updated").asBoolean(false)) {
                        failed.put(HttpJson.text(r, "offer_id"), HttpJson.abbreviate(r.path("errors").toString()));
                    }
                }
                batch.forEach(u -> out.add(failed.containsKey(u.sellerSku()) ? PriceResult.fail(u.sellerSku(), failed.get(u.sellerSku()))
                        : PriceResult.ok(u.sellerSku())));
            } catch (BizException e) {
                batch.forEach(u -> out.add(PriceResult.fail(u.sellerSku(), e.getMessage())));
            }
        }
        return out;
    }

    private static JsonNode post(ConnectorContext ctx, String path, Object body) {
        StoreCredentials c = ctx.credentials();
        Map<String, String> headers = Map.of("Client-Id", c.require("clientId"), "Api-Key", c.require("apiKey"), "Accept", "application/json");
        return HttpJson.postJson(PublishSupport.baseUrl(ctx, GATEWAY, null) + path, headers, body, PLATFORM);
    }
}
