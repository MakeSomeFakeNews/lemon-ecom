package com.lemon.erp.modules.ecom.oper.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorSigns;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.connector.StoreCredentials;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.oper.entity.EcomPromotion;
import com.lemon.erp.modules.ecom.oper.entity.EcomPromotionItem;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * TikTok Shop Partner API 的促销（Promotion 202309）：
 * <ol>
 *   <li>POST /promotion/202309/activities（title、activity_type = FIXED_PRICE 限时价、begin_time、end_time（Unix 秒）、product_level = VARIATION）→ data.activity_id</li>
 *   <li>PUT /promotion/202309/activities/{activity_id}/products（products[{id = product_id, quantity_limit, quantity_per_user,
 *       skus[{id = sku_id, activity_price_amount}]}]）</li>
 *   <li>取消：POST /promotion/202309/activities/{activity_id}/deactivate</li>
 * </ol>
 * 商品编码与在线商品一致：parentItemId 为 product_id、platformItemId 为 sku_id。
 * 授权复用店铺的渠道授权（appKey、appSecret、shopCipher、accessToken），令牌由调用方先用 {@code TikTokShopConnector.ensureToken} 刷新；
 * 签名：app_secret + path + 排序后的 k+v（不含 sign、access_token）+ body + app_secret，HMAC-SHA256(app_secret) 小写十六进制。
 * <p>
 * 说明：按 Partner API 公开文档实现，尚未用真实店铺联调。需要真实账号联调确认：
 * <ul>
 *   <li>闪购（FLASHSALE）需要报名平台活动，不能通过本接口直接创建，这里统一创建限时价活动；直降折扣（DIRECT_DISCOUNT）暂未使用</li>
 *   <li>activity_price_amount 的格式（字符串两位小数）与币种最小单位</li>
 *   <li>同一 SKU 同时参加多个活动时的冲突提示</li>
 * </ul>
 */
@Component
public class TikTokPromotionConnector implements PromotionConnector {

    static final String PLATFORM = "TikTok Shop";
    static final String API_BASE = "https://open-api.tiktokglobalshop.com";
    static final String ACTIVITIES = "/promotion/202309/activities";

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.TIKTOK;
    }

    @Override
    public String push(ConnectorContext ctx, EcomPromotion p, List<EcomPromotionItem> items) {
        String activityId = p.getPlatformPromoId();
        if (activityId == null || activityId.isBlank()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("title", p.getName().length() > 50 ? p.getName().substring(0, 50) : p.getName());
            body.put("activity_type", "FIXED_PRICE");
            body.put("begin_time", OperConnectorSupport.seconds(p.getStartTime()));
            body.put("end_time", OperConnectorSupport.seconds(p.getEndTime()));
            body.put("product_level", "VARIATION");
            activityId = HttpJson.text(call(ctx, "POST", ACTIVITIES, body), "activity_id");
            if (activityId == null) {
                throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "activity_id missing");
            }
        }
        if (!items.isEmpty()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("activity_id", activityId);
            body.put("products", products(items));
            call(ctx, "PUT", ACTIVITIES + "/" + ConnectorSigns.enc(activityId) + "/products", body);
        }
        return activityId;
    }

    /** 按 product_id 分组的商品与 SKU 活动价 */
    static List<Map<String, Object>> products(List<EcomPromotionItem> items) {
        Map<String, Map<String, Object>> byProduct = new LinkedHashMap<>();
        for (EcomPromotionItem it : items) {
            if (it.getParentItemId() == null || it.getParentItemId().isBlank() || it.getPlatformItemId() == null || it.getPromoPrice() == null) {
                throw new BizException("ecom.error.oper.pushItemInvalid", it.getSellerSku());
            }
            Map<String, Object> product = byProduct.computeIfAbsent(it.getParentItemId(), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", k);
                if (it.getPromoQty() != null && it.getPromoQty() > 0) {
                    m.put("quantity_limit", it.getPromoQty());
                }
                if (it.getLimitPerBuyer() != null && it.getLimitPerBuyer() > 0) {
                    m.put("quantity_per_user", it.getLimitPerBuyer());
                }
                m.put("skus", new ArrayList<Map<String, Object>>());
                return m;
            });
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> skus = (List<Map<String, Object>>) product.get("skus");
            Map<String, Object> sku = new LinkedHashMap<>();
            sku.put("id", it.getPlatformItemId());
            sku.put("activity_price_amount", OperConnectorSupport.money(it.getPromoPrice()));
            skus.add(sku);
        }
        return new ArrayList<>(byProduct.values());
    }

    @Override
    public void cancel(ConnectorContext ctx, EcomPromotion p) {
        if (p.getPlatformPromoId() == null || p.getPlatformPromoId().isBlank()) {
            return;
        }
        call(ctx, "POST", ACTIVITIES + "/" + ConnectorSigns.enc(p.getPlatformPromoId()) + "/deactivate", Map.of());
    }

    // ---------------------------------------------------------------- HTTP

    static String sign(String appSecret, String path, Map<String, String> query, String body) {
        TreeMap<String, String> sorted = new TreeMap<>(query);
        sorted.remove("sign");
        sorted.remove("access_token");
        return ConnectorSigns.hmacSha256Hex(appSecret, appSecret + path + ConnectorSigns.concatSorted(sorted) + (body == null ? "" : body) + appSecret);
    }

    private static JsonNode call(ConnectorContext ctx, String method, String path, Object body) {
        StoreCredentials c = ctx.credentials();
        Map<String, String> q = new LinkedHashMap<>();
        q.put("app_key", c.require("appKey"));
        q.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));
        q.put("shop_cipher", c.require("shopCipher"));
        String json = HttpJson.MAPPER.writeValueAsString(body);
        q.put("sign", sign(c.require("appSecret"), path, q, json));
        String url = OperConnectorSupport.baseUrl(ctx, API_BASE, API_BASE) + path + "?" + ConnectorSigns.query(q);
        Map<String, String> headers = Map.of("x-tts-access-token", c.require("accessToken"), "Content-Type", "application/json");
        JsonNode resp = HttpJson.requestRaw(method, url, headers, json, "application/json", PLATFORM);
        if (resp.path("code").asLong(-1) != 0) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM,
                    HttpJson.abbreviate(HttpJson.text(resp, "code") + ": " + HttpJson.text(resp, "message")));
        }
        return resp.path("data");
    }
}
