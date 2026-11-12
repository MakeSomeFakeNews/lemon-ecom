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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 虾皮 Shopee Open Platform v2 的折扣活动（discount）：
 * <ol>
 *   <li>POST /api/v2/discount/add_discount（discount_name、start_time、end_time，Unix 秒）→ response.discount_id</li>
 *   <li>POST /api/v2/discount/add_discount_item（discount_id、item_list[{item_id, purchase_limit, item_promotion_price 或 model_list[{model_id, model_promotion_price}]}]），
 *       response.error_list 中的商品添加失败，全部失败时报错，部分失败时报错并列出失败的商品</li>
 *   <li>取消：未开始的 POST /api/v2/discount/delete_discount，进行中的 POST /api/v2/discount/end_discount（discount_id）</li>
 * </ol>
 * 商品编码与在线商品一致：无规格的 platformItemId 为 item_id；有规格的 parentItemId 为 item_id、platformItemId 为 model_id（兼容旧编码 "itemId:modelId"）。
 * 授权复用店铺的渠道授权（partnerId、partnerKey、shopId、accessToken），令牌由调用方先用 {@code ShopeeConnector.ensureToken} 刷新；
 * 签名 HMAC-SHA256(partner_key, partner_id + path + timestamp + access_token + shop_id)。
 * <p>
 * 说明：按开放平台公开文档实现，尚未用真实店铺联调。需要真实账号联调确认：
 * <ul>
 *   <li>开始时间需晚于当前时间至少 1 小时、活动时长不超过 180 天（平台校验，失败时显示平台返回的原因）</li>
 *   <li>同一商品同时只能参加一个折扣活动，冲突时 error_list 的提示</li>
 *   <li>促销价按店铺币种的最小单位（如印尼盾无小数）是否需要取整</li>
 * </ul>
 */
@Component
public class ShopeeDiscountConnector implements PromotionConnector {

    static final String PLATFORM = "Shopee";
    static final String PRODUCTION = "https://partner.shopeemobile.com";
    static final String SANDBOX = "https://partner.test-stable.shopeemobile.com";
    static final String ADD_DISCOUNT = "/api/v2/discount/add_discount";
    static final String ADD_ITEMS = "/api/v2/discount/add_discount_item";
    static final String DELETE_DISCOUNT = "/api/v2/discount/delete_discount";
    static final String END_DISCOUNT = "/api/v2/discount/end_discount";

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.SHOPEE;
    }

    @Override
    public String push(ConnectorContext ctx, EcomPromotion p, List<EcomPromotionItem> items) {
        String base = OperConnectorSupport.baseUrl(ctx, PRODUCTION, SANDBOX);
        StoreCredentials c = ctx.credentials();
        String discountId = p.getPlatformPromoId();
        if (discountId == null || discountId.isBlank()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("discount_name", p.getName().length() > 50 ? p.getName().substring(0, 50) : p.getName());
            body.put("start_time", OperConnectorSupport.seconds(p.getStartTime()));
            body.put("end_time", OperConnectorSupport.seconds(p.getEndTime()));
            discountId = HttpJson.text(post(base, c, ADD_DISCOUNT, body), "discount_id");
            if (discountId == null) {
                throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "discount_id missing");
            }
        }
        if (!items.isEmpty()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("discount_id", OperConnectorSupport.parseLong(discountId));
            body.put("item_list", itemList(items));
            JsonNode resp = post(base, c, ADD_ITEMS, body);
            List<String> errors = new ArrayList<>();
            for (JsonNode e : resp.path("error_list")) {
                errors.add(HttpJson.text(e, "item_id") + " " + HttpJson.text(e, "fail_message"));
            }
            if (!errors.isEmpty()) {
                throw new BizException("ecom.error.oper.pushPartial", PLATFORM, discountId, HttpJson.abbreviate(String.join("; ", errors)));
            }
        }
        return discountId;
    }

    /** 按 item_id 分组：有规格的放 model_list，无规格的用 item_promotion_price */
    static List<Map<String, Object>> itemList(List<EcomPromotionItem> items) {
        Map<Long, Map<String, Object>> byItem = new LinkedHashMap<>();
        for (EcomPromotionItem it : items) {
            long[] ids = ids(it);
            if (ids[0] == 0 || it.getPromoPrice() == null) {
                throw new BizException("ecom.error.oper.pushItemInvalid", it.getSellerSku());
            }
            Map<String, Object> entry = byItem.computeIfAbsent(ids[0], k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("item_id", k);
                if (it.getLimitPerBuyer() != null && it.getLimitPerBuyer() > 0) {
                    m.put("purchase_limit", it.getLimitPerBuyer());
                }
                return m;
            });
            if (ids[1] == 0) {
                entry.put("item_promotion_price", it.getPromoPrice());
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> models = (List<Map<String, Object>>) entry.computeIfAbsent("model_list", k -> new ArrayList<Map<String, Object>>());
                Map<String, Object> model = new LinkedHashMap<>();
                model.put("model_id", ids[1]);
                model.put("model_promotion_price", it.getPromoPrice());
                models.add(model);
            }
        }
        return new ArrayList<>(byItem.values());
    }

    /** [item_id, model_id]（无规格 model_id = 0） */
    static long[] ids(EcomPromotionItem it) {
        String pid = it.getPlatformItemId();
        if (it.getParentItemId() != null && !it.getParentItemId().isBlank()) {
            return new long[]{OperConnectorSupport.parseLong(it.getParentItemId()), pid == null ? 0 : OperConnectorSupport.parseLong(pid)};
        }
        if (pid == null || pid.isBlank()) {
            return new long[]{0, 0};
        }
        int i = pid.indexOf(':');
        return i < 0 ? new long[]{OperConnectorSupport.parseLong(pid), 0}
                : new long[]{OperConnectorSupport.parseLong(pid.substring(0, i)), OperConnectorSupport.parseLong(pid.substring(i + 1))};
    }

    @Override
    public void cancel(ConnectorContext ctx, EcomPromotion p) {
        if (p.getPlatformPromoId() == null || p.getPlatformPromoId().isBlank()) {
            return;
        }
        String base = OperConnectorSupport.baseUrl(ctx, PRODUCTION, SANDBOX);
        String path = p.getStartTime().isAfter(LocalDateTime.now()) ? DELETE_DISCOUNT : END_DISCOUNT;
        post(base, ctx.credentials(), path, Map.of("discount_id", OperConnectorSupport.parseLong(p.getPlatformPromoId())));
    }

    // ---------------------------------------------------------------- HTTP

    static String sign(String partnerKey, String partnerId, String path, long timestamp, String accessToken, String shopId) {
        return ConnectorSigns.hmacSha256Hex(partnerKey, partnerId + path + timestamp + accessToken + shopId);
    }

    private static JsonNode post(String base, StoreCredentials c, String path, Object body) {
        String partnerId = c.require("partnerId");
        String token = c.require("accessToken");
        String shopId = c.require("shopId");
        long ts = Instant.now().getEpochSecond();
        Map<String, String> q = new LinkedHashMap<>();
        q.put("partner_id", partnerId);
        q.put("timestamp", String.valueOf(ts));
        q.put("access_token", token);
        q.put("shop_id", shopId);
        q.put("sign", sign(c.require("partnerKey"), partnerId, path, ts, token, shopId));
        JsonNode resp = HttpJson.postJson(base + path + "?" + ConnectorSigns.query(q), Map.of(), body, PLATFORM);
        String error = HttpJson.text(resp, "error");
        if (error != null && !error.isBlank()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(error + ": " + HttpJson.text(resp, "message")));
        }
        return resp.path("response");
    }
}
