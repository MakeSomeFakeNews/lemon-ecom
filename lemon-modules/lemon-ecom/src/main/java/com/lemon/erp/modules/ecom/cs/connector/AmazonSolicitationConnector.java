package com.lemon.erp.modules.ecom.cs.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorHttpException;
import com.lemon.erp.modules.ecom.connector.ConnectorSigns;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.connector.StoreCredentials;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 亚马逊 Selling Partner API 的 Solicitations API v1：请求买家留评（商品评价 + 卖家反馈，createProductReviewAndSellerFeedbackSolicitation）。
 * 亚马逊的买家与卖家消息（Buyer-Seller Messaging）不开放读取，Messaging API 只能发起有限类型的消息，所以亚马逊店铺不同步会话，
 * 只提供「请求评价」；商品评价、店铺反馈（Feedback Manager）通过 Excel 导入。
 * <p>
 * 授权复用店铺的渠道授权（{@code AmazonSpConnector}：region、marketplaceId、clientId、clientSecret、refreshToken），
 * 访问令牌由调用方先用 {@code AmazonSpConnector.ensureToken} 换取（缓存在授权信息 accessToken 中），请求头 x-amz-access-token。
 * 地址按区域（NA / EU / FE）与沙箱开关选择，店铺填写了自定义地址时指向它。
 * <ol>
 *   <li>GET /solicitations/v1/orders/{amazonOrderId}?marketplaceIds=…（getSolicitationActionsForOrder）：返回当前可用的请求类型，
 *       _links.actions 中没有 productReviewAndSellerFeedback 时不符合条件（不在发货后 5–30 天的窗口内、已请求过、订单已退款等），不再请求</li>
 *   <li>POST /solicitations/v1/orders/{amazonOrderId}/solicitations/productReviewAndSellerFeedback?marketplaceIds=…，成功为 201</li>
 * </ol>
 * 限速每秒 1 次（突发 5），429 由 {@link HttpJson} 自动退避重试；批量请求按订单逐个调用。
 * <p>
 * 说明：按亚马逊公开文档实现，尚未用真实卖家账号联调。需要真实账号联调确认：
 * <ul>
 *   <li>应用需要 Solicitations 角色（Buyer Solicitation），未授权时返回 403 Unauthorized</li>
 *   <li>getSolicitationActionsForOrder 在不符合条件时是返回空的 actions 还是 4xx 错误（两种都按「不符合条件」处理：4xx 中 400 / 403 / 404 记为不符合条件）</li>
 *   <li>同一订单重复请求时平台的返回</li>
 * </ul>
 */
@Component
public class AmazonSolicitationConnector implements MessageConnector {

    static final String PLATFORM = "Amazon";
    static final String ACTION = "productReviewAndSellerFeedback";
    private static final Map<String, String> ENDPOINTS = Map.of(
            "NA", "https://sellingpartnerapi-na.amazon.com",
            "EU", "https://sellingpartnerapi-eu.amazon.com",
            "FE", "https://sellingpartnerapi-fe.amazon.com");
    private static final Map<String, String> SANDBOX_ENDPOINTS = Map.of(
            "NA", "https://sandbox.sellingpartnerapi-na.amazon.com",
            "EU", "https://sandbox.sellingpartnerapi-eu.amazon.com",
            "FE", "https://sandbox.sellingpartnerapi-fe.amazon.com");

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.AMAZON;
    }

    @Override
    public Set<CsCapability> capabilities() {
        return EnumSet.of(CsCapability.REVIEW_REQUEST);
    }

    @Override
    public SolicitationResult requestReview(ConnectorContext ctx, String platformOrderNo) {
        StoreCredentials c = ctx.credentials();
        String base = endpoint(ctx) + "/solicitations/v1/orders/" + path(platformOrderNo);
        String marketplace = "?marketplaceIds=" + ConnectorSigns.enc(c.require("marketplaceId"));
        Map<String, String> headers = Map.of("x-amz-access-token", c.require("accessToken"), "Accept", "application/json");
        JsonNode actions;
        try {
            actions = HttpJson.get(base + marketplace, headers, PLATFORM);
        } catch (ConnectorHttpException e) {
            if (notEligible(e)) {
                return SolicitationResult.notEligible(HttpJson.abbreviate(e.getMessage()));
            }
            throw e;
        }
        if (!hasAction(actions)) {
            return SolicitationResult.notEligible(ACTION + " not available");
        }
        try {
            HttpJson.request("POST", base + "/solicitations/" + ACTION + marketplace, headers, null, PLATFORM);
        } catch (ConnectorHttpException e) {
            if (notEligible(e)) {
                return SolicitationResult.notEligible(HttpJson.abbreviate(e.getMessage()));
            }
            throw e;
        }
        return SolicitationResult.sent();
    }

    /** _links.actions（或 _embedded.actions）中有 productReviewAndSellerFeedback */
    static boolean hasAction(JsonNode resp) {
        for (String container : new String[]{"_links", "_embedded"}) {
            for (JsonNode a : resp.path(container).path("actions")) {
                String name = HttpJson.text(a, "name");
                String href = HttpJson.text(a, "href");
                if (ACTION.equals(name) || (href != null && href.endsWith("/" + ACTION))
                        || ACTION.equals(HttpJson.text(a.path("_links").path("self"), "name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean notEligible(ConnectorHttpException e) {
        int s = e.statusCode();
        return s == 400 || s == 403 || s == 404;
    }

    private static String endpoint(ConnectorContext ctx) {
        if (ctx.baseUrlOverride() != null) {
            return ctx.baseUrlOverride();
        }
        String region = ctx.credentials().get("region") == null ? "NA" : ctx.credentials().get("region").toUpperCase();
        String endpoint = (ctx.sandbox() ? SANDBOX_ENDPOINTS : ENDPOINTS).get(region);
        if (endpoint == null) {
            throw new BizException("ecom.error.connectorRegionInvalid", region);
        }
        return endpoint;
    }

    private static String path(String s) {
        return ConnectorSigns.enc(s).replace("+", "%20");
    }
}
