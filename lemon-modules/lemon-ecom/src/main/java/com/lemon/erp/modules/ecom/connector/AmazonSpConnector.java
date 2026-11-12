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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 亚马逊 Selling Partner API（Orders API v0）：LWA 刷新令牌换取访问令牌后调用 getOrders / getOrderItems，
 * 发货回传使用 confirmShipment。授权信息：region（NA / EU / FE）、marketplaceId、clientId、clientSecret、refreshToken。
 * <p>
 * 说明：接口按亚马逊公开文档实现，尚未用真实卖家账号联调；getOrders 限速约每分钟 1 次（突发 20），定时同步间隔不要过短
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
            CredentialField.secret("refreshToken", "ecom.label.cred.refreshToken")),
            ENDPOINTS.get("NA"), SANDBOX_ENDPOINTS.get("NA"),
            java.util.EnumSet.of(ConnectorCapability.ORDERS, ConnectorCapability.SHIP_CONFIRM),
            "https://developer-docs.amazon.com/sp-api/");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public ConnectorInfo info() {
        return INFO;
    }

    @Override
    public List<OrderDraft> fetchOrders(ConnectorContext ctx, LocalDateTime since) {
        EcomStore store = ctx.store();
        StoreCredentials c = ctx.credentials();
        String endpoint = endpoint(ctx);
        String token = accessToken(c);
        Map<String, String> headers = Map.of("x-amz-access-token", token, "Accept", "application/json");
        String marketplace = c.require("marketplaceId");
        String after = since.atOffset(ZoneOffset.UTC).format(ISO);
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
        } while (nextToken != null && pages < 20);
        return drafts;
    }

    private List<JsonNode> orderItems(String endpoint, Map<String, String> headers, String orderId) {
        List<JsonNode> items = new ArrayList<>();
        String nextToken = null;
        do {
            String url = endpoint + "/orders/v0/orders/" + enc(orderId) + "/orderItems" + (nextToken == null ? "" : "?NextToken=" + enc(nextToken));
            JsonNode payload = HttpJson.get(url, headers, PLATFORM).path("payload");
            payload.path("OrderItems").forEach(items::add);
            nextToken = HttpJson.text(payload, "NextToken");
        } while (nextToken != null);
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
        d.setShipAddress(join(HttpJson.text(addr, "AddressLine1"), HttpJson.text(addr, "AddressLine2"), HttpJson.text(addr, "AddressLine3")));
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
        l.setQty(qty == null ? java.math.BigDecimal.ONE : qty);
        java.math.BigDecimal amount = HttpJson.decimal(item, "ItemPrice", "Amount");
        l.setAmount(amount);
        l.setPrice(amount == null || l.getQty().signum() == 0 ? java.math.BigDecimal.ZERO : amount.divide(l.getQty(), 4, java.math.RoundingMode.HALF_UP));
        l.setShipping(HttpJson.decimal(item, "ShippingPrice", "Amount"));
        java.math.BigDecimal tax = HttpJson.decimal(item, "ItemTax", "Amount");
        java.math.BigDecimal shipTax = HttpJson.decimal(item, "ShippingTax", "Amount");
        l.setTax(tax == null ? shipTax : shipTax == null ? tax : tax.add(shipTax));
        java.math.BigDecimal promo = HttpJson.decimal(item, "PromotionDiscount", "Amount");
        java.math.BigDecimal shipPromo = HttpJson.decimal(item, "ShippingDiscount", "Amount");
        l.setDiscount(promo == null ? shipPromo : shipPromo == null ? promo : promo.add(shipPromo));
        return l;
    }

    @Override
    public void confirmShipment(ConnectorContext ctx, EcomOrder order, List<EcomOrderItem> items) {
        StoreCredentials c = ctx.credentials();
        String endpoint = endpoint(ctx);
        Map<String, String> headers = Map.of("x-amz-access-token", accessToken(c), "Accept", "application/json");
        List<Map<String, Object>> lines = new ArrayList<>();
        for (EcomOrderItem it : items) {
            if (it.getPlatformLineId() == null) {
                throw new BizException("ecom.error.connectorLineIdMissing", order.getPlatformOrderNo());
            }
            lines.add(Map.of("orderItemId", it.getPlatformLineId(), "quantity", it.getQty().intValue()));
        }
        Map<String, Object> pkg = new LinkedHashMap<>();
        pkg.put("packageReferenceId", "1");
        pkg.put("carrierCode", order.getCarrier());
        pkg.put("trackingNumber", order.getTrackingNo());
        pkg.put("shipDate", (order.getShipTime() == null ? LocalDateTime.now() : order.getShipTime()).atOffset(ZoneOffset.UTC).format(ISO));
        pkg.put("orderItems", lines);
        Map<String, Object> body = new HashMap<>();
        body.put("marketplaceId", c.require("marketplaceId"));
        body.put("packageDetail", pkg);
        HttpJson.postJson(endpoint + "/orders/v0/orders/" + enc(order.getPlatformOrderNo()) + "/shipmentConfirmation", headers, body, PLATFORM);
    }

    /** LWA：refresh_token 换 access_token */
    private static String accessToken(StoreCredentials c) {
        String form = "grant_type=refresh_token&refresh_token=" + enc(c.require("refreshToken")) + "&client_id=" + enc(c.require("clientId"))
                + "&client_secret=" + enc(c.require("clientSecret"));
        JsonNode resp = HttpJson.postForm(LWA_URL, Map.of(), form, PLATFORM);
        String token = HttpJson.text(resp, "access_token");
        if (token == null) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, HttpJson.abbreviate(resp.toString()));
        }
        return token;
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
        if (iso == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso).atZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (!sb.isEmpty()) sb.append(' ');
                sb.append(p.trim());
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
