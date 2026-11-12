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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Temu 卖家开放平台（Partner Platform，partner.temu.com）：面向<b>半托管</b>和<b>本土（Local，本本对本）</b>卖家——
 * 卖家自己接单、自己发货，平台负责前台销售。<b>全托管</b>模式下 Temu 自己负责销售和配送，卖家只对接备货单 / 发货单
 * （bg.purchaseorder.* 一类接口），不产生 C 端订单，不适用本连接器。
 * <p>
 * 授权（KEYS）：appKey、appSecret、accessToken（卖家在开放平台授权应用后生成），region 决定网关：
 * US → openapi-b-us.temu.com、EU → openapi-b-eu.temu.com、GLOBAL → openapi-b-global.temu.com（/openapi/router）。
 * 店铺填写了自定义接口地址时一律用它（全部接口都走同一个 router 地址）。
 * <p>
 * 协议：POST JSON 到 router，公共参数 type（接口名）、app_key、access_token、timestamp（秒）、data_type=JSON、version
 * 与业务参数放在同一层；签名 = MD5(app_secret + 按键排序拼接的 k+v + app_secret) 大写，嵌套对象 / 数组的值为其 JSON 串。
 * 响应 {success, errorCode, errorMsg, result}，success=false 抛 ecom.error.connectorCallFailed。
 * <p>
 * 接口（需按开放平台最新文档核对）：
 * <ul>
 *   <li>订单列表 bg.order.list.v2.get（updateAtStart / updateAtEnd 秒级时间戳，pageNumber / pageSize 分页），
 *       收货信息 bg.order.shippinginfo.v2.get，金额 bg.order.amount.query（每个父订单一次）</li>
 *   <li>发货 bg.logistics.shipment.v2.confirm（物流公司 ID 由 bg.logistics.companies.get 按名称匹配，订单承运商填数字时直接作为 ID）</li>
 *   <li>库存 bg.goods.quantity.update（全量覆盖），商品 bg.local.goods.list.query（本土店铺；半托管店铺的商品列表接口不同）</li>
 * </ul>
 * 编码约定：platformItemId = {@code goodsId:skuId}（库存更新需要同时提供商品 ID 与 SKU ID，不含冒号时视为 skuId，
 * 此时不能回传库存）；sellerSku 取卖家自定义 SKU 编码（outSkuSn / extCode），取不到时退回平台 skuId。
 * 订单行 platformLineId 为子订单号 orderSn，平台订单号为父订单号 parentOrderSn。
 * <p>
 * 沙箱：Temu 没有公开的沙箱网关，联调使用开放平台提供的测试店铺（测试店铺的令牌直接调正式网关），因此 sandboxBaseUrl 为空。
 * 说明：按公开资料实现，尚未用真实店铺联调
 */
@Component
public class TemuConnector implements ChannelConnector {

    static final String PLATFORM = "Temu";
    static final String API_VERSION = "V1";
    private static final Map<String, String> GATEWAYS = Map.of(
            "US", "https://openapi-b-us.temu.com/openapi/router",
            "EU", "https://openapi-b-eu.temu.com/openapi/router",
            "GLOBAL", "https://openapi-b-global.temu.com/openapi/router");
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 50;
    private static final ConnectorInfo INFO = new ConnectorInfo(EcomPlatform.TEMU, AuthType.KEYS, List.of(
            CredentialField.text("region", "ecom.label.cred.temuRegion", true),
            CredentialField.text("appKey", "ecom.label.cred.appKey", true),
            CredentialField.secret("appSecret", "ecom.label.cred.appSecret"),
            CredentialField.secret("accessToken", "ecom.label.cred.accessToken")),
            GATEWAYS.get("US"), null,
            EnumSet.of(ConnectorCapability.ORDERS, ConnectorCapability.SHIP_CONFIRM, ConnectorCapability.INVENTORY_PUSH,
                    ConnectorCapability.LISTINGS),
            "https://partner.temu.com/documentation");

    @Override
    public ConnectorInfo info() {
        return INFO;
    }

    // ---------------------------------------------------------------- 订单

    @Override
    public List<OrderDraft> fetchOrders(ConnectorContext ctx, LocalDateTime since) {
        EcomStore store = ctx.store();
        String gateway = gateway(ctx);
        long start = since.atZone(ZoneId.systemDefault()).toEpochSecond();
        long end = Instant.now().getEpochSecond();
        List<OrderDraft> drafts = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            Map<String, Object> biz = new LinkedHashMap<>();
            biz.put("pageNumber", page);
            biz.put("pageSize", PAGE_SIZE);
            biz.put("updateAtStart", start);
            biz.put("updateAtEnd", end);
            JsonNode result = call(gateway, ctx.credentials(), "bg.order.list.v2.get", biz);
            JsonNode items = result.path("pageItems");
            if (!items.isArray() || items.isEmpty()) {
                break;
            }
            for (JsonNode item : items) {
                OrderDraft d = toDraft(store, item);
                fillShipping(d, call(gateway, ctx.credentials(), "bg.order.shippinginfo.v2.get", Map.of("parentOrderSn", d.getPlatformOrderNo())));
                fillAmount(d, call(gateway, ctx.credentials(), "bg.order.amount.query", Map.of("parentOrderSn", d.getPlatformOrderNo())));
                if (d.getCurrency() == null) {
                    d.setCurrency(store.getCurrency());
                }
                drafts.add(d);
            }
            int total = result.path("totalItemNum").asInt(0);
            if (items.size() < PAGE_SIZE || (total > 0 && page * PAGE_SIZE >= total)) {
                break;
            }
        }
        return drafts;
    }

    private static OrderDraft toDraft(EcomStore store, JsonNode item) {
        JsonNode parent = item.path("parentOrderMap");
        OrderDraft d = new OrderDraft();
        d.setStoreId(store.getId());
        d.setPlatformOrderNo(HttpJson.text(parent, "parentOrderSn"));
        d.setOrderTime(epoch(parent.get("parentOrderTime")));
        d.setPaidTime(epoch(parent.get("parentOrderPayTime")));
        d.setShipBy(epoch(parent.get("expectShipLatestTime")));
        d.setDeliverBy(epoch(parent.get("latestDeliveryTime")));
        d.setStatus(status(parent.path("parentOrderStatus").asInt(0)));
        d.setFulfillment(FulfillmentType.SELLER);
        d.setShipCountry(HttpJson.text(parent, "regionName1"));
        for (JsonNode o : item.path("orderList")) {
            OrderDraft.Line l = new OrderDraft.Line();
            l.setPlatformLineId(HttpJson.text(o, "orderSn"));
            String goodsId = HttpJson.text(o, "goodsId");
            String skuId = HttpJson.text(o, "skuId");
            l.setPlatformItemId(itemId(goodsId, skuId));
            l.setSellerSku(firstNonBlank(HttpJson.text(o, "outSkuSn"), HttpJson.text(o, "extCode"), skuId));
            l.setTitle(join(HttpJson.text(o, "goodsName"), HttpJson.text(o, "spec")));
            BigDecimal qty = HttpJson.decimal(o, "quantity");
            if (qty == null) {
                qty = HttpJson.decimal(o, "originalOrderQuantity");
            }
            l.setQty(qty == null ? BigDecimal.ONE : qty);
            d.getLines().add(l);
        }
        return d;
    }

    private static void fillShipping(OrderDraft d, JsonNode r) {
        d.setShipName(HttpJson.text(r, "receiptName"));
        d.setShipPhone(firstNonBlank(HttpJson.text(r, "mobile"), HttpJson.text(r, "backupMobile")));
        d.setBuyerEmail(HttpJson.text(r, "mail"));
        d.setBuyerName(d.getShipName());
        String country = HttpJson.text(r, "regionName1");
        if (country != null) {
            d.setShipCountry(country);
        }
        d.setShipState(HttpJson.text(r, "regionName2"));
        d.setShipCity(HttpJson.text(r, "regionName3"));
        d.setShipPostcode(HttpJson.text(r, "postCode"));
        d.setShipAddress(join(HttpJson.text(r, "addressLine1"), HttpJson.text(r, "addressLine2"), HttpJson.text(r, "addressLine3")));
    }

    /**
     * 金额：子订单的成交单价 unitRetailPriceVat（{amount, currency}，或直接数值）与运费 shippingAmount；
     * 字段与金额单位需按开放平台最新文档核对
     */
    private static void fillAmount(OrderDraft d, JsonNode r) {
        Map<String, JsonNode> bySn = new HashMap<>();
        for (JsonNode o : r.path("orderList")) {
            String sn = HttpJson.text(o, "orderSn");
            if (sn != null) {
                bySn.put(sn, o);
            }
        }
        for (OrderDraft.Line l : d.getLines()) {
            JsonNode o = bySn.get(l.getPlatformLineId());
            if (o == null) {
                continue;
            }
            JsonNode unit = o.path("unitRetailPriceVat");
            BigDecimal price = money(unit);
            if (price != null) {
                l.setPrice(price);
                l.setAmount(price.multiply(l.getQty()));
            }
            l.setShipping(money(o.path("shippingAmount")));
            l.setTax(money(o.path("taxAmount")));
            l.setDiscount(money(o.path("discountAmount")));
            String currency = HttpJson.text(unit, "currency");
            if (currency != null) {
                d.setCurrency(currency.toUpperCase(Locale.ROOT));
            }
        }
    }

    // ---------------------------------------------------------------- 发货

    @Override
    public void confirmShipment(ConnectorContext ctx, EcomOrder order, List<EcomOrderItem> items) {
        String gateway = gateway(ctx);
        Long companyId = shipCompanyId(gateway, ctx.credentials(), order.getCarrier());
        List<Map<String, Object>> lines = new ArrayList<>();
        for (EcomOrderItem it : items) {
            if (it.getPlatformLineId() == null) {
                throw new BizException("ecom.error.connectorLineIdMissing", order.getPlatformOrderNo());
            }
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("orderSn", it.getPlatformLineId());
            String[] ids = splitItemId(it.getPlatformItemId());
            if (ids != null) {
                line.put("goodsId", Long.valueOf(ids[0]));
                line.put("skuId", Long.valueOf(ids[1]));
            }
            line.put("quantity", it.getQty().intValue());
            lines.add(line);
        }
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("parentOrderSn", order.getPlatformOrderNo());
        req.put("shipCompanyId", companyId);
        req.put("trackingNumber", order.getTrackingNo());
        req.put("orderSendInfoList", lines);
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("sendType", 0);
        biz.put("sendRequestList", List.of(req));
        call(gateway, ctx.credentials(), "bg.logistics.shipment.v2.confirm", biz);
    }

    /** 承运商：填数字时直接作为 Temu 物流公司 ID，否则在 bg.logistics.companies.get 中按名称（忽略大小写）匹配 */
    private Long shipCompanyId(String gateway, StoreCredentials c, String carrier) {
        if (carrier == null || carrier.isBlank()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "carrier is required");
        }
        String name = carrier.trim();
        if (name.chars().allMatch(Character::isDigit)) {
            return Long.valueOf(name);
        }
        JsonNode result = call(gateway, c, "bg.logistics.companies.get", Map.of());
        JsonNode list = result.path("shipmentList");
        if (!list.isArray()) {
            list = result;
        }
        for (JsonNode co : list) {
            String n = HttpJson.text(co, "shippingCompanyName");
            if (n != null && n.trim().equalsIgnoreCase(name)) {
                return co.path("shipId").asLong();
            }
        }
        throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "unknown carrier: " + name);
    }

    // ---------------------------------------------------------------- 库存与商品

    /** 按商品分组，每个商品一次 bg.goods.quantity.update（全量覆盖 SKU 可售数量） */
    @Override
    public void pushInventory(ConnectorContext ctx, List<InventoryUpdate> updates) {
        String gateway = gateway(ctx);
        Map<String, List<Map<String, Object>>> byGoods = new LinkedHashMap<>();
        for (InventoryUpdate u : updates) {
            String[] ids = splitItemId(u.platformItemId());
            if (ids == null) {
                throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "platformItemId must be goodsId:skuId, sku " + u.sellerSku());
            }
            Map<String, Object> sku = new LinkedHashMap<>();
            sku.put("skuId", Long.valueOf(ids[1]));
            sku.put("targetQuantity", Math.max(0, u.qty().setScale(0, RoundingMode.DOWN).intValue()));
            byGoods.computeIfAbsent(ids[0], k -> new ArrayList<>()).add(sku);
        }
        for (Map.Entry<String, List<Map<String, Object>>> e : byGoods.entrySet()) {
            Map<String, Object> biz = new LinkedHashMap<>();
            biz.put("goodsId", Long.valueOf(e.getKey()));
            biz.put("quantityChangeType", 2);
            biz.put("skuStockChangeList", e.getValue());
            call(gateway, ctx.credentials(), "bg.goods.quantity.update", biz);
        }
    }

    @Override
    public List<ListingDraft> fetchListings(ConnectorContext ctx) {
        String gateway = gateway(ctx);
        List<ListingDraft> list = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            Map<String, Object> biz = new LinkedHashMap<>();
            biz.put("pageNo", page);
            biz.put("pageSize", PAGE_SIZE);
            JsonNode result = call(gateway, ctx.credentials(), "bg.local.goods.list.query", biz);
            JsonNode goods = result.path("goodsList");
            if (!goods.isArray() || goods.isEmpty()) {
                break;
            }
            for (JsonNode g : goods) {
                String goodsId = HttpJson.text(g, "goodsId");
                String status = listingStatus(g.path("goodsStatus").asInt(-1));
                for (JsonNode s : g.path("skuInfoList")) {
                    String skuId = HttpJson.text(s, "skuId");
                    JsonNode price = s.path("price");
                    list.add(new ListingDraft(firstNonBlank(HttpJson.text(s, "outSkuSn"), HttpJson.text(s, "extCode"), skuId),
                            itemId(goodsId, skuId), goodsId, HttpJson.text(g, "goodsName"), HttpJson.text(s, "specName"),
                            money(price), upper(HttpJson.text(price, "currency")), HttpJson.decimal(s, "quantity"),
                            FulfillmentType.SELLER.name(), status, HttpJson.text(s, "thumbUrl"), null));
                }
            }
            int total = result.path("total").asInt(0);
            if (goods.size() < PAGE_SIZE || (total > 0 && page * PAGE_SIZE >= total)) {
                break;
            }
        }
        return list;
    }

    // ---------------------------------------------------------------- 协议

    /** 调用一个接口：公共参数 + 业务参数同层，签名后 POST JSON，返回 result */
    static JsonNode call(String gateway, StoreCredentials c, String type, Map<String, ?> business) {
        String secret = c.require("appSecret");
        Map<String, Object> body = new TreeMap<>(business);
        body.put("type", type);
        body.put("app_key", c.require("appKey"));
        body.put("access_token", c.require("accessToken"));
        body.put("timestamp", Instant.now().getEpochSecond());
        body.put("data_type", "JSON");
        body.put("version", API_VERSION);
        body.put("sign", sign(secret, body));
        JsonNode resp = HttpJson.postJson(gateway, Map.of(), body, PLATFORM);
        if (!resp.path("success").asBoolean(false)) {
            String summary = resp.path("errorCode").asString("") + " " + resp.path("errorMsg").asString("");
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(summary.isBlank() ? resp.toString() : summary.trim()));
        }
        return resp.path("result");
    }

    /** 签名：按键排序（不含 sign），值为字符串原文或 JSON 串，MD5(secret + k1v1k2v2… + secret) 大写 */
    static String sign(String secret, Map<String, Object> params) {
        TreeMap<String, String> sorted = new TreeMap<>();
        params.forEach((k, v) -> {
            if (!"sign".equals(k) && v != null) {
                sorted.put(k, v instanceof String s ? s : v instanceof Number || v instanceof Boolean ? v.toString() : HttpJson.MAPPER.writeValueAsString(v));
            }
        });
        return ConnectorSigns.md5Upper(secret + ConnectorSigns.concatSorted(sorted) + secret);
    }

    private static String gateway(ConnectorContext ctx) {
        if (ctx.baseUrlOverride() != null) {
            return ctx.baseUrlOverride();
        }
        String region = ctx.credentials().get("region") == null ? "US" : ctx.credentials().get("region").toUpperCase(Locale.ROOT);
        String gateway = GATEWAYS.get(region);
        if (gateway == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, "region must be US, EU or GLOBAL: " + region);
        }
        return gateway;
    }

    /** 父订单状态：1 待处理、2 待发货、3 已取消、4 已发货、5 已签收、41 部分发货（仍有待发） */
    static EcomOrderStatus status(int s) {
        return switch (s) {
            case 1 -> EcomOrderStatus.PENDING;
            case 3 -> EcomOrderStatus.CANCELLED;
            case 4, 5 -> EcomOrderStatus.SHIPPED;
            default -> EcomOrderStatus.UNSHIPPED;
        };
    }

    /** 商品状态：1 在售、2 下架、0 / 其他未完成（需核对） */
    private static String listingStatus(int s) {
        return switch (s) {
            case 1 -> ListingStatus.ACTIVE.name();
            case 2 -> ListingStatus.INACTIVE.name();
            case 3 -> ListingStatus.DELETED.name();
            case 0 -> ListingStatus.INCOMPLETE.name();
            default -> null;
        };
    }

    static String itemId(String goodsId, String skuId) {
        if (skuId == null) {
            return goodsId;
        }
        return goodsId == null ? skuId : goodsId + ":" + skuId;
    }

    private static String[] splitItemId(String platformItemId) {
        if (platformItemId == null) {
            return null;
        }
        int i = platformItemId.indexOf(':');
        if (i <= 0 || i == platformItemId.length() - 1) {
            return null;
        }
        return new String[]{platformItemId.substring(0, i), platformItemId.substring(i + 1)};
    }

    private static BigDecimal money(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode v = node.isObject() ? node.get("amount") : node;
        if (v == null || v.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(v.asString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 秒级时间戳（大于 10^12 视为毫秒）转为服务器时区 */
    private static LocalDateTime epoch(JsonNode v) {
        if (v == null || v.isNull() || !v.canConvertToLong() && !v.isString()) {
            return null;
        }
        long n;
        try {
            n = v.isString() ? Long.parseLong(v.asString()) : v.asLong();
        } catch (NumberFormatException e) {
            return null;
        }
        if (n <= 0) {
            return null;
        }
        Instant instant = n > 1_000_000_000_000L ? Instant.ofEpochMilli(n) : Instant.ofEpochSecond(n);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private static String upper(String s) {
        return s == null ? null : s.toUpperCase(Locale.ROOT);
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
