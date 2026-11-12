package com.lemon.erp.modules.ecom.publish.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorJson;
import com.lemon.erp.modules.ecom.connector.ConnectorSigns;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 沃尔玛 Marketplace API 的刊登与改价：
 * <ul>
 *   <li>发布：Feeds API POST /v3/feeds?feedType=MP_ITEM（MPItemFeedHeader + MPItem[Orderable（sku、条码、名称、品牌、价格、重量、备货天数）、
 *       Visible.{商品类型}（简述、要点 keyFeatures、主图 / 附图、属性、变体组 variantGroupId 与 variantAttributeNames）]），返回 feedId，
 *       平台异步处理，刊登记录保持「发布中」；定时任务查询 GET /v3/feeds/{feedId}?includeDetails=true：feedStatus = PROCESSED 时按
 *       itemIngestionStatus 判断每个 SKU 成功（wpid）或失败（ingestionErrors），ERROR 为失败</li>
 *   <li>改价：PUT /v3/price（每个 SKU 一次，currentPriceType = BASE）；下架：DELETE /v3/items/{sku}（Retire）</li>
 * </ul>
 * 沃尔玛没有公开的类目查询接口，「类目」填写商品类型（Product Type，如 Home Decor），属性按 Item Spec 手工填写键值。
 * 授权复用店铺的渠道授权（clientId、clientSecret），令牌由调用方先用 {@code WalmartConnector.ensureToken} 换取。
 * <p>
 * 说明：按开发者文档实现，尚未用真实卖家账号联调。需要真实账号联调确认：
 * <ul>
 *   <li>MP_ITEM 的 Item Spec 版本（代码用 4.2）与各商品类型的必填属性</li>
 *   <li>feed 处理通常需要数分钟到数小时，itemDetails 的分页（每页 50）</li>
 * </ul>
 */
@Component
public class WalmartPublishConnector implements PublishConnector {

    static final String PLATFORM = "Walmart";
    static final String PRODUCTION = "https://marketplace.walmartapis.com";
    static final String SANDBOX = "https://sandbox.walmartapis.com";
    static final String SVC_NAME = "Walmart Marketplace";
    static final String SPEC_VERSION = "4.2";

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.WALMART;
    }

    @Override
    public Set<PublishCapability> capabilities() {
        return EnumSet.of(PublishCapability.PUBLISH, PublishCapability.OFFLINE, PublishCapability.PRICE);
    }

    /** 商品类型（Visible 下的键）必须填写 */
    @Override
    public boolean categoryRequired() {
        return true;
    }

    @Override
    public String note() {
        return "ecom.text.pub.note.walmart";
    }

    @Override
    public PublishResult publish(ConnectorContext ctx, PublishPayload p) {
        if (p.categoryId() == null) {
            throw new BizException("ecom.error.pub.categoryRequired");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (PublishSku s : p.skus()) {
            Map<String, Object> orderable = new LinkedHashMap<>();
            orderable.put("sku", s.sellerSku());
            if (!p.upcExempt() && s.barcode() != null && !s.barcode().isBlank()) {
                orderable.put("productIdentifiers", Map.of("productIdType", p.barcodeType() == null ? "UPC" : p.barcodeType(), "productId", s.barcode().trim()));
            }
            orderable.put("productName", p.title());
            if (p.brand() != null) {
                orderable.put("brand", p.brand());
            }
            orderable.put("price", s.price());
            orderable.put("ShippingWeight", new BigDecimal(ConnectorJson.firstNonBlank(p.attribute("ShippingWeight"), p.extra("weight"), "1")));
            if (p.handlingDays() != null) {
                orderable.put("fulfillmentLagTime", p.handlingDays());
            }
            Map<String, Object> visible = new LinkedHashMap<>();
            p.attributes().forEach((k, v) -> {
                if (v != null && !v.isBlank() && !"ShippingWeight".equals(k)) {
                    visible.put(k, PublishSupport.isJson(v) ? PublishSupport.json(v) : v.trim());
                }
            });
            visible.put("shortDescription", p.description() == null || p.description().isBlank() ? p.title() : p.description());
            if (!p.bullets().isEmpty()) {
                visible.put("keyFeatures", p.bullets());
            }
            if (!p.images().isEmpty()) {
                visible.put("mainImageUrl", p.images().get(0));
                if (p.images().size() > 1) {
                    visible.put("productSecondaryImageURL", p.images().subList(1, p.images().size()));
                }
            }
            if (p.multiVariant()) {
                visible.put("variantGroupId", p.parentSku());
                visible.put("variantAttributeNames", new ArrayList<>(s.variantAttrs().keySet()));
                visible.putAll(s.variantAttrs());
            }
            items.add(Map.of("Orderable", orderable, "Visible", Map.of(p.categoryId(), visible)));
        }
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("version", SPEC_VERSION);
        header.put("processMode", "REPLACE");
        header.put("subset", "EXTERNAL");
        header.put("locale", "en");
        header.put("sellingChannel", "marketplace");
        JsonNode resp = HttpJson.postJson(base(ctx) + "/v3/feeds?feedType=MP_ITEM", headers(ctx), Map.of("MPItemFeedHeader", header, "MPItem", items), PLATFORM);
        String feedId = HttpJson.text(resp, "feedId");
        if (feedId == null) {
            return PublishResult.failed(HttpJson.abbreviate(resp.toString()));
        }
        return PublishResult.pending(feedId, "feedId " + feedId);
    }

    @Override
    public PublishResult checkPublish(ConnectorContext ctx, PublishPayload p, String ref) {
        JsonNode resp = HttpJson.get(base(ctx) + "/v3/feeds/" + ConnectorSigns.enc(ref) + "?includeDetails=true&limit=50", headers(ctx), PLATFORM);
        return feedResult(resp, ref);
    }

    /** feed 状态：处理完成时按每个 SKU 的结果汇总 */
    static PublishResult feedResult(JsonNode resp, String ref) {
        String status = String.valueOf(HttpJson.text(resp, "feedStatus"));
        if ("ERROR".equals(status)) {
            return PublishResult.failed(HttpJson.abbreviate(resp.path("ingestionErrors").toString()));
        }
        if (!"PROCESSED".equals(status)) {
            return PublishResult.pending(ref, status);
        }
        Map<String, String> skuIds = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (JsonNode i : resp.path("itemDetails").path("itemIngestionStatus")) {
            String sku = HttpJson.text(i, "sku");
            if ("SUCCESS".equals(HttpJson.text(i, "ingestionStatus"))) {
                skuIds.put(sku, HttpJson.text(i, "wpid"));
            } else {
                List<String> msgs = new ArrayList<>();
                for (JsonNode e : i.path("ingestionErrors").path("ingestionError")) {
                    msgs.add(HttpJson.text(e, "code") + " " + HttpJson.text(e, "description"));
                }
                errors.add(sku + ": " + (msgs.isEmpty() ? HttpJson.text(i, "ingestionStatus") : String.join(", ", msgs)));
            }
        }
        if (!errors.isEmpty()) {
            return PublishResult.failed(PublishSupport.cut(String.join("; ", errors), 1500));
        }
        String first = skuIds.values().stream().filter(v -> v != null).findFirst().orElse(null);
        return PublishResult.success(skuIds.size() == 1 ? first : null, first == null ? null : "https://www.walmart.com/ip/" + first, skuIds, Map.of(), null);
    }

    @Override
    public void offline(ConnectorContext ctx, PublishPayload payload, List<ListingRef> refs) {
        for (ListingRef r : refs) {
            HttpJson.request("DELETE", base(ctx) + "/v3/items/" + ConnectorSigns.enc(r.sellerSku()).replace("+", "%20"), headers(ctx), null, PLATFORM);
        }
    }

    @Override
    public List<PriceResult> updatePrices(ConnectorContext ctx, List<PriceUpdate> updates) {
        List<PriceResult> out = new ArrayList<>();
        for (PriceUpdate u : updates) {
            try {
                Map<String, Object> body = Map.of("sku", u.sellerSku(), "pricing", List.of(Map.of("currentPriceType", "BASE",
                        "currentPrice", Map.of("currency", u.currency() == null ? "USD" : u.currency(), "amount", u.price()))));
                JsonNode resp = HttpJson.putJson(base(ctx) + "/v3/price", headers(ctx), body, PLATFORM);
                JsonNode errors = resp.path("errors");
                out.add(errors.isArray() && !errors.isEmpty() ? PriceResult.fail(u.sellerSku(), HttpJson.abbreviate(errors.toString())) : PriceResult.ok(u.sellerSku()));
            } catch (BizException e) {
                out.add(PriceResult.fail(u.sellerSku(), e.getMessage()));
            }
        }
        return out;
    }

    private static String base(ConnectorContext ctx) {
        return PublishSupport.baseUrl(ctx, PRODUCTION, SANDBOX);
    }

    private static Map<String, String> headers(ConnectorContext ctx) {
        return Map.of("WM_SEC.ACCESS_TOKEN", ctx.credentials().require("accessToken"), "WM_QOS.CORRELATION_ID", UUID.randomUUID().toString(),
                "WM_SVC.NAME", SVC_NAME, "Accept", "application/json");
    }
}
