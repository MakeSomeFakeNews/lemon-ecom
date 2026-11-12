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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Temu 卖家开放平台（Partner Platform，半托管 / 本土店铺）的刊登与改价，协议与订单连接器相同：POST JSON 到 router，
 * 公共参数 type、app_key、access_token、timestamp、data_type、version 与业务参数同层，签名 MD5(secret + 排序后的 k+v + secret) 大写，
 * 嵌套对象 / 数组按 JSON 串参与签名；响应 {success, errorCode, errorMsg, result}。
 * <ul>
 *   <li>类目：bg.goods.cats.get（parentCatId，0 为顶级；isLeaf 为叶子）；属性：bg.goods.attrs.get（catId；isSale 为销售属性即规格，不列出）</li>
 *   <li>图片：bg.goods.image.upload（按图片地址上传到 Temu 图片空间，返回 imageUrl）</li>
 *   <li>发布：bg.goods.add（goodsBasic 名称 / 类目 / 外部编码、goodsGallery 轮播图、goodsDesc、bulletPoints、goodsProperty 属性、
 *       goodsServicePromise 运费模板 costTemplateId 与备货天数 shipmentLimitDay、skuList[outSkuSn、price.basePrice、quantity、specList]），返回 goodsId 与 SKU ID</li>
 *   <li>改价：bg.local.goods.sku.list.price.update（goodsId + skuId + 价格）；下架：bg.local.goods.sale.status.set（onSale = 0）</li>
 * </ul>
 * 网关按授权信息 region（US / EU / GLOBAL）选择，店铺填写了自定义地址时用它。
 * <p>
 * 说明：按公开资料实现，尚未用真实店铺联调。需要真实账号联调确认：
 * <ul>
 *   <li>接口名与版本（半托管与本土店铺的商品接口不同：bg.goods.add / bg.local.goods.add、价格与上下架接口名）</li>
 *   <li>价格字段单位（元 / 分）与币种字段、规格 specList 是否需要先用 bg.goods.spec.create 生成规格 ID</li>
 *   <li>属性值提交需要 vid（可选值 ID），自定义属性的写法；图片上传接口与图片尺寸要求</li>
 *   <li>全托管店铺不能通过本接口刊登（全托管由 Temu 定价、卖家只报价备货）</li>
 * </ul>
 */
@Component
public class TemuPublishConnector implements PublishConnector {

    static final String PLATFORM = "Temu";
    static final String API_VERSION = "V1";
    private static final Map<String, String> GATEWAYS = Map.of(
            "US", "https://openapi-b-us.temu.com/openapi/router",
            "EU", "https://openapi-b-eu.temu.com/openapi/router",
            "GLOBAL", "https://openapi-b-global.temu.com/openapi/router");

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.TEMU;
    }

    @Override
    public Set<PublishCapability> capabilities() {
        return EnumSet.of(PublishCapability.CATEGORIES, PublishCapability.ATTRIBUTES, PublishCapability.PUBLISH, PublishCapability.OFFLINE,
                PublishCapability.PRICE);
    }

    @Override
    public String note() {
        return "ecom.text.pub.note.temu";
    }

    @Override
    public List<PlatformCategory> categories(ConnectorContext ctx, String parentId, String keyword) {
        String parent = parentId == null || parentId.isBlank() ? "0" : parentId;
        JsonNode result = call(ctx, "bg.goods.cats.get", Map.of("parentCatId", Long.parseLong(parent)));
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase(Locale.ROOT);
        List<PlatformCategory> out = new ArrayList<>();
        for (JsonNode c : result.path("goodsCatsList")) {
            String name = String.valueOf(HttpJson.text(c, "catName"));
            if (kw == null || name.toLowerCase(Locale.ROOT).contains(kw)) {
                out.add(new PlatformCategory(HttpJson.text(c, "catId"), name, "0".equals(parent) ? null : parent, c.path("isLeaf").asBoolean(false), null));
            }
        }
        return out;
    }

    @Override
    public List<PlatformAttribute> attributes(ConnectorContext ctx, String categoryId) {
        JsonNode result = call(ctx, "bg.goods.attrs.get", Map.of("catId", Long.parseLong(categoryId)));
        List<PlatformAttribute> req = new ArrayList<>();
        List<PlatformAttribute> opt = new ArrayList<>();
        for (JsonNode a : result.path("properties")) {
            if (a.path("isSale").asBoolean(false)) {
                continue;
            }
            List<String> options = new ArrayList<>();
            for (JsonNode v : a.path("values")) {
                options.add(HttpJson.text(v, "value"));
            }
            boolean required = a.path("required").asBoolean(false);
            PlatformAttribute pa = new PlatformAttribute(HttpJson.text(a, "pid"), HttpJson.text(a, "name"), required,
                    options.isEmpty() ? PlatformAttribute.TEXT : PlatformAttribute.SELECT, options, null);
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
        List<String> images = new ArrayList<>();
        for (String url : p.images()) {
            String uploaded = HttpJson.text(call(ctx, "bg.goods.image.upload", Map.of("url", url)), "imageUrl");
            if (uploaded == null) {
                throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "imageUrl missing");
            }
            images.add(uploaded);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("goodsName", PublishSupport.cut(p.title(), 250));
        basic.put("catId", Long.parseLong(p.categoryId().trim()));
        basic.put("outGoodsSn", p.multiVariant() ? p.parentSku() : p.skus().get(0).sellerSku());
        body.put("goodsBasic", basic);
        body.put("goodsGallery", Map.of("carouselImageUrls", images));
        if (p.description() != null && !p.description().isBlank()) {
            body.put("goodsDesc", Map.of("goodsDesc", p.description()));
        }
        if (!p.bullets().isEmpty()) {
            body.put("bulletPoints", p.bullets());
        }
        List<Map<String, Object>> props = new ArrayList<>();
        p.attributes().forEach((k, v) -> {
            if (v != null && !v.isBlank() && k.chars().allMatch(Character::isDigit)) {
                for (String s : PublishSupport.values(v)) {
                    props.add(Map.of("pid", Long.parseLong(k), "value", s));
                }
            }
        });
        if (!props.isEmpty()) {
            body.put("goodsProperty", Map.of("goodsProperties", props));
        }
        Map<String, Object> promise = new LinkedHashMap<>();
        if (p.shippingTemplate() != null) {
            promise.put("costTemplateId", p.shippingTemplate());
        }
        promise.put("shipmentLimitDay", p.handlingDays() == null ? 2 : p.handlingDays());
        body.put("goodsServicePromise", promise);
        List<Map<String, Object>> skus = new ArrayList<>();
        for (PublishSku s : p.skus()) {
            Map<String, Object> sku = new LinkedHashMap<>();
            sku.put("outSkuSn", s.sellerSku());
            sku.put("price", Map.of("basePrice", Map.of("amount", PublishSupport.money(s.price()), "currency", p.currency())));
            sku.put("quantity", s.qty());
            if (!s.variantAttrs().isEmpty()) {
                sku.put("specList", s.variantAttrs().entrySet().stream().map(e -> Map.of("parentSpecName", e.getKey(), "specName", e.getValue())).toList());
            }
            if (!images.isEmpty()) {
                sku.put("images", List.of(images.get(0)));
            }
            skus.add(sku);
        }
        body.put("skuList", skus);
        JsonNode result = call(ctx, "bg.goods.add", body);
        String goodsId = HttpJson.text(result, "goodsId");
        if (goodsId == null) {
            return PublishResult.failed(HttpJson.abbreviate(result.toString()));
        }
        Map<String, String> skuIds = new LinkedHashMap<>();
        for (JsonNode s : result.path("skuInfoList")) {
            skuIds.put(HttpJson.text(s, "outSkuSn"), HttpJson.text(s, "skuId"));
        }
        return PublishResult.success(goodsId, null, skuIds, Map.of(), null);
    }

    @Override
    public void offline(ConnectorContext ctx, PublishPayload payload, List<ListingRef> refs) {
        refs.stream().map(ListingRef::productId).filter(id -> id != null && !id.isBlank()).distinct()
                .forEach(id -> call(ctx, "bg.local.goods.sale.status.set", Map.of("goodsId", Long.parseLong(id), "onSale", 0)));
    }

    @Override
    public List<PriceResult> updatePrices(ConnectorContext ctx, List<PriceUpdate> updates) {
        Map<String, List<PriceUpdate>> byGoods = new LinkedHashMap<>();
        List<PriceResult> out = new ArrayList<>();
        for (PriceUpdate u : updates) {
            if (u.ref().parentItemId() == null || u.ref().platformItemId() == null) {
                out.add(PriceResult.fail(u.sellerSku(), "goodsId / skuId missing"));
            } else {
                byGoods.computeIfAbsent(u.ref().parentItemId(), k -> new ArrayList<>()).add(u);
            }
        }
        byGoods.forEach((goodsId, list) -> {
            try {
                List<Map<String, Object>> skus = list.stream().map(u -> Map.<String, Object>of("skuId", Long.parseLong(u.ref().platformItemId()),
                        "price", Map.of("amount", PublishSupport.money(u.price()), "currency", u.currency() == null ? "USD" : u.currency()))).toList();
                call(ctx, "bg.local.goods.sku.list.price.update", Map.of("goodsId", Long.parseLong(goodsId), "skuPriceList", skus));
                list.forEach(u -> out.add(PriceResult.ok(u.sellerSku())));
            } catch (BizException | NumberFormatException e) {
                list.forEach(u -> out.add(PriceResult.fail(u.sellerSku(), e.getMessage())));
            }
        });
        return out;
    }

    // ---------------------------------------------------------------- 协议

    static JsonNode call(ConnectorContext ctx, String type, Map<String, ?> business) {
        StoreCredentials c = ctx.credentials();
        Map<String, Object> body = new TreeMap<>(business);
        body.put("type", type);
        body.put("app_key", c.require("appKey"));
        body.put("access_token", c.require("accessToken"));
        body.put("timestamp", Instant.now().getEpochSecond());
        body.put("data_type", "JSON");
        body.put("version", API_VERSION);
        body.put("sign", sign(c.require("appSecret"), body));
        JsonNode resp = HttpJson.postJson(gateway(ctx), Map.of(), body, PLATFORM);
        if (!resp.path("success").asBoolean(false)) {
            String summary = resp.path("errorCode").asString("") + " " + resp.path("errorMsg").asString("");
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(summary.isBlank() ? resp.toString() : summary.trim()));
        }
        return resp.path("result");
    }

    static String sign(String secret, Map<String, Object> params) {
        TreeMap<String, String> sorted = new TreeMap<>();
        params.forEach((k, v) -> {
            if (!"sign".equals(k) && v != null) {
                sorted.put(k, v instanceof String s ? s : v instanceof Number || v instanceof Boolean ? v.toString() : HttpJson.MAPPER.writeValueAsString(v));
            }
        });
        return ConnectorSigns.md5Upper(secret + ConnectorSigns.concatSorted(sorted) + secret);
    }

    private static String gateway(ConnectorContext ctx) {
        if (ctx.baseUrlOverride() != null) {
            return ctx.baseUrlOverride();
        }
        String region = ConnectorJson.firstNonBlank(ctx.credentials().get("region"), "US").toUpperCase(Locale.ROOT);
        String gateway = GATEWAYS.get(region);
        if (gateway == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "region must be US, EU or GLOBAL: " + region);
        }
        return gateway;
    }
}
