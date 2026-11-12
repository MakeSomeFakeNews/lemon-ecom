package com.lemon.erp.modules.ecom.cs.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorJson;
import com.lemon.erp.modules.ecom.connector.ConnectorSigns;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.connector.StoreCredentials;
import com.lemon.erp.modules.ecom.cs.model.CsDirection;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * TikTok Shop 客服消息（Customer Service API 202309，https://partner.tiktokshop.com/docv2）。
 * <p>
 * 授权复用店铺的渠道授权（{@code TikTokShopConnector}：appKey、appSecret、accessToken、shopCipher），令牌由调用方先用
 * {@code TikTokShopConnector.ensureToken} 刷新。签名与订单接口相同：查询参数去掉 sign、access_token 后按键排序拼 k+v，
 * 原文 = app_secret + path + 拼接串 + 请求体 + app_secret，HMAC-SHA256(app_secret) 小写十六进制；公共参数 app_key、timestamp、shop_cipher、sign，
 * 令牌放请求头 x-tts-access-token。请求体只序列化一次，签名与发送用同一个字符串。地址 https://open-api.tiktokglobalshop.com（没有独立沙箱，
 * 用 Partner Center 的测试店铺），店铺填写了自定义地址时指向它。
 * <ul>
 *   <li>会话：GET /customer_service/202309/conversations（page_size、page_token），按最新消息倒序，latest_message.create_time 早于 since 时停止；
 *       participants 中 role = BUYER 的为买家（nickname、im_user_id）</li>
 *   <li>消息：GET /customer_service/202309/conversations/{id}/messages（page_size、page_token、sort_field = create_time、sort_order = DESC），
 *       早于 since 时停止；sender.role 为 BUYER 的是买家消息，SHOP / CUSTOMER_SERVICE 为卖家，SYSTEM / ROBOT 等系统消息跳过；
 *       content 是 JSON 字符串：TEXT 取 content，IMAGE 取 url，ORDER_CARD 的 order_id 作为关联订单</li>
 *   <li>回复：POST /customer_service/202309/conversations/{id}/messages（type = TEXT，content = {"content": 文字} 的 JSON 字符串）</li>
 *   <li>已读：POST /customer_service/202309/conversations/{id}/messages/read</li>
 * </ul>
 * 评价：TikTok Shop 公开文档中没有查询商品评价的接口，只支持 Excel 导入。回复时限取 24 小时。
 * <p>
 * 说明：按 Partner Center 公开文档实现，尚未用真实店铺联调。需要真实账号联调确认：
 * <ul>
 *   <li>应用需要申请 Customer Service 权限范围（seller.customer_service），未授权时返回权限错误</li>
 *   <li>会话列表是否按最新消息倒序、page_size 上限（代码用 20）、latest_message.create_time 的精度（自动识别秒 / 毫秒）</li>
 *   <li>消息 sort_order / sort_field 参数名，以及 sender.role 的取值（BUYER、SHOP、CUSTOMER_SERVICE、SYSTEM、ROBOT）</li>
 *   <li>发送消息的 content 是否要求为 JSON 字符串（代码按字符串发送），以及买家 48 小时未发消息时 can_send_message = false 的限制</li>
 *   <li>店铺回复时限：TikTok Shop 的客服考核口径（12 / 24 小时）按站点不同</li>
 * </ul>
 */
@Component
public class TikTokCsConnector implements MessageConnector {

    static final String PLATFORM = "TikTok Shop";
    static final String API_BASE = "https://open-api.tiktokglobalshop.com";
    static final String CONVERSATIONS = "/customer_service/202309/conversations";
    static final int PAGE_SIZE = 20;
    static final int REPLY_HOURS = 24;

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.TIKTOK;
    }

    @Override
    public Set<CsCapability> capabilities() {
        return EnumSet.of(CsCapability.MESSAGES, CsCapability.SEND_MESSAGE);
    }

    @Override
    public int replyHours() {
        return REPLY_HOURS;
    }

    /**
     * 签名：app_secret + path + 排序后的 k+v（不含 sign、access_token）+ body + app_secret，HMAC-SHA256(app_secret) 小写十六进制
     */
    static String sign(String appSecret, String path, Map<String, String> query, String body) {
        TreeMap<String, String> sorted = new TreeMap<>(query);
        sorted.remove("sign");
        sorted.remove("access_token");
        return ConnectorSigns.hmacSha256Hex(appSecret, appSecret + path + ConnectorSigns.concatSorted(sorted) + (body == null ? "" : body) + appSecret);
    }

    // ---------------------------------------------------------------- 消息

    @Override
    public List<ThreadDraft> fetchThreads(ConnectorContext ctx, LocalDateTime since) {
        List<ThreadDraft> threads = new ArrayList<>();
        String token = null;
        boolean reachedOld = false;
        for (int page = 0; page < CsConnectorSupport.MAX_PAGES && !reachedOld; page++) {
            Map<String, String> q = new LinkedHashMap<>();
            q.put("page_size", String.valueOf(PAGE_SIZE));
            if (token != null) {
                q.put("page_token", token);
            }
            JsonNode data = call(ctx, "GET", CONVERSATIONS, q, null);
            for (JsonNode conv : data.path("conversations")) {
                JsonNode latest = conv.path("latest_message");
                LocalDateTime last = CsConnectorSupport.epoch(latest.path("create_time"));
                if (since != null && last != null && last.isBefore(since)) {
                    reachedOld = true;
                    break;
                }
                String id = HttpJson.text(conv, "id");
                JsonNode buyer = null;
                for (JsonNode p : conv.path("participants")) {
                    if ("BUYER".equals(HttpJson.text(p, "role"))) {
                        buyer = p;
                    }
                }
                List<MessageDraft> messages = fetchMessages(ctx, id, since);
                String orderNo = messages.stream().map(MessageDraft::platformOrderNo).filter(o -> o != null && !o.isBlank())
                        .reduce((a, b) -> b).orElse(null);
                threads.add(new ThreadDraft(id, buyer == null ? null : HttpJson.text(buyer, "im_user_id"),
                        buyer == null ? null : HttpJson.text(buyer, "nickname"), null,
                        conv.path("unread_count").isMissingNode() ? null : conv.path("unread_count").asInt(0), last, orderNo, messages));
            }
            token = HttpJson.text(data, "next_page_token");
            if (token == null || token.isBlank()) {
                break;
            }
        }
        return threads;
    }

    private List<MessageDraft> fetchMessages(ConnectorContext ctx, String conversationId, LocalDateTime since) {
        List<MessageDraft> out = new ArrayList<>();
        String token = null;
        for (int page = 0; page < CsConnectorSupport.MAX_PAGES; page++) {
            Map<String, String> q = new LinkedHashMap<>();
            q.put("page_size", String.valueOf(PAGE_SIZE));
            q.put("sort_field", "create_time");
            q.put("sort_order", "DESC");
            if (token != null) {
                q.put("page_token", token);
            }
            JsonNode data = call(ctx, "GET", CONVERSATIONS + "/" + ConnectorSigns.enc(conversationId) + "/messages", q, null);
            boolean old = false;
            for (JsonNode m : data.path("messages")) {
                MessageDraft d = toMessage(m);
                if (d == null) {
                    continue;
                }
                if (since != null && d.sentTime() != null && d.sentTime().isBefore(since)) {
                    old = true;
                    continue;
                }
                out.add(d);
            }
            token = HttpJson.text(data, "next_page_token");
            if (old || token == null || token.isBlank()) {
                break;
            }
        }
        return out;
    }

    /** 消息转换，系统 / 机器人消息返回 null */
    static MessageDraft toMessage(JsonNode m) {
        String role = HttpJson.text(m.path("sender"), "role");
        CsDirection direction;
        if ("BUYER".equals(role)) {
            direction = CsDirection.BUYER;
        } else if ("SHOP".equals(role) || "CUSTOMER_SERVICE".equals(role)) {
            direction = CsDirection.SELLER;
        } else {
            return null;
        }
        String type = HttpJson.text(m, "type");
        JsonNode content = parseContent(HttpJson.text(m, "content"));
        List<String> attachments = new ArrayList<>();
        String url = HttpJson.text(content, "url");
        if (url != null) {
            attachments.add(url);
        }
        String orderId = HttpJson.text(content, "order_id");
        String text = ConnectorJson.firstNonBlank(HttpJson.text(content, "content"), orderId);
        return new MessageDraft(HttpJson.text(m, "id"), direction, type == null ? "TEXT" : type, text, attachments,
                CsConnectorSupport.epoch(m.path("create_time")), orderId);
    }

    /** content 是 JSON 字符串；不是 JSON 时当作文字 */
    private static JsonNode parseContent(String raw) {
        if (raw == null || raw.isBlank()) {
            return HttpJson.MAPPER.createObjectNode();
        }
        try {
            JsonNode n = HttpJson.MAPPER.readTree(raw);
            if (n.isObject()) {
                return n;
            }
        } catch (RuntimeException ignored) {
            // 不是 JSON，按文字处理
        }
        return HttpJson.MAPPER.createObjectNode().put("content", raw);
    }

    @Override
    public SentMessage sendMessage(ConnectorContext ctx, ThreadRef thread, String content) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "TEXT");
        body.put("content", HttpJson.MAPPER.writeValueAsString(Map.of("content", content)));
        JsonNode data = call(ctx, "POST", CONVERSATIONS + "/" + ConnectorSigns.enc(thread.conversationId()) + "/messages", new LinkedHashMap<>(), body);
        String id = HttpJson.text(data, "message_id");
        if (id == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "message_id missing");
        }
        return new SentMessage(id, null);
    }

    @Override
    public void markRead(ConnectorContext ctx, ThreadRef thread) {
        call(ctx, "POST", CONVERSATIONS + "/" + ConnectorSigns.enc(thread.conversationId()) + "/messages/read", new LinkedHashMap<>(), Map.of());
    }

    // ---------------------------------------------------------------- HTTP

    private static JsonNode call(ConnectorContext ctx, String method, String path, Map<String, String> query, Object body) {
        StoreCredentials c = ctx.credentials();
        Map<String, String> q = new LinkedHashMap<>(query);
        q.put("app_key", c.require("appKey"));
        q.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));
        q.put("shop_cipher", c.require("shopCipher"));
        String json = body == null ? null : HttpJson.MAPPER.writeValueAsString(body);
        q.put("sign", sign(c.require("appSecret"), path, q, json));
        String url = CsConnectorSupport.baseUrl(ctx, API_BASE, API_BASE) + path + "?" + ConnectorSigns.query(q);
        Map<String, String> headers = Map.of("x-tts-access-token", c.require("accessToken"), "Content-Type", "application/json");
        JsonNode resp = json == null ? HttpJson.get(url, headers, PLATFORM) : HttpJson.requestRaw(method, url, headers, json, "application/json", PLATFORM);
        if (resp.path("code").asLong(-1) != 0) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM,
                    HttpJson.abbreviate(HttpJson.text(resp, "code") + ": " + HttpJson.text(resp, "message")));
        }
        return resp.path("data");
    }
}
