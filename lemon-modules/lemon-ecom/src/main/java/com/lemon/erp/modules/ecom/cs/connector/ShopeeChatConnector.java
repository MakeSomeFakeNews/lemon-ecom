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

/**
 * 虾皮 Shopee Open Platform v2 的买家聊天（sellerchat）与商品评价（product.get_comment / reply_comment）。
 * <p>
 * 授权复用店铺的渠道授权（{@code ShopeeConnector}：partnerId、partnerKey、shopId、accessToken），令牌由调用方先用
 * {@code ShopeeConnector.ensureToken} 刷新。签名与订单接口相同：HMAC-SHA256(partner_key, partner_id + path + timestamp + access_token + shop_id)
 * 小写十六进制，公共参数 partner_id、timestamp、access_token、shop_id、sign 放在查询串；地址正式 https://partner.shopeemobile.com、
 * 沙箱 https://partner.test-stable.shopeemobile.com，店铺填写了自定义地址时指向它。
 * <ul>
 *   <li>会话：GET /api/v2/sellerchat/get_conversation_list（direction = latest 起、之后 older + next_timestamp_nano 翻页，type = all，
 *       page_size 最大 60），按 last_message_timestamp（纳秒）倒序，早于 since 时停止；会话的 to_id / to_name 为买家</li>
 *   <li>消息：GET /api/v2/sellerchat/get_message（conversation_id，offset = page_result.next_offset 翻页，从新到旧），早于 since 时停止；
 *       from_shop_id 等于本店 shopId 的为卖家消息；message_type 为 order 时 content.order_sn 作为关联订单</li>
 *   <li>回复：POST /api/v2/sellerchat/send_message（to_id 为买家 ID，message_type = text，content.text）</li>
 *   <li>已读：POST /api/v2/sellerchat/read_conversation（conversation_id、last_read_message_id）</li>
 *   <li>评价：GET /api/v2/product/get_comment（cursor 翻页，page_size 最大 100），create_time 早于 since 且没有变化的跳过；
 *       回复 POST /api/v2/product/reply_comment（comment_list[{comment_id, comment}]，按 result_list 的 fail_error 判断失败）</li>
 * </ul>
 * 回复时限按虾皮「聊天回复率」的口径取 12 小时。
 * <p>
 * 说明：按开放平台公开文档实现，尚未用真实店铺联调。需要真实账号联调确认：
 * <ul>
 *   <li>sellerchat 接口需要在开放平台为应用单独申请权限（部分站点 / 卖家类型不开放），未开通时返回 error_permission 等错误</li>
 *   <li>get_conversation_list 的 last_message_timestamp、next_timestamp_nano 的精度（文档为纳秒，代码按数值大小自动识别秒 / 毫秒 / 微秒 / 纳秒）</li>
 *   <li>get_message 返回的顺序（按新到旧处理）与 next_offset 为空串表示没有更多</li>
 *   <li>conversation_id、to_id 在请求体中要求为数字（代码按数字发送）</li>
 *   <li>get_comment 不带 item_id 时是否返回全店评价、是否按时间倒序（代码不依赖顺序，按 create_time 过滤）</li>
 * </ul>
 */
@Component
public class ShopeeChatConnector implements MessageConnector {

    static final String PLATFORM = "Shopee";
    static final String PRODUCTION = "https://partner.shopeemobile.com";
    static final String SANDBOX = "https://partner.test-stable.shopeemobile.com";
    static final String CONVERSATIONS = "/api/v2/sellerchat/get_conversation_list";
    static final String MESSAGES = "/api/v2/sellerchat/get_message";
    static final String SEND = "/api/v2/sellerchat/send_message";
    static final String READ = "/api/v2/sellerchat/read_conversation";
    static final String COMMENTS = "/api/v2/product/get_comment";
    static final String REPLY_COMMENT = "/api/v2/product/reply_comment";
    static final int CONVERSATION_PAGE = 60;
    static final int MESSAGE_PAGE = 60;
    static final int COMMENT_PAGE = 100;
    /** 回复时限（小时） */
    static final int REPLY_HOURS = 12;

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.SHOPEE;
    }

    @Override
    public Set<CsCapability> capabilities() {
        return EnumSet.of(CsCapability.MESSAGES, CsCapability.SEND_MESSAGE, CsCapability.REVIEWS, CsCapability.REVIEW_REPLY);
    }

    @Override
    public int replyHours() {
        return REPLY_HOURS;
    }

    // ---------------------------------------------------------------- 签名

    /** 店铺接口签名：HMAC-SHA256(partner_key, partner_id + path + timestamp + access_token + shop_id)，小写十六进制 */
    static String sign(String partnerKey, String partnerId, String path, long timestamp, String accessToken, String shopId) {
        return ConnectorSigns.hmacSha256Hex(partnerKey, partnerId + path + timestamp + accessToken + shopId);
    }

    private static String commonQuery(StoreCredentials c, String path) {
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
        return ConnectorSigns.query(q);
    }

    // ---------------------------------------------------------------- 消息

    @Override
    public List<ThreadDraft> fetchThreads(ConnectorContext ctx, LocalDateTime since) {
        String base = CsConnectorSupport.baseUrl(ctx, PRODUCTION, SANDBOX);
        StoreCredentials c = ctx.credentials();
        String shopId = c.require("shopId");
        List<ThreadDraft> threads = new ArrayList<>();
        String nextNano = null;
        boolean reachedOld = false;
        for (int page = 0; page < CsConnectorSupport.MAX_PAGES && !reachedOld; page++) {
            Map<String, String> q = new LinkedHashMap<>();
            q.put("direction", nextNano == null ? "latest" : "older");
            q.put("type", "all");
            q.put("page_size", String.valueOf(CONVERSATION_PAGE));
            if (nextNano != null) {
                q.put("next_timestamp_nano", nextNano);
            }
            JsonNode resp = get(base, c, CONVERSATIONS, ConnectorSigns.query(q));
            for (JsonNode conv : resp.path("conversations")) {
                LocalDateTime last = CsConnectorSupport.epoch(conv.path("last_message_timestamp"));
                if (since != null && last != null && last.isBefore(since)) {
                    reachedOld = true;
                    break;
                }
                String id = HttpJson.text(conv, "conversation_id");
                List<MessageDraft> messages = fetchMessages(base, c, id, shopId, since);
                String orderNo = messages.stream().map(MessageDraft::platformOrderNo).filter(o -> o != null && !o.isBlank())
                        .reduce((a, b) -> b).orElse(null);
                threads.add(new ThreadDraft(id, HttpJson.text(conv, "to_id"), HttpJson.text(conv, "to_name"), null,
                        conv.path("unread_count").isMissingNode() ? null : conv.path("unread_count").asInt(0), last, orderNo, messages));
            }
            JsonNode pr = resp.path("page_result");
            nextNano = ConnectorJson.str(pr, "next_cursor", "next_message_time_nano");
            if (!pr.path("more").asBoolean(false) || nextNano == null || nextNano.isBlank() || "0".equals(nextNano)) {
                break;
            }
        }
        return threads;
    }

    /** 一个会话 since 之后的消息（从新到旧翻页） */
    private List<MessageDraft> fetchMessages(String base, StoreCredentials c, String conversationId, String shopId, LocalDateTime since) {
        List<MessageDraft> out = new ArrayList<>();
        String offset = null;
        for (int page = 0; page < CsConnectorSupport.MAX_PAGES; page++) {
            Map<String, String> q = new LinkedHashMap<>();
            q.put("conversation_id", conversationId);
            q.put("page_size", String.valueOf(MESSAGE_PAGE));
            if (offset != null) {
                q.put("offset", offset);
            }
            JsonNode resp = get(base, c, MESSAGES, ConnectorSigns.query(q));
            boolean old = false;
            for (JsonNode m : resp.path("messages")) {
                MessageDraft d = toMessage(m, shopId);
                if (since != null && d.sentTime() != null && d.sentTime().isBefore(since)) {
                    old = true;
                    continue;
                }
                out.add(d);
            }
            offset = ConnectorJson.str(resp, "page_result", "next_offset");
            if (old || offset == null || offset.isBlank() || "0".equals(offset)) {
                break;
            }
        }
        return out;
    }

    static MessageDraft toMessage(JsonNode m, String shopId) {
        String type = HttpJson.text(m, "message_type");
        JsonNode content = m.path("content");
        boolean seller = shopId != null && shopId.equals(HttpJson.text(m, "from_shop_id"));
        List<String> attachments = new ArrayList<>();
        String url = ConnectorJson.firstNonBlank(HttpJson.text(content, "url"), HttpJson.text(content, "thumb_url"));
        if (url != null) {
            attachments.add(url);
        }
        String text = HttpJson.text(content, "text");
        String orderSn = HttpJson.text(content, "order_sn");
        if (text == null && orderSn != null) {
            text = orderSn;
        }
        return new MessageDraft(HttpJson.text(m, "message_id"), seller ? CsDirection.SELLER : CsDirection.BUYER,
                type == null ? "text" : type, text, attachments, CsConnectorSupport.epoch(m.path("created_timestamp")), orderSn);
    }

    @Override
    public SentMessage sendMessage(ConnectorContext ctx, ThreadRef thread, String content) {
        if (thread.buyerId() == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "to_id missing");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("to_id", parseLong(thread.buyerId()));
        body.put("message_type", "text");
        body.put("content", Map.of("text", content));
        JsonNode resp = post(CsConnectorSupport.baseUrl(ctx, PRODUCTION, SANDBOX), ctx.credentials(), SEND, body);
        String id = HttpJson.text(resp, "message_id");
        if (id == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "message_id missing");
        }
        return new SentMessage(id, CsConnectorSupport.epoch(resp.path("created_timestamp")));
    }

    @Override
    public void markRead(ConnectorContext ctx, ThreadRef thread) {
        if (thread.lastMessageId() == null) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("conversation_id", parseLong(thread.conversationId()));
        body.put("last_read_message_id", thread.lastMessageId());
        post(CsConnectorSupport.baseUrl(ctx, PRODUCTION, SANDBOX), ctx.credentials(), READ, body);
    }

    // ---------------------------------------------------------------- 评价

    @Override
    public List<ReviewDraft> fetchReviews(ConnectorContext ctx, LocalDateTime since) {
        String base = CsConnectorSupport.baseUrl(ctx, PRODUCTION, SANDBOX);
        List<ReviewDraft> out = new ArrayList<>();
        String cursor = "";
        for (int page = 0; page < CsConnectorSupport.MAX_PAGES; page++) {
            JsonNode resp = get(base, ctx.credentials(), COMMENTS, "cursor=" + ConnectorSigns.enc(cursor) + "&page_size=" + COMMENT_PAGE);
            for (JsonNode cm : resp.path("item_comment_list")) {
                ReviewDraft d = toReview(cm);
                LocalDateTime changed = d.replyTime() != null && (d.reviewTime() == null || d.replyTime().isAfter(d.reviewTime())) ? d.replyTime()
                        : d.reviewTime();
                if (since != null && changed != null && changed.isBefore(since)) {
                    continue;
                }
                out.add(d);
            }
            cursor = HttpJson.text(resp, "next_cursor");
            if (!resp.path("more").asBoolean(false) || cursor == null || cursor.isBlank()) {
                break;
            }
        }
        return out;
    }

    static ReviewDraft toReview(JsonNode cm) {
        String itemId = HttpJson.text(cm, "item_id");
        long modelId = cm.path("model_id").asLong(0);
        JsonNode reply = cm.path("comment_reply");
        String replyText = HttpJson.text(reply, "reply");
        return new ReviewDraft(HttpJson.text(cm, "comment_id"), modelId == 0 ? itemId : String.valueOf(modelId), modelId == 0 ? null : itemId,
                null, cm.path("rating_star").asInt(0), null, HttpJson.text(cm, "comment"), HttpJson.text(cm, "buyer_username"),
                CsConnectorSupport.epoch(cm.path("create_time")), HttpJson.text(cm, "order_sn"),
                replyText == null || replyText.isBlank() ? null : replyText, CsConnectorSupport.epoch(reply.path("create_time")));
    }

    @Override
    public void replyReview(ConnectorContext ctx, String reviewId, String content) {
        Map<String, Object> body = Map.of("comment_list", List.of(Map.of("comment_id", parseLong(reviewId), "comment", content)));
        JsonNode resp = post(CsConnectorSupport.baseUrl(ctx, PRODUCTION, SANDBOX), ctx.credentials(), REPLY_COMMENT, body);
        for (JsonNode r : resp.path("result_list")) {
            String error = HttpJson.text(r, "fail_error");
            if (error != null && !error.isBlank()) {
                throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(error + ": " + HttpJson.text(r, "fail_message")));
            }
        }
    }

    // ---------------------------------------------------------------- HTTP

    private static JsonNode get(String base, StoreCredentials c, String path, String query) {
        String url = base + path + "?" + commonQuery(c, path) + (query == null || query.isEmpty() ? "" : "&" + query);
        return check(HttpJson.get(url, Map.of(), PLATFORM));
    }

    private static JsonNode post(String base, StoreCredentials c, String path, Object body) {
        return check(HttpJson.postJson(base + path + "?" + commonQuery(c, path), Map.of(), body, PLATFORM));
    }

    /** error 不为空时抛出异常，返回 response 节点 */
    private static JsonNode check(JsonNode resp) {
        String error = HttpJson.text(resp, "error");
        if (error != null && !error.isBlank()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(error + ": " + HttpJson.text(resp, "message")));
        }
        return resp.path("response");
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "invalid id: " + s);
        }
    }
}
