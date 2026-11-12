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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Lazada Open Platform（https://open.lazada.com/apps/doc/doc）。
 * <p>
 * 授权（OAuth）：店铺填写 appKey、appSecret、region（SG / MY / TH / PH / VN / ID，决定业务接口地址，如 https://api.lazada.sg/rest），
 * 可选 shippingAllocateType（发货打包的分配方式，本地店铺 TFS、跨境店铺 NTFS，默认 TFS）。
 * 授权页 https://auth.lazada.com/oauth/authorize，回调带 code 与 state，由 https://auth.lazada.com/rest/auth/token/create 换取令牌，
 * 写入 accessToken、refreshToken、expiresAt（access_token 过期时间，Unix 秒）、refreshExpiresAt、country、sellerId、account；
 * 没有填写 region 时按授权返回的国家（跨境账号取 country_user_info 第一个国家）补上。过期前 10 分钟用 /auth/token/refresh 刷新。
 * <p>
 * 签名：系统参数 app_key、timestamp（毫秒）、sign_method=sha256、access_token 与业务参数一起按参数名排序，
 * 原文 = API 路径 + k1v1k2v2…，HMAC-SHA256(appSecret) 大写十六进制；GET 放查询串，POST 用表单。
 * <p>
 * 地址：店铺填写了自定义地址时，业务接口、令牌接口（{自定义地址}/auth/token/create）和授权页（{自定义地址}/oauth/authorize）都指向它；
 * Lazada 没有公开沙箱，联调用测试店铺（正式环境）。
 * <p>
 * 编码约定：平台商品 ID 为 {@code "itemId:skuId"}（在线商品、订单明细、库存回传一致），订单行 ID 为 order_item_id；
 * Lazada 订单明细每行是一件商品（数量 1），同一 SKU 买多件会有多行。卖家 SKU 取 sku（SellerSku），为空取 shop_sku，仍为空取平台商品 ID。
 * 金额：单价 / 商品金额取 item_price，运费 shipping_amount，税 tax_amount，折扣取卖家承担的 voucher_seller；币种取明细的 currency，缺少时按站点。
 * 订单状态由各行状态汇总：任一行待付款为待付款，否则任一行待处理为待发货，其次已发货、已退货、已取消。
 * <p>
 * 发货：Lazada 只能使用平台分配的物流，开放接口不能写入 ERP 的运单号；回传发货按「打包 /order/fulfill/pack → 通知揽收 /order/package/rts」两步，
 * 运单号与承运商由平台分配（结果中返回，当前框架不回写）。自配送（Seller Own Fleet）店铺的妥投回传未实现。
 * <p>
 * 说明：按开放平台公开文档实现，尚未用真实店铺联调
 */
@Component
public class LazadaConnector implements ChannelConnector {

    static final String PLATFORM = "Lazada";
    static final String AUTH_BASE = "https://auth.lazada.com/rest";
    static final String AUTHORIZE_URL = "https://auth.lazada.com/oauth/authorize";
    static final Map<String, String> ENDPOINTS = Map.of(
            "SG", "https://api.lazada.sg/rest",
            "MY", "https://api.lazada.com.my/rest",
            "TH", "https://api.lazada.co.th/rest",
            "PH", "https://api.lazada.com.ph/rest",
            "VN", "https://api.lazada.vn/rest",
            "ID", "https://api.lazada.co.id/rest");
    private static final Map<String, String> CURRENCIES = Map.of(
            "SG", "SGD", "MY", "MYR", "TH", "THB", "PH", "PHP", "VN", "VND", "ID", "IDR");
    static final long REFRESH_AHEAD_SECONDS = 600;
    static final int ORDER_PAGE_SIZE = 100;
    static final int ITEMS_BATCH = 50;
    static final int PRODUCT_PAGE_SIZE = 50;
    static final int STOCK_BATCH = 50;
    static final int MAX_PAGES = 200;
    private static final DateTimeFormatter LAZADA_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");
    private static final DateTimeFormatter LAZADA_TIME_NO_ZONE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String OWN_WAREHOUSE = "Own Warehouse";
    private static final Set<String> PACKABLE = Set.of("pending", "repacked");
    private static final String PACKED = "packed";

    private static final ConnectorInfo INFO = new ConnectorInfo(EcomPlatform.LAZADA, AuthType.OAUTH, List.of(
            CredentialField.text("appKey", "ecom.label.cred.appKey", true),
            CredentialField.secret("appSecret", "ecom.label.cred.appSecret"),
            CredentialField.text("region", "ecom.label.cred.lazada.region", true),
            CredentialField.text("shippingAllocateType", "ecom.label.cred.lazada.shippingAllocateType", false),
            CredentialField.token("accessToken", "ecom.label.cred.accessToken"),
            CredentialField.token("refreshToken", "ecom.label.cred.refreshToken"),
            new CredentialField("expiresAt", "ecom.label.cred.lazada.expiresAt", false, false, true),
            new CredentialField("sellerId", "ecom.label.cred.lazada.sellerId", false, false, true)),
            ENDPOINTS.get("SG"), null,
            EnumSet.of(ConnectorCapability.ORDERS, ConnectorCapability.SHIP_CONFIRM, ConnectorCapability.INVENTORY_PUSH,
                    ConnectorCapability.LISTINGS),
            "https://open.lazada.com/apps/doc/doc");

    @Override
    public ConnectorInfo info() {
        return INFO;
    }

    // ---------------------------------------------------------------- 签名

    /** HMAC-SHA256(appSecret, apiPath + k1v1k2v2…) 大写十六进制（params 不含 sign） */
    static String sign(String appSecret, String apiPath, Map<String, String> params) {
        return ConnectorSigns.hmacSha256Hex(appSecret, apiPath + ConnectorSigns.concatSorted(new TreeMap<>(params))).toUpperCase();
    }

    private static TreeMap<String, String> signed(StoreCredentials c, String apiPath, Map<String, String> business, boolean withToken) {
        TreeMap<String, String> p = new TreeMap<>(business);
        p.put("app_key", c.require("appKey"));
        p.put("timestamp", String.valueOf(System.currentTimeMillis()));
        p.put("sign_method", "sha256");
        if (withToken) {
            p.put("access_token", c.require("accessToken"));
        }
        p.put("sign", sign(c.require("appSecret"), apiPath, p));
        return p;
    }

    // ---------------------------------------------------------------- 地址

    static String apiBase(ConnectorContext ctx) {
        if (ctx.baseUrlOverride() != null) {
            return ctx.baseUrlOverride();
        }
        String region = region(ctx.credentials());
        String url = region == null ? null : ENDPOINTS.get(region);
        if (url == null) {
            throw new BizException("ecom.error.connectorRegionUnsupported", PLATFORM, String.valueOf(region), String.join(" / ", ENDPOINTS.keySet()));
        }
        return url;
    }

    private static String authBase(ConnectorContext ctx) {
        return ctx.baseUrlOverride() != null ? ctx.baseUrlOverride() : AUTH_BASE;
    }

    private static String region(StoreCredentials c) {
        String r = c.get("region");
        return r == null ? null : r.toUpperCase();
    }

    // ---------------------------------------------------------------- 授权

    @Override
    public String authorizeUrl(ConnectorContext ctx, String redirectUri, String state) {
        String page = ctx.baseUrlOverride() != null ? ctx.baseUrlOverride() + "/oauth/authorize" : AUTHORIZE_URL;
        Map<String, String> q = new LinkedHashMap<>();
        q.put("response_type", "code");
        q.put("force_auth", "true");
        q.put("redirect_uri", redirectUri);
        q.put("client_id", ctx.credentials().require("appKey"));
        q.put("state", state);
        return page + "?" + ConnectorSigns.query(q);
    }

    @Override
    public OAuthResult exchangeCode(ConnectorContext ctx, Map<String, String> params, String redirectUri) {
        String code = params.get("code");
        if (code == null || code.isBlank()) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, "code missing");
        }
        JsonNode resp = authCall(ctx, "/auth/token/create", Map.of("code", code));
        Map<String, String> creds = tokenFields(resp);
        String region = region(ctx.credentials());
        String country = HttpJson.text(resp, "country");
        JsonNode chosen = null;
        for (JsonNode u : resp.path("country_user_info")) {
            String uc = HttpJson.text(u, "country");
            if (chosen == null || (region != null && uc != null && region.equalsIgnoreCase(uc))) {
                chosen = u;
            }
        }
        if (chosen != null) {
            country = HttpJson.text(chosen, "country");
        }
        String sellerId = chosen == null ? null : HttpJson.text(chosen, "seller_id");
        if (country != null) {
            creds.put("country", country);
            if (region == null && ENDPOINTS.containsKey(country.toUpperCase())) {
                creds.put("region", country.toUpperCase());
            }
        }
        if (sellerId != null) {
            creds.put("sellerId", sellerId);
        }
        String account = HttpJson.text(resp, "account");
        if (account != null) {
            creds.put("account", account);
        }
        return new OAuthResult(creds, sellerId);
    }

    @Override
    public void ensureToken(ConnectorContext ctx) {
        StoreCredentials c = ctx.credentials();
        long expireAt = parseLongOr(c.get("expiresAt"));
        if (c.get("accessToken") != null && expireAt - REFRESH_AHEAD_SECONDS > Instant.now().getEpochSecond()) {
            return;
        }
        JsonNode resp = authCall(ctx, "/auth/token/refresh", Map.of("refresh_token", c.require("refreshToken")));
        ctx.updateCredentials(tokenFields(resp));
    }

    private static JsonNode authCall(ConnectorContext ctx, String path, Map<String, String> business) {
        TreeMap<String, String> p = signed(ctx.credentials(), path, business, false);
        JsonNode resp = HttpJson.postForm(authBase(ctx) + path, Map.of(), ConnectorSigns.query(p), PLATFORM);
        if (!isOk(resp) || HttpJson.text(resp, "access_token") == null) {
            throw new BizException("ecom.error.connectorAuthFailed", PLATFORM, errorSummary(resp));
        }
        return resp;
    }

    private static Map<String, String> tokenFields(JsonNode resp) {
        long now = Instant.now().getEpochSecond();
        Map<String, String> m = new LinkedHashMap<>();
        m.put("accessToken", HttpJson.text(resp, "access_token"));
        String refresh = HttpJson.text(resp, "refresh_token");
        if (refresh != null) {
            m.put("refreshToken", refresh);
        }
        m.put("expiresAt", String.valueOf(now + resp.path("expires_in").asLong(0)));
        if (resp.has("refresh_expires_in")) {
            m.put("refreshExpiresAt", String.valueOf(now + resp.path("refresh_expires_in").asLong(0)));
        }
        return m;
    }

    // ---------------------------------------------------------------- 订单

    @Override
    public List<OrderDraft> fetchOrders(ConnectorContext ctx, LocalDateTime since) {
        ensureToken(ctx);
        String base = apiBase(ctx);
        StoreCredentials c = ctx.credentials();
        String after = (since == null ? LocalDateTime.now().minusDays(3) : since).atZone(ZoneId.systemDefault()).toOffsetDateTime()
                .withNano(0).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Map<String, JsonNode> orders = new LinkedHashMap<>();
        int offset = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            Map<String, String> q = new LinkedHashMap<>();
            q.put("update_after", after);
            q.put("sort_by", "updated_at");
            q.put("sort_direction", "ASC");
            q.put("offset", String.valueOf(offset));
            q.put("limit", String.valueOf(ORDER_PAGE_SIZE));
            JsonNode data = get(base, c, "/orders/get", q).path("data");
            JsonNode list = data.path("orders");
            for (JsonNode o : list) {
                orders.put(HttpJson.text(o, "order_id"), o);
            }
            offset += list.size();
            int total = data.path("countTotal").asInt(-1);
            if (list.size() < ORDER_PAGE_SIZE || (total >= 0 && offset >= total)) {
                break;
            }
        }
        Map<String, List<JsonNode>> itemsByOrder = new LinkedHashMap<>();
        List<String> ids = new ArrayList<>(orders.keySet());
        for (int i = 0; i < ids.size(); i += ITEMS_BATCH) {
            List<String> batch = ids.subList(i, Math.min(i + ITEMS_BATCH, ids.size()));
            JsonNode data = get(base, c, "/orders/items/get", Map.of("order_ids", "[" + String.join(",", batch) + "]")).path("data");
            for (JsonNode o : data) {
                List<JsonNode> items = itemsByOrder.computeIfAbsent(HttpJson.text(o, "order_id"), k -> new ArrayList<>());
                o.path("order_items").forEach(items::add);
            }
        }
        String region = region(c);
        List<OrderDraft> drafts = new ArrayList<>();
        orders.forEach((id, o) -> drafts.add(toDraft(ctx.store(), region, o, itemsByOrder.getOrDefault(id, List.of()))));
        return drafts;
    }

    static OrderDraft toDraft(EcomStore store, String region, JsonNode o, List<JsonNode> items) {
        OrderDraft d = new OrderDraft();
        d.setStoreId(store.getId());
        d.setPlatformOrderNo(HttpJson.text(o, "order_id"));
        d.setOrderTime(time(HttpJson.text(o, "created_at")));
        List<String> statuses = new ArrayList<>();
        o.path("statuses").forEach(s -> statuses.add(s.asString()));
        if (statuses.isEmpty()) {
            items.forEach(it -> statuses.add(HttpJson.text(it, "status")));
        }
        d.setStatus(orderStatus(statuses));
        d.setBuyerName(join(HttpJson.text(o, "customer_first_name"), HttpJson.text(o, "customer_last_name")));
        d.setNote(HttpJson.text(o, "remarks"));
        JsonNode addr = o.path("address_shipping");
        d.setShipName(join(HttpJson.text(addr, "first_name"), HttpJson.text(addr, "last_name")));
        d.setShipPhone(HttpJson.text(addr, "phone"));
        d.setShipCountry(region != null ? region : HttpJson.text(addr, "country"));
        d.setShipState(HttpJson.text(addr, "address3"));
        d.setShipCity(HttpJson.text(addr, "city"));
        d.setShipPostcode(HttpJson.text(addr, "post_code"));
        d.setShipAddress(join(HttpJson.text(addr, "address1"), HttpJson.text(addr, "address2"), HttpJson.text(addr, "address5")));
        boolean platform = false;
        String currency = null;
        for (JsonNode it : items) {
            d.getLines().add(toLine(it));
            platform |= OWN_WAREHOUSE.equalsIgnoreCase(HttpJson.text(it, "shipping_type"));
            if (currency == null) {
                currency = HttpJson.text(it, "currency");
            }
            if (d.getShipBy() == null) {
                d.setShipBy(time(HttpJson.text(it, "promised_shipping_time")));
            }
        }
        d.setFulfillment(platform ? FulfillmentType.PLATFORM : FulfillmentType.SELLER);
        d.setCurrency(currency != null ? currency : region == null ? null : CURRENCIES.get(region));
        return d;
    }

    private static OrderDraft.Line toLine(JsonNode it) {
        OrderDraft.Line l = new OrderDraft.Line();
        l.setPlatformLineId(HttpJson.text(it, "order_item_id"));
        String productId = HttpJson.text(it, "product_id");
        String skuId = HttpJson.text(it, "sku_id");
        l.setPlatformItemId(productId == null ? skuId : skuId == null ? productId : productId + ":" + skuId);
        l.setSellerSku(firstNonBlank(HttpJson.text(it, "sku"), HttpJson.text(it, "shop_sku"), l.getPlatformItemId()));
        String variation = HttpJson.text(it, "variation");
        String name = HttpJson.text(it, "name");
        l.setTitle(variation == null || variation.isBlank() ? name : name + " " + variation);
        l.setQty(BigDecimal.ONE);
        BigDecimal price = decimal(it, "item_price");
        if (price == null) {
            price = decimal(it, "paid_price");
        }
        l.setPrice(price);
        l.setAmount(price);
        l.setShipping(decimal(it, "shipping_amount"));
        l.setTax(decimal(it, "tax_amount"));
        BigDecimal voucher = decimal(it, "voucher_seller");
        l.setDiscount(voucher != null && voucher.signum() > 0 ? voucher : null);
        return l;
    }

    /** 单行状态 */
    static EcomOrderStatus itemStatus(String s) {
        if (s == null) {
            return EcomOrderStatus.UNSHIPPED;
        }
        String v = s.toLowerCase();
        if (v.startsWith("return")) {
            return EcomOrderStatus.REFUNDED;
        }
        return switch (v) {
            case "unpaid" -> EcomOrderStatus.PENDING;
            case "shipped", "delivered", "confirmed" -> EcomOrderStatus.SHIPPED;
            case "canceled", "cancelled", "failed", "failed_delivery", "shipped_back", "shipped_back_success",
                 "lost_by_3pl", "damaged_by_3pl" -> EcomOrderStatus.CANCELLED;
            // pending、packed、repacked、topack、toship、ready_to_ship、ready_to_ship_pending 等
            default -> EcomOrderStatus.UNSHIPPED;
        };
    }

    /** 多行状态汇总：待付款 &gt; 待发货 &gt; 已发货 &gt; 已退款 &gt; 已取消 */
    static EcomOrderStatus orderStatus(List<String> statuses) {
        Set<EcomOrderStatus> set = new HashSet<>();
        statuses.forEach(s -> set.add(itemStatus(s)));
        for (EcomOrderStatus s : List.of(EcomOrderStatus.PENDING, EcomOrderStatus.UNSHIPPED, EcomOrderStatus.SHIPPED,
                EcomOrderStatus.REFUNDED, EcomOrderStatus.CANCELLED)) {
            if (set.contains(s)) {
                return s;
            }
        }
        return EcomOrderStatus.UNSHIPPED;
    }

    // ---------------------------------------------------------------- 发货

    /**
     * 打包（pending / repacked 的行）后通知揽收（含已打包的包裹）；ERP 的运单号不会写到平台（Lazada 由平台分配物流单号）。
     * 明细有订单行 ID 时只处理这些行，否则处理整单
     */
    @Override
    public void confirmShipment(ConnectorContext ctx, EcomOrder order, List<EcomOrderItem> items) {
        ensureToken(ctx);
        String base = apiBase(ctx);
        StoreCredentials c = ctx.credentials();
        Set<String> wanted = new HashSet<>();
        for (EcomOrderItem it : items) {
            if (it.getPlatformLineId() != null) {
                wanted.add(it.getPlatformLineId());
            }
        }
        JsonNode remote = get(base, c, "/order/items/get", Map.of("order_id", order.getPlatformOrderNo())).path("data");
        List<Object> toPack = new ArrayList<>();
        Set<String> packages = new LinkedHashSet<>();
        for (JsonNode it : remote) {
            String id = HttpJson.text(it, "order_item_id");
            if (id == null || (!wanted.isEmpty() && !wanted.contains(id))) {
                continue;
            }
            String status = HttpJson.text(it, "status");
            if (status != null && PACKABLE.contains(status.toLowerCase())) {
                toPack.add(idValue(id));
            } else if (PACKED.equalsIgnoreCase(status) && HttpJson.text(it, "package_id") != null) {
                packages.add(HttpJson.text(it, "package_id"));
            }
        }
        if (!toPack.isEmpty()) {
            String allocate = c.get("shippingAllocateType") == null ? "TFS" : c.get("shippingAllocateType").toUpperCase();
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("pack_order_list", List.of(Map.of("order_id", idValue(order.getPlatformOrderNo()), "order_item_list", toPack)));
            req.put("delivery_type", "dropship");
            req.put("shipping_allocate_type", allocate);
            JsonNode resp = post(base, c, "/order/fulfill/pack", Map.of("packReq", HttpJson.MAPPER.writeValueAsString(req)));
            collectPackages(resp.path("result"), packages);
        }
        if (packages.isEmpty()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "no pending / packed items in order " + order.getPlatformOrderNo());
        }
        List<Map<String, String>> list = packages.stream().map(p -> Map.of("package_id", p)).toList();
        JsonNode resp = post(base, c, "/order/package/rts", Map.of("readyToShipReq", HttpJson.MAPPER.writeValueAsString(Map.of("packages", list))));
        collectPackages(resp.path("result"), new LinkedHashSet<>());
    }

    /** 检查 result.success 与每个包裹 / 行的 item_err_code，收集 package_id */
    private static void collectPackages(JsonNode result, Set<String> packages) {
        if (result.has("success") && !result.path("success").asBoolean(false)) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(result.toString()));
        }
        walk(result.path("data"), packages);
    }

    private static void walk(JsonNode node, Set<String> packages) {
        if (node.isArray()) {
            node.forEach(n -> walk(n, packages));
        } else if (node.isObject()) {
            if (node.has("item_err_code") && !"0".equals(node.path("item_err_code").asString())) {
                throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(node.toString()));
            }
            String pkg = HttpJson.text(node, "package_id");
            if (pkg != null && !pkg.isBlank()) {
                packages.add(pkg);
            }
            node.properties().forEach(e -> {
                if (e.getValue().isContainer()) {
                    walk(e.getValue(), packages);
                }
            });
        }
    }

    // ---------------------------------------------------------------- 库存

    /** /product/price_quantity/update，XML payload，每次最多 50 个 SKU；没有平台商品 ID 时只按 SellerSku 更新 */
    @Override
    public void pushInventory(ConnectorContext ctx, List<InventoryUpdate> updates) {
        ensureToken(ctx);
        String base = apiBase(ctx);
        for (int i = 0; i < updates.size(); i += STOCK_BATCH) {
            String payload = stockPayload(updates.subList(i, Math.min(i + STOCK_BATCH, updates.size())));
            post(base, ctx.credentials(), "/product/price_quantity/update", Map.of("payload", payload));
        }
    }

    static String stockPayload(List<InventoryUpdate> updates) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Request><Product><Skus>");
        for (InventoryUpdate u : updates) {
            sb.append("<Sku>");
            String key = u.platformItemId();
            if (key != null && !key.isBlank()) {
                int idx = key.indexOf(':');
                if (idx > 0) {
                    sb.append("<ItemId>").append(xml(key.substring(0, idx))).append("</ItemId>");
                    sb.append("<SkuId>").append(xml(key.substring(idx + 1))).append("</SkuId>");
                } else {
                    sb.append("<ItemId>").append(xml(key)).append("</ItemId>");
                }
            }
            if (u.sellerSku() != null) {
                sb.append("<SellerSku>").append(xml(u.sellerSku())).append("</SellerSku>");
            }
            sb.append("<Quantity>").append(u.qty() == null ? 0 : Math.max(0, u.qty().intValue())).append("</Quantity>");
            sb.append("</Sku>");
        }
        return sb.append("</Skus></Product></Request>").toString();
    }

    // ---------------------------------------------------------------- 商品

    @Override
    public List<ListingDraft> fetchListings(ConnectorContext ctx) {
        ensureToken(ctx);
        String base = apiBase(ctx);
        StoreCredentials c = ctx.credentials();
        String region = region(c);
        String currency = region == null ? null : CURRENCIES.get(region);
        List<ListingDraft> drafts = new ArrayList<>();
        int offset = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            Map<String, String> q = new LinkedHashMap<>();
            q.put("filter", "all");
            q.put("offset", String.valueOf(offset));
            q.put("limit", String.valueOf(PRODUCT_PAGE_SIZE));
            JsonNode data = get(base, c, "/products/get", q).path("data");
            JsonNode products = data.path("products");
            for (JsonNode p : products) {
                String itemId = HttpJson.text(p, "item_id");
                String title = HttpJson.text(p.path("attributes"), "name");
                String productStatus = HttpJson.text(p, "status");
                for (JsonNode s : p.path("skus")) {
                    String skuId = HttpJson.text(s, "SkuId");
                    String key = skuId == null ? itemId : itemId + ":" + skuId;
                    String status = listingStatus(HttpJson.text(s, "Status"), productStatus);
                    JsonNode images = s.path("Images");
                    String image = images.path(0).isMissingNode() ? str(p.path("images").path(0)) : str(images.path(0));
                    drafts.add(new ListingDraft(firstNonBlank(HttpJson.text(s, "SellerSku"), HttpJson.text(s, "ShopSku"), key), key, itemId, title,
                            variant(s.path("saleProp")), decimal(s, "price"), currency, decimal(s, "quantity"), null, status, image,
                            HttpJson.text(s, "Url")));
                }
            }
            offset += products.size();
            int total = data.path("total_products").asInt(-1);
            if (products.size() < PRODUCT_PAGE_SIZE || (total >= 0 && offset >= total)) {
                break;
            }
        }
        return drafts;
    }

    private static String variant(JsonNode saleProp) {
        List<String> parts = new ArrayList<>();
        if (saleProp.isObject()) {
            saleProp.properties().forEach(e -> {
                if (!e.getValue().isNull() && !e.getValue().asString().isBlank()) {
                    parts.add(e.getValue().asString());
                }
            });
        }
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    static String listingStatus(String skuStatus, String productStatus) {
        String s = skuStatus != null ? skuStatus : productStatus;
        if (s == null) {
            return null;
        }
        ListingStatus st = switch (s.toLowerCase()) {
            case "active" -> ListingStatus.ACTIVE;
            case "inactive", "suspended" -> ListingStatus.INACTIVE;
            case "pending qc", "rejected", "draft" -> ListingStatus.INCOMPLETE;
            case "deleted" -> ListingStatus.DELETED;
            default -> null;
        };
        return st == null ? null : st.name();
    }

    // ---------------------------------------------------------------- HTTP

    private static JsonNode get(String base, StoreCredentials c, String path, Map<String, String> business) {
        TreeMap<String, String> p = signed(c, path, business, true);
        return check(HttpJson.get(base + path + "?" + ConnectorSigns.query(p), Map.of(), PLATFORM));
    }

    private static JsonNode post(String base, StoreCredentials c, String path, Map<String, String> business) {
        TreeMap<String, String> p = signed(c, path, business, true);
        return check(HttpJson.postForm(base + path, Map.of(), ConnectorSigns.query(p), PLATFORM));
    }

    private static JsonNode check(JsonNode resp) {
        if (!isOk(resp)) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, errorSummary(resp));
        }
        return resp;
    }

    private static boolean isOk(JsonNode resp) {
        String code = HttpJson.text(resp, "code");
        return code == null || "0".equals(code);
    }

    private static String errorSummary(JsonNode resp) {
        return HttpJson.abbreviate(HttpJson.text(resp, "code") + ": " + HttpJson.text(resp, "message")
                + (resp.has("detail") ? " " + resp.path("detail") : ""));
    }

    // ---------------------------------------------------------------- 工具

    /** "2014-10-21 12:29:08 +0800" → 系统默认时区；没有时区的按系统默认时区 */
    static LocalDateTime time(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(s.trim(), LAZADA_TIME).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (RuntimeException e) {
            try {
                return LocalDateTime.parse(s.trim(), LAZADA_TIME_NO_ZONE);
            } catch (RuntimeException e2) {
                return null;
            }
        }
    }

    /** 金额字符串可能带千分位逗号 */
    private static BigDecimal decimal(JsonNode node, String field) {
        String v = HttpJson.text(node, field);
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(v.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 数字 ID 按数字传（Lazada 示例中 order_id、order_item_id 是数字） */
    private static Object idValue(String s) {
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return s;
        }
    }

    private static String xml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static long parseLongOr(String s) {
        if (s == null) {
            return 0;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String str(JsonNode v) {
        return v == null || v.isMissingNode() || v.isNull() ? null : v.asString();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
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
