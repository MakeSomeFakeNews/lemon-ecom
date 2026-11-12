package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.dto.OrderDraft;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * eBay Sell APIs（REST）：Fulfillment API 拉取订单与回传发货，Inventory API 回传库存与拉取库存商品。
 * <p>
 * <b>授权（OAuth 2.0 授权码模式）</b>：店铺填写开发者后台应用的 clientId（App ID）、clientSecret（Cert ID）与 ruName。
 * eBay 的 redirect_uri 不是 URL 而是 RuName（应用后台为回调地址生成的名称），卖家需要在 eBay 开发者后台
 * 「User Tokens → Get a Token from eBay via Your Application」中把该 RuName 的 Your auth accepted URL 设为本系统回调地址
 * （{后端地址}/api/ecom/oauth/callback/ebay），因此 {@link #authorizeUrl} / {@link #exchangeCode} 忽略框架传入的 redirectUri，
 * 一律使用 ruName；state 由 eBay 原样回传。授权码换取的 access_token 约 2 小时过期、refresh_token 约 18 个月，
 * 每次调用前 {@link #ensureToken} 在过期前 5 分钟用 refresh_token 刷新（eBay 刷新时不返回新的 refresh_token）。
 * 授权字段 accessToken / refreshToken / accessTokenExpiresAt（毫秒时间戳）/ refreshTokenExpiresAt 由授权回调写入。
 * 卖家 ID：eBay 的令牌响应不带用户信息（需要额外的 commerce.identity.readonly 权限调用 apiz.ebay.com 的 Identity API），
 * 本连接器不申请该权限，授权后不改写店铺的卖家 ID。
 * <p>
 * <b>地址规则</b>：业务接口与令牌接口（/identity/v1/oauth2/token）都在 api.ebay.com（沙箱 api.sandbox.ebay.com），
 * 都取 {@code ctx.baseUrl(INFO)}——店铺填写了自定义地址时两者都指向它；授权页是浏览器跳转，始终用 eBay 真实域名
 * auth.ebay.com（沙箱开关打开时 auth.sandbox.ebay.com）。
 * <p>
 * <b>订单</b>：GET /sell/fulfillment/v1/order?filter=lastmodifieddate:[since..]，每页 200 条按 offset 分页（最多 50 页）。
 * 状态：cancelStatus.cancelState = CANCELED → 已取消；orderPaymentStatus = FULLY_REFUNDED → 已退款；
 * PENDING / FAILED → 待付款；orderFulfillmentStatus = FULFILLED → 已发货；其余（NOT_STARTED、IN_PROGRESS 部分发货）→ 待发货。
 * fulfillmentStartInstructions 的类型为 FULFILLED_BY_EBAY 时为平台仓配送，其余为自发货。
 * 明细：sellerSku 取 lineItem.sku（卖家设置的自定义标签），没有 SKU 的老刊登退回 legacyItemId（有变体时为 legacyItemId:legacyVariationId）；
 * platformItemId = legacyItemId，有变体时编码为 {@code legacyItemId:legacyVariationId}；金额取 lineItemCost（单价 × 数量），
 * 运费 deliveryCost.shippingCost，税为 taxes 与 ebayCollectAndRemitTaxes 之和（代扣代缴税由 eBay 收取，仅作记录），
 * 折扣为 appliedPromotions 之和。
 * <p>
 * <b>发货</b>：POST /sell/fulfillment/v1/order/{orderId}/shipping_fulfillment（lineItems + 承运商代码 + 运单号），
 * 承运商代码要用 eBay 支持的名称（如 USPS、UPS、FedEx）。
 * <p>
 * <b>库存与商品</b>：只覆盖用 Inventory API 创建的库存商品（Trading API / 网页刊登的老商品不在 inventory_item 中，需迁移或改用 Trading API）。
 * 回传库存：POST /sell/inventory/v1/bulk_update_price_quantity，每次最多 25 个 SKU，按 SKU 更新 shipToLocationAvailability.quantity；
 * 拉取商品：GET /sell/inventory/v1/inventory_item，每页 100 条按 offset 分页（最多 50 页），platformItemId 为 SKU
 * （inventory_item 不含刊登 ID 与价格，价格在 offer 中，未逐个查询）。
 * <p>
 * 说明：按 eBay 公开文档实现，尚未用真实卖家账号联调。需要真实账号联调确认：
 * <ul>
 *   <li>RuName 的 accept URL 配置为本系统回调地址后，回调参数（code、state）是否原样带回</li>
 *   <li>getOrders 的 lastmodifieddate 过滤与 offset 上限；FULFILLED_BY_EBAY 订单的识别</li>
 *   <li>shipping_fulfillment 的 shippingCarrierCode 取值（需使用 eBay 承运商枚举，ERP 承运商名称可能不匹配）</li>
 *   <li>Trading API / 网页刊登的老商品不在 Inventory API 中：拉取商品、回传库存都覆盖不到</li>
 *   <li>ebayCollectAndRemitTaxes 计入订单税额是否符合财务口径（该税由 eBay 代收代缴）</li>
 * </ul>
 */
@Component
public class EbayConnector implements ChannelConnector {

    static final String PLATFORM = "eBay";
    static final String AUTH_URL = "https://auth.ebay.com/oauth2/authorize";
    static final String SANDBOX_AUTH_URL = "https://auth.sandbox.ebay.com/oauth2/authorize";
    static final String SCOPES = String.join(" ",
            "https://api.ebay.com/oauth/api_scope",
            "https://api.ebay.com/oauth/api_scope/sell.fulfillment",
            "https://api.ebay.com/oauth/api_scope/sell.inventory");
    /** 访问令牌提前刷新的时间（毫秒） */
    static final long REFRESH_AHEAD_MILLIS = 5 * 60 * 1000L;
    private static final int ORDER_PAGE_SIZE = 200;
    private static final int LISTING_PAGE_SIZE = 100;
    private static final int MAX_PAGES = 50;
    private static final int INVENTORY_BATCH = 25;
    private static final DateTimeFormatter FILTER_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final ConnectorInfo INFO = new ConnectorInfo(EcomPlatform.EBAY, AuthType.OAUTH, List.of(
            CredentialField.text("clientId", "ecom.label.cred.ebayClientId", true),
            CredentialField.secret("clientSecret", "ecom.label.cred.ebayClientSecret"),
            CredentialField.text("ruName", "ecom.label.cred.ebayRuName", true),
            CredentialField.token("accessToken", "ecom.label.cred.accessToken"),
            CredentialField.token("refreshToken", "ecom.label.cred.refreshToken"),
            CredentialField.cached("accessTokenExpiresAt", false),
            CredentialField.cached("refreshTokenExpiresAt", false)),
            "https://api.ebay.com", "https://api.sandbox.ebay.com",
            EnumSet.of(ConnectorCapability.ORDERS, ConnectorCapability.SHIP_CONFIRM, ConnectorCapability.INVENTORY_PUSH,
                    ConnectorCapability.LISTINGS),
            "https://developer.ebay.com/develop/apis/restful-apis/sell-apis").withRedirectNote("ecom.text.connector.redirect.ebay");

    @Override
    public ConnectorInfo info() {
        return INFO;
    }

    // ---------------------------------------------------------------- 授权

    /** 授权页：redirect_uri 填 RuName（框架传入的回调地址需在 eBay 后台配置为该 RuName 的 accept URL） */
    @Override
    public String authorizeUrl(ConnectorContext ctx, String redirectUri, String state) {
        StoreCredentials c = ctx.credentials();
        Map<String, String> q = new LinkedHashMap<>();
        q.put("client_id", c.require("clientId"));
        q.put("redirect_uri", c.require("ruName"));
        q.put("response_type", "code");
        q.put("scope", SCOPES);
        q.put("state", state);
        return (ctx.sandbox() ? SANDBOX_AUTH_URL : AUTH_URL) + "?" + ConnectorSigns.query(q).replace("+", "%20");
    }

    @Override
    public OAuthResult exchangeCode(ConnectorContext ctx, Map<String, String> params, String redirectUri) {
        StoreCredentials c = ctx.credentials();
        String code = params.get("code");
        if (code == null || code.isBlank()) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, "code missing" + (params.get("error") == null ? "" : ": "
                    + HttpJson.abbreviate(params.get("error"))));
        }
        String form = "grant_type=authorization_code&code=" + ConnectorSigns.enc(code) + "&redirect_uri=" + ConnectorSigns.enc(c.require("ruName"));
        JsonNode resp = token(ctx, form);
        long now = System.currentTimeMillis();
        Map<String, String> creds = new HashMap<>();
        creds.put("accessToken", HttpJson.text(resp, "access_token"));
        creds.put("accessTokenExpiresAt", String.valueOf(now + resp.path("expires_in").asLong(7200) * 1000));
        String refresh = HttpJson.text(resp, "refresh_token");
        if (refresh != null) {
            creds.put("refreshToken", refresh);
            creds.put("refreshTokenExpiresAt", String.valueOf(now + resp.path("refresh_token_expires_in").asLong(0) * 1000));
        }
        return new OAuthResult(creds, null);
    }

    /** access_token 过期前 5 分钟内用 refresh_token 刷新并保存 */
    @Override
    public void ensureToken(ConnectorContext ctx) {
        StoreCredentials c = ctx.credentials();
        if (c.get("accessToken") != null && expiresAt(c) - REFRESH_AHEAD_MILLIS > System.currentTimeMillis()) {
            return;
        }
        String form = "grant_type=refresh_token&refresh_token=" + ConnectorSigns.enc(c.require("refreshToken"))
                + "&scope=" + ConnectorSigns.enc(SCOPES);
        JsonNode resp = token(ctx, form);
        Map<String, String> changes = new HashMap<>();
        changes.put("accessToken", HttpJson.text(resp, "access_token"));
        changes.put("accessTokenExpiresAt", String.valueOf(System.currentTimeMillis() + resp.path("expires_in").asLong(7200) * 1000));
        ctx.updateCredentials(changes);
    }

    private static long expiresAt(StoreCredentials c) {
        return ConnectorJson.parseLong(c.get("accessTokenExpiresAt"), 0);
    }

    /** 令牌接口：Basic base64(clientId:clientSecret)，表单参数 */
    private static JsonNode token(ConnectorContext ctx, String form) {
        StoreCredentials c = ctx.credentials();
        String basic = Base64.getEncoder().encodeToString((c.require("clientId") + ":" + c.require("clientSecret")).getBytes(StandardCharsets.UTF_8));
        JsonNode resp = HttpJson.postForm(ctx.baseUrl(INFO) + "/identity/v1/oauth2/token", Map.of("Authorization", "Basic " + basic), form, PLATFORM);
        if (HttpJson.text(resp, "access_token") == null) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, HttpJson.abbreviate(resp.toString()));
        }
        return resp;
    }

    private Map<String, String> headers(ConnectorContext ctx) {
        ensureToken(ctx);
        return Map.of("Authorization", "Bearer " + ctx.credentials().require("accessToken"), "Accept", "application/json",
                "Content-Language", "en-US");
    }

    // ---------------------------------------------------------------- 订单

    @Override
    public List<OrderDraft> fetchOrders(ConnectorContext ctx, LocalDateTime since) {
        Map<String, String> headers = headers(ctx);
        String base = ctx.baseUrl(INFO);
        String from = since.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).format(FILTER_TS);
        String filter = ConnectorSigns.enc("lastmodifieddate:[" + from + "..]");
        List<OrderDraft> drafts = new ArrayList<>();
        int offset = 0;
        for (int page = 0; ; page++) {
            if (page >= MAX_PAGES) {
                ConnectorJson.warnPageLimit(PLATFORM, "orders", MAX_PAGES);
                break;
            }
            JsonNode resp = HttpJson.get(base + "/sell/fulfillment/v1/order?filter=" + filter + "&limit=" + ORDER_PAGE_SIZE + "&offset=" + offset,
                    headers, PLATFORM);
            JsonNode orders = resp.path("orders");
            for (JsonNode o : orders) {
                drafts.add(toDraft(ctx.store(), o));
            }
            offset += ORDER_PAGE_SIZE;
            if (orders.size() < ORDER_PAGE_SIZE || offset >= resp.path("total").asInt(0)) {
                break;
            }
        }
        return drafts;
    }

    static OrderDraft toDraft(EcomStore store, JsonNode o) {
        OrderDraft d = new OrderDraft();
        d.setStoreId(store.getId());
        d.setPlatformOrderNo(HttpJson.text(o, "orderId"));
        d.setOrderTime(time(HttpJson.text(o, "creationDate")));
        d.setStatus(status(o));
        d.setCurrency(HttpJson.text(o.path("pricingSummary").path("total"), "currency"));
        for (JsonNode p : o.path("paymentSummary").path("payments")) {
            LocalDateTime paid = time(HttpJson.text(p, "paymentDate"));
            if (paid != null) {
                d.setPaidTime(paid);
                break;
            }
        }
        JsonNode buyer = o.path("buyer");
        d.setBuyerName(HttpJson.text(buyer, "username"));
        d.setBuyerEmail(HttpJson.text(buyer.path("buyerRegistrationAddress"), "email"));
        d.setFulfillment(FulfillmentType.SELLER);
        JsonNode instructions = o.path("fulfillmentStartInstructions");
        if (instructions.isArray() && !instructions.isEmpty()) {
            JsonNode fi = instructions.get(0);
            if ("FULFILLED_BY_EBAY".equals(HttpJson.text(fi, "fulfillmentInstructionsType"))) {
                d.setFulfillment(FulfillmentType.PLATFORM);
            }
            d.setDeliverBy(time(HttpJson.text(fi, "maxEstimatedDeliveryDate")));
            JsonNode shipTo = fi.path("shippingStep").path("shipTo");
            JsonNode addr = shipTo.path("contactAddress");
            d.setShipName(HttpJson.text(shipTo, "fullName"));
            d.setShipPhone(HttpJson.text(shipTo.path("primaryPhone"), "phoneNumber"));
            d.setShipCountry(HttpJson.text(addr, "countryCode"));
            d.setShipState(HttpJson.text(addr, "stateOrProvince"));
            d.setShipCity(HttpJson.text(addr, "city"));
            d.setShipPostcode(HttpJson.text(addr, "postalCode"));
            d.setShipAddress(ConnectorJson.join(HttpJson.text(addr, "addressLine1"), HttpJson.text(addr, "addressLine2")));
            if (d.getBuyerEmail() == null) {
                d.setBuyerEmail(HttpJson.text(shipTo, "email"));
            }
        }
        for (JsonNode li : o.path("lineItems")) {
            OrderDraft.Line l = toLine(li);
            LocalDateTime shipBy = time(HttpJson.text(li.path("lineItemFulfillmentInstructions"), "shipByDate"));
            if (shipBy != null && (d.getShipBy() == null || shipBy.isBefore(d.getShipBy()))) {
                d.setShipBy(shipBy);
            }
            d.getLines().add(l);
        }
        return d;
    }

    private static OrderDraft.Line toLine(JsonNode li) {
        OrderDraft.Line l = new OrderDraft.Line();
        l.setPlatformLineId(HttpJson.text(li, "lineItemId"));
        String itemId = HttpJson.text(li, "legacyItemId");
        String variationId = HttpJson.text(li, "legacyVariationId");
        String platformItemId = itemId != null && variationId != null ? itemId + ":" + variationId : itemId;
        l.setPlatformItemId(platformItemId);
        String sku = HttpJson.text(li, "sku");
        l.setSellerSku(sku == null || sku.isBlank() ? platformItemId : sku);
        l.setTitle(HttpJson.text(li, "title"));
        BigDecimal qty = HttpJson.decimal(li, "quantity");
        l.setQty(qty == null || qty.signum() == 0 ? BigDecimal.ONE : qty);
        BigDecimal amount = HttpJson.decimal(li, "lineItemCost", "value");
        l.setAmount(amount);
        l.setPrice(amount == null ? BigDecimal.ZERO : amount.divide(l.getQty(), 4, RoundingMode.HALF_UP));
        l.setShipping(HttpJson.decimal(li, "deliveryCost", "shippingCost", "value"));
        l.setTax(sum(sum(null, li.path("taxes"), "amount"), li.path("ebayCollectAndRemitTaxes"), "amount"));
        l.setDiscount(sum(null, li.path("appliedPromotions"), "discountAmount"));
        return l;
    }

    /** 数组中各元素 field.value 之和（加到 base 上），都没有时返回 base */
    private static BigDecimal sum(BigDecimal base, JsonNode array, String field) {
        BigDecimal total = base;
        for (JsonNode n : array) {
            BigDecimal v = HttpJson.decimal(n, field, "value");
            if (v != null) {
                total = total == null ? v.abs() : total.add(v.abs());
            }
        }
        return total;
    }

    static EcomOrderStatus status(JsonNode o) {
        String cancel = HttpJson.text(o.path("cancelStatus"), "cancelState");
        if ("CANCELED".equals(cancel)) {
            return EcomOrderStatus.CANCELLED;
        }
        String payment = HttpJson.text(o, "orderPaymentStatus");
        if ("FULLY_REFUNDED".equals(payment)) {
            return EcomOrderStatus.REFUNDED;
        }
        if ("PENDING".equals(payment) || "FAILED".equals(payment)) {
            return EcomOrderStatus.PENDING;
        }
        return "FULFILLED".equals(HttpJson.text(o, "orderFulfillmentStatus")) ? EcomOrderStatus.SHIPPED : EcomOrderStatus.UNSHIPPED;
    }

    // ---------------------------------------------------------------- 发货

    /** 按订单行回传，支持一张订单分多个包裹部分发货 */
    @Override
    public boolean partialShipment() {
        return true;
    }

    @Override
    public ShipmentResult confirmShipment(ConnectorContext ctx, EcomOrder order, List<EcomOrderItem> items) {
        if (order.getFulfillment() == FulfillmentType.PLATFORM) {
            return ShipmentResult.skipped("FULFILLED_BY_EBAY");
        }
        if (order.getTrackingNo() == null || order.getTrackingNo().isBlank()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "tracking number missing");
        }
        Map<String, String> headers = headers(ctx);
        List<Map<String, Object>> lines = new ArrayList<>();
        for (EcomOrderItem it : items) {
            if (it.getPlatformLineId() == null) {
                throw new BizException("ecom.error.connectorLineIdMissing", order.getPlatformOrderNo());
            }
            lines.add(Map.of("lineItemId", it.getPlatformLineId(), "quantity", it.getQty() == null ? 1 : Math.max(1, it.getQty().intValue())));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("lineItems", lines);
        body.put("shippedDate", (order.getShipTime() == null ? LocalDateTime.now() : order.getShipTime())
                .atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).format(FILTER_TS));
        body.put("shippingCarrierCode", order.getCarrier());
        body.put("trackingNumber", order.getTrackingNo());
        HttpJson.postJson(ctx.baseUrl(INFO) + "/sell/fulfillment/v1/order/" + ConnectorSigns.enc(order.getPlatformOrderNo()) + "/shipping_fulfillment",
                headers, body, PLATFORM);
        return ShipmentResult.confirmed(order.getTrackingNo(), order.getCarrier());
    }

    // ---------------------------------------------------------------- 库存与商品

    @Override
    public void pushInventory(ConnectorContext ctx, List<InventoryUpdate> updates) {
        Map<String, String> headers = headers(ctx);
        String url = ctx.baseUrl(INFO) + "/sell/inventory/v1/bulk_update_price_quantity";
        for (int i = 0; i < updates.size(); i += INVENTORY_BATCH) {
            List<Map<String, Object>> requests = new ArrayList<>();
            for (InventoryUpdate u : updates.subList(i, Math.min(updates.size(), i + INVENTORY_BATCH))) {
                requests.add(Map.of("sku", u.sellerSku(), "shipToLocationAvailability", Map.of("quantity", u.intQty())));
            }
            JsonNode resp = HttpJson.postJson(url, headers, Map.of("requests", requests), PLATFORM);
            List<String> failed = new ArrayList<>();
            for (JsonNode r : resp.path("responses")) {
                int code = r.path("statusCode").asInt(200);
                if (code < 200 || code >= 300) {
                    failed.add(HttpJson.text(r, "sku") + " " + r.path("errors"));
                }
            }
            if (!failed.isEmpty()) {
                throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(String.join("; ", failed)));
            }
        }
    }

    @Override
    public List<ListingDraft> fetchListings(ConnectorContext ctx) {
        Map<String, String> headers = headers(ctx);
        String base = ctx.baseUrl(INFO);
        List<ListingDraft> listings = new ArrayList<>();
        int offset = 0;
        for (int page = 0; ; page++) {
            if (page >= MAX_PAGES) {
                ConnectorJson.warnPageLimit(PLATFORM, "inventory items", MAX_PAGES);
                break;
            }
            JsonNode resp = HttpJson.get(base + "/sell/inventory/v1/inventory_item?limit=" + LISTING_PAGE_SIZE + "&offset=" + offset, headers, PLATFORM);
            JsonNode items = resp.path("inventoryItems");
            for (JsonNode it : items) {
                String sku = HttpJson.text(it, "sku");
                JsonNode product = it.path("product");
                JsonNode images = product.path("imageUrls");
                String image = images.isArray() && !images.isEmpty() ? images.get(0).asString() : null;
                BigDecimal qty = HttpJson.decimal(it, "availability", "shipToLocationAvailability", "quantity");
                listings.add(new ListingDraft(sku, sku, null, HttpJson.text(product, "title"), null, null, null, qty, null, null, image, null));
            }
            offset += LISTING_PAGE_SIZE;
            if (items.size() < LISTING_PAGE_SIZE || offset >= resp.path("total").asInt(0)) {
                break;
            }
        }
        return listings;
    }

    // ---------------------------------------------------------------- 工具

    private static LocalDateTime time(String iso) {
        return ConnectorJson.isoTime(iso);
    }
}
