package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.dto.OrderDraft;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 虾皮 Shopee Open Platform v2（https://open.shopee.com/documents）。
 * <p>
 * 授权（OAuth）：店铺填写 partnerId、partnerKey（可选 region：SG / MY / TH / PH / VN / ID / TW / BR / MX / CO / CL，只用于生成商品链接），
 * 卖家打开 {@code /api/v2/shop/auth_partner} 授权后回调带 code 与 shop_id，由 {@code /api/v2/auth/token/get} 换取令牌，
 * 写入 shopId、accessToken（约 4 小时有效）、refreshToken、tokenExpireAt（access_token 过期时间，Unix 秒）。
 * 虾皮回调不会原样带回自定义参数，所以把框架的 state 拼进 redirect 地址的查询串。
 * 每次调用前 {@link #ensureToken} 在过期前 10 分钟用 {@code /api/v2/auth/access_token/get} 刷新（refresh_token 也会轮换，一并保存）。
 * <p>
 * 签名：HMAC-SHA256(partner_key, 原文) 小写十六进制；公共接口原文 = partner_id + path + timestamp，
 * 店铺接口原文 = partner_id + path + timestamp + access_token + shop_id，签名与公共参数都放在查询串。
 * <p>
 * 地址：正式 https://partner.shopeemobile.com，沙箱 https://partner.test-stable.shopeemobile.com；授权页、令牌接口与业务接口同一域名，
 * 店铺填写了自定义地址时全部指向它（测试用的模拟服务器）。
 * <p>
 * 编码约定：
 * <ul>
 *   <li>在线商品：无规格的商品 platformItemId 为 itemId（parentItemId 为空）；有规格（model）的 platformItemId 为 modelId、parentItemId 为 itemId。
 *       订单明细没有父 ID 字段，平台商品 ID 仍为 {@code "itemId"} / {@code "itemId:modelId"}；回传库存按 parentItemId + platformItemId，
 *       兼容旧编码 {@code "itemId:modelId"}，无规格按 model_id = 0 更新</li>
 *   <li>订单行 ID：{@code "order_item_id:model_id"}</li>
 *   <li>卖家 SKU：取 model_sku，为空取 item_sku，仍为空取平台商品 ID</li>
 * </ul>
 * 金额：单价取 model_original_price，商品金额 = 原价 × 数量，折扣 = (原价 − 折后价) × 数量；
 * 买家支付的运费只在结算（escrow）接口中，订单同步不写运费。
 * <p>
 * 发货：先查 {@code logistics/get_shipping_parameter}：物流渠道需要非集成物流信息（info_needed 含 non_integrated）时，
 * 用 {@code logistics/ship_order} 回传 ERP 的运单号；平台物流（SLS，info_needed 为 pickup / dropoff）的运单号由平台分配，
 * 本连接器不代为安排揽收 / 寄件（需要选择地址与时间段，在虾皮卖家中心操作），只用 {@code logistics/get_tracking_number}
 * 取平台运单号，返回 {@link ShipmentResultStatus#PLATFORM_MANAGED}（还没有安排时运单号为空）。虾皮仓配送（fulfilled_by_shopee）的订单跳过。
 * <p>
 * 说明：按开放平台公开文档实现，尚未用真实店铺联调。需要真实账号联调确认：
 * <ul>
 *   <li>get_shipping_parameter 的 info_needed 结构，以及已发货订单调用时的返回</li>
 *   <li>SLS 订单是否需要由 ERP 安排揽收（ship_order 的 pickup / dropoff 参数与地址、时间段），面单下载接口未实现</li>
 *   <li>买家支付的运费只在结算（escrow）接口中，订单同步没有运费</li>
 *   <li>update_stock 的 seller_stock 是否需要 location_id（多仓店铺）</li>
 * </ul>
 */
@Component
public class ShopeeConnector implements ChannelConnector {

    static final String PLATFORM = "Shopee";
    static final String PRODUCTION = "https://partner.shopeemobile.com";
    static final String SANDBOX = "https://partner.test-stable.shopeemobile.com";
    static final String AUTH_PATH = "/api/v2/shop/auth_partner";
    static final String TOKEN_PATH = "/api/v2/auth/token/get";
    static final String REFRESH_PATH = "/api/v2/auth/access_token/get";
    /** access_token 提前刷新的秒数 */
    static final long REFRESH_AHEAD_SECONDS = 600;
    /** get_order_list 单次时间窗口最多 15 天 */
    static final long WINDOW_SECONDS = 15L * 24 * 3600;
    static final int ORDER_PAGE_SIZE = 100;
    static final int DETAIL_BATCH = 50;
    static final int ITEM_PAGE_SIZE = 100;
    static final int STOCK_BATCH = 50;
    /** 分页上限，防止平台返回异常时死循环 */
    static final int MAX_PAGES = 500;
    private static final String DETAIL_FIELDS = "buyer_user_id,buyer_username,recipient_address,item_list,pay_time,total_amount,"
            + "note,fulfillment_flag,shipping_carrier,package_list";
    private static final String FULFILLED_BY_SHOPEE = "fulfilled_by_shopee";
    private static final Map<String, String> SHOP_DOMAINS = Map.ofEntries(
            Map.entry("SG", "shopee.sg"), Map.entry("MY", "shopee.com.my"), Map.entry("TH", "shopee.co.th"),
            Map.entry("PH", "shopee.ph"), Map.entry("VN", "shopee.vn"), Map.entry("ID", "shopee.co.id"),
            Map.entry("TW", "shopee.tw"), Map.entry("BR", "shopee.com.br"), Map.entry("MX", "shopee.com.mx"),
            Map.entry("CO", "shopee.com.co"), Map.entry("CL", "shopee.cl"));

    private static final ConnectorInfo INFO = new ConnectorInfo(EcomPlatform.SHOPEE, AuthType.OAUTH, List.of(
            CredentialField.text("partnerId", "ecom.label.cred.shopee.partnerId", true),
            CredentialField.secret("partnerKey", "ecom.label.cred.shopee.partnerKey"),
            CredentialField.text("region", "ecom.label.cred.shopee.region", false),
            CredentialField.oauthInfo("shopId", "ecom.label.cred.shopee.shopId"),
            CredentialField.token("accessToken", "ecom.label.cred.accessToken"),
            CredentialField.token("refreshToken", "ecom.label.cred.refreshToken"),
            CredentialField.cached("tokenExpireAt", false)),
            PRODUCTION, SANDBOX,
            EnumSet.of(ConnectorCapability.ORDERS, ConnectorCapability.SHIP_CONFIRM, ConnectorCapability.INVENTORY_PUSH,
                    ConnectorCapability.LISTINGS),
            "https://open.shopee.com/documents")
            .withRedirectNote("ecom.text.connector.redirect.shopee").withOrderWindowNote("ecom.text.connector.window.shopee");

    @Override
    public ConnectorInfo info() {
        return INFO;
    }

    // ---------------------------------------------------------------- 签名

    /** 签名：HMAC-SHA256(partner_key, partner_id + path + timestamp [+ access_token + shop_id])，小写十六进制 */
    static String sign(String partnerKey, String partnerId, String path, long timestamp, String accessToken, String shopId) {
        String base = partnerId + path + timestamp + (accessToken == null ? "" : accessToken) + (shopId == null ? "" : shopId);
        return ConnectorSigns.hmacSha256Hex(partnerKey, base);
    }

    /** 公共参数查询串（公共接口不带 access_token、shop_id） */
    private static String commonQuery(StoreCredentials c, String path, boolean shop) {
        String partnerId = c.require("partnerId");
        long ts = nowSeconds();
        Map<String, String> q = new LinkedHashMap<>();
        q.put("partner_id", partnerId);
        q.put("timestamp", String.valueOf(ts));
        String token = null;
        String shopId = null;
        if (shop) {
            token = c.require("accessToken");
            shopId = c.require("shopId");
            q.put("access_token", token);
            q.put("shop_id", shopId);
        }
        q.put("sign", sign(c.require("partnerKey"), partnerId, path, ts, token, shopId));
        return ConnectorSigns.query(q);
    }

    // ---------------------------------------------------------------- 授权

    @Override
    public String authorizeUrl(ConnectorContext ctx, String redirectUri, String state) {
        StoreCredentials c = ctx.credentials();
        String redirect = redirectUri + (redirectUri.contains("?") ? "&" : "?") + "state=" + ConnectorSigns.enc(state);
        return ctx.baseUrl(INFO) + AUTH_PATH + "?" + commonQuery(c, AUTH_PATH, false) + "&redirect=" + ConnectorSigns.enc(redirect);
    }

    @Override
    public OAuthResult exchangeCode(ConnectorContext ctx, Map<String, String> params, String redirectUri) {
        StoreCredentials c = ctx.credentials();
        String code = params.get("code");
        String shopId = params.get("shop_id");
        if (code == null || shopId == null) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, "code / shop_id missing");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("shop_id", parseLong(shopId));
        body.put("partner_id", parseLong(c.require("partnerId")));
        JsonNode resp = HttpJson.postJson(ctx.baseUrl(INFO) + TOKEN_PATH + "?" + commonQuery(c, TOKEN_PATH, false), Map.of(), body, PLATFORM);
        checkAuth(resp);
        Map<String, String> creds = tokenFields(resp);
        creds.put("shopId", shopId);
        return new OAuthResult(creds, shopId);
    }

    @Override
    public void ensureToken(ConnectorContext ctx) {
        StoreCredentials c = ctx.credentials();
        long expireAt = ConnectorJson.parseLong(c.get("tokenExpireAt"), 0);
        if (c.get("accessToken") != null && expireAt - REFRESH_AHEAD_SECONDS > nowSeconds()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("refresh_token", c.require("refreshToken"));
        body.put("partner_id", parseLong(c.require("partnerId")));
        body.put("shop_id", parseLong(c.require("shopId")));
        JsonNode resp = HttpJson.postJson(ctx.baseUrl(INFO) + REFRESH_PATH + "?" + commonQuery(c, REFRESH_PATH, false), Map.of(), body, PLATFORM);
        checkAuth(resp);
        ctx.updateCredentials(tokenFields(resp));
    }

    private static Map<String, String> tokenFields(JsonNode resp) {
        String access = HttpJson.text(resp, "access_token");
        String refresh = HttpJson.text(resp, "refresh_token");
        if (access == null || refresh == null) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, "access_token missing");
        }
        Map<String, String> m = new LinkedHashMap<>();
        m.put("accessToken", access);
        m.put("refreshToken", refresh);
        m.put("tokenExpireAt", String.valueOf(nowSeconds() + resp.path("expire_in").asLong(14400)));
        return m;
    }

    private static void checkAuth(JsonNode resp) {
        String error = HttpJson.text(resp, "error");
        if (error != null && !error.isBlank()) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, HttpJson.abbreviate(error + ": " + HttpJson.text(resp, "message")));
        }
    }

    // ---------------------------------------------------------------- 订单

    @Override
    public List<OrderDraft> fetchOrders(ConnectorContext ctx, LocalDateTime since) {
        ensureToken(ctx);
        String base = ctx.baseUrl(INFO);
        StoreCredentials c = ctx.credentials();
        long now = nowSeconds();
        long from = ConnectorJson.toUnixSeconds(since == null ? LocalDateTime.now().minusDays(3) : since);
        Set<String> orderSns = new LinkedHashSet<>();
        int pages = 0;
        for (long start = from; start < now && pages < MAX_PAGES; start += WINDOW_SECONDS) {
            long end = Math.min(start + WINDOW_SECONDS, now);
            String cursor = "";
            boolean more;
            do {
                Map<String, String> q = new LinkedHashMap<>();
                q.put("time_range_field", "update_time");
                q.put("time_from", String.valueOf(start));
                q.put("time_to", String.valueOf(end));
                q.put("page_size", String.valueOf(ORDER_PAGE_SIZE));
                q.put("cursor", cursor);
                q.put("response_optional_fields", "order_status");
                JsonNode resp = get(base, c, "/api/v2/order/get_order_list", ConnectorSigns.query(q));
                for (JsonNode o : resp.path("order_list")) {
                    String sn = HttpJson.text(o, "order_sn");
                    if (sn != null) {
                        orderSns.add(sn);
                    }
                }
                more = resp.path("more").asBoolean(false);
                cursor = HttpJson.text(resp, "next_cursor");
                pages++;
            } while (more && cursor != null && !cursor.isEmpty() && pages < MAX_PAGES);
        }
        if (pages >= MAX_PAGES) {
            ConnectorJson.warnPageLimit(PLATFORM, "orders", MAX_PAGES);
        }
        List<OrderDraft> drafts = new ArrayList<>();
        List<String> all = new ArrayList<>(orderSns);
        for (int i = 0; i < all.size(); i += DETAIL_BATCH) {
            List<String> batch = all.subList(i, Math.min(i + DETAIL_BATCH, all.size()));
            String q = "order_sn_list=" + ConnectorSigns.enc(String.join(",", batch)) + "&response_optional_fields=" + ConnectorSigns.enc(DETAIL_FIELDS);
            JsonNode resp = get(base, c, "/api/v2/order/get_order_detail", q);
            for (JsonNode o : resp.path("order_list")) {
                drafts.add(toDraft(ctx.store(), o));
            }
        }
        return drafts;
    }

    static OrderDraft toDraft(EcomStore store, JsonNode o) {
        OrderDraft d = new OrderDraft();
        d.setStoreId(store.getId());
        d.setPlatformOrderNo(HttpJson.text(o, "order_sn"));
        d.setOrderTime(epochSeconds(o.path("create_time")));
        d.setPaidTime(epochSeconds(o.path("pay_time")));
        d.setShipBy(epochSeconds(o.path("ship_by_date")));
        d.setStatus(status(HttpJson.text(o, "order_status")));
        d.setFulfillment(FULFILLED_BY_SHOPEE.equals(HttpJson.text(o, "fulfillment_flag")) ? FulfillmentType.PLATFORM : FulfillmentType.SELLER);
        d.setCurrency(HttpJson.text(o, "currency"));
        d.setBuyerName(HttpJson.text(o, "buyer_username"));
        d.setNote(HttpJson.text(o, "message_to_seller"));
        JsonNode addr = o.path("recipient_address");
        d.setShipName(HttpJson.text(addr, "name"));
        d.setShipPhone(HttpJson.text(addr, "phone"));
        d.setShipCountry(HttpJson.text(addr, "region"));
        d.setShipState(HttpJson.text(addr, "state"));
        d.setShipCity(HttpJson.text(addr, "city"));
        d.setShipPostcode(HttpJson.text(addr, "zipcode"));
        String full = HttpJson.text(addr, "full_address");
        d.setShipAddress(full != null ? full : ConnectorJson.join(HttpJson.text(addr, "town"), HttpJson.text(addr, "district")));
        for (JsonNode it : o.path("item_list")) {
            d.getLines().add(toLine(it));
        }
        return d;
    }

    private static OrderDraft.Line toLine(JsonNode it) {
        OrderDraft.Line l = new OrderDraft.Line();
        String itemId = HttpJson.text(it, "item_id");
        long modelId = it.path("model_id").asLong(0);
        String orderItemId = HttpJson.text(it, "order_item_id");
        l.setPlatformItemId(itemKey(itemId, modelId));
        l.setPlatformLineId((orderItemId == null ? itemId : orderItemId) + ":" + modelId);
        l.setSellerSku(ConnectorJson.firstNonBlank(HttpJson.text(it, "model_sku"), HttpJson.text(it, "item_sku"), l.getPlatformItemId()));
        l.setTitle(ConnectorJson.join(HttpJson.text(it, "item_name"), HttpJson.text(it, "model_name")));
        BigDecimal qty = HttpJson.decimal(it, "model_quantity_purchased");
        l.setQty(qty == null || qty.signum() <= 0 ? BigDecimal.ONE : qty);
        BigDecimal original = HttpJson.decimal(it, "model_original_price");
        BigDecimal discounted = HttpJson.decimal(it, "model_discounted_price");
        if (original == null) {
            original = discounted;
        }
        l.setPrice(original);
        if (original != null) {
            l.setAmount(original.multiply(l.getQty()));
            if (discounted != null && original.compareTo(discounted) > 0) {
                l.setDiscount(original.subtract(discounted).multiply(l.getQty()));
            }
        }
        return l;
    }

    static EcomOrderStatus status(String s) {
        if (s == null) {
            return EcomOrderStatus.UNSHIPPED;
        }
        return switch (s) {
            case "UNPAID", "INVOICE_PENDING" -> EcomOrderStatus.PENDING;
            case "SHIPPED", "TO_CONFIRM_RECEIVE", "COMPLETED" -> EcomOrderStatus.SHIPPED;
            case "CANCELLED", "IN_CANCEL" -> EcomOrderStatus.CANCELLED;
            case "TO_RETURN" -> EcomOrderStatus.REFUNDED;
            // READY_TO_SHIP、PROCESSED、RETRY_SHIP 及未知状态
            default -> EcomOrderStatus.UNSHIPPED;
        };
    }

    // ---------------------------------------------------------------- 发货

    /**
     * 物流渠道需要非集成物流信息时 ship_order 回传运单号（CONFIRMED）；平台物流（SLS）只取平台运单号（PLATFORM_MANAGED），不代为安排揽收
     */
    @Override
    public ShipmentResult confirmShipment(ConnectorContext ctx, EcomOrder order, List<EcomOrderItem> items) {
        if (order.getFulfillment() == FulfillmentType.PLATFORM) {
            return ShipmentResult.skipped(FULFILLED_BY_SHOPEE);
        }
        ensureToken(ctx);
        String base = ctx.baseUrl(INFO);
        String sn = ConnectorSigns.enc(order.getPlatformOrderNo());
        JsonNode param = get(base, ctx.credentials(), "/api/v2/logistics/get_shipping_parameter", "order_sn=" + sn);
        JsonNode needed = param.path("info_needed");
        boolean nonIntegrated = needed.has("non_integrated") || (!needed.has("pickup") && !needed.has("dropoff"));
        if (nonIntegrated) {
            if (order.getTrackingNo() == null || order.getTrackingNo().isBlank()) {
                throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "tracking number missing");
            }
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("order_sn", order.getPlatformOrderNo());
            body.put("non_integrated", Map.of("tracking_number", order.getTrackingNo()));
            post(base, ctx.credentials(), "/api/v2/logistics/ship_order", body);
            return ShipmentResult.confirmed(order.getTrackingNo(), order.getCarrier());
        }
        JsonNode tn = get(base, ctx.credentials(), "/api/v2/logistics/get_tracking_number", "order_sn=" + sn);
        String tracking = HttpJson.text(tn, "tracking_number");
        ShipmentResult r = ShipmentResult.platformManaged(tracking, null);
        return tracking == null || tracking.isBlank() ? r.withMessage("SLS: arrange pickup / drop-off in Shopee Seller Center") : r;
    }

    // ---------------------------------------------------------------- 库存

    /** 按 itemId 分组调用 update_stock（seller_stock 不区分仓位，每次最多 50 个规格） */
    @Override
    public void pushInventory(ConnectorContext ctx, List<InventoryUpdate> updates) {
        ensureToken(ctx);
        Map<Long, List<Map<String, Object>>> byItem = new LinkedHashMap<>();
        for (InventoryUpdate u : updates) {
            if (u.platformItemId() == null || u.platformItemId().isBlank()) {
                throw new BizException("ecom.error.connectorPlatformItemIdRequired", PLATFORM, u.sellerSku());
            }
            String[] pc = u.parentAndChild();
            long[] ids = pc != null ? new long[]{parseLong(pc[0]), parseLong(pc[1])} : parseItemKey(u.platformItemId());
            int stock = u.intQty();
            Map<String, Object> model = new LinkedHashMap<>();
            model.put("model_id", ids[1]);
            model.put("seller_stock", List.of(Map.of("stock", stock)));
            byItem.computeIfAbsent(ids[0], k -> new ArrayList<>()).add(model);
        }
        String base = ctx.baseUrl(INFO);
        for (Map.Entry<Long, List<Map<String, Object>>> e : byItem.entrySet()) {
            List<Map<String, Object>> models = e.getValue();
            for (int i = 0; i < models.size(); i += STOCK_BATCH) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("item_id", e.getKey());
                body.put("stock_list", models.subList(i, Math.min(i + STOCK_BATCH, models.size())));
                JsonNode resp = post(base, ctx.credentials(), "/api/v2/product/update_stock", body);
                JsonNode failures = resp.path("failure_list");
                if (failures.isArray() && !failures.isEmpty()) {
                    throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(failures.toString()));
                }
            }
        }
    }

    // ---------------------------------------------------------------- 商品

    @Override
    public List<ListingDraft> fetchListings(ConnectorContext ctx) {
        ensureToken(ctx);
        String base = ctx.baseUrl(INFO);
        StoreCredentials c = ctx.credentials();
        List<String> itemIds = new ArrayList<>();
        int offset = 0;
        for (int page = 0; ; page++) {
            if (page >= MAX_PAGES) {
                ConnectorJson.warnPageLimit(PLATFORM, "items", MAX_PAGES);
                break;
            }
            String q = "offset=" + offset + "&page_size=" + ITEM_PAGE_SIZE
                    + "&item_status=NORMAL&item_status=UNLIST&item_status=BANNED&item_status=REVIEWING";
            JsonNode resp = get(base, c, "/api/v2/product/get_item_list", q);
            for (JsonNode it : resp.path("item")) {
                itemIds.add(HttpJson.text(it, "item_id"));
            }
            if (!resp.path("has_next_page").asBoolean(false)) {
                break;
            }
            int next = resp.path("next_offset").asInt(offset + ITEM_PAGE_SIZE);
            if (next <= offset) {
                break;
            }
            offset = next;
        }
        String domain = c.get("region") == null ? null : SHOP_DOMAINS.get(c.get("region").toUpperCase());
        List<ListingDraft> drafts = new ArrayList<>();
        for (int i = 0; i < itemIds.size(); i += DETAIL_BATCH) {
            List<String> batch = itemIds.subList(i, Math.min(i + DETAIL_BATCH, itemIds.size()));
            JsonNode resp = get(base, c, "/api/v2/product/get_item_base_info", "item_id_list=" + ConnectorSigns.enc(String.join(",", batch)));
            for (JsonNode item : resp.path("item_list")) {
                String itemId = HttpJson.text(item, "item_id");
                String title = HttpJson.text(item, "item_name");
                String status = listingStatus(HttpJson.text(item, "item_status"));
                String image = ConnectorJson.str(item, "image", "image_url_list", "0");
                String url = domain == null ? null : "https://" + domain + "/product/" + c.get("shopId") + "/" + itemId;
                if (item.path("has_model").asBoolean(false)) {
                    JsonNode models = get(base, c, "/api/v2/product/get_model_list", "item_id=" + ConnectorSigns.enc(itemId));
                    JsonNode tiers = models.path("tier_variation");
                    for (JsonNode m : models.path("model")) {
                        long modelId = m.path("model_id").asLong(0);
                        String key = itemKey(itemId, modelId);
                        JsonNode price = m.path("price_info").path(0);
                        drafts.add(new ListingDraft(ConnectorJson.firstNonBlank(HttpJson.text(m, "model_sku"), HttpJson.text(item, "item_sku"), key),
                                modelId == 0 ? itemId : String.valueOf(modelId), modelId == 0 ? null : itemId,
                                title, variant(m, tiers), HttpJson.decimal(price, "current_price"), HttpJson.text(price, "currency"),
                                HttpJson.decimal(m, "stock_info_v2", "summary_info", "total_available_stock"), null, status, image, url));
                    }
                } else {
                    JsonNode price = item.path("price_info").path(0);
                    drafts.add(new ListingDraft(ConnectorJson.firstNonBlank(HttpJson.text(item, "item_sku"), itemId), itemId, null, title, null,
                            HttpJson.decimal(price, "current_price"), HttpJson.text(price, "currency"),
                            HttpJson.decimal(item, "stock_info_v2", "summary_info", "total_available_stock"), null, status, image, url));
                }
            }
        }
        return drafts;
    }

    /** 规格名称：model_name，没有时按 tier_index 取 tier_variation 的选项拼接 */
    private static String variant(JsonNode model, JsonNode tiers) {
        String name = HttpJson.text(model, "model_name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        List<String> parts = new ArrayList<>();
        JsonNode index = model.path("tier_index");
        for (int i = 0; i < index.size(); i++) {
            String option = HttpJson.text(tiers.path(i).path("option_list").path(index.path(i).asInt()), "option");
            if (option != null) {
                parts.add(option);
            }
        }
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    static String listingStatus(String s) {
        if (s == null) {
            return null;
        }
        ListingStatus status = switch (s) {
            case "NORMAL" -> ListingStatus.ACTIVE;
            case "UNLIST", "BANNED" -> ListingStatus.INACTIVE;
            case "REVIEWING" -> ListingStatus.INCOMPLETE;
            case "DELETED", "SELLER_DELETE", "SHOPEE_DELETE" -> ListingStatus.DELETED;
            default -> null;
        };
        return status == null ? null : status.name();
    }

    // ---------------------------------------------------------------- HTTP

    private static JsonNode get(String base, StoreCredentials c, String path, String query) {
        String url = base + path + "?" + commonQuery(c, path, true) + (query == null || query.isEmpty() ? "" : "&" + query);
        return check(HttpJson.get(url, Map.of(), PLATFORM));
    }

    private static JsonNode post(String base, StoreCredentials c, String path, Object body) {
        return check(HttpJson.postJson(base + path + "?" + commonQuery(c, path, true), Map.of(), body, PLATFORM));
    }

    /** error 不为空时抛出异常，返回 response 节点 */
    private static JsonNode check(JsonNode resp) {
        String error = HttpJson.text(resp, "error");
        if (error != null && !error.isBlank()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(error + ": " + HttpJson.text(resp, "message")));
        }
        return resp.path("response");
    }

    // ---------------------------------------------------------------- 工具

    static String itemKey(String itemId, long modelId) {
        return modelId == 0 ? itemId : itemId + ":" + modelId;
    }

    /** "itemId" 或 "itemId:modelId" → [itemId, modelId]（无规格 modelId = 0） */
    static long[] parseItemKey(String key) {
        int i = key.indexOf(':');
        if (i < 0) {
            return new long[]{parseLong(key), 0};
        }
        return new long[]{parseLong(key.substring(0, i)), parseLong(key.substring(i + 1))};
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "invalid id: " + s);
        }
    }

    private static long nowSeconds() {
        return Instant.now().getEpochSecond();
    }

    /** Unix 秒 → 系统默认时区的本地时间，0 或缺失为 null */
    static LocalDateTime epochSeconds(JsonNode v) {
        return ConnectorJson.unixSeconds(v);
    }

}
