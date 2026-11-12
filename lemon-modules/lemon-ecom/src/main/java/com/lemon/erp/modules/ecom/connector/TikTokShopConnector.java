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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * TikTok Shop Partner Center（Open API 202309 版本，https://partner.tiktokshop.com/docv2）。
 * <p>
 * 授权（OAuth）：店铺填写 appKey、appSecret、serviceId（服务市场里应用的 service_id），可选 warehouseId（回传库存的仓库，不填用默认仓）。
 * 卖家打开 https://services.tiktokshop.com/open/authorize?service_id=…&amp;state=… 授权，平台跳转到 Partner Center 中为应用配置的回调地址
 * （要配置成 ERP 的 /api/ecom/oauth/callback/tiktok，redirectUri 参数不会传给平台），带 code 与 state；
 * 由 GET https://auth.tiktok-shops.com/api/v2/token/get（grant_type=authorized_code）换取令牌，再调 /authorization/202309/shops 取 shop_cipher。
 * 写入 accessToken、refreshToken、accessTokenExpireIn（access_token 过期时间，Unix 秒，平台原值）、refreshTokenExpireIn、
 * shopCipher、shopId、sellerName、shopRegion；授权多个店铺时取授权信息中已有的 shopId 对应的店铺，否则取第一个。
 * 过期前 10 分钟用 /api/v2/token/refresh（grant_type=refresh_token）刷新。
 * <p>
 * 签名：查询参数去掉 sign、access_token 后按键排序拼 k+v，原文 = app_secret + path + 拼接串 + 请求体 JSON（非 multipart 时）+ app_secret，
 * HMAC-SHA256(app_secret) 小写十六进制；公共参数 app_key、timestamp（秒）、sign、shop_cipher，令牌放请求头 x-tts-access-token。
 * <p>
 * 地址：业务接口 https://open-api.tiktokglobalshop.com，令牌接口 https://auth.tiktok-shops.com，授权页 https://services.tiktokshop.com；
 * 店铺填写了自定义地址时三者都指向它（{自定义地址}/api/v2/token/get、{自定义地址}/open/authorize），便于用模拟服务器测试。
 * TikTok Shop 没有独立的沙箱域名：在 Partner Center 创建测试店铺（sandbox shop），接口地址与正式相同，因此沙箱地址与正式地址相同。
 * <p>
 * 编码约定：在线商品 platformItemId 为 sku_id、parentItemId 为 product_id（附加 ID warehouseId 为 SKU 库存所在的第一个仓库）；
 * 订单明细没有父 ID 字段，平台商品 ID 仍为 {@code "productId:skuId"}；回传库存按 parentItemId + platformItemId，兼容旧编码 {@code "productId:skuId"}。
 * 订单行 ID 为 line_items[].id；TikTok 订单的每个 line item 是一件商品（数量 1）。卖家 SKU 取 seller_sku，为空取 sku_id，仍为空取平台商品 ID。
 * 金额：单价 / 商品金额取 original_price，折扣取卖家承担的 seller_discount（平台补贴不算卖家折扣），
 * 订单级的运费 payment.shipping_fee 与税 payment.tax 写入订单级运费 / 税。FBT（fulfillment_type = FULFILLMENT_BY_TIKTOK）为平台仓配送，
 * 回传发货时跳过。签名包含请求体：请求体先序列化一次，签名与发送用同一个字符串（{@link HttpJson#requestRaw}）。
 * <p>
 * 发货：卖家自选物流 + 运单号，调用 Mark Package As Shipped（POST /fulfillment/202309/orders/{order_id}/packages）。
 * 承运商为物流商 ID（纯数字）时直接使用，否则按订单的 delivery_option_id 查询可用物流商，按名称（不区分大小写）匹配。
 * 该接口只对部分站点开放（US、UK、欧洲、JP 等）；东南亚站点使用平台物流，需要另行实现 Ship Package / 面单。
 * <p>
 * 说明：按 Partner Center 公开文档实现，尚未用真实店铺联调。需要真实账号联调确认：
 * <ul>
 *   <li>签名原文中查询参数的排序与 body 拼接（含 shop_cipher、不含 access_token）与平台计算一致</li>
 *   <li>token/get 与 token/refresh 返回的 access_token_expire_in 是过期时刻（Unix 秒）而不是有效秒数</li>
 *   <li>Mark Package As Shipped 在目标站点是否开放；东南亚站点平台物流的 Ship Package / 面单流程未实现</li>
 *   <li>订单搜索 update_time_ge 的过滤口径，以及 payment.shipping_fee 是否已含平台运费补贴</li>
 *   <li>多仓店铺回传库存的 warehouse_id 取值（在线商品附加 ID 中的第一个仓库或授权信息 warehouseId）</li>
 * </ul>
 */
@Component
public class TikTokShopConnector implements ChannelConnector {

    static final String PLATFORM = "TikTok Shop";
    static final String API_BASE = "https://open-api.tiktokglobalshop.com";
    static final String AUTH_BASE = "https://auth.tiktok-shops.com";
    static final String AUTHORIZE_BASE = "https://services.tiktokshop.com";
    static final long REFRESH_AHEAD_SECONDS = 600;
    static final int PAGE_SIZE = 50;
    static final int MAX_PAGES = 500;
    private static final String FBT = "FULFILLMENT_BY_TIKTOK";

    private static final ConnectorInfo INFO = new ConnectorInfo(EcomPlatform.TIKTOK, AuthType.OAUTH, List.of(
            CredentialField.text("appKey", "ecom.label.cred.appKey", true),
            CredentialField.secret("appSecret", "ecom.label.cred.appSecret"),
            CredentialField.text("serviceId", "ecom.label.cred.tiktok.serviceId", true),
            CredentialField.text("warehouseId", "ecom.label.cred.tiktok.warehouseId", false),
            CredentialField.token("accessToken", "ecom.label.cred.accessToken"),
            CredentialField.token("refreshToken", "ecom.label.cred.refreshToken"),
            CredentialField.token("shopCipher", "ecom.label.cred.tiktok.shopCipher"),
            CredentialField.oauthInfo("shopId", "ecom.label.cred.tiktok.shopId"),
            CredentialField.oauthInfo("sellerName", "ecom.label.cred.tiktok.sellerName"),
            CredentialField.cached("accessTokenExpireIn", false),
            CredentialField.cached("refreshTokenExpireIn", false),
            CredentialField.cached("shopRegion", false)),
            API_BASE, API_BASE,
            EnumSet.of(ConnectorCapability.ORDERS, ConnectorCapability.SHIP_CONFIRM, ConnectorCapability.INVENTORY_PUSH,
                    ConnectorCapability.LISTINGS),
            "https://partner.tiktokshop.com/docv2").withRedirectNote("ecom.text.connector.redirect.tiktok");

    @Override
    public ConnectorInfo info() {
        return INFO;
    }

    // ---------------------------------------------------------------- 签名

    /**
     * 签名：app_secret + path + 排序后的 k+v（不含 sign、access_token）+ body + app_secret，HMAC-SHA256(app_secret) 小写十六进制
     *
     * @param body 请求体 JSON 原文，GET / multipart 为 null
     */
    static String sign(String appSecret, String path, Map<String, String> query, String body) {
        TreeMap<String, String> sorted = new TreeMap<>(query);
        sorted.remove("sign");
        sorted.remove("access_token");
        String base = appSecret + path + ConnectorSigns.concatSorted(sorted) + (body == null ? "" : body) + appSecret;
        return ConnectorSigns.hmacSha256Hex(appSecret, base);
    }

    // ---------------------------------------------------------------- 授权

    @Override
    public String authorizeUrl(ConnectorContext ctx, String redirectUri, String state) {
        String base = ctx.baseUrlOverride() != null ? ctx.baseUrlOverride() : AUTHORIZE_BASE;
        Map<String, String> q = new LinkedHashMap<>();
        q.put("service_id", ctx.credentials().require("serviceId"));
        q.put("state", state);
        return base + "/open/authorize?" + ConnectorSigns.query(q);
    }

    @Override
    public OAuthResult exchangeCode(ConnectorContext ctx, Map<String, String> params, String redirectUri) {
        String code = params.get("code");
        if (code == null || code.isBlank()) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, "code missing");
        }
        StoreCredentials c = ctx.credentials();
        Map<String, String> q = new LinkedHashMap<>();
        q.put("app_key", c.require("appKey"));
        q.put("app_secret", c.require("appSecret"));
        q.put("auth_code", code);
        q.put("grant_type", "authorized_code");
        Map<String, String> creds = tokenFields(tokenCall(ctx, "/api/v2/token/get", q));
        // 用新令牌查询授权的店铺，取 shop_cipher
        JsonNode shops = call(ctx, c, creds.get("accessToken"), "GET", "/authorization/202309/shops", new LinkedHashMap<>(), null, false)
                .path("shops");
        String wantedShop = c.get("shopId");
        JsonNode chosen = null;
        for (JsonNode s : shops) {
            if (chosen == null || (wantedShop != null && wantedShop.equals(HttpJson.text(s, "id")))) {
                chosen = s;
            }
        }
        if (chosen == null) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, "no authorized shop");
        }
        creds.put("shopCipher", HttpJson.text(chosen, "cipher"));
        creds.put("shopId", HttpJson.text(chosen, "id"));
        String region = HttpJson.text(chosen, "region");
        if (region != null) {
            creds.put("shopRegion", region);
        }
        if (creds.get("sellerName") == null && HttpJson.text(chosen, "name") != null) {
            creds.put("sellerName", HttpJson.text(chosen, "name"));
        }
        return new OAuthResult(creds, creds.get("shopId"));
    }

    @Override
    public void ensureToken(ConnectorContext ctx) {
        StoreCredentials c = ctx.credentials();
        long expireAt = ConnectorJson.parseLong(c.get("accessTokenExpireIn"), 0);
        if (c.get("accessToken") != null && expireAt - REFRESH_AHEAD_SECONDS > Instant.now().getEpochSecond()) {
            return;
        }
        Map<String, String> q = new LinkedHashMap<>();
        q.put("app_key", c.require("appKey"));
        q.put("app_secret", c.require("appSecret"));
        q.put("refresh_token", c.require("refreshToken"));
        q.put("grant_type", "refresh_token");
        ctx.updateCredentials(tokenFields(tokenCall(ctx, "/api/v2/token/refresh", q)));
    }

    private static JsonNode tokenCall(ConnectorContext ctx, String path, Map<String, String> q) {
        String base = ctx.baseUrlOverride() != null ? ctx.baseUrlOverride() : AUTH_BASE;
        JsonNode resp = HttpJson.get(base + path + "?" + ConnectorSigns.query(q), Map.of(), PLATFORM);
        if (resp.path("code").asLong(-1) != 0 || HttpJson.text(resp.path("data"), "access_token") == null) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, errorSummary(resp));
        }
        return resp.path("data");
    }

    private static Map<String, String> tokenFields(JsonNode data) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("accessToken", HttpJson.text(data, "access_token"));
        m.put("refreshToken", HttpJson.text(data, "refresh_token"));
        m.put("accessTokenExpireIn", HttpJson.text(data, "access_token_expire_in"));
        if (HttpJson.text(data, "refresh_token_expire_in") != null) {
            m.put("refreshTokenExpireIn", HttpJson.text(data, "refresh_token_expire_in"));
        }
        if (HttpJson.text(data, "seller_name") != null) {
            m.put("sellerName", HttpJson.text(data, "seller_name"));
        }
        return m;
    }

    // ---------------------------------------------------------------- 订单

    @Override
    public List<OrderDraft> fetchOrders(ConnectorContext ctx, LocalDateTime since) {
        ensureToken(ctx);
        long from = ConnectorJson.toUnixSeconds(since == null ? LocalDateTime.now().minusDays(3) : since);
        Map<String, Object> body = Map.of("update_time_ge", from);
        List<OrderDraft> drafts = new ArrayList<>();
        String pageToken = null;
        int pages = 0;
        do {
            Map<String, String> q = new LinkedHashMap<>();
            q.put("page_size", String.valueOf(PAGE_SIZE));
            q.put("sort_field", "update_time");
            q.put("sort_order", "ASC");
            if (pageToken != null) {
                q.put("page_token", pageToken);
            }
            JsonNode data = shopCall(ctx, "POST", "/order/202309/orders/search", q, body);
            for (JsonNode o : data.path("orders")) {
                drafts.add(toDraft(ctx.store(), o));
            }
            pageToken = HttpJson.text(data, "next_page_token");
            pages++;
        } while (pageToken != null && !pageToken.isBlank() && pages < MAX_PAGES);
        if (pageToken != null && !pageToken.isBlank()) {
            ConnectorJson.warnPageLimit(PLATFORM, "orders", MAX_PAGES);
        }
        return drafts;
    }

    static OrderDraft toDraft(EcomStore store, JsonNode o) {
        OrderDraft d = new OrderDraft();
        d.setStoreId(store.getId());
        d.setPlatformOrderNo(HttpJson.text(o, "id"));
        d.setOrderTime(epochSeconds(o.path("create_time")));
        d.setPaidTime(epochSeconds(o.path("paid_time")));
        d.setShipBy(epochSeconds(o.path("rts_sla_time")));
        d.setDeliverBy(epochSeconds(o.path("delivery_due_time")));
        d.setStatus(status(HttpJson.text(o, "status")));
        d.setFulfillment(FBT.equals(HttpJson.text(o, "fulfillment_type")) ? FulfillmentType.PLATFORM : FulfillmentType.SELLER);
        JsonNode payment = o.path("payment");
        d.setCurrency(HttpJson.text(payment, "currency"));
        d.setBuyerEmail(HttpJson.text(o, "buyer_email"));
        d.setNote(HttpJson.text(o, "buyer_message"));
        JsonNode addr = o.path("recipient_address");
        d.setShipName(HttpJson.text(addr, "name"));
        d.setBuyerName(HttpJson.text(addr, "name"));
        d.setShipPhone(HttpJson.text(addr, "phone_number"));
        d.setShipCountry(HttpJson.text(addr, "region_code"));
        d.setShipPostcode(HttpJson.text(addr, "postal_code"));
        Map<String, String> levels = new LinkedHashMap<>();
        for (JsonNode di : addr.path("district_info")) {
            levels.put(HttpJson.text(di, "address_level"), HttpJson.text(di, "address_name"));
        }
        d.setShipState(levels.get("L1"));
        d.setShipCity(levels.get("L3") != null ? levels.get("L3") : levels.get("L2"));
        String lines = ConnectorJson.join(HttpJson.text(addr, "address_line1"), HttpJson.text(addr, "address_line2"),
                HttpJson.text(addr, "address_line3"), HttpJson.text(addr, "address_line4"));
        d.setShipAddress(lines != null ? lines : HttpJson.text(addr, "full_address"));
        for (JsonNode li : o.path("line_items")) {
            d.getLines().add(toLine(li));
            if (d.getCurrency() == null) {
                d.setCurrency(HttpJson.text(li, "currency"));
            }
        }
        d.setShippingAmount(HttpJson.decimal(payment, "shipping_fee"));
        d.setTaxAmount(HttpJson.decimal(payment, "tax"));
        return d;
    }

    private static OrderDraft.Line toLine(JsonNode li) {
        OrderDraft.Line l = new OrderDraft.Line();
        l.setPlatformLineId(HttpJson.text(li, "id"));
        String productId = HttpJson.text(li, "product_id");
        String skuId = HttpJson.text(li, "sku_id");
        l.setPlatformItemId(productId == null ? skuId : skuId == null ? productId : productId + ":" + skuId);
        l.setSellerSku(ConnectorJson.firstNonBlank(HttpJson.text(li, "seller_sku"), skuId, l.getPlatformItemId()));
        String skuName = HttpJson.text(li, "sku_name");
        String name = HttpJson.text(li, "product_name");
        l.setTitle(ConnectorJson.join(name, skuName));
        l.setQty(BigDecimal.ONE);
        BigDecimal original = HttpJson.decimal(li, "original_price");
        if (original == null) {
            original = HttpJson.decimal(li, "sale_price");
        }
        l.setPrice(original);
        l.setAmount(original);
        BigDecimal sellerDiscount = HttpJson.decimal(li, "seller_discount");
        l.setDiscount(sellerDiscount != null && sellerDiscount.signum() > 0 ? sellerDiscount : null);
        return l;
    }

    static EcomOrderStatus status(String s) {
        if (s == null) {
            return EcomOrderStatus.UNSHIPPED;
        }
        return switch (s) {
            // ON_HOLD：已付款但在买家可取消的冷静期内，还不能发货
            case "UNPAID", "ON_HOLD" -> EcomOrderStatus.PENDING;
            case "IN_TRANSIT", "DELIVERED", "COMPLETED" -> EcomOrderStatus.SHIPPED;
            case "CANCELLED" -> EcomOrderStatus.CANCELLED;
            // AWAITING_SHIPMENT、PARTIALLY_SHIPPING、AWAITING_COLLECTION 及未知状态
            default -> EcomOrderStatus.UNSHIPPED;
        };
    }

    // ---------------------------------------------------------------- 发货

    @Override
    public ShipmentResult confirmShipment(ConnectorContext ctx, EcomOrder order, List<EcomOrderItem> items) {
        if (order.getFulfillment() == FulfillmentType.PLATFORM) {
            return ShipmentResult.skipped("FBT");
        }
        ensureToken(ctx);
        if (order.getTrackingNo() == null || order.getTrackingNo().isBlank()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "tracking number missing");
        }
        String orderId = order.getPlatformOrderNo();
        JsonNode detail = shopCall(ctx, "GET", "/order/202309/orders", new LinkedHashMap<>(Map.of("ids", orderId)), null)
                .path("orders").path(0);
        List<String> lineIds = new ArrayList<>();
        for (EcomOrderItem it : items) {
            if (it.getPlatformLineId() != null) {
                lineIds.add(it.getPlatformLineId());
            }
        }
        if (lineIds.isEmpty()) {
            detail.path("line_items").forEach(li -> lineIds.add(HttpJson.text(li, "id")));
        }
        if (lineIds.isEmpty()) {
            throw new BizException("ecom.error.connectorLineIdMissing", orderId);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_line_item_ids", lineIds);
        body.put("tracking_number", order.getTrackingNo());
        body.put("shipping_provider_id", shippingProviderId(ctx, order.getCarrier(), HttpJson.text(detail, "delivery_option_id")));
        shopCall(ctx, "POST", "/fulfillment/202309/orders/" + ConnectorSigns.enc(orderId) + "/packages", new LinkedHashMap<>(), body);
        return ShipmentResult.confirmed(order.getTrackingNo(), order.getCarrier());
    }

    /** 承运商是纯数字时视为物流商 ID，否则在订单配送方式可用的物流商中按名称匹配 */
    private String shippingProviderId(ConnectorContext ctx, String carrier, String deliveryOptionId) {
        if (carrier != null && carrier.trim().matches("\\d{6,}")) {
            return carrier.trim();
        }
        if (deliveryOptionId == null) {
            throw new BizException("ecom.error.connectorShippingProviderNotFound", PLATFORM, String.valueOf(carrier), "-");
        }
        JsonNode providers = shopCall(ctx, "GET", "/logistics/202309/delivery_options/" + ConnectorSigns.enc(deliveryOptionId) + "/shipping_providers",
                new LinkedHashMap<>(), null).path("shipping_providers");
        List<String> names = new ArrayList<>();
        for (JsonNode p : providers) {
            String name = HttpJson.text(p, "name");
            if (name != null && carrier != null && name.trim().equalsIgnoreCase(carrier.trim())) {
                return HttpJson.text(p, "id");
            }
            names.add(name);
        }
        throw new BizException("ecom.error.connectorShippingProviderNotFound", PLATFORM, String.valueOf(carrier), String.join(", ", names));
    }

    // ---------------------------------------------------------------- 库存

    /**
     * 按商品分组调用 /product/202309/products/{product_id}/inventory/update：商品 ID 取 parentItemId、SKU ID 取 platformItemId
     * （兼容旧编码 "productId:skuId"）；仓库取在线商品附加 ID 的 warehouseId，其次授权信息 warehouseId，都没有时不传（默认仓）
     */
    @Override
    public void pushInventory(ConnectorContext ctx, List<InventoryUpdate> updates) {
        ensureToken(ctx);
        String defaultWarehouse = ctx.credentials().get("warehouseId");
        Map<String, List<Map<String, Object>>> byProduct = new LinkedHashMap<>();
        for (InventoryUpdate u : updates) {
            String[] ids = u.parentAndChild();
            if (ids == null) {
                throw new BizException("ecom.error.connectorPlatformItemIdRequired", PLATFORM, u.sellerSku());
            }
            String warehouseId = u.platformWarehouseId() != null ? u.platformWarehouseId() : defaultWarehouse;
            Map<String, Object> inv = new LinkedHashMap<>();
            if (warehouseId != null) {
                inv.put("warehouse_id", warehouseId);
            }
            inv.put("quantity", u.intQty());
            Map<String, Object> sku = new LinkedHashMap<>();
            sku.put("id", ids[1]);
            sku.put("inventory", List.of(inv));
            byProduct.computeIfAbsent(ids[0], k -> new ArrayList<>()).add(sku);
        }
        for (Map.Entry<String, List<Map<String, Object>>> e : byProduct.entrySet()) {
            shopCall(ctx, "POST", "/product/202309/products/" + ConnectorSigns.enc(e.getKey()) + "/inventory/update", new LinkedHashMap<>(),
                    Map.of("skus", e.getValue()));
        }
    }

    // ---------------------------------------------------------------- 商品

    @Override
    public List<ListingDraft> fetchListings(ConnectorContext ctx) {
        ensureToken(ctx);
        List<ListingDraft> drafts = new ArrayList<>();
        String pageToken = null;
        int pages = 0;
        do {
            Map<String, String> q = new LinkedHashMap<>();
            q.put("page_size", String.valueOf(PAGE_SIZE));
            if (pageToken != null) {
                q.put("page_token", pageToken);
            }
            JsonNode data = shopCall(ctx, "POST", "/product/202309/products/search", q, Map.of("status", "ALL"));
            for (JsonNode p : data.path("products")) {
                String productId = HttpJson.text(p, "id");
                String status = listingStatus(HttpJson.text(p, "status"));
                String title = HttpJson.text(p, "title");
                for (JsonNode s : p.path("skus")) {
                    String skuId = HttpJson.text(s, "id");
                    JsonNode price = s.path("price");
                    BigDecimal amount = HttpJson.decimal(price, "tax_exclusive_price");
                    if (amount == null) {
                        amount = HttpJson.decimal(price, "sale_price");
                    }
                    BigDecimal qty = null;
                    String warehouse = null;
                    for (JsonNode inv : s.path("inventory")) {
                        qty = ConnectorJson.add(qty, HttpJson.decimal(inv, "quantity"));
                        if (warehouse == null) {
                            warehouse = HttpJson.text(inv, "warehouse_id");
                        }
                    }
                    Map<String, String> extra = new LinkedHashMap<>();
                    extra.put(ListingDraft.EXTRA_WAREHOUSE_ID, warehouse);
                    drafts.add(new ListingDraft(ConnectorJson.firstNonBlank(HttpJson.text(s, "seller_sku"), skuId, productId), skuId, productId, title,
                            null, amount, HttpJson.text(price, "currency"), qty, null, status, null, null, extra));
                }
            }
            pageToken = HttpJson.text(data, "next_page_token");
            pages++;
        } while (pageToken != null && !pageToken.isBlank() && pages < MAX_PAGES);
        if (pageToken != null && !pageToken.isBlank()) {
            ConnectorJson.warnPageLimit(PLATFORM, "products", MAX_PAGES);
        }
        return drafts;
    }

    static String listingStatus(String s) {
        if (s == null) {
            return null;
        }
        ListingStatus st = switch (s) {
            case "ACTIVATE" -> ListingStatus.ACTIVE;
            case "SELLER_DEACTIVATED", "PLATFORM_DEACTIVATED", "FREEZE" -> ListingStatus.INACTIVE;
            case "DRAFT", "PENDING", "FAILED" -> ListingStatus.INCOMPLETE;
            case "DELETED" -> ListingStatus.DELETED;
            default -> null;
        };
        return st == null ? null : st.name();
    }

    // ---------------------------------------------------------------- HTTP

    /** 店铺接口：带 shop_cipher 与当前令牌，返回 data 节点 */
    private static JsonNode shopCall(ConnectorContext ctx, String method, String path, Map<String, String> query, Object body) {
        StoreCredentials c = ctx.credentials();
        return call(ctx, c, c.require("accessToken"), method, path, query, body, true);
    }

    private static JsonNode call(ConnectorContext ctx, StoreCredentials c, String token, String method, String path,
                                 Map<String, String> query, Object body, boolean shop) {
        Map<String, String> q = new LinkedHashMap<>(query);
        q.put("app_key", c.require("appKey"));
        q.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));
        if (shop) {
            q.put("shop_cipher", c.require("shopCipher"));
        }
        // 请求体只序列化一次：签名原文与发送的报文逐字一致
        String json = body == null ? null : HttpJson.MAPPER.writeValueAsString(body);
        q.put("sign", sign(c.require("appSecret"), path, q, json));
        String url = ctx.baseUrl(INFO) + path + "?" + ConnectorSigns.query(q);
        Map<String, String> headers = Map.of("x-tts-access-token", token, "Content-Type", "application/json");
        JsonNode resp = json == null && "GET".equals(method)
                ? HttpJson.get(url, headers, PLATFORM)
                : HttpJson.requestRaw(method, url, headers, json, "application/json", PLATFORM);
        if (resp.path("code").asLong(-1) != 0) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, errorSummary(resp));
        }
        return resp.path("data");
    }

    private static String errorSummary(JsonNode resp) {
        return HttpJson.abbreviate(HttpJson.text(resp, "code") + ": " + HttpJson.text(resp, "message"));
    }

    // ---------------------------------------------------------------- 工具

    static LocalDateTime epochSeconds(JsonNode v) {
        return ConnectorJson.unixSeconds(v);
    }
}
