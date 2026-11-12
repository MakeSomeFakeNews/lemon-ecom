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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 速卖通开放平台（TOP 协议，网关 gw.api.taobao.com/router/rest）：
 * <ul>
 *   <li>订单：aliexpress.solution.order.get 分页拉取，aliexpress.solution.order.info.get 补收货地址</li>
 *   <li>回传发货：aliexpress.solution.order.fulfill</li>
 *   <li>在线商品：aliexpress.solution.product.list.get 按商品状态（onSelling 在售、offline 下架、auditing 审核中、
 *       editingRequired 待修改）分页，再逐个商品 aliexpress.solution.product.info.get 取 SKU（sku_code、价格、ipm_sku_stock 库存）</li>
 *   <li>回传库存：aliexpress.solution.batch.product.inventory.update，按商品分组，每次最多 20 个商品</li>
 * </ul>
 * 授权信息：appKey、appSecret、accessToken（授权后得到的 session）。
 * <p>
 * 编码约定：在线商品与订单明细的 platformItemId 均为速卖通商品 ID（product_id），sellerSku 为卖家填写的商品编码 sku_code；
 * 没有 sku_code 的 SKU 以「商品 ID:SKU ID」（如 1005001234:200000182:193）作为 sellerSku，这类 SKU 不能回传库存
 * （库存接口按 sku_code 定位），请在速卖通后台补填商品编码。回传库存时 InventoryUpdate.platformItemId 必须是商品 ID（先拉取在线商品）。
 * <p>
 * 说明：按开放平台公开文档实现（MD5 签名：secret + 按键排序的参数名值串 + secret，结果大写），尚未用真实店铺联调；
 * 速卖通已在推广新版开放平台（api-sg.aliexpress.com，HMAC-SHA256 签名），老应用仍可使用 TOP 网关，如需迁移另行适配
 */
@Component
public class AliExpressConnector implements ChannelConnector {

    static final String PLATFORM = "AliExpress";
    private static final String GATEWAY = "https://gw.api.taobao.com/router/rest";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAGE_SIZE = 50;
    private static final int MAX_PAGES = 20;
    private static final int MAX_LISTING_PAGES = 100;
    /** 批量改库存每次的商品数上限 */
    private static final int INVENTORY_BATCH = 20;
    /** 商品状态 → 在线商品状态（按此顺序拉取） */
    private static final Map<String, ListingStatus> PRODUCT_STATUSES = new java.util.LinkedHashMap<>();

    static {
        PRODUCT_STATUSES.put("onSelling", ListingStatus.ACTIVE);
        PRODUCT_STATUSES.put("offline", ListingStatus.INACTIVE);
        PRODUCT_STATUSES.put("auditing", ListingStatus.INACTIVE);
        PRODUCT_STATUSES.put("editingRequired", ListingStatus.INCOMPLETE);
    }
    private static final ConnectorInfo INFO = new ConnectorInfo(EcomPlatform.ALIEXPRESS, AuthType.KEYS, List.of(
            CredentialField.text("appKey", "ecom.label.cred.appKey", true),
            CredentialField.secret("appSecret", "ecom.label.cred.appSecret"),
            CredentialField.secret("accessToken", "ecom.label.cred.accessToken")),
            GATEWAY, null, java.util.EnumSet.of(ConnectorCapability.ORDERS, ConnectorCapability.SHIP_CONFIRM,
                    ConnectorCapability.INVENTORY_PUSH, ConnectorCapability.LISTINGS),
            "https://open.aliexpress.com/");

    @Override
    public ConnectorInfo info() {
        return INFO;
    }

    @Override
    public List<OrderDraft> fetchOrders(ConnectorContext ctx, LocalDateTime since) {
        EcomStore store = ctx.store();
        StoreCredentials c = ctx.credentials();
        String gateway = ctx.baseUrl(INFO);
        List<OrderDraft> drafts = new ArrayList<>();
        int page = 1;
        while (page <= MAX_PAGES) {
            String query = HttpJson.MAPPER.writeValueAsString(Map.of("page_size", PAGE_SIZE, "current_page", page,
                    "modified_date_start", since.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("Asia/Shanghai")).format(TS)));
            JsonNode resp = call(gateway, c, "aliexpress.solution.order.get", Map.of("param_aeop_order_query", query));
            JsonNode result = resp.path("aliexpress_solution_order_get_response").path("result");
            JsonNode list = result.path("target_list").path("order_dto");
            if (!list.isArray() || list.isEmpty()) {
                break;
            }
            for (JsonNode o : list) {
                OrderDraft d = toDraft(store, o);
                JsonNode detail = call(gateway, c, "aliexpress.solution.order.info.get",
                        Map.of("param1", HttpJson.MAPPER.writeValueAsString(Map.of("order_id", d.getPlatformOrderNo()))))
                        .path("aliexpress_solution_order_info_get_response").path("result");
                fillDetail(d, detail);
                drafts.add(d);
            }
            int total = result.path("total_count").asInt(0);
            if (page * PAGE_SIZE >= total) {
                break;
            }
            page++;
        }
        return drafts;
    }

    private static OrderDraft toDraft(EcomStore store, JsonNode o) {
        OrderDraft d = new OrderDraft();
        d.setStoreId(store.getId());
        d.setPlatformOrderNo(HttpJson.text(o, "order_id"));
        d.setOrderTime(time(HttpJson.text(o, "gmt_create")));
        d.setPaidTime(time(HttpJson.text(o, "gmt_pay_time")));
        d.setStatus(status(HttpJson.text(o, "order_status")));
        d.setFulfillment(FulfillmentType.SELLER);
        d.setCurrency(HttpJson.text(o.path("order_amount"), "currency_code"));
        d.setBuyerName(HttpJson.text(o, "buyer_signer_fullname"));
        d.setShipBy(time(HttpJson.text(o, "timeout_left_time")));
        for (JsonNode p : o.path("product_list").path("order_product_dto")) {
            OrderDraft.Line l = new OrderDraft.Line();
            l.setPlatformLineId(HttpJson.text(p, "child_id"));
            l.setSellerSku(HttpJson.text(p, "sku_code"));
            l.setPlatformItemId(HttpJson.text(p, "product_id"));
            l.setTitle(HttpJson.text(p, "product_name"));
            BigDecimal qty = HttpJson.decimal(p, "product_count");
            l.setQty(qty == null ? BigDecimal.ONE : qty);
            l.setPrice(HttpJson.decimal(p, "product_unit_price", "amount"));
            l.setAmount(HttpJson.decimal(p, "total_product_amount", "amount"));
            l.setShipping(HttpJson.decimal(p, "logistics_amount", "amount"));
            d.getLines().add(l);
        }
        return d;
    }

    private static void fillDetail(OrderDraft d, JsonNode detail) {
        JsonNode addr = detail.path("receipt_address");
        if (addr.isMissingNode() || addr.isNull()) {
            return;
        }
        d.setShipName(HttpJson.text(addr, "contact_person"));
        d.setShipPhone(join(HttpJson.text(addr, "phone_country"), HttpJson.text(addr, "mobile_no")));
        d.setShipCountry(HttpJson.text(addr, "country"));
        d.setShipState(HttpJson.text(addr, "province"));
        d.setShipCity(HttpJson.text(addr, "city"));
        d.setShipPostcode(HttpJson.text(addr, "zip"));
        d.setShipAddress(join(HttpJson.text(addr, "detail_address"), HttpJson.text(addr, "address2")));
    }

    @Override
    public void confirmShipment(ConnectorContext ctx, EcomOrder order, List<EcomOrderItem> items) {
        StoreCredentials c = ctx.credentials();
        Map<String, String> params = new TreeMap<>();
        params.put("service_name", order.getCarrier());
        params.put("logistics_no", order.getTrackingNo());
        params.put("out_ref", order.getPlatformOrderNo());
        params.put("send_type", "all");
        params.put("description", "shipped");
        JsonNode resp = call(ctx.baseUrl(INFO), c, "aliexpress.solution.order.fulfill", params);
        JsonNode result = resp.path("aliexpress_solution_order_fulfill_response").path("result");
        if (!result.path("result_success").asBoolean(false)) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(result.toString()));
        }
    }

    // ---------------------------------------------------------------- 在线商品与库存

    @Override
    public List<ListingDraft> fetchListings(ConnectorContext ctx) {
        StoreCredentials c = ctx.credentials();
        String gateway = ctx.baseUrl(INFO);
        String storeCurrency = ctx.store() == null ? null : ctx.store().getCurrency();
        List<ListingDraft> drafts = new ArrayList<>();
        for (Map.Entry<String, ListingStatus> st : PRODUCT_STATUSES.entrySet()) {
            int page = 1;
            while (page <= MAX_LISTING_PAGES) {
                String query = HttpJson.MAPPER.writeValueAsString(Map.of("current_page", page, "page_size", PAGE_SIZE,
                        "product_status_type", st.getKey()));
                JsonNode result = call(gateway, c, "aliexpress.solution.product.list.get", Map.of("aeop_a_e_product_list_query", query))
                        .path("aliexpress_solution_product_list_get_response").path("result");
                JsonNode list = result.path("aeop_a_e_product_display_d_t_o_list").path("item_display_dto");
                if (!list.isArray() || list.isEmpty()) {
                    break;
                }
                for (JsonNode p : list) {
                    String productId = HttpJson.text(p, "product_id");
                    JsonNode info = call(gateway, c, "aliexpress.solution.product.info.get", Map.of("product_id", productId))
                            .path("aliexpress_solution_product_info_get_response").path("result");
                    drafts.addAll(toListings(p, info, st.getValue(), storeCurrency));
                }
                int totalPage = result.path("total_page").asInt(page);
                if (page >= totalPage) {
                    break;
                }
                page++;
            }
        }
        return drafts;
    }

    private static List<ListingDraft> toListings(JsonNode summary, JsonNode info, ListingStatus status, String storeCurrency) {
        String productId = HttpJson.text(summary, "product_id");
        String title = HttpJson.text(summary, "subject");
        if (title == null) {
            title = HttpJson.text(info, "subject");
        }
        String image = firstImage(HttpJson.text(summary, "image_u_r_ls"));
        if (image == null) {
            image = firstImage(HttpJson.text(info, "image_u_r_ls"));
        }
        String productCurrency = HttpJson.text(info, "currency_code");
        if (productCurrency == null) {
            productCurrency = HttpJson.text(summary, "currency_code");
        }
        String url = productId == null ? null : "https://www.aliexpress.com/item/" + productId + ".html";
        List<ListingDraft> drafts = new ArrayList<>();
        JsonNode skus = info.path("aeop_ae_product_s_k_us").path("global_aeop_ae_product_sku");
        for (JsonNode sku : skus) {
            String code = HttpJson.text(sku, "sku_code");
            String sellerSku = code != null && !code.isBlank() ? code : productId + ":" + HttpJson.text(sku, "id");
            String currency = HttpJson.text(sku, "currency_code");
            BigDecimal price = HttpJson.decimal(sku, "sku_price");
            BigDecimal qty = HttpJson.decimal(sku, "ipm_sku_stock");
            if (qty == null) {
                qty = HttpJson.decimal(sku, "s_k_u_available_stock");
            }
            drafts.add(new ListingDraft(sellerSku, productId, null, title, variant(sku), price,
                    currency != null ? currency : productCurrency != null ? productCurrency : storeCurrency, qty,
                    FulfillmentType.SELLER.name(), status.name(), image, url));
        }
        if (drafts.isEmpty() && productId != null) {
            drafts.add(new ListingDraft(productId, productId, null, title, null, HttpJson.decimal(summary, "product_min_price"),
                    productCurrency != null ? productCurrency : storeCurrency, null, FulfillmentType.SELLER.name(), status.name(), image, url));
        }
        return drafts;
    }

    /** SKU 属性（颜色、尺码等）拼成变体名称：优先卖家自定义名称 */
    private static String variant(JsonNode sku) {
        List<String> parts = new ArrayList<>();
        for (JsonNode p : sku.path("aeop_s_k_u_property_list").path("global_aeop_sku_property")) {
            String name = HttpJson.text(p, "property_value_definition_name");
            if (name == null || name.isBlank()) {
                name = HttpJson.text(p, "sku_property_value");
            }
            if (name != null && !name.isBlank()) {
                parts.add(name.trim());
            }
        }
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    private static String firstImage(String urls) {
        if (urls == null || urls.isBlank()) {
            return null;
        }
        String first = urls.split(";")[0].trim();
        return first.isEmpty() ? null : first;
    }

    @Override
    public void pushInventory(ConnectorContext ctx, List<InventoryUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        Map<String, List<Map<String, Object>>> byProduct = new LinkedHashMap<>();
        for (InventoryUpdate u : updates) {
            if (u.platformItemId() == null || u.platformItemId().isBlank()) {
                throw new BizException("ecom.error.connectorItemIdMissing", u.sellerSku());
            }
            Map<String, Object> sku = new LinkedHashMap<>();
            sku.put("sku_code", u.sellerSku());
            sku.put("inventory", u.qty() == null ? 0 : Math.max(0, u.qty().setScale(0, java.math.RoundingMode.DOWN).intValue()));
            byProduct.computeIfAbsent(u.platformItemId().trim(), k -> new ArrayList<>()).add(sku);
        }
        List<Map<String, Object>> products = new ArrayList<>();
        byProduct.forEach((productId, skus) -> {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("product_id", productId.chars().allMatch(Character::isDigit) && productId.length() < 19 ? (Object) Long.valueOf(productId) : productId);
            p.put("multiple_sku_update_list", skus);
            products.add(p);
        });
        String gateway = ctx.baseUrl(INFO);
        for (int i = 0; i < products.size(); i += INVENTORY_BATCH) {
            List<Map<String, Object>> batch = products.subList(i, Math.min(products.size(), i + INVENTORY_BATCH));
            JsonNode resp = call(gateway, ctx.credentials(), "aliexpress.solution.batch.product.inventory.update",
                    Map.of("mutiple_product_update_list", HttpJson.MAPPER.writeValueAsString(batch)))
                    .path("aliexpress_solution_batch_product_inventory_update_response");
            if (!resp.path("update_success").asBoolean(false)) {
                String msg = HttpJson.text(resp, "update_error_message");
                throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(
                        (HttpJson.text(resp, "update_error_code") == null ? "" : HttpJson.text(resp, "update_error_code") + " ")
                                + (msg == null ? resp.toString() : msg)));
            }
        }
    }

    /** 调用 TOP 接口：系统参数 + 业务参数，MD5 签名，错误响应转为业务异常 */
    private static JsonNode call(String gateway, StoreCredentials c, String method, Map<String, String> business) {
        TreeMap<String, String> params = new TreeMap<>(business);
        params.put("method", method);
        params.put("app_key", c.require("appKey"));
        params.put("session", c.require("accessToken"));
        params.put("timestamp", LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(TS));
        params.put("format", "json");
        params.put("v", "2.0");
        params.put("sign_method", "md5");
        params.put("sign", ConnectorSigns.md5Upper(c.require("appSecret") + ConnectorSigns.concatSorted(params) + c.require("appSecret")));
        StringBuilder form = new StringBuilder();
        params.forEach((k, v) -> {
            if (!form.isEmpty()) form.append('&');
            form.append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        JsonNode resp = HttpJson.postForm(gateway, Map.of(), form.toString(), PLATFORM);
        JsonNode error = resp.path("error_response");
        if (!error.isMissingNode()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(error.toString()));
        }
        return resp;
    }


    private static EcomOrderStatus status(String s) {
        if (s == null) {
            return EcomOrderStatus.UNSHIPPED;
        }
        return switch (s) {
            case "PLACE_ORDER_SUCCESS", "WAIT_SELLER_EXAMINE_MONEY", "RISK_CONTROL" -> EcomOrderStatus.PENDING;
            case "WAIT_SELLER_SEND_GOODS", "SELLER_PART_SEND_GOODS" -> EcomOrderStatus.UNSHIPPED;
            case "WAIT_BUYER_ACCEPT_GOODS", "FUND_PROCESSING", "FINISH" -> EcomOrderStatus.SHIPPED;
            case "IN_ISSUE", "IN_FROZEN" -> EcomOrderStatus.REFUNDED;
            default -> EcomOrderStatus.CANCELLED;
        };
    }

    private static LocalDateTime time(String s) {
        if (s == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(s, TS);
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
