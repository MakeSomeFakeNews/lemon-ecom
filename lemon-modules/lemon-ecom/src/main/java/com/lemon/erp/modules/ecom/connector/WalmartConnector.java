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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * 沃尔玛 Marketplace API（美国站）。授权（KEYS）：clientId、clientSecret（Developer Portal 生成的 API Key），
 * 可选 shipNode（多发货节点时回传库存的节点 ID）。
 * <p>
 * 认证：POST {网关}/v3/token（Basic clientId:clientSecret，grant_type=client_credentials）得到约 15 分钟有效的 access_token，
 * {@link #ensureToken} 把 accessToken 与过期时间 accessTokenExpiresAt（毫秒时间戳）缓存到授权信息并 updateCredentials，
 * 过期前 60 秒刷新；业务请求带 WM_SEC.ACCESS_TOKEN、WM_QOS.CORRELATION_ID（每次随机）、WM_SVC.NAME，返回 401 时重新取令牌重试一次。
 * 令牌接口与业务接口在同一网关：正式 marketplace.walmartapis.com、沙箱 sandbox.walmartapis.com，店铺填写自定义地址时都指向它。
 * <p>
 * 接口：订单 GET /v3/orders（lastModifiedStartDate + limit，meta.nextCursor 翻页；默认只返回自发货订单，
 * 因此按 shipNodeType=SellerFulfilled 与 WFSFulfilled 各拉一遍，WFS 订单映射为 PLATFORM）；
 * 发货 POST /v3/orders/{purchaseOrderId}/shipping（按订单行 orderLineStatuses）；
 * 库存 PUT /v3/inventory?sku=…（逐个 SKU；SKU 很多时应改用 feeds 批量）；商品 GET /v3/items（offset + limit）。
 * <p>
 * 编码约定：平台订单号为 purchaseOrderId，订单行 platformLineId 为 lineNumber，sellerSku 取 item.sku（沃尔玛订单行只有卖家 SKU，
 * 没有退路），订单行 platformItemId 为空；在线商品 platformItemId 为 wpid（沃尔玛商品 ID），库存只按 sellerSku 回传。
 * 金额：订单行 charges 中 PRODUCT 计为商品金额（行合计，单价 = 金额 / 数量）、SHIPPING 计为运费，税额为各项 tax 之和。
 * 访问令牌 accessToken / accessTokenExpiresAt 是连接器自己缓存的（{@link CredentialField#cached}），店铺表单不显示。WFS 订单回传发货时跳过。
 * <p>
 * 说明：按公开文档实现，尚未用真实卖家账号联调。需要真实账号联调确认：
 * <ul>
 *   <li>订单列表默认只返回自发货订单、shipNodeType=WFSFulfilled 的取值，以及 nextCursor 的格式（是否以 ? 开头）</li>
 *   <li>承运商枚举（carrierName.carrier）与 otherCarrier 的使用条件，methodCode 是否需要按订单的 shippingInfo.methodCode</li>
 *   <li>商品列表 /v3/items 的 offset 上限（大目录可能需要改用 nextCursor）与 limit 上限</li>
 *   <li>逐个 SKU 的 PUT /v3/inventory 限速；SKU 多时应改为 feeds 批量</li>
 * </ul>
 */
@Component
public class WalmartConnector implements ChannelConnector {

    static final String PLATFORM = "Walmart";
    static final String SVC_NAME = "Walmart Marketplace";
    static final long REFRESH_AHEAD_MILLIS = 60_000L;
    private static final int ORDER_LIMIT = 100;
    private static final int ITEM_LIMIT = 50;
    private static final int MAX_PAGES = 50;
    private static final int MAX_ITEM_PAGES = 200;
    private static final List<String> SHIP_NODE_TYPES = List.of("SellerFulfilled", "WFSFulfilled");
    /** 沃尔玛承运商枚举中的常见值，其他承运商按 otherCarrier 传 */
    private static final Set<String> CARRIERS = Set.of("UPS", "USPS", "FedEx", "Airborne", "OnTrac", "DHL", "LS", "UDS", "UPSMI", "FDX",
            "PILOT", "ESTES", "SAIA", "LaserShip", "Amazon Shipping");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final ConnectorInfo INFO = new ConnectorInfo(EcomPlatform.WALMART, AuthType.KEYS, List.of(
            CredentialField.text("clientId", "ecom.label.cred.walmartClientId", true),
            CredentialField.secret("clientSecret", "ecom.label.cred.walmartClientSecret"),
            CredentialField.text("shipNode", "ecom.label.cred.walmartShipNode", false),
            CredentialField.cached("accessToken", true),
            CredentialField.cached("accessTokenExpiresAt", false)),
            "https://marketplace.walmartapis.com", "https://sandbox.walmartapis.com",
            EnumSet.of(ConnectorCapability.ORDERS, ConnectorCapability.SHIP_CONFIRM, ConnectorCapability.INVENTORY_PUSH,
                    ConnectorCapability.LISTINGS),
            "https://developer.walmart.com/us-marketplace/docs");

    @Override
    public ConnectorInfo info() {
        return INFO;
    }

    // ---------------------------------------------------------------- 令牌

    @Override
    public void ensureToken(ConnectorContext ctx) {
        StoreCredentials c = ctx.credentials();
        String token = c.get("accessToken");
        String expires = c.get("accessTokenExpiresAt");
        if (token != null && expires != null) {
            try {
                if (Long.parseLong(expires) - REFRESH_AHEAD_MILLIS > System.currentTimeMillis()) {
                    return;
                }
            } catch (NumberFormatException ignored) {
                // 过期时间无法解析时重新获取
            }
        }
        String basic = Base64.getEncoder().encodeToString((c.require("clientId") + ":" + c.require("clientSecret")).getBytes(StandardCharsets.UTF_8));
        Map<String, String> headers = Map.of("Authorization", "Basic " + basic, "Accept", "application/json",
                "WM_SVC.NAME", SVC_NAME, "WM_QOS.CORRELATION_ID", UUID.randomUUID().toString());
        JsonNode resp = HttpJson.postForm(ctx.baseUrl(INFO) + "/v3/token", headers, "grant_type=client_credentials", PLATFORM);
        String accessToken = HttpJson.text(resp, "access_token");
        if (accessToken == null) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, HttpJson.abbreviate(resp.toString()));
        }
        long expiresIn = resp.path("expires_in").asLong(900);
        Map<String, String> changes = new HashMap<>();
        changes.put("accessToken", accessToken);
        changes.put("accessTokenExpiresAt", String.valueOf(System.currentTimeMillis() + expiresIn * 1000));
        ctx.updateCredentials(changes);
    }

    /** 执行一个业务请求：先确保令牌有效，返回 401 时清掉缓存的令牌重新获取后重试一次 */
    private JsonNode exec(ConnectorContext ctx, Function<Map<String, String>, JsonNode> request) {
        ensureToken(ctx);
        try {
            return checked(request.apply(headers(ctx)));
        } catch (BizException e) {
            if (!isUnauthorized(e)) {
                throw e;
            }
            Map<String, String> clear = new HashMap<>();
            clear.put("accessToken", null);
            clear.put("accessTokenExpiresAt", null);
            ctx.updateCredentials(clear);
            ensureToken(ctx);
            return checked(request.apply(headers(ctx)));
        }
    }

    private static boolean isUnauthorized(BizException e) {
        return ConnectorHttpException.is(e, 401);
    }

    private static Map<String, String> headers(ConnectorContext ctx) {
        return Map.of("WM_SEC.ACCESS_TOKEN", ctx.credentials().require("accessToken"), "WM_QOS.CORRELATION_ID", UUID.randomUUID().toString(),
                "WM_SVC.NAME", SVC_NAME, "Accept", "application/json");
    }

    /** 2xx 但带 errors 的响应（部分接口如此）也视为失败 */
    private static JsonNode checked(JsonNode resp) {
        JsonNode errors = resp.path("errors");
        if (!errors.isMissingNode() && !errors.isNull() && !errors.isEmpty()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(errors.toString()));
        }
        return resp;
    }

    // ---------------------------------------------------------------- 订单

    @Override
    public List<OrderDraft> fetchOrders(ConnectorContext ctx, LocalDateTime since) {
        String base = ctx.baseUrl(INFO);
        String after = since.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime().format(ISO);
        List<OrderDraft> drafts = new ArrayList<>();
        for (String nodeType : SHIP_NODE_TYPES) {
            String query = "?lastModifiedStartDate=" + ConnectorSigns.enc(after) + "&limit=" + ORDER_LIMIT
                    + "&shipNodeType=" + nodeType + "&productInfo=false";
            for (int page = 0; query != null; page++) {
                if (page >= MAX_PAGES) {
                    ConnectorJson.warnPageLimit(PLATFORM, "orders " + nodeType, MAX_PAGES);
                    break;
                }
                String url = base + "/v3/orders" + query;
                JsonNode list = exec(ctx, h -> HttpJson.get(url, h, PLATFORM)).path("list");
                for (JsonNode o : list.path("elements").path("order")) {
                    drafts.add(toDraft(ctx.store(), o, nodeType));
                }
                String next = HttpJson.text(list.path("meta"), "nextCursor");
                query = next == null || next.isBlank() ? null : next.startsWith("?") ? next : "?" + next;
            }
        }
        return drafts;
    }

    static OrderDraft toDraft(EcomStore store, JsonNode o, String requestedNodeType) {
        OrderDraft d = new OrderDraft();
        d.setStoreId(store.getId());
        d.setPlatformOrderNo(HttpJson.text(o, "purchaseOrderId"));
        d.setOrderTime(ConnectorJson.unixMillis(o.get("orderDate")));
        d.setPaidTime(d.getOrderTime());
        d.setBuyerEmail(HttpJson.text(o, "customerEmailId"));
        String nodeType = HttpJson.text(o.path("shipNode"), "type");
        if (nodeType == null) {
            nodeType = requestedNodeType;
        }
        d.setFulfillment("WFSFulfilled".equals(nodeType) ? FulfillmentType.PLATFORM : FulfillmentType.SELLER);
        JsonNode ship = o.path("shippingInfo");
        d.setShipPhone(HttpJson.text(ship, "phone"));
        d.setShipBy(ConnectorJson.unixMillis(ship.get("estimatedShipDate")));
        d.setDeliverBy(ConnectorJson.unixMillis(ship.get("estimatedDeliveryDate")));
        JsonNode addr = ship.path("postalAddress");
        d.setShipName(HttpJson.text(addr, "name"));
        d.setBuyerName(d.getShipName());
        d.setShipAddress(ConnectorJson.join(HttpJson.text(addr, "address1"), HttpJson.text(addr, "address2")));
        d.setShipCity(HttpJson.text(addr, "city"));
        d.setShipState(HttpJson.text(addr, "state"));
        d.setShipPostcode(HttpJson.text(addr, "postalCode"));
        d.setShipCountry(country(HttpJson.text(addr, "country")));
        d.setNote(HttpJson.text(o, "customerOrderId"));
        List<String> lineStatuses = new ArrayList<>();
        for (JsonNode line : o.path("orderLines").path("orderLine")) {
            OrderDraft.Line l = new OrderDraft.Line();
            l.setPlatformLineId(HttpJson.text(line, "lineNumber"));
            l.setSellerSku(HttpJson.text(line.path("item"), "sku"));
            l.setTitle(HttpJson.text(line.path("item"), "productName"));
            BigDecimal qty = HttpJson.decimal(line, "orderLineQuantity", "amount");
            l.setQty(qty == null || qty.signum() == 0 ? BigDecimal.ONE : qty);
            BigDecimal product = null;
            BigDecimal shipping = null;
            BigDecimal tax = null;
            for (JsonNode ch : line.path("charges").path("charge")) {
                BigDecimal amount = HttpJson.decimal(ch, "chargeAmount", "amount");
                String type = HttpJson.text(ch, "chargeType");
                if ("PRODUCT".equals(type)) {
                    product = ConnectorJson.add(product, amount);
                } else if ("SHIPPING".equals(type)) {
                    shipping = ConnectorJson.add(shipping, amount);
                }
                tax = ConnectorJson.add(tax, HttpJson.decimal(ch, "tax", "taxAmount", "amount"));
                if (d.getCurrency() == null) {
                    d.setCurrency(HttpJson.text(ch.path("chargeAmount"), "currency"));
                }
            }
            l.setAmount(product);
            l.setPrice(product == null ? BigDecimal.ZERO : product.divide(l.getQty(), 4, RoundingMode.HALF_UP));
            l.setShipping(shipping);
            l.setTax(tax);
            d.getLines().add(l);
            for (JsonNode st : line.path("orderLineStatuses").path("orderLineStatus")) {
                String s = HttpJson.text(st, "status");
                if (s != null) {
                    lineStatuses.add(s);
                }
            }
        }
        d.setStatus(status(lineStatuses));
        if (d.getCurrency() == null) {
            d.setCurrency(store.getCurrency());
        }
        return d;
    }

    /**
     * 订单状态按行状态汇总：全部 Cancelled → CANCELLED；还有 Created / Acknowledged → UNSHIPPED；
     * 其余（Shipped / Delivered，含部分取消）→ SHIPPED
     */
    static EcomOrderStatus status(List<String> lineStatuses) {
        if (lineStatuses.isEmpty()) {
            return EcomOrderStatus.UNSHIPPED;
        }
        boolean allCancelled = true;
        boolean open = false;
        for (String s : lineStatuses) {
            if (!"Cancelled".equals(s)) {
                allCancelled = false;
            }
            if ("Created".equals(s) || "Acknowledged".equals(s)) {
                open = true;
            }
        }
        if (allCancelled) {
            return EcomOrderStatus.CANCELLED;
        }
        return open ? EcomOrderStatus.UNSHIPPED : EcomOrderStatus.SHIPPED;
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
            return ShipmentResult.skipped("WFS");
        }
        if (order.getTrackingNo() == null || order.getTrackingNo().isBlank()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "tracking number missing");
        }
        long shipTime = (order.getShipTime() == null ? LocalDateTime.now() : order.getShipTime())
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Map<String, Object> carrierName = new LinkedHashMap<>();
        String carrier = order.getCarrier() == null ? "" : order.getCarrier().trim();
        String known = CARRIERS.stream().filter(k -> k.equalsIgnoreCase(carrier)).findFirst().orElse(null);
        if (known != null) {
            carrierName.put("carrier", known);
        } else {
            carrierName.put("otherCarrier", carrier);
        }
        List<Map<String, Object>> lines = new ArrayList<>();
        for (EcomOrderItem it : items) {
            if (it.getPlatformLineId() == null) {
                throw new BizException("ecom.error.connectorLineIdMissing", order.getPlatformOrderNo());
            }
            Map<String, Object> tracking = new LinkedHashMap<>();
            tracking.put("shipDateTime", shipTime);
            tracking.put("carrierName", carrierName);
            tracking.put("methodCode", "Standard");
            tracking.put("trackingNumber", order.getTrackingNo());
            Map<String, Object> st = new LinkedHashMap<>();
            st.put("status", "Shipped");
            BigDecimal qty = it.getQty() == null || it.getQty().signum() <= 0 ? BigDecimal.ONE : it.getQty();
            st.put("statusQuantity", Map.of("unitOfMeasurement", "EACH", "amount", qty.stripTrailingZeros().toPlainString()));
            st.put("trackingInfo", tracking);
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("lineNumber", it.getPlatformLineId());
            line.put("orderLineStatuses", Map.of("orderLineStatus", List.of(st)));
            lines.add(line);
        }
        Map<String, Object> body = Map.of("orderShipment", Map.of("orderLines", Map.of("orderLine", lines)));
        String url = ctx.baseUrl(INFO) + "/v3/orders/" + ConnectorSigns.enc(order.getPlatformOrderNo()) + "/shipping";
        exec(ctx, h -> HttpJson.postJson(url, h, body, PLATFORM));
        return ShipmentResult.confirmed(order.getTrackingNo(), order.getCarrier());
    }

    // ---------------------------------------------------------------- 库存与商品

    @Override
    public void pushInventory(ConnectorContext ctx, List<InventoryUpdate> updates) {
        String base = ctx.baseUrl(INFO);
        String shipNode = ctx.credentials().get("shipNode");
        for (InventoryUpdate u : updates) {
            int qty = u.intQty();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sku", u.sellerSku());
            body.put("quantity", Map.of("unit", "EACH", "amount", qty));
            String url = base + "/v3/inventory?sku=" + ConnectorSigns.enc(u.sellerSku())
                    + (shipNode == null ? "" : "&shipNode=" + ConnectorSigns.enc(shipNode));
            exec(ctx, h -> HttpJson.putJson(url, h, body, PLATFORM));
        }
    }

    @Override
    public List<ListingDraft> fetchListings(ConnectorContext ctx) {
        String base = ctx.baseUrl(INFO);
        List<ListingDraft> list = new ArrayList<>();
        for (int page = 0; ; page++) {
            if (page >= MAX_ITEM_PAGES) {
                ConnectorJson.warnPageLimit(PLATFORM, "items", MAX_ITEM_PAGES);
                break;
            }
            String url = base + "/v3/items?limit=" + ITEM_LIMIT + "&offset=" + (page * ITEM_LIMIT);
            JsonNode resp = exec(ctx, h -> HttpJson.get(url, h, PLATFORM));
            JsonNode items = resp.path("ItemResponse");
            for (JsonNode it : items) {
                list.add(new ListingDraft(HttpJson.text(it, "sku"), HttpJson.text(it, "wpid"), null, HttpJson.text(it, "productName"),
                        null, HttpJson.decimal(it, "price", "amount"), HttpJson.text(it.path("price"), "currency"), null,
                        null, listingStatus(HttpJson.text(it, "publishedStatus"), HttpJson.text(it, "lifecycleStatus")), null, null));
            }
            int total = resp.path("totalItems").asInt(0);
            if (!items.isArray() || items.size() < ITEM_LIMIT || (total > 0 && (page + 1) * ITEM_LIMIT >= total)) {
                break;
            }
        }
        return list;
    }

    /** RETIRED → DELETED；PUBLISHED → ACTIVE；UNPUBLISHED → INACTIVE；STAGE / IN_PROGRESS / SYSTEM_PROBLEM → INCOMPLETE */
    static String listingStatus(String published, String lifecycle) {
        if ("RETIRED".equals(lifecycle)) {
            return ListingStatus.DELETED.name();
        }
        if (published == null) {
            return null;
        }
        return switch (published) {
            case "PUBLISHED" -> ListingStatus.ACTIVE.name();
            case "UNPUBLISHED" -> ListingStatus.INACTIVE.name();
            case "STAGE", "IN_PROGRESS", "SYSTEM_PROBLEM", "READY_TO_PUBLISH" -> ListingStatus.INCOMPLETE.name();
            default -> null;
        };
    }

    // ---------------------------------------------------------------- 工具

    private static String country(String c) {
        if (c == null) {
            return null;
        }
        return "USA".equalsIgnoreCase(c) ? "US" : c.toUpperCase(Locale.ROOT);
    }
}
