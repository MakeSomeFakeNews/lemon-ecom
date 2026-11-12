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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 美客多 Mercado Libre（拉美，api.mercadolibre.com）：订单、发货、库存、在线商品。
 * <p>
 * <b>授权（OAuth 2.0 授权码模式）</b>：店铺填写应用的 clientId（App ID）、clientSecret 与站点 site（MLM 墨西哥、MLB 巴西、
 * MLA 阿根廷、MLC 智利、MCO 哥伦比亚、MLU 乌拉圭、MPE 秘鲁、MEC 厄瓜多尔、MLV 委内瑞拉）；授权页按站点选择域名
 * （如 auth.mercadolibre.com.mx、auth.mercadolivre.com.br），redirect_uri 必须与应用后台登记的回调地址完全一致
 * （{后端地址}/api/ecom/oauth/callback/mercado_libre）。POST /oauth/token 用 authorization_code 换取令牌，access_token 6 小时过期，
 * {@link #ensureToken} 在过期前 5 分钟用 refresh_token 刷新——美客多的 refresh_token 只能使用一次，刷新后返回新的 refresh_token，
 * 必须与 access_token 一起保存。授权字段 accessToken / refreshToken / accessTokenExpiresAt（毫秒时间戳）/ userId 由授权回调写入，
 * user_id 同时写入店铺的卖家 ID。
 * <p>
 * <b>地址规则</b>：业务接口与令牌接口都在 api.mercadolibre.com，都取 {@code ctx.baseUrl(INFO)}（店铺的自定义地址优先）；
 * 授权页是浏览器跳转，始终用站点的真实授权域名。美客多没有沙箱网关，联调用 POST /users/test_user 创建的测试用户（买家、卖家各一个），
 * 因此沙箱地址为空。
 * <p>
 * <b>订单</b>：GET /orders/search?seller={userId}&amp;order.date_last_updated.from=…，每页 50 条按 offset 分页（最多 40 页），
 * 每个订单再查 GET /shipments/{shipping.id} 取收货地址、物流方式与配送状态。
 * 状态：订单 cancelled / invalid → 已取消；confirmed / payment_required / payment_in_process / partially_paid → 待付款；
 * paid / partially_refunded 时看运单：shipped / delivered / not_delivered → 已发货，运单 cancelled → 已取消，其余 → 待发货。
 * 运单 logistic_type = fulfillment（Mercado Envíos Full，美客多仓发货）→ 平台仓配送，其余自发货。
 * 同一购物车的多个订单有相同 pack_id，仍按订单分别同步。运费、税（payments 的 shipping_cost / taxes_amount）写入订单级运费 / 税。
 * <p>
 * <b>编码约定</b>：在线商品没有变体时 platformItemId = 刊登 ID（如 MLM123456789）；有变体时 platformItemId = 变体 ID、parentItemId = 刊登 ID。
 * 订单明细没有父 ID 字段，平台商品 ID 仍为刊登 ID 或 {@code 刊登ID:变体ID}；回传库存兼容旧编码 {@code 刊登ID:变体ID}。
 * sellerSku 取 seller_sku（属性 SELLER_SKU），没有时退回 seller_custom_field，都没有时用平台商品 ID。
 * <p>
 * <b>发货</b>：Mercado Envíos（me2，含 Full / Flex / drop-off）的运单由平台生成，卖家打印平台面单即可，不需要也不能回传运单号，
 * {@link #confirmShipment} 对这类订单返回平台物流托管，并带回运单上的 tracking_number 与 tracking_method（承运商）；
 * Full（美客多仓发货）的订单跳过；只有 me1 / custom / not_specified（卖家自配送）需要回传：
 * PUT /shipments/{id} 写入 tracking_number 与 shipped 状态。
 * <p>
 * <b>库存</b>：PUT /items/{id}（available_quantity），有变体时 PUT /items/{id}/variations/{变体ID}；Full 仓的库存由美客多管理，不能修改。
 * <b>商品</b>：GET /users/{id}/items/search（search_type=scan 滚动，突破 offset 1000 的限制，最多 100 页）+ GET /items?ids=（每次 20 个），
 * 有变体的按变体展开。
 * <p>
 * 说明：按美客多开发者文档实现，尚未用真实卖家账号联调。需要真实账号联调确认：
 * <ul>
 *   <li>/orders/search 的 order.date_last_updated.from 时间格式与 offset 上限（超过时需要按时间分段）</li>
 *   <li>me2 运单在打印面单前 tracking_number 是否为空；Flex 运单的承运商字段</li>
 *   <li>自配送 PUT /shipments/{id} 是否仍被接受（部分站点已要求改用 me1 专用接口）</li>
 *   <li>payments 的 shipping_cost 是否为买家支付的运费（免运费由卖家承担时为 0）</li>
 * </ul>
 */
@Component
public class MercadoLibreConnector implements ChannelConnector {

    static final String PLATFORM = "Mercado Libre";
    static final long REFRESH_AHEAD_MILLIS = 5 * 60 * 1000L;
    static final Map<String, String> AUTH_DOMAINS = Map.of(
            "MLA", "https://auth.mercadolibre.com.ar",
            "MLB", "https://auth.mercadolivre.com.br",
            "MLM", "https://auth.mercadolibre.com.mx",
            "MLC", "https://auth.mercadolibre.cl",
            "MCO", "https://auth.mercadolibre.com.co",
            "MLU", "https://auth.mercadolibre.com.uy",
            "MPE", "https://auth.mercadolibre.com.pe",
            "MEC", "https://auth.mercadolibre.com.ec",
            "MLV", "https://auth.mercadolibre.com.ve");
    private static final int ORDER_PAGE_SIZE = 50;
    private static final int MAX_ORDER_PAGES = 200;
    private static final int ITEM_PAGE_SIZE = 100;
    private static final int MAX_ITEM_PAGES = 100;
    private static final int ITEMS_PER_MULTIGET = 20;
    private static final Set<String> SELF_SHIPPING_MODES = Set.of("me1", "custom", "not_specified");
    private static final DateTimeFormatter FROM_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'-00:00'");
    private static final ConnectorInfo INFO = new ConnectorInfo(EcomPlatform.MERCADO_LIBRE, AuthType.OAUTH, List.of(
            CredentialField.text("clientId", "ecom.label.cred.mercadoLibreClientId", true),
            CredentialField.secret("clientSecret", "ecom.label.cred.mercadoLibreClientSecret"),
            CredentialField.text("site", "ecom.label.cred.mercadoLibreSite", true),
            CredentialField.token("accessToken", "ecom.label.cred.accessToken"),
            CredentialField.token("refreshToken", "ecom.label.cred.refreshToken"),
            CredentialField.cached("accessTokenExpiresAt", false),
            CredentialField.cached("userId", false)),
            "https://api.mercadolibre.com", null,
            EnumSet.of(ConnectorCapability.ORDERS, ConnectorCapability.SHIP_CONFIRM, ConnectorCapability.INVENTORY_PUSH,
                    ConnectorCapability.LISTINGS),
            "https://developers.mercadolibre.com/").withRedirectNote("ecom.text.connector.redirect.mercadoLibre");

    @Override
    public ConnectorInfo info() {
        return INFO;
    }

    // ---------------------------------------------------------------- 授权

    @Override
    public String authorizeUrl(ConnectorContext ctx, String redirectUri, String state) {
        StoreCredentials c = ctx.credentials();
        Map<String, String> q = new LinkedHashMap<>();
        q.put("response_type", "code");
        q.put("client_id", c.require("clientId"));
        q.put("redirect_uri", redirectUri);
        q.put("state", state);
        return authDomain(c) + "/authorization?" + ConnectorSigns.query(q);
    }

    static String authDomain(StoreCredentials c) {
        String site = c.require("site").toUpperCase();
        String domain = AUTH_DOMAINS.get(site);
        if (domain == null) {
            throw new BizException("ecom.error.mercadoLibreSiteInvalid", site);
        }
        return domain;
    }

    @Override
    public OAuthResult exchangeCode(ConnectorContext ctx, Map<String, String> params, String redirectUri) {
        StoreCredentials c = ctx.credentials();
        String code = params.get("code");
        if (code == null || code.isBlank()) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, "code missing" + (params.get("error") == null ? "" : ": "
                    + HttpJson.abbreviate(params.get("error"))));
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("client_id", c.require("clientId"));
        form.put("client_secret", c.require("clientSecret"));
        form.put("code", code);
        form.put("redirect_uri", redirectUri);
        JsonNode resp = token(ctx, form);
        Map<String, String> creds = tokenValues(resp);
        String userId = HttpJson.text(resp, "user_id");
        if (userId != null) {
            creds.put("userId", userId);
        }
        return new OAuthResult(creds, userId);
    }

    /** access_token 过期前 5 分钟内刷新；refresh_token 一次性，新的一并保存 */
    @Override
    public void ensureToken(ConnectorContext ctx) {
        StoreCredentials c = ctx.credentials();
        if (c.get("accessToken") != null && expiresAt(c) - REFRESH_AHEAD_MILLIS > System.currentTimeMillis()) {
            return;
        }
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", c.require("clientId"));
        form.put("client_secret", c.require("clientSecret"));
        form.put("refresh_token", c.require("refreshToken"));
        ctx.updateCredentials(tokenValues(token(ctx, form)));
    }

    private static Map<String, String> tokenValues(JsonNode resp) {
        Map<String, String> v = new HashMap<>();
        v.put("accessToken", HttpJson.text(resp, "access_token"));
        v.put("accessTokenExpiresAt", String.valueOf(System.currentTimeMillis() + resp.path("expires_in").asLong(21600) * 1000));
        String refresh = HttpJson.text(resp, "refresh_token");
        if (refresh != null) {
            v.put("refreshToken", refresh);
        }
        return v;
    }

    private static long expiresAt(StoreCredentials c) {
        return ConnectorJson.parseLong(c.get("accessTokenExpiresAt"), 0);
    }

    private static JsonNode token(ConnectorContext ctx, Map<String, String> form) {
        JsonNode resp = HttpJson.postForm(ctx.baseUrl(INFO) + "/oauth/token", Map.of("Accept", "application/json"), ConnectorSigns.query(form), PLATFORM);
        if (HttpJson.text(resp, "access_token") == null) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, HttpJson.abbreviate(resp.toString()));
        }
        return resp;
    }

    private Map<String, String> headers(ConnectorContext ctx) {
        ensureToken(ctx);
        return Map.of("Authorization", "Bearer " + ctx.credentials().require("accessToken"), "Accept", "application/json");
    }

    private static String userId(ConnectorContext ctx) {
        String id = ctx.credentials().get("userId");
        if (id == null && ctx.store().getSellerId() != null && !ctx.store().getSellerId().isBlank()) {
            id = ctx.store().getSellerId().trim();
        }
        if (id == null) {
            throw new BizException("ecom.error.credentialMissing", "userId");
        }
        return id;
    }

    // ---------------------------------------------------------------- 订单

    @Override
    public List<OrderDraft> fetchOrders(ConnectorContext ctx, LocalDateTime since) {
        Map<String, String> headers = headers(ctx);
        String base = ctx.baseUrl(INFO);
        String seller = userId(ctx);
        String from = since.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).format(FROM_TS);
        List<OrderDraft> drafts = new ArrayList<>();
        int offset = 0;
        for (int page = 0; ; page++) {
            if (page >= MAX_ORDER_PAGES) {
                ConnectorJson.warnPageLimit(PLATFORM, "orders", MAX_ORDER_PAGES);
                break;
            }
            String url = base + "/orders/search?seller=" + ConnectorSigns.enc(seller) + "&order.date_last_updated.from=" + ConnectorSigns.enc(from)
                    + "&sort=date_asc&offset=" + offset + "&limit=" + ORDER_PAGE_SIZE;
            JsonNode resp = HttpJson.get(url, headers, PLATFORM);
            JsonNode results = resp.path("results");
            for (JsonNode o : results) {
                String shipmentId = HttpJson.text(o.path("shipping"), "id");
                JsonNode shipment = shipmentId == null ? null : HttpJson.get(base + "/shipments/" + ConnectorSigns.enc(shipmentId), headers, PLATFORM);
                drafts.add(toDraft(ctx.store(), o, shipment));
            }
            offset += ORDER_PAGE_SIZE;
            if (results.size() < ORDER_PAGE_SIZE || offset >= resp.path("paging").path("total").asInt(0)) {
                break;
            }
        }
        return drafts;
    }

    static OrderDraft toDraft(EcomStore store, JsonNode o, JsonNode shipment) {
        OrderDraft d = new OrderDraft();
        d.setStoreId(store.getId());
        d.setPlatformOrderNo(HttpJson.text(o, "id"));
        d.setOrderTime(time(HttpJson.text(o, "date_created")));
        d.setCurrency(HttpJson.text(o, "currency_id"));
        JsonNode buyer = o.path("buyer");
        String fullName = ConnectorJson.join(HttpJson.text(buyer, "first_name"), HttpJson.text(buyer, "last_name"));
        d.setBuyerName(fullName == null ? HttpJson.text(buyer, "nickname") : fullName);
        BigDecimal shippingCost = null;
        BigDecimal taxes = null;
        for (JsonNode p : o.path("payments")) {
            if (!"approved".equals(HttpJson.text(p, "status"))) {
                continue;
            }
            LocalDateTime approved = time(HttpJson.text(p, "date_approved"));
            if (approved != null && (d.getPaidTime() == null || approved.isBefore(d.getPaidTime()))) {
                d.setPaidTime(approved);
            }
            shippingCost = ConnectorJson.add(shippingCost, HttpJson.decimal(p, "shipping_cost"));
            taxes = ConnectorJson.add(taxes, HttpJson.decimal(p, "taxes_amount"));
        }
        String shipStatus = null;
        d.setFulfillment(FulfillmentType.SELLER);
        if (shipment != null) {
            shipStatus = HttpJson.text(shipment, "status");
            String logisticType = firstText(shipment.path("logistic"), "type", shipment, "logistic_type");
            if ("fulfillment".equals(logisticType)) {
                d.setFulfillment(FulfillmentType.PLATFORM);
            }
            JsonNode addr = shipment.path("receiver_address");
            JsonNode dest = shipment.path("destination");
            if (addr.isMissingNode() || addr.isNull()) {
                addr = dest.path("shipping_address");
            }
            d.setShipName(firstText(addr, "receiver_name", dest, "receiver_name"));
            d.setShipPhone(firstText(addr, "receiver_phone", dest, "receiver_phone"));
            d.setShipCountry(firstText(addr.path("country"), "id", addr, "country_id"));
            d.setShipState(firstText(addr.path("state"), "name", addr, "state_name"));
            d.setShipCity(firstText(addr.path("city"), "name", addr, "city_name"));
            d.setShipPostcode(HttpJson.text(addr, "zip_code"));
            String line = HttpJson.text(addr, "address_line");
            if (line == null) {
                line = ConnectorJson.join(HttpJson.text(addr, "street_name"), HttpJson.text(addr, "street_number"));
            }
            d.setShipAddress(ConnectorJson.join(line, HttpJson.text(addr, "comment")));
            JsonNode option = shipment.path("shipping_option");
            d.setDeliverBy(time(HttpJson.text(option.path("estimated_delivery_time"), "date")));
            d.setShipBy(time(HttpJson.text(option.path("estimated_handling_limit"), "date")));
            if (d.getShipBy() == null) {
                d.setShipBy(time(HttpJson.text(shipment.path("lead_time").path("estimated_handling_limit"), "date")));
            }
        }
        d.setStatus(status(HttpJson.text(o, "status"), shipStatus));
        for (JsonNode oi : o.path("order_items")) {
            d.getLines().add(toLine(oi));
        }
        d.setShippingAmount(shippingCost);
        d.setTaxAmount(taxes);
        return d;
    }

    private static OrderDraft.Line toLine(JsonNode oi) {
        JsonNode item = oi.path("item");
        OrderDraft.Line l = new OrderDraft.Line();
        String itemId = HttpJson.text(item, "id");
        String variationId = HttpJson.text(item, "variation_id");
        String platformItemId = variationId == null ? itemId : itemId + ":" + variationId;
        l.setPlatformItemId(platformItemId);
        l.setPlatformLineId(platformItemId);
        l.setSellerSku(sku(HttpJson.text(item, "seller_sku"), HttpJson.text(item, "seller_custom_field"), platformItemId));
        l.setTitle(HttpJson.text(item, "title"));
        BigDecimal qty = HttpJson.decimal(oi, "quantity");
        l.setQty(qty == null || qty.signum() == 0 ? BigDecimal.ONE : qty);
        BigDecimal price = HttpJson.decimal(oi, "unit_price");
        l.setPrice(price == null ? BigDecimal.ZERO : price);
        l.setAmount(l.getPrice().multiply(l.getQty()).setScale(2, RoundingMode.HALF_UP));
        return l;
    }

    static EcomOrderStatus status(String orderStatus, String shipmentStatus) {
        if (orderStatus == null) {
            return EcomOrderStatus.UNSHIPPED;
        }
        switch (orderStatus) {
            case "cancelled", "invalid":
                return EcomOrderStatus.CANCELLED;
            case "confirmed", "payment_required", "payment_in_process", "partially_paid":
                return EcomOrderStatus.PENDING;
            default:
                break;
        }
        if (shipmentStatus == null) {
            return EcomOrderStatus.UNSHIPPED;
        }
        return switch (shipmentStatus) {
            case "shipped", "delivered", "not_delivered" -> EcomOrderStatus.SHIPPED;
            case "cancelled" -> EcomOrderStatus.CANCELLED;
            default -> EcomOrderStatus.UNSHIPPED;
        };
    }

    // ---------------------------------------------------------------- 发货

    /** 自配送（me1 / custom）回传运单号；Mercado Envíos 平台面单的订单无需回传，返回平台物流托管与平台运单号；Full 订单跳过 */
    @Override
    public ShipmentResult confirmShipment(ConnectorContext ctx, EcomOrder order, List<EcomOrderItem> items) {
        Map<String, String> headers = headers(ctx);
        String base = ctx.baseUrl(INFO);
        JsonNode o = HttpJson.get(base + "/orders/" + ConnectorSigns.enc(order.getPlatformOrderNo()), headers, PLATFORM);
        String shipmentId = HttpJson.text(o.path("shipping"), "id");
        if (shipmentId == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "order " + order.getPlatformOrderNo() + " has no shipment");
        }
        JsonNode shipment = HttpJson.get(base + "/shipments/" + ConnectorSigns.enc(shipmentId), headers, PLATFORM);
        String mode = firstText(shipment, "mode", shipment.path("logistic"), "mode");
        if (mode != null && !SELF_SHIPPING_MODES.contains(mode)) {
            if ("fulfillment".equals(firstText(shipment.path("logistic"), "type", shipment, "logistic_type"))) {
                return ShipmentResult.skipped("Mercado Envios Full");
            }
            return ShipmentResult.platformManaged(HttpJson.text(shipment, "tracking_number"), HttpJson.text(shipment, "tracking_method"))
                    .withMessage(mode);
        }
        if (order.getTrackingNo() == null || order.getTrackingNo().isBlank()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "tracking number missing");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tracking_number", order.getTrackingNo());
        body.put("status", "shipped");
        if (order.getCarrier() != null) {
            body.put("comment", order.getCarrier());
        }
        HttpJson.putJson(base + "/shipments/" + ConnectorSigns.enc(shipmentId), headers, body, PLATFORM);
        return ShipmentResult.confirmed(order.getTrackingNo(), order.getCarrier());
    }

    // ---------------------------------------------------------------- 库存与商品

    @Override
    public void pushInventory(ConnectorContext ctx, List<InventoryUpdate> updates) {
        Map<String, String> headers = headers(ctx);
        String base = ctx.baseUrl(INFO);
        for (InventoryUpdate u : updates) {
            String id = u.platformItemId();
            if (id == null || id.isBlank()) {
                throw new BizException("ecom.error.connectorItemIdMissing", u.sellerSku());
            }
            int qty = u.intQty();
            String[] ids = u.parentAndChild();
            String url = ids == null ? base + "/items/" + ConnectorSigns.enc(id.trim())
                    : base + "/items/" + ConnectorSigns.enc(ids[0]) + "/variations/" + ConnectorSigns.enc(ids[1]);
            HttpJson.putJson(url, headers, Map.of("available_quantity", qty), PLATFORM);
        }
    }

    @Override
    public List<ListingDraft> fetchListings(ConnectorContext ctx) {
        Map<String, String> headers = headers(ctx);
        String base = ctx.baseUrl(INFO);
        String seller = userId(ctx);
        List<String> ids = new ArrayList<>();
        String scrollId = null;
        for (int page = 0; page < MAX_ITEM_PAGES; page++) {
            String url = base + "/users/" + ConnectorSigns.enc(seller) + "/items/search?search_type=scan&limit=" + ITEM_PAGE_SIZE
                    + (scrollId == null ? "" : "&scroll_id=" + ConnectorSigns.enc(scrollId));
            JsonNode resp = HttpJson.get(url, headers, PLATFORM);
            JsonNode results = resp.path("results");
            results.forEach(n -> ids.add(n.asString()));
            scrollId = HttpJson.text(resp, "scroll_id");
            if (results.isEmpty() || results.size() < ITEM_PAGE_SIZE || scrollId == null) {
                break;
            }
        }
        List<ListingDraft> listings = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += ITEMS_PER_MULTIGET) {
            String joined = String.join(",", ids.subList(i, Math.min(ids.size(), i + ITEMS_PER_MULTIGET)));
            JsonNode resp = HttpJson.get(base + "/items?ids=" + ConnectorSigns.enc(joined) + "&include_attributes=all", headers, PLATFORM);
            for (JsonNode entry : resp) {
                if (entry.path("code").asInt(200) != 200) {
                    continue;
                }
                addListings(listings, entry.path("body"));
            }
        }
        return listings;
    }

    static void addListings(List<ListingDraft> out, JsonNode item) {
        String itemId = HttpJson.text(item, "id");
        String title = HttpJson.text(item, "title");
        String currency = HttpJson.text(item, "currency_id");
        boolean deleted = false;
        for (JsonNode sub : item.path("sub_status")) {
            deleted |= "deleted".equals(sub.asString());
        }
        String status = listingStatus(HttpJson.text(item, "status"), deleted);
        String fulfillment = "fulfillment".equals(HttpJson.text(item.path("shipping"), "logistic_type"))
                ? FulfillmentType.PLATFORM.name() : FulfillmentType.SELLER.name();
        JsonNode pictures = item.path("pictures");
        String image = pictures.isArray() && !pictures.isEmpty() ? firstText(pictures.get(0), "secure_url", pictures.get(0), "url")
                : HttpJson.text(item, "thumbnail");
        String url = HttpJson.text(item, "permalink");
        String itemSku = sku(attribute(item, "SELLER_SKU"), HttpJson.text(item, "seller_custom_field"), null);
        JsonNode variations = item.path("variations");
        if (!variations.isArray() || variations.isEmpty()) {
            out.add(new ListingDraft(sku(itemSku, null, itemId), itemId, null, title, null, HttpJson.decimal(item, "price"), currency,
                    HttpJson.decimal(item, "available_quantity"), fulfillment, status, image, url));
            return;
        }
        for (JsonNode v : variations) {
            String variationId = HttpJson.text(v, "id");
            List<String> names = new ArrayList<>();
            for (JsonNode a : v.path("attribute_combinations")) {
                String value = HttpJson.text(a, "value_name");
                if (value != null) {
                    names.add(value);
                }
            }
            String sku = sku(attribute(v, "SELLER_SKU"), HttpJson.text(v, "seller_custom_field"), itemId + ":" + variationId);
            BigDecimal price = HttpJson.decimal(v, "price");
            out.add(new ListingDraft(sku, variationId, itemId, title, names.isEmpty() ? null : String.join(" / ", names),
                    price == null ? HttpJson.decimal(item, "price") : price, currency, HttpJson.decimal(v, "available_quantity"),
                    fulfillment, status, image, url));
        }
    }

    private static String listingStatus(String s, boolean deleted) {
        if (s == null) {
            return null;
        }
        return switch (s) {
            case "active" -> ListingStatus.ACTIVE.name();
            case "paused", "inactive" -> ListingStatus.INACTIVE.name();
            case "under_review", "not_yet_active", "payment_required" -> ListingStatus.INCOMPLETE.name();
            case "closed" -> deleted ? ListingStatus.DELETED.name() : ListingStatus.INACTIVE.name();
            default -> null;
        };
    }

    // ---------------------------------------------------------------- 工具

    private static String attribute(JsonNode node, String id) {
        for (JsonNode a : node.path("attributes")) {
            if (id.equals(HttpJson.text(a, "id"))) {
                return HttpJson.text(a, "value_name");
            }
        }
        return null;
    }

    private static String sku(String primary, String secondary, String fallback) {
        String s = ConnectorJson.firstNonBlank(primary, secondary);
        return s != null ? s : fallback;
    }

    private static String firstText(JsonNode a, String fa, JsonNode b, String fb) {
        String v = HttpJson.text(a, fa);
        return v != null ? v : HttpJson.text(b, fb);
    }

    private static LocalDateTime time(String iso) {
        return ConnectorJson.isoTime(iso);
    }
}
