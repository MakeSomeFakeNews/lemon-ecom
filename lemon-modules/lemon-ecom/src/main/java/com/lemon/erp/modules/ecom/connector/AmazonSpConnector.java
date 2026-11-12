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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 亚马逊 Selling Partner API：LWA 刷新令牌换取访问令牌后调用
 * <ul>
 *   <li>订单（Orders API v0）：getOrders 按 LastUpdatedAfter + NextToken 分页、getOrderItems；FulfillmentChannel = AFN（FBA）映射为平台仓配送</li>
 *   <li>回传发货：confirmShipment</li>
 *   <li>回传库存（Listings Items API 2021-08-01）：patchListingsItem 替换 fulfillment_availability（渠道 DEFAULT，即自发货库存），
 *       productType 用通用值 PRODUCT（只改报价属性时亚马逊接受）；FBA 库存由亚马逊维护，不回传</li>
 *   <li>在线商品：searchListingsItems（includedData = summaries,offers,fulfillmentAvailability，pageToken 分页，每页 20）</li>
 * </ul>
 * 授权信息：region（NA / EU / FE）、marketplaceId、clientId、clientSecret、refreshToken，
 * sellingPartnerId（卖家 ID / Merchant Token，Listings 接口路径需要；店铺上填写了卖家 ID 时优先用店铺的）。
 * <p>
 * 地址：店铺填写了自定义地址时业务接口与 LWA 令牌接口都指向它（令牌为 {自定义地址}/auth/o2/token），否则按区域与沙箱开关选择。
 * 访问令牌（约 1 小时有效）缓存在授权信息 accessToken / accessTokenExpiresAt（毫秒）中，提前 60 秒刷新；
 * accessTokenOwner 记录令牌对应的 refreshToken 摘要，卖家更换 refreshToken 后立即重新换取。
 * <p>
 * 编码约定：订单明细 sellerSku = SellerSKU（亚马逊必有），platformItemId = ASIN；在线商品 platformItemId = ASIN，
 * fulfillment 按 fulfillmentAvailability 的渠道（DEFAULT 自发货，AMAZON_* 平台仓），status：summaries.status 含 BUYABLE 为在售，
 * 其他为下架，没有 summaries 为信息不完整。
 * <p>
 * 时间：LastUpdatedAfter、shipDate 按服务器时区换算为 UTC。FBA（AFN）订单回传发货时跳过。
 * 限流返回的 429 由 {@link HttpJson} 自动退避重试（亚马逊不带 Retry-After，按 1、2、4 秒）。
 * <p>
 * 说明：接口按亚马逊公开文档实现，尚未用真实卖家账号联调；getOrders 限速约每分钟 1 次（突发 20），定时同步间隔不要过短；
 * patchListingsItem 限速每秒 5 次，库存回传逐个 SKU 调用，部分失败时其余 SKU 照常提交，最后汇总报错。需要真实账号联调确认：
 * <ul>
 *   <li>不使用 RDT（受限数据令牌）时 ShippingAddress / BuyerInfo 是否返回（多数情况下需要 RDT 才有收件人信息）</li>
 *   <li>patchListingsItem 用通用 productType = PRODUCT 是否被接受</li>
 *   <li>getOrders 每次最多 {@value #MAX_ORDER_PAGES} 页（约 2000 单），大卖家需要缩短同步间隔；getOrderItems 每单一次调用的限速</li>
 *   <li>confirmShipment 的 carrierCode 取值（亚马逊承运商代码表），以及 packageReferenceId 多包裹的处理</li>
 * </ul>
 */
@Component
public class AmazonSpConnector implements ChannelConnector {

    static final String PLATFORM = "Amazon";
    private static final String LWA_URL = "https://api.amazon.com/auth/o2/token";
    private static final Map<String, String> ENDPOINTS = Map.of(
            "NA", "https://sellingpartnerapi-na.amazon.com",
            "EU", "https://sellingpartnerapi-eu.amazon.com",
            "FE", "https://sellingpartnerapi-fe.amazon.com");
    /** SP-API 静态沙箱（返回固定示例数据，用于验证授权和报文） */
    private static final Map<String, String> SANDBOX_ENDPOINTS = Map.of(
            "NA", "https://sandbox.sellingpartnerapi-na.amazon.com",
            "EU", "https://sandbox.sellingpartnerapi-eu.amazon.com",
            "FE", "https://sandbox.sellingpartnerapi-fe.amazon.com");
    private static final ConnectorInfo INFO = new ConnectorInfo(EcomPlatform.AMAZON, AuthType.KEYS, List.of(
            CredentialField.text("region", "ecom.label.cred.amazonRegion", true),
            CredentialField.text("marketplaceId", "ecom.label.cred.marketplaceId", true),
            CredentialField.text("clientId", "ecom.label.cred.clientId", true),
            CredentialField.secret("clientSecret", "ecom.label.cred.clientSecret"),
            CredentialField.secret("refreshToken", "ecom.label.cred.refreshToken"),
            CredentialField.text("sellingPartnerId", "ecom.label.cred.amazonSellingPartnerId", false),
            CredentialField.cached("accessToken", true),
            CredentialField.cached("accessTokenExpiresAt", false),
            CredentialField.cached("accessTokenOwner", false)),
            ENDPOINTS.get("NA"), SANDBOX_ENDPOINTS.get("NA"),
            java.util.EnumSet.of(ConnectorCapability.ORDERS, ConnectorCapability.SHIP_CONFIRM, ConnectorCapability.INVENTORY_PUSH,
                    ConnectorCapability.LISTINGS),
            "https://developer-docs.amazon.com/sp-api/").withOrderWindowNote("ecom.text.connector.window.amazon");
    private static final String LISTINGS_PATH = "/listings/2021-08-01/items/";
    private static final int LISTING_PAGE_SIZE = 20;
    private static final int MAX_LISTING_PAGES = 250;
    private static final int MAX_ORDER_PAGES = 20;
    /** 提前刷新访问令牌的秒数 */
    private static final long TOKEN_SKEW_SECONDS = 60;
    /** 商品详情页域名（按 marketplaceId，未列出的不生成链接） */
    private static final Map<String, String> MARKETPLACE_DOMAINS = Map.ofEntries(
            Map.entry("ATVPDKIKX0DER", "www.amazon.com"), Map.entry("A2EUQ1WTGCTBG2", "www.amazon.ca"),
            Map.entry("A1AM78C64UM0Y8", "www.amazon.com.mx"), Map.entry("A2Q3Y263D00KWC", "www.amazon.com.br"),
            Map.entry("A1F83G8C2ARO7P", "www.amazon.co.uk"), Map.entry("A1PA6795UKMFR9", "www.amazon.de"),
            Map.entry("A13V1IB3VIYZZH", "www.amazon.fr"), Map.entry("APJ6JRA9NG5V4", "www.amazon.it"),
            Map.entry("A1RKKUPIHCS9HS", "www.amazon.es"), Map.entry("A1805IZSGTT6HS", "www.amazon.nl"),
            Map.entry("A2NODRKZP88ZB9", "www.amazon.se"), Map.entry("A1C3SOZRARQ6R3", "www.amazon.pl"),
            Map.entry("A33AVAJ2PDY3EV", "www.amazon.com.tr"), Map.entry("A2VIGQ35RCS4UG", "www.amazon.ae"),
            Map.entry("A17E79C6D8DWNP", "www.amazon.sa"), Map.entry("A21TJRUUN4KGV", "www.amazon.in"),
            Map.entry("A1VC38T7YXB528", "www.amazon.co.jp"), Map.entry("A39IBJ37TRP1C6", "www.amazon.com.au"),
            Map.entry("A19VAU5U5O7RUS", "www.amazon.sg"));

    @Override
    public ConnectorInfo info() {
        return INFO;
    }

    @Override
    public List<OrderDraft> fetchOrders(ConnectorContext ctx, LocalDateTime since) {
        EcomStore store = ctx.store();
        StoreCredentials c = ctx.credentials();
        String endpoint = endpoint(ctx);
        Map<String, String> headers = headers(ctx);
        String marketplace = c.require("marketplaceId");
        String after = ConnectorJson.toUtcIso(since);
        List<OrderDraft> drafts = new ArrayList<>();
        String nextToken = null;
        int pages = 0;
        do {
            StringBuilder url = new StringBuilder(endpoint).append("/orders/v0/orders?MarketplaceIds=").append(enc(marketplace));
            if (nextToken != null) {
                url.append("&NextToken=").append(enc(nextToken));
            } else {
                url.append("&LastUpdatedAfter=").append(enc(after));
            }
            JsonNode payload = HttpJson.get(url.toString(), headers, PLATFORM).path("payload");
            for (JsonNode o : payload.path("Orders")) {
                OrderDraft d = toDraft(store, o);
                for (JsonNode item : orderItems(endpoint, headers, d.getPlatformOrderNo())) {
                    d.getLines().add(toLine(item));
                }
                drafts.add(d);
            }
            nextToken = HttpJson.text(payload, "NextToken");
            pages++;
        } while (nextToken != null && pages < MAX_ORDER_PAGES);
        if (nextToken != null) {
            ConnectorJson.warnPageLimit(PLATFORM, "orders", MAX_ORDER_PAGES);
        }
        return drafts;
    }

    private List<JsonNode> orderItems(String endpoint, Map<String, String> headers, String orderId) {
        List<JsonNode> items = new ArrayList<>();
        String nextToken = null;
        int pages = 0;
        do {
            String url = endpoint + "/orders/v0/orders/" + path(orderId) + "/orderItems" + (nextToken == null ? "" : "?NextToken=" + enc(nextToken));
            JsonNode payload = HttpJson.get(url, headers, PLATFORM).path("payload");
            payload.path("OrderItems").forEach(items::add);
            nextToken = HttpJson.text(payload, "NextToken");
            pages++;
        } while (nextToken != null && pages < MAX_ORDER_PAGES);
        return items;
    }

    private static OrderDraft toDraft(EcomStore store, JsonNode o) {
        OrderDraft d = new OrderDraft();
        d.setStoreId(store.getId());
        d.setPlatformOrderNo(HttpJson.text(o, "AmazonOrderId"));
        d.setOrderTime(time(HttpJson.text(o, "PurchaseDate")));
        d.setStatus(status(HttpJson.text(o, "OrderStatus")));
        d.setFulfillment("AFN".equals(HttpJson.text(o, "FulfillmentChannel")) ? FulfillmentType.PLATFORM : FulfillmentType.SELLER);
        d.setCurrency(HttpJson.text(o.path("OrderTotal"), "CurrencyCode"));
        d.setShipBy(time(HttpJson.text(o, "LatestShipDate")));
        d.setDeliverBy(time(HttpJson.text(o, "LatestDeliveryDate")));
        JsonNode addr = o.path("ShippingAddress");
        d.setShipName(HttpJson.text(addr, "Name"));
        d.setShipPhone(HttpJson.text(addr, "Phone"));
        d.setShipCountry(HttpJson.text(addr, "CountryCode"));
        d.setShipState(HttpJson.text(addr, "StateOrRegion"));
        d.setShipCity(HttpJson.text(addr, "City"));
        d.setShipPostcode(HttpJson.text(addr, "PostalCode"));
        d.setShipAddress(ConnectorJson.join(HttpJson.text(addr, "AddressLine1"), HttpJson.text(addr, "AddressLine2"), HttpJson.text(addr, "AddressLine3")));
        d.setBuyerName(HttpJson.text(o.path("BuyerInfo"), "BuyerName"));
        d.setBuyerEmail(HttpJson.text(o.path("BuyerInfo"), "BuyerEmail"));
        return d;
    }

    private static OrderDraft.Line toLine(JsonNode item) {
        OrderDraft.Line l = new OrderDraft.Line();
        l.setPlatformLineId(HttpJson.text(item, "OrderItemId"));
        l.setSellerSku(HttpJson.text(item, "SellerSKU"));
        l.setPlatformItemId(HttpJson.text(item, "ASIN"));
        l.setTitle(HttpJson.text(item, "Title"));
        java.math.BigDecimal qty = HttpJson.decimal(item, "QuantityOrdered");
        l.setQty(qty == null || qty.signum() <= 0 ? java.math.BigDecimal.ONE : qty);
        java.math.BigDecimal amount = HttpJson.decimal(item, "ItemPrice", "Amount");
        l.setAmount(amount);
        l.setPrice(amount == null || l.getQty().signum() == 0 ? java.math.BigDecimal.ZERO : amount.divide(l.getQty(), 4, java.math.RoundingMode.HALF_UP));
        l.setShipping(HttpJson.decimal(item, "ShippingPrice", "Amount"));
        java.math.BigDecimal tax = HttpJson.decimal(item, "ItemTax", "Amount");
        java.math.BigDecimal shipTax = HttpJson.decimal(item, "ShippingTax", "Amount");
        l.setTax(ConnectorJson.add(tax, shipTax));
        java.math.BigDecimal promo = HttpJson.decimal(item, "PromotionDiscount", "Amount");
        java.math.BigDecimal shipPromo = HttpJson.decimal(item, "ShippingDiscount", "Amount");
        l.setDiscount(ConnectorJson.add(promo, shipPromo));
        return l;
    }

    @Override
    public ShipmentResult confirmShipment(ConnectorContext ctx, EcomOrder order, List<EcomOrderItem> items) {
        if (order.getFulfillment() == FulfillmentType.PLATFORM) {
            return ShipmentResult.skipped("AFN");
        }
        StoreCredentials c = ctx.credentials();
        String endpoint = endpoint(ctx);
        Map<String, String> headers = headers(ctx);
        List<Map<String, Object>> lines = new ArrayList<>();
        for (EcomOrderItem it : items) {
            if (it.getPlatformLineId() == null) {
                throw new BizException("ecom.error.connectorLineIdMissing", order.getPlatformOrderNo());
            }
            lines.add(Map.of("orderItemId", it.getPlatformLineId(), "quantity", it.getQty() == null ? 1 : Math.max(1, it.getQty().intValue())));
        }
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("packageReferenceId", "1");
        pkg.put("carrierCode", order.getCarrier());
        pkg.put("trackingNumber", order.getTrackingNo());
        pkg.put("shipDate", ConnectorJson.toUtcIso(order.getShipTime() == null ? LocalDateTime.now() : order.getShipTime()));
        pkg.put("orderItems", lines);
        Map<String, Object> body = new HashMap<>();
        body.put("marketplaceId", c.require("marketplaceId"));
        body.put("packageDetail", pkg);
        HttpJson.postJson(endpoint + "/orders/v0/orders/" + path(order.getPlatformOrderNo()) + "/shipmentConfirmation", headers, body, PLATFORM);
        return ShipmentResult.confirmed(order.getTrackingNo(), order.getCarrier());
    }

    // ---------------------------------------------------------------- 库存与在线商品

    @Override
    public void pushInventory(ConnectorContext ctx, List<InventoryUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        String endpoint = endpoint(ctx);
        Map<String, String> headers = headers(ctx);
        String sellerId = sellerId(ctx);
        String marketplace = ctx.credentials().require("marketplaceId");
        List<String> failures = new ArrayList<>();
        for (InventoryUpdate u : updates) {
            int qty = u.intQty();
            Map<String, Object> availability = new LinkedHashMap<>();
            availability.put("fulfillment_channel_code", "DEFAULT");
            availability.put("quantity", qty);
            Map<String, Object> patch = new LinkedHashMap<>();
            patch.put("op", "replace");
            patch.put("path", "/attributes/fulfillment_availability");
            patch.put("value", List.of(availability));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("productType", "PRODUCT");
            body.put("patches", List.of(patch));
            String url = endpoint + LISTINGS_PATH + path(sellerId) + "/" + path(u.sellerSku()) + "?marketplaceIds=" + enc(marketplace);
            try {
                JsonNode resp = HttpJson.request("PATCH", url, headers, body, PLATFORM);
                if (!"ACCEPTED".equals(HttpJson.text(resp, "status"))) {
                    failures.add(u.sellerSku() + ": " + issues(resp));
                }
            } catch (BizException e) {
                failures.add(u.sellerSku() + ": " + e.getMessage());
            }
        }
        if (!failures.isEmpty()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(String.join("; ", failures)));
        }
    }

    private static String issues(JsonNode resp) {
        List<String> list = new ArrayList<>();
        for (JsonNode i : resp.path("issues")) {
            list.add(HttpJson.text(i, "code") + " " + HttpJson.text(i, "message"));
        }
        return list.isEmpty() ? resp.toString() : String.join(", ", list);
    }

    @Override
    public List<ListingDraft> fetchListings(ConnectorContext ctx) {
        String endpoint = endpoint(ctx);
        Map<String, String> headers = headers(ctx);
        String sellerId = sellerId(ctx);
        String marketplace = ctx.credentials().require("marketplaceId");
        String currency = ctx.store() == null ? null : ctx.store().getCurrency();
        List<ListingDraft> drafts = new ArrayList<>();
        String pageToken = null;
        int pages = 0;
        do {
            StringBuilder url = new StringBuilder(endpoint).append(LISTINGS_PATH).append(path(sellerId))
                    .append("?marketplaceIds=").append(enc(marketplace))
                    .append("&includedData=").append(enc("summaries,offers,fulfillmentAvailability"))
                    .append("&pageSize=").append(LISTING_PAGE_SIZE);
            if (pageToken != null) {
                url.append("&pageToken=").append(enc(pageToken));
            }
            JsonNode resp = HttpJson.get(url.toString(), headers, PLATFORM);
            for (JsonNode item : resp.path("items")) {
                drafts.add(toListing(item, marketplace, currency));
            }
            pageToken = HttpJson.text(resp.path("pagination"), "nextToken");
            pages++;
        } while (pageToken != null && pages < MAX_LISTING_PAGES);
        return drafts;
    }

    private static ListingDraft toListing(JsonNode item, String marketplace, String storeCurrency) {
        JsonNode summary = forMarketplace(item.path("summaries"), marketplace);
        JsonNode offer = null;
        for (JsonNode o : item.path("offers")) {
            if ((marketplace.equals(HttpJson.text(o, "marketplaceId")) || HttpJson.text(o, "marketplaceId") == null)
                    && (offer == null || "B2C".equals(HttpJson.text(o, "offerType")))) {
                offer = o;
            }
        }
        String fulfillment = null;
        java.math.BigDecimal qty = null;
        for (JsonNode f : item.path("fulfillmentAvailability")) {
            String channel = HttpJson.text(f, "fulfillmentChannelCode");
            boolean seller = "DEFAULT".equals(channel);
            if (fulfillment == null || seller) {
                fulfillment = seller ? FulfillmentType.SELLER.name() : FulfillmentType.PLATFORM.name();
                qty = HttpJson.decimal(f, "quantity");
            }
        }
        String asin = summary == null ? null : HttpJson.text(summary, "asin");
        String status;
        if (summary == null) {
            status = ListingStatus.INCOMPLETE.name();
        } else {
            boolean buyable = false;
            for (JsonNode st : summary.path("status")) {
                buyable |= "BUYABLE".equals(st.asString());
            }
            status = buyable ? ListingStatus.ACTIVE.name() : ListingStatus.INACTIVE.name();
        }
        String domain = MARKETPLACE_DOMAINS.get(marketplace);
        String currency = offer == null ? null : HttpJson.text(offer.path("price"), "currencyCode");
        return new ListingDraft(HttpJson.text(item, "sku"), asin, null,
                summary == null ? null : HttpJson.text(summary, "itemName"), null,
                offer == null ? null : HttpJson.decimal(offer, "price", "amount"), currency == null ? storeCurrency : currency, qty,
                fulfillment, status, summary == null ? null : HttpJson.text(summary.path("mainImage"), "link"),
                asin == null || domain == null ? null : "https://" + domain + "/dp/" + asin);
    }

    private static JsonNode forMarketplace(JsonNode list, String marketplace) {
        JsonNode first = null;
        for (JsonNode n : list) {
            if (marketplace.equals(HttpJson.text(n, "marketplaceId"))) {
                return n;
            }
            if (first == null) {
                first = n;
            }
        }
        return first;
    }

    /** Listings 接口路径中的卖家 ID：店铺上的卖家 ID 优先，否则取授权信息 sellingPartnerId */
    private static String sellerId(ConnectorContext ctx) {
        String id = ctx.store() == null ? null : ctx.store().getSellerId();
        return id != null && !id.isBlank() ? id.trim() : ctx.credentials().require("sellingPartnerId");
    }

    // ---------------------------------------------------------------- 令牌与地址

    private Map<String, String> headers(ConnectorContext ctx) {
        ensureToken(ctx);
        return Map.of("x-amz-access-token", ctx.credentials().require("accessToken"), "Accept", "application/json");
    }

    /** LWA：refresh_token 换 access_token，缓存到授权信息，快过期或 refreshToken 变更后重新换取 */
    @Override
    public void ensureToken(ConnectorContext ctx) {
        StoreCredentials c = ctx.credentials();
        String owner = tokenOwner(c.require("refreshToken"), c.require("clientId"));
        String expires = c.get("accessTokenExpiresAt");
        if (c.get("accessToken") != null && owner.equals(c.get("accessTokenOwner")) && expires != null
                && parseLong(expires) - TOKEN_SKEW_SECONDS * 1000 > System.currentTimeMillis()) {
            return;
        }
        String form = "grant_type=refresh_token&refresh_token=" + enc(c.require("refreshToken")) + "&client_id=" + enc(c.require("clientId"))
                + "&client_secret=" + enc(c.require("clientSecret"));
        JsonNode resp = HttpJson.postForm(tokenUrl(ctx), Map.of(), form, PLATFORM);
        String token = HttpJson.text(resp, "access_token");
        if (token == null) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, HttpJson.abbreviate(resp.toString()));
        }
        long expiresIn = resp.path("expires_in").asLong(3600);
        Map<String, String> changes = new HashMap<>();
        changes.put("accessToken", token);
        changes.put("accessTokenExpiresAt", String.valueOf(System.currentTimeMillis() + expiresIn * 1000));
        changes.put("accessTokenOwner", owner);
        ctx.updateCredentials(changes);
    }

    /** LWA 令牌地址：有自定义地址时为 {自定义地址}/auth/o2/token（模拟服务器、私有网关），否则为亚马逊 LWA */
    static String tokenUrl(ConnectorContext ctx) {
        return ctx.baseUrlOverride() != null ? ctx.baseUrlOverride() + "/auth/o2/token" : LWA_URL;
    }

    private static String tokenOwner(String refreshToken, String clientId) {
        return ConnectorSigns.sha256Hex(clientId + "|" + refreshToken).substring(0, 16);
    }

    private static long parseLong(String s) {
        return ConnectorJson.parseLong(s, 0);
    }

    private static String endpoint(ConnectorContext ctx) {
        if (ctx.baseUrlOverride() != null) {
            return ctx.baseUrlOverride();
        }
        StoreCredentials c = ctx.credentials();
        String region = c.get("region") == null ? "NA" : c.get("region").toUpperCase();
        String endpoint = (ctx.sandbox() ? SANDBOX_ENDPOINTS : ENDPOINTS).get(region);
        if (endpoint == null) {
            throw new BizException("ecom.error.connectorRegionInvalid", region);
        }
        return endpoint;
    }

    private static EcomOrderStatus status(String s) {
        if (s == null) {
            return EcomOrderStatus.UNSHIPPED;
        }
        return switch (s) {
            case "Pending", "PendingAvailability" -> EcomOrderStatus.PENDING;
            case "Shipped", "InvoiceUnconfirmed" -> EcomOrderStatus.SHIPPED;
            case "Canceled", "Unfulfillable" -> EcomOrderStatus.CANCELLED;
            default -> EcomOrderStatus.UNSHIPPED;
        };
    }

    private static LocalDateTime time(String iso) {
        return ConnectorJson.isoTime(iso);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** 路径段编码（空格为 %20，SKU 中的 / 等字符也要编码） */
    private static String path(String s) {
        return enc(s).replace("+", "%20");
    }
}
