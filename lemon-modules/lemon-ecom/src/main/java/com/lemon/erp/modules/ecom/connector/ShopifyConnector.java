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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shopify 独立站（Admin API，GraphQL，版本 {@link #API_VERSION}）：使用店铺后台「自定义应用」生成的 Admin API access token，
 * 请求头 X-Shopify-Access-Token。授权信息：shopDomain（xxx.myshopify.com）、adminAccessToken、locationId（可选，回传库存的地点）。
 * 应用需要的权限（scopes）：read_orders、read_merchant_managed_fulfillment_orders / write_merchant_managed_fulfillment_orders、
 * read_products、read_inventory / write_inventory、read_locations。
 * <p>
 * 接口地址：店铺上填写了自定义地址时为 {自定义地址}/admin/api/{版本}/graphql.json（测试指向模拟服务器），
 * 否则为 https://{shopDomain}/admin/api/{版本}/graphql.json。Shopify 没有沙箱环境，联调用免费的开发商店（development store，
 * 在 Partner 后台创建），因此沙箱开关对本连接器不起作用。
 * <p>
 * 编码约定：
 * <ul>
 *   <li>platformOrderNo 为订单的数字 ID（legacyResourceId，回传发货时拼成 gid://shopify/Order/{id}）；订单名称（如 #1001）写入备注开头</li>
 *   <li>明细 platformLineId 为订单行（LineItem）的数字 ID</li>
 *   <li>platformItemId 为库存项（InventoryItem）的数字 ID —— 回传库存的 inventorySetQuantities 需要它；在线商品的 parentItemId 为商品（Product）数字 ID。
 *       回传库存时 platformItemId 也接受完整 gid；为空时按 sellerSku 查询变体（productVariants sku:xxx）取库存项</li>
 *   <li>sellerSku 取订单行 / 变体的 SKU；变体没有填 SKU 时退为变体数字 ID（在线商品与订单两边一致，便于在 ERP 中映射）</li>
 *   <li>订单运费（totalShippingPriceSet）在 GraphQL 中只有订单级金额，记在第一行明细上</li>
 * </ul>
 * GraphQL 返回的 errors（含限流 THROTTLED）与 mutation 的 userErrors 都转为业务异常。
 * <p>
 * 说明：按 Shopify 公开文档实现，尚未用真实开发商店联调
 */
@Component
public class ShopifyConnector implements ChannelConnector {

    static final String PLATFORM = "Shopify";
    /** Admin API 版本（Shopify 每季度发布一个版本，每个版本至少支持 12 个月） */
    static final String API_VERSION = "2025-01";
    private static final int ORDER_PAGE_SIZE = 10;
    private static final int LINE_PAGE_SIZE = 20;
    private static final int LISTING_PAGE_SIZE = 100;
    private static final int MAX_PAGES = 50;
    private static final int INVENTORY_BATCH = 100;
    private static final ConnectorInfo INFO = new ConnectorInfo(EcomPlatform.SHOPIFY, AuthType.KEYS, List.of(
            CredentialField.text("shopDomain", "ecom.label.cred.shopifyShopDomain", true),
            CredentialField.secret("adminAccessToken", "ecom.label.cred.shopifyAdminAccessToken"),
            CredentialField.text("locationId", "ecom.label.cred.shopifyLocationId", false)),
            "https://{shopDomain}", null,
            EnumSet.of(ConnectorCapability.ORDERS, ConnectorCapability.SHIP_CONFIRM, ConnectorCapability.INVENTORY_PUSH,
                    ConnectorCapability.LISTINGS),
            "https://shopify.dev/docs/api/admin-graphql");

    private static final String LINE_FIELDS = """
            pageInfo { hasNextPage endCursor }
            nodes {
              id sku name variantTitle quantity
              originalUnitPriceSet { shopMoney { amount currencyCode } }
              totalDiscountSet { shopMoney { amount } }
              taxLines { priceSet { shopMoney { amount } } }
              variant { id sku inventoryItem { id } }
            }""";

    static final String ORDERS_QUERY = """
            query Orders($first: Int!, $after: String, $query: String) {
              orders(first: $first, after: $after, query: $query, sortKey: UPDATED_AT) {
                pageInfo { hasNextPage endCursor }
                nodes {
                  id legacyResourceId name createdAt processedAt updatedAt cancelledAt
                  displayFinancialStatus displayFulfillmentStatus currencyCode email note
                  customer { displayName }
                  shippingAddress { name phone address1 address2 city province countryCodeV2 zip }
                  totalShippingPriceSet { shopMoney { amount } }
                  lineItems(first: %d) { %s }
                }
              }
            }""".formatted(LINE_PAGE_SIZE, LINE_FIELDS);

    static final String ORDER_LINES_QUERY = """
            query OrderLines($id: ID!, $after: String) {
              order(id: $id) { lineItems(first: %d, after: $after) { %s } }
            }""".formatted(LINE_PAGE_SIZE, LINE_FIELDS);

    static final String FULFILLMENT_ORDERS_QUERY = """
            query FulfillmentOrders($id: ID!) {
              order(id: $id) {
                fulfillmentOrders(first: 20) {
                  nodes {
                    id status
                    lineItems(first: 100) { nodes { id remainingQuantity lineItem { id } } }
                  }
                }
              }
            }""";

    static final String FULFILLMENT_CREATE = """
            mutation FulfillmentCreate($fulfillment: FulfillmentInput!) {
              fulfillmentCreate(fulfillment: $fulfillment) {
                fulfillment { id status }
                userErrors { field message }
              }
            }""";

    static final String INVENTORY_SET = """
            mutation InventorySet($input: InventorySetQuantitiesInput!) {
              inventorySetQuantities(input: $input) {
                inventoryAdjustmentGroup { id }
                userErrors { field message code }
              }
            }""";

    static final String VARIANT_BY_SKU = """
            query VariantBySku($query: String!) {
              productVariants(first: 1, query: $query) { nodes { sku inventoryItem { id } } }
            }""";

    static final String PRIMARY_LOCATION = "query { location { id } }";

    static final String VARIANTS_QUERY = """
            query Variants($first: Int!, $after: String) {
              shop { currencyCode myshopifyDomain }
              productVariants(first: $first, after: $after) {
                pageInfo { hasNextPage endCursor }
                nodes {
                  id legacyResourceId sku title displayName price inventoryQuantity
                  image { url }
                  inventoryItem { id }
                  product { id legacyResourceId title status handle onlineStoreUrl featuredMedia { preview { image { url } } } }
                }
              }
            }""";

    @Override
    public ConnectorInfo info() {
        return INFO;
    }

    // ---------------------------------------------------------------- 订单

    @Override
    public List<OrderDraft> fetchOrders(ConnectorContext ctx, LocalDateTime since) {
        Api api = api(ctx);
        String filter = "updated_at:>='" + since.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "'";
        List<OrderDraft> drafts = new ArrayList<>();
        String after = null;
        int pages = 0;
        do {
            Map<String, Object> vars = new HashMap<>();
            vars.put("first", ORDER_PAGE_SIZE);
            vars.put("after", after);
            vars.put("query", filter);
            JsonNode orders = api.call(ORDERS_QUERY, vars).path("orders");
            for (JsonNode o : orders.path("nodes")) {
                OrderDraft d = toDraft(ctx.store(), o);
                List<JsonNode> lines = new ArrayList<>();
                o.path("lineItems").path("nodes").forEach(lines::add);
                JsonNode linePage = o.path("lineItems").path("pageInfo");
                int linePages = 0;
                while (linePage.path("hasNextPage").asBoolean(false) && linePages++ < MAX_PAGES) {
                    Map<String, Object> lv = new HashMap<>();
                    lv.put("id", HttpJson.text(o, "id"));
                    lv.put("after", HttpJson.text(linePage, "endCursor"));
                    JsonNode more = api.call(ORDER_LINES_QUERY, lv).path("order").path("lineItems");
                    more.path("nodes").forEach(lines::add);
                    linePage = more.path("pageInfo");
                }
                for (JsonNode li : lines) {
                    d.getLines().add(toLine(li));
                }
                BigDecimal shipping = HttpJson.decimal(o, "totalShippingPriceSet", "shopMoney", "amount");
                if (shipping != null && shipping.signum() != 0 && !d.getLines().isEmpty()) {
                    d.getLines().get(0).setShipping(shipping);
                }
                drafts.add(d);
            }
            JsonNode pageInfo = orders.path("pageInfo");
            after = pageInfo.path("hasNextPage").asBoolean(false) ? HttpJson.text(pageInfo, "endCursor") : null;
            pages++;
        } while (after != null && pages < MAX_PAGES);
        return drafts;
    }

    private static OrderDraft toDraft(EcomStore store, JsonNode o) {
        OrderDraft d = new OrderDraft();
        d.setStoreId(store.getId());
        String id = HttpJson.text(o, "legacyResourceId");
        d.setPlatformOrderNo(id == null ? numericId(HttpJson.text(o, "id")) : id);
        d.setOrderTime(time(HttpJson.text(o, "createdAt")));
        String financial = HttpJson.text(o, "displayFinancialStatus");
        if (financial != null && !"PENDING".equals(financial) && !"AUTHORIZED".equals(financial) && !"EXPIRED".equals(financial)
                && !"VOIDED".equals(financial)) {
            d.setPaidTime(time(HttpJson.text(o, "processedAt")));
        }
        d.setStatus(status(HttpJson.text(o, "cancelledAt"), financial, HttpJson.text(o, "displayFulfillmentStatus")));
        d.setFulfillment(FulfillmentType.SELLER);
        d.setCurrency(HttpJson.text(o, "currencyCode"));
        d.setBuyerEmail(HttpJson.text(o, "email"));
        d.setBuyerName(HttpJson.text(o.path("customer"), "displayName"));
        JsonNode addr = o.path("shippingAddress");
        d.setShipName(HttpJson.text(addr, "name"));
        d.setShipPhone(HttpJson.text(addr, "phone"));
        d.setShipCountry(HttpJson.text(addr, "countryCodeV2"));
        d.setShipState(HttpJson.text(addr, "province"));
        d.setShipCity(HttpJson.text(addr, "city"));
        d.setShipPostcode(HttpJson.text(addr, "zip"));
        d.setShipAddress(join(HttpJson.text(addr, "address1"), HttpJson.text(addr, "address2")));
        if (d.getBuyerName() == null) {
            d.setBuyerName(d.getShipName());
        }
        d.setNote(join(HttpJson.text(o, "name"), HttpJson.text(o, "note")));
        return d;
    }

    private static OrderDraft.Line toLine(JsonNode li) {
        OrderDraft.Line l = new OrderDraft.Line();
        l.setPlatformLineId(numericId(HttpJson.text(li, "id")));
        JsonNode variant = li.path("variant");
        l.setSellerSku(sku(HttpJson.text(li, "sku"), HttpJson.text(variant, "sku"), numericId(HttpJson.text(variant, "id"))));
        l.setPlatformItemId(numericId(HttpJson.text(variant.path("inventoryItem"), "id")));
        String variantTitle = HttpJson.text(li, "variantTitle");
        l.setTitle(HttpJson.text(li, "name") == null ? variantTitle : HttpJson.text(li, "name"));
        BigDecimal qty = HttpJson.decimal(li, "quantity");
        l.setQty(qty == null ? BigDecimal.ONE : qty);
        BigDecimal price = HttpJson.decimal(li, "originalUnitPriceSet", "shopMoney", "amount");
        l.setPrice(price == null ? BigDecimal.ZERO : price);
        l.setAmount(l.getPrice().multiply(l.getQty()).setScale(2, RoundingMode.HALF_UP));
        l.setDiscount(HttpJson.decimal(li, "totalDiscountSet", "shopMoney", "amount"));
        BigDecimal tax = null;
        for (JsonNode t : li.path("taxLines")) {
            BigDecimal v = HttpJson.decimal(t, "priceSet", "shopMoney", "amount");
            if (v != null) {
                tax = tax == null ? v : tax.add(v);
            }
        }
        l.setTax(tax);
        return l;
    }

    /**
     * 状态：cancelledAt 不为空为已取消；财务状态 REFUNDED 为已退款，未付款（PENDING、EXPIRED、VOIDED）为待付款（AUTHORIZED 已授权扣款视为已付款）；
     * 配送状态 FULFILLED 为已发货，其余（UNFULFILLED、PARTIALLY_FULFILLED、IN_PROGRESS、ON_HOLD、SCHEDULED 等）为待发货
     */
    static EcomOrderStatus status(String cancelledAt, String financial, String fulfillment) {
        if (cancelledAt != null) {
            return EcomOrderStatus.CANCELLED;
        }
        if (financial != null) {
            switch (financial) {
                case "REFUNDED":
                    return EcomOrderStatus.REFUNDED;
                case "PENDING", "EXPIRED", "VOIDED":
                    return EcomOrderStatus.PENDING;
                default:
                    break;
            }
        }
        return "FULFILLED".equals(fulfillment) ? EcomOrderStatus.SHIPPED : EcomOrderStatus.UNSHIPPED;
    }

    // ---------------------------------------------------------------- 发货

    /**
     * 回传发货：查询订单的配送单（fulfillmentOrders），对状态为 OPEN / IN_PROGRESS 的配送单调用 fulfillmentCreate 并带上运单号。
     * 明细带 platformLineId 时只发这些订单行（数量取明细数量与剩余数量的较小值），否则发全部剩余数量
     */
    @Override
    public void confirmShipment(ConnectorContext ctx, EcomOrder order, List<EcomOrderItem> items) {
        Api api = api(ctx);
        Map<String, BigDecimal> wanted = new LinkedHashMap<>();
        if (items != null) {
            for (EcomOrderItem it : items) {
                if (it.getPlatformLineId() != null) {
                    wanted.merge(numericId(it.getPlatformLineId()), it.getQty() == null ? BigDecimal.ONE : it.getQty(), BigDecimal::add);
                }
            }
        }
        JsonNode fos = api.call(FULFILLMENT_ORDERS_QUERY, Map.of("id", gid("Order", order.getPlatformOrderNo())))
                .path("order").path("fulfillmentOrders").path("nodes");
        List<Map<String, Object>> groups = new ArrayList<>();
        for (JsonNode fo : fos) {
            String st = HttpJson.text(fo, "status");
            if (!"OPEN".equals(st) && !"IN_PROGRESS".equals(st)) {
                continue;
            }
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("fulfillmentOrderId", HttpJson.text(fo, "id"));
            if (!wanted.isEmpty()) {
                List<Map<String, Object>> lines = new ArrayList<>();
                for (JsonNode fl : fo.path("lineItems").path("nodes")) {
                    String lineId = numericId(HttpJson.text(fl.path("lineItem"), "id"));
                    BigDecimal want = wanted.get(lineId);
                    int remaining = fl.path("remainingQuantity").asInt(0);
                    if (want == null || remaining <= 0) {
                        continue;
                    }
                    int qty = Math.min(remaining, want.intValue());
                    if (qty > 0) {
                        lines.add(Map.of("id", HttpJson.text(fl, "id"), "quantity", qty));
                        wanted.put(lineId, want.subtract(BigDecimal.valueOf(qty)));
                    }
                }
                if (lines.isEmpty()) {
                    continue;
                }
                group.put("fulfillmentOrderLineItems", lines);
            }
            groups.add(group);
        }
        if (groups.isEmpty()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "no open fulfillment order for " + order.getPlatformOrderNo());
        }
        Map<String, Object> tracking = new LinkedHashMap<>();
        if (order.getCarrier() != null) {
            tracking.put("company", order.getCarrier());
        }
        if (order.getTrackingNo() != null) {
            tracking.put("number", order.getTrackingNo());
        }
        Map<String, Object> fulfillment = new LinkedHashMap<>();
        fulfillment.put("lineItemsByFulfillmentOrder", groups);
        fulfillment.put("trackingInfo", tracking);
        fulfillment.put("notifyCustomer", true);
        JsonNode payload = api.call(FULFILLMENT_CREATE, Map.of("fulfillment", fulfillment)).path("fulfillmentCreate");
        checkUserErrors(payload);
    }

    // ---------------------------------------------------------------- 库存

    /**
     * 回传可售库存：inventorySetQuantities（name = available，忽略比较数量），每批 100 个库存项；
     * 地点取授权信息中的 locationId，没有填时用店铺的主地点
     */
    @Override
    public void pushInventory(ConnectorContext ctx, List<InventoryUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        Api api = api(ctx);
        String location = ctx.credentials().get("locationId");
        if (location == null) {
            location = HttpJson.text(api.call(PRIMARY_LOCATION, Map.of()).path("location"), "id");
            if (location == null) {
                throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "primary location not found");
            }
        }
        String locationGid = gid("Location", location);
        List<Map<String, Object>> quantities = new ArrayList<>();
        for (InventoryUpdate u : updates) {
            String item = u.platformItemId();
            if (item == null) {
                item = inventoryItemBySku(api, u.sellerSku());
            }
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("inventoryItemId", gid("InventoryItem", item));
            q.put("locationId", locationGid);
            q.put("quantity", u.qty() == null ? 0 : Math.max(0, u.qty().intValue()));
            quantities.add(q);
        }
        for (int i = 0; i < quantities.size(); i += INVENTORY_BATCH) {
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("name", "available");
            input.put("reason", "correction");
            input.put("ignoreCompareQuantity", true);
            input.put("quantities", quantities.subList(i, Math.min(quantities.size(), i + INVENTORY_BATCH)));
            checkUserErrors(api.call(INVENTORY_SET, Map.of("input", input)).path("inventorySetQuantities"));
        }
    }

    private static String inventoryItemBySku(Api api, String sku) {
        if (sku == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "inventory item id and sku are both empty");
        }
        JsonNode nodes = api.call(VARIANT_BY_SKU, Map.of("query", "sku:\"" + sku.replace("\"", "\\\"") + "\"")).path("productVariants").path("nodes");
        for (JsonNode n : nodes) {
            String id = HttpJson.text(n.path("inventoryItem"), "id");
            if (id != null) {
                return id;
            }
        }
        throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "variant not found for sku " + sku);
    }

    // ---------------------------------------------------------------- 在线商品

    /** 在线商品：productVariants 分页（每页 100），一个变体一条；价格为店铺币种 */
    @Override
    public List<ListingDraft> fetchListings(ConnectorContext ctx) {
        Api api = api(ctx);
        List<ListingDraft> list = new ArrayList<>();
        String after = null;
        int pages = 0;
        do {
            Map<String, Object> vars = new HashMap<>();
            vars.put("first", LISTING_PAGE_SIZE);
            vars.put("after", after);
            JsonNode data = api.call(VARIANTS_QUERY, vars);
            String currency = HttpJson.text(data.path("shop"), "currencyCode");
            String domain = HttpJson.text(data.path("shop"), "myshopifyDomain");
            JsonNode variants = data.path("productVariants");
            for (JsonNode v : variants.path("nodes")) {
                list.add(toListing(v, currency, domain));
            }
            JsonNode pageInfo = variants.path("pageInfo");
            after = pageInfo.path("hasNextPage").asBoolean(false) ? HttpJson.text(pageInfo, "endCursor") : null;
            pages++;
        } while (after != null && pages < MAX_PAGES);
        return list;
    }

    private static ListingDraft toListing(JsonNode v, String currency, String domain) {
        JsonNode product = v.path("product");
        String variantId = HttpJson.text(v, "legacyResourceId");
        if (variantId == null) {
            variantId = numericId(HttpJson.text(v, "id"));
        }
        String productId = HttpJson.text(product, "legacyResourceId");
        if (productId == null) {
            productId = numericId(HttpJson.text(product, "id"));
        }
        String variantTitle = HttpJson.text(v, "title");
        String image = HttpJson.text(v.path("image"), "url");
        if (image == null) {
            image = HttpJson.text(product.path("featuredMedia").path("preview").path("image"), "url");
        }
        String url = HttpJson.text(product, "onlineStoreUrl");
        if (url == null && domain != null && productId != null) {
            url = "https://" + domain + "/admin/products/" + productId + (variantId == null ? "" : "/variants/" + variantId);
        }
        BigDecimal qty = HttpJson.decimal(v, "inventoryQuantity");
        return new ListingDraft(sku(HttpJson.text(v, "sku"), null, variantId),
                numericId(HttpJson.text(v.path("inventoryItem"), "id")), productId,
                HttpJson.text(product, "title"), "Default Title".equals(variantTitle) ? null : variantTitle,
                HttpJson.decimal(v, "price"), currency, qty, FulfillmentType.SELLER.name(),
                listingStatus(HttpJson.text(product, "status")), image, url);
    }

    /** 商品状态 ACTIVE 在售，ARCHIVED / DRAFT 下架 */
    static String listingStatus(String s) {
        if (s == null) {
            return null;
        }
        return switch (s) {
            case "ACTIVE" -> ListingStatus.ACTIVE.name();
            case "ARCHIVED", "DRAFT" -> ListingStatus.INACTIVE.name();
            default -> null;
        };
    }

    // ---------------------------------------------------------------- 调用

    /** 接口地址：自定义地址优先，否则按 shopDomain 拼 https://{shopDomain} */
    static String graphqlUrl(ConnectorContext ctx) {
        String base = ctx.baseUrlOverride();
        if (base == null) {
            String domain = ctx.credentials().require("shopDomain").toLowerCase();
            domain = domain.replaceFirst("^https?://", "");
            int slash = domain.indexOf('/');
            if (slash >= 0) {
                domain = domain.substring(0, slash);
            }
            if (!domain.contains(".")) {
                domain = domain + ".myshopify.com";
            }
            base = "https://" + domain;
        }
        return base + "/admin/api/" + API_VERSION + "/graphql.json";
    }

    private static Api api(ConnectorContext ctx) {
        return new Api(graphqlUrl(ctx), Map.of("X-Shopify-Access-Token", ctx.credentials().require("adminAccessToken"),
                "Accept", "application/json"));
    }

    private record Api(String url, Map<String, String> headers) {

        JsonNode call(String query, Map<String, Object> variables) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("query", query);
            body.put("variables", variables);
            JsonNode resp = HttpJson.postJson(url, headers, body, PLATFORM);
            JsonNode errors = resp.path("errors");
            if ((errors.isArray() && !errors.isEmpty()) || errors.isString() || errors.isObject()) {
                throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(errors.toString()));
            }
            return resp.path("data");
        }
    }

    private static void checkUserErrors(JsonNode payload) {
        JsonNode errors = payload.path("userErrors");
        if (errors.isArray() && !errors.isEmpty()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(errors.toString()));
        }
    }

    /** gid://shopify/Order/123 → 123（已是数字原样返回） */
    static String numericId(String gid) {
        if (gid == null) {
            return null;
        }
        String s = gid;
        int q = s.indexOf('?');
        if (q >= 0) {
            s = s.substring(0, q);
        }
        return s.substring(s.lastIndexOf('/') + 1);
    }

    /** 123 → gid://shopify/{type}/123（已是 gid 原样返回） */
    static String gid(String type, String id) {
        return id.startsWith("gid://") ? id : "gid://shopify/" + type + "/" + id;
    }

    private static String sku(String lineSku, String variantSku, String variantId) {
        if (lineSku != null && !lineSku.isBlank()) {
            return lineSku.trim();
        }
        if (variantSku != null && !variantSku.isBlank()) {
            return variantSku.trim();
        }
        return variantId;
    }

    private static LocalDateTime time(String iso) {
        if (iso == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
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
}
