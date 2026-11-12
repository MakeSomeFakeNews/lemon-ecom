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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ozon Seller API（https://api-seller.ozon.ru）：请求头 Client-Id、Api-Key（卖家后台「设置 → API 密钥」生成），全部为 POST + JSON。
 * 授权信息：clientId、apiKey、warehouseId（FBS 仓库 ID，回传库存时必填，在卖家后台「物流 → 仓库」或 /v1/warehouse/list 查看）。
 * <ul>
 *   <li>订单：FBS / rFBS（卖家仓发货）POST /v3/posting/fbs/list，FBO（Ozon 仓配送，映射为 PLATFORM）POST /v2/posting/fbo/list，
 *       按 since / to 过滤、offset 分页。一个 Ozon 订单可能拆成多个货件（posting），本连接器以货件号 posting_number 作为平台订单号，
 *       发货、取消都按货件进行</li>
 *   <li>发货：POST /v4/posting/fbs/ship 备货（装箱）；Ozon 物流的运单与面单由平台生成，不需要回传运单号。
 *       自配送（rFBS、卖家自己的物流）在订单上有运单号时再调用 POST /v2/fbs/posting/tracking-number/set 回传</li>
 *   <li>库存：POST /v2/products/stocks（offer_id + warehouse_id，每次最多 100 条）</li>
 *   <li>在线商品：POST /v3/product/list（last_id 分页）+ POST /v3/product/info/list（按 product_id 批量取详情）</li>
 * </ul>
 * 编码约定：明细 sellerSku 为卖家货号 offer_id（Ozon 必填，无退路）；platformItemId 与 platformLineId 均为 Ozon SKU（products[].sku，
 * 备货接口按它指定商品；货件中没有独立的行 ID）；在线商品的 platformItemId 为 Ozon SKU、parentItemId 为 product_id。回传库存只按 offer_id。
 * <p>
 * 金额为商品单价（products[].price）× 数量，币种取商品的 currency_code（跨境店铺通常为 CNY 或 USD，本土店铺为 RUB）；时间为 UTC。
 * 注意：posting 列表按货件创建 / 开始处理时间过滤，不按更新时间，已拉过的货件状态变化只有落在同步时间窗内才会更新。
 * <p>
 * 沙箱：Ozon Seller API 没有公开的沙箱环境，联调用真实店铺（或 Ozon 提供的测试账号），因此沙箱地址为空。
 * 说明：按 Ozon Seller API 公开文档实现，尚未用真实店铺联调；Ozon 经常升级接口版本（如 v2 → v3），上线前按最新文档核对
 */
@Component
public class OzonConnector implements ChannelConnector {

    static final String PLATFORM = "Ozon";
    private static final String GATEWAY = "https://api-seller.ozon.ru";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int POSTING_PAGE_SIZE = 500;
    private static final int LISTING_PAGE_SIZE = 500;
    private static final int STOCK_BATCH = 100;
    private static final int MAX_PAGES = 20;
    private static final ConnectorInfo INFO = new ConnectorInfo(EcomPlatform.OZON, AuthType.KEYS, List.of(
            CredentialField.text("clientId", "ecom.label.cred.ozonClientId", true),
            CredentialField.secret("apiKey", "ecom.label.cred.ozonApiKey"),
            CredentialField.text("warehouseId", "ecom.label.cred.ozonWarehouseId", false)),
            GATEWAY, null,
            EnumSet.of(ConnectorCapability.ORDERS, ConnectorCapability.SHIP_CONFIRM, ConnectorCapability.INVENTORY_PUSH,
                    ConnectorCapability.LISTINGS),
            "https://docs.ozon.ru/api/seller/");

    @Override
    public ConnectorInfo info() {
        return INFO;
    }

    // ---------------------------------------------------------------- 订单

    @Override
    public List<OrderDraft> fetchOrders(ConnectorContext ctx, LocalDateTime since) {
        String base = ctx.baseUrl(INFO);
        Map<String, String> headers = headers(ctx.credentials());
        String from = utc(since);
        String to = utc(LocalDateTime.now().plusMinutes(1));
        List<OrderDraft> drafts = new ArrayList<>();
        // FBS / rFBS：result.postings + has_next
        int offset = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            Map<String, Object> body = listBody(from, to, offset);
            JsonNode result = HttpJson.postJson(base + "/v3/posting/fbs/list", headers, body, PLATFORM).path("result");
            JsonNode postings = result.path("postings");
            for (JsonNode p : postings) {
                drafts.add(toDraft(ctx.store(), p, FulfillmentType.SELLER));
            }
            if (!result.path("has_next").asBoolean(false) || postings.isEmpty()) {
                break;
            }
            offset += POSTING_PAGE_SIZE;
        }
        // FBO：result 为数组，不足一页即结束
        offset = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            Map<String, Object> body = listBody(from, to, offset);
            body.put("translit", true);
            JsonNode result = HttpJson.postJson(base + "/v2/posting/fbo/list", headers, body, PLATFORM).path("result");
            for (JsonNode p : result) {
                drafts.add(toDraft(ctx.store(), p, FulfillmentType.PLATFORM));
            }
            if (result.size() < POSTING_PAGE_SIZE) {
                break;
            }
            offset += POSTING_PAGE_SIZE;
        }
        return drafts;
    }

    private static Map<String, Object> listBody(String from, String to, int offset) {
        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("since", from);
        filter.put("to", to);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dir", "ASC");
        body.put("filter", filter);
        body.put("limit", POSTING_PAGE_SIZE);
        body.put("offset", offset);
        body.put("with", Map.of("analytics_data", true, "financial_data", false));
        return body;
    }

    private static OrderDraft toDraft(EcomStore store, JsonNode p, FulfillmentType fulfillment) {
        OrderDraft d = new OrderDraft();
        d.setStoreId(store.getId());
        d.setPlatformOrderNo(HttpJson.text(p, "posting_number"));
        String created = HttpJson.text(p, "created_at");
        d.setOrderTime(time(created == null ? HttpJson.text(p, "in_process_at") : created));
        d.setPaidTime(time(HttpJson.text(p, "in_process_at")));
        d.setStatus(status(HttpJson.text(p, "status")));
        d.setFulfillment(fulfillment);
        d.setShipBy(time(HttpJson.text(p, "shipment_date")));
        JsonNode addressee = p.path("addressee");
        JsonNode customer = p.path("customer");
        d.setShipName(first(HttpJson.text(addressee, "name"), HttpJson.text(customer, "name")));
        d.setShipPhone(first(HttpJson.text(addressee, "phone"), HttpJson.text(customer, "phone")));
        d.setBuyerName(HttpJson.text(customer, "name"));
        JsonNode address = customer.path("address");
        JsonNode analytics = p.path("analytics_data");
        d.setShipCountry(first(HttpJson.text(address, "country"), "RU"));
        d.setShipState(first(HttpJson.text(address, "region"), HttpJson.text(analytics, "region")));
        d.setShipCity(first(HttpJson.text(address, "city"), HttpJson.text(analytics, "city")));
        d.setShipPostcode(HttpJson.text(address, "zip_code"));
        d.setShipAddress(HttpJson.text(address, "address_tail"));
        d.setNote(HttpJson.text(p, "order_number"));
        for (JsonNode it : p.path("products")) {
            OrderDraft.Line l = new OrderDraft.Line();
            String sku = HttpJson.text(it, "sku");
            l.setPlatformLineId(sku);
            l.setPlatformItemId(sku);
            l.setSellerSku(HttpJson.text(it, "offer_id"));
            l.setTitle(HttpJson.text(it, "name"));
            BigDecimal qty = HttpJson.decimal(it, "quantity");
            l.setQty(qty == null ? BigDecimal.ONE : qty);
            BigDecimal price = HttpJson.decimal(it, "price");
            l.setPrice(price == null ? BigDecimal.ZERO : price);
            l.setAmount(l.getPrice().multiply(l.getQty()).setScale(2, RoundingMode.HALF_UP));
            if (d.getCurrency() == null) {
                d.setCurrency(HttpJson.text(it, "currency_code"));
            }
            d.getLines().add(l);
        }
        return d;
    }

    /**
     * 货件状态：awaiting_registration / acceptance_in_progress / awaiting_approve 待付款（待确认），
     * awaiting_packaging / awaiting_deliver / arbitration / client_arbitration 等待发货，
     * delivering / driver_pickup / sent_by_seller / delivered 已发货，cancelled / not_accepted / cancelled_from_split_pending 已取消
     */
    static EcomOrderStatus status(String s) {
        if (s == null) {
            return EcomOrderStatus.UNSHIPPED;
        }
        return switch (s) {
            case "awaiting_registration", "acceptance_in_progress", "awaiting_approve" -> EcomOrderStatus.PENDING;
            case "delivering", "driver_pickup", "sent_by_seller", "delivered" -> EcomOrderStatus.SHIPPED;
            case "cancelled", "not_accepted", "cancelled_from_split_pending" -> EcomOrderStatus.CANCELLED;
            default -> EcomOrderStatus.UNSHIPPED;
        };
    }

    // ---------------------------------------------------------------- 发货

    /**
     * 备货（/v4/posting/fbs/ship，全部商品装一个包裹），订单上有运单号时再回传运单号（自配送 rFBS）。
     * 商品按 Ozon SKU 指定：取明细的 platformItemId，为空取 platformLineId
     */
    @Override
    public void confirmShipment(ConnectorContext ctx, EcomOrder order, List<EcomOrderItem> items) {
        String base = ctx.baseUrl(INFO);
        Map<String, String> headers = headers(ctx.credentials());
        List<Map<String, Object>> products = new ArrayList<>();
        for (EcomOrderItem it : items) {
            String sku = it.getPlatformItemId() != null ? it.getPlatformItemId() : it.getPlatformLineId();
            if (sku == null) {
                throw new BizException("ecom.error.connectorLineIdMissing", order.getPlatformOrderNo());
            }
            long ozonSku;
            try {
                ozonSku = Long.parseLong(sku.trim());
            } catch (NumberFormatException e) {
                throw new BizException("ecom.error.connectorLineIdMissing", order.getPlatformOrderNo());
            }
            products.add(Map.of("product_id", ozonSku, "quantity", it.getQty() == null ? 1 : it.getQty().intValue()));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("packages", List.of(Map.of("products", products)));
        body.put("posting_number", order.getPlatformOrderNo());
        body.put("with", Map.of("additional_data", false));
        HttpJson.postJson(base + "/v4/posting/fbs/ship", headers, body, PLATFORM);
        if (order.getTrackingNo() != null && !order.getTrackingNo().isBlank()) {
            Map<String, Object> tn = Map.of("tracking_numbers",
                    List.of(Map.of("posting_number", order.getPlatformOrderNo(), "tracking_number", order.getTrackingNo())));
            JsonNode resp = HttpJson.postJson(base + "/v2/fbs/posting/tracking-number/set", headers, tn, PLATFORM);
            for (JsonNode r : resp.path("result")) {
                if (!r.path("result").asBoolean(false)) {
                    throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(r.toString()));
                }
            }
        }
    }

    // ---------------------------------------------------------------- 库存

    @Override
    public void pushInventory(ConnectorContext ctx, List<InventoryUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        String base = ctx.baseUrl(INFO);
        Map<String, String> headers = headers(ctx.credentials());
        long warehouse = Long.parseLong(ctx.credentials().require("warehouseId"));
        List<String> failures = new ArrayList<>();
        for (int i = 0; i < updates.size(); i += STOCK_BATCH) {
            List<Map<String, Object>> stocks = new ArrayList<>();
            for (InventoryUpdate u : updates.subList(i, Math.min(updates.size(), i + STOCK_BATCH))) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("offer_id", u.sellerSku());
                s.put("stock", u.qty() == null ? 0 : Math.max(0, u.qty().longValue()));
                s.put("warehouse_id", warehouse);
                stocks.add(s);
            }
            JsonNode resp = HttpJson.postJson(base + "/v2/products/stocks", headers, Map.of("stocks", stocks), PLATFORM);
            for (JsonNode r : resp.path("result")) {
                if (!r.path("updated").asBoolean(false)) {
                    failures.add(HttpJson.text(r, "offer_id") + ":" + r.path("errors").toString());
                }
            }
        }
        if (!failures.isEmpty()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(String.join("; ", failures)));
        }
    }

    // ---------------------------------------------------------------- 在线商品

    @Override
    public List<ListingDraft> fetchListings(ConnectorContext ctx) {
        String base = ctx.baseUrl(INFO);
        Map<String, String> headers = headers(ctx.credentials());
        List<ListingDraft> list = new ArrayList<>();
        String lastId = "";
        for (int page = 0; page < MAX_PAGES; page++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("filter", Map.of("visibility", "ALL"));
            body.put("last_id", lastId);
            body.put("limit", LISTING_PAGE_SIZE);
            JsonNode result = HttpJson.postJson(base + "/v3/product/list", headers, body, PLATFORM).path("result");
            List<Long> ids = new ArrayList<>();
            for (JsonNode it : result.path("items")) {
                if (it.hasNonNull("product_id")) {
                    ids.add(it.path("product_id").asLong());
                }
            }
            if (ids.isEmpty()) {
                break;
            }
            JsonNode info = HttpJson.postJson(base + "/v3/product/info/list", headers, Map.of("product_id", ids), PLATFORM);
            JsonNode items = info.has("items") ? info.path("items") : info.path("result").path("items");
            for (JsonNode p : items) {
                list.add(toListing(p));
            }
            lastId = HttpJson.text(result, "last_id");
            if (lastId == null || lastId.isEmpty() || ids.size() < LISTING_PAGE_SIZE) {
                break;
            }
        }
        return list;
    }

    private static ListingDraft toListing(JsonNode p) {
        String sku = HttpJson.text(p, "sku");
        if (sku == null || "0".equals(sku)) {
            for (JsonNode s : p.path("sources")) {
                sku = HttpJson.text(s, "sku");
                if (sku != null) {
                    break;
                }
            }
        }
        BigDecimal qty = BigDecimal.ZERO;
        boolean fbo = false;
        boolean fbs = false;
        for (JsonNode s : p.path("stocks").path("stocks")) {
            BigDecimal present = HttpJson.decimal(s, "present");
            BigDecimal reserved = HttpJson.decimal(s, "reserved");
            if (present != null) {
                qty = qty.add(reserved == null ? present : present.subtract(reserved));
            }
            if ("fbo".equals(HttpJson.text(s, "source"))) {
                fbo = true;
            } else {
                fbs = true;
            }
        }
        String fulfillment = fbo && !fbs ? FulfillmentType.PLATFORM.name() : FulfillmentType.SELLER.name();
        return new ListingDraft(HttpJson.text(p, "offer_id"), sku, HttpJson.text(p, "id"), HttpJson.text(p, "name"), null,
                HttpJson.decimal(p, "price"), HttpJson.text(p, "currency_code"), qty, fulfillment, listingStatus(p),
                image(p), sku == null ? null : "https://www.ozon.ru/product/" + sku);
    }

    /** 归档为下架；未创建成功或审核 / 校验失败为信息不完整；其余在售 */
    static String listingStatus(JsonNode p) {
        if (p.path("is_archived").asBoolean(false) || p.path("is_autoarchived").asBoolean(false) || p.path("archived").asBoolean(false)) {
            return ListingStatus.INACTIVE.name();
        }
        JsonNode st = p.path("statuses");
        if (!st.isMissingNode() && (!st.path("is_created").asBoolean(true) || "failed".equals(HttpJson.text(st, "validation_status"))
                || "failed_moderation".equals(HttpJson.text(st, "status")))) {
            return ListingStatus.INCOMPLETE.name();
        }
        return ListingStatus.ACTIVE.name();
    }

    private static String image(JsonNode p) {
        JsonNode primary = p.path("primary_image");
        if (primary.isArray() && !primary.isEmpty()) {
            return primary.get(0).asString();
        }
        String s = primary.isString() ? primary.asString() : null;
        if (s != null && !s.isBlank()) {
            return s;
        }
        JsonNode images = p.path("images");
        return images.isArray() && !images.isEmpty() ? images.get(0).asString() : null;
    }

    // ---------------------------------------------------------------- 工具

    private static Map<String, String> headers(StoreCredentials c) {
        return Map.of("Client-Id", c.require("clientId"), "Api-Key", c.require("apiKey"), "Accept", "application/json");
    }

    private static String utc(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime().format(ISO);
    }

    private static LocalDateTime time(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (RuntimeException e) {
            try {
                return LocalDateTime.ofInstant(Instant.parse(iso), ZoneId.systemDefault());
            } catch (RuntimeException ignored) {
                return null;
            }
        }
    }

    private static String first(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }
}
