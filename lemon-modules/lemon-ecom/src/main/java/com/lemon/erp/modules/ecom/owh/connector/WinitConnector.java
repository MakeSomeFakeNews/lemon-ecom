package com.lemon.erp.modules.ecom.owh.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.carrier.CarrierField;
import com.lemon.erp.modules.ecom.carrier.CarrierSigns;
import com.lemon.erp.modules.ecom.owh.model.OwhCapability;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhOutboundStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhProvider;
import com.lemon.erp.modules.ecom.owh.model.OwhReturnStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 万邑通 WINIT 开放平台：统一入口 {@code /openapi/service}，POST JSON
 * {@code {"action","app_key","data",format:"json",language,"platform",sign,sign_method:"md5",timestamp,version:"1.0"}}。
 * 签名：MD5（大写 32 位）of {@code token + "action"+action + "app_key"+appKey + "data"+data + "format"+"json" + "platform"+platform
 * + "sign_method"+"md5" + "timestamp"+timestamp + "version"+"1.0" + token}，data 为请求中 data 的 JSON 原文（与发送的完全一致）。
 * 返回 {@code {"code":"0","msg":"...","data":...}}，code 为 0 时成功。
 * <ul>
 *   <li>仓库 queryWarehouse（warehouseCode / warehouseID）；物流产品 queryDeliveryWay（按仓库 ID）</li>
 *   <li>库存 queryWarehouseStorage（分页 pageNum / pageSize；qtyAvailable 可用、qtyOnway 在途、qtyReserved 预留）</li>
 *   <li>入库单 createInboundOrder / queryInboundOrder（merchandiseList 中 receivedQuantity）</li>
 *   <li>出库单 createOutboundOrder（sellerOrderNo = 包裹号、deliveryWayID、productList）/ queryOutboundOrder（status、trackingNum）/
 *   voidOutboundOrder（已开始作业的返回截单申请）</li>
 *   <li>费用 queryBillDetail（chargeDate 区间分页）</li>
 *   <li>退件 createReturnOrder / queryReturnOrder（productList 中 qualifiedQty 良品、unqualifiedQty 不良品）</li>
 *   <li>调拨出库：createOutboundOrder 加 orderType = B2B，收件人为调入仓地址</li>
 *   <li>取消：入库单 voidInboundOrder；公开文档中没有作废退件单的接口（需在万邑通后台作废，系统中「标记已作废」）</li>
 * </ul>
 * 授权：appKey（开放平台账号）、token（开放平台生成），platform（ISV 应用标识，可空）。测试环境 openapi.sandbox.winit.com.cn。
 * <p>
 * 说明：按万邑通开放平台公开文档实现，尚未用真实账号联调。需要用真实账号核对的点：入库 / 出库单的状态码（下面按关键字归一：
 * 完成 / 上架 / RECEIVED、出库 / DLC / SHIPPED、作废 / VO / CANCEL、截单 / INTERCEPT）；费用接口 queryBillDetail 的字段名；
 * 仓库 ID 与仓库代码的使用（出库单要求 warehouseID，本实现在映射中保存仓库代码，下单前按 queryWarehouse 换取 ID）；
 * 退件接口 createReturnOrder / queryReturnOrder 的字段与状态；B2B 出库是否需要单独开通
 */
@Component
public class WinitConnector implements OwhConnector {

    static final String NAME = "WINIT";
    static final String PROD_URL = "https://openapi.winit.com.cn/openapi/service";
    static final String TEST_URL = "https://openapi.sandbox.winit.com.cn/openapi/service";
    private static final String CODE_OK = "0";
    private static final String VERSION = "1.0";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final OwhInfo INFO = new OwhInfo(OwhProvider.WINIT, "ecom.label.owh.provider.winit", List.of(
            CarrierField.text("appKey", "ecom.label.owh.cred.appKey", true),
            CarrierField.secret("token", "ecom.label.owh.cred.token"),
            CarrierField.text("platform", "ecom.label.owh.cred.platform", false)),
            PROD_URL, TEST_URL, EnumSet.complementOf(EnumSet.of(OwhCapability.CANCEL_RETURN)), "https://developer.winit.com.cn/");

    @Override
    public OwhProvider provider() {
        return OwhProvider.WINIT;
    }

    @Override
    public OwhInfo info() {
        return INFO;
    }

    @Override
    public List<OwhData.Warehouse> warehouses(OwhContext ctx) {
        List<OwhData.Warehouse> out = new ArrayList<>();
        for (JsonNode n : OwhHttp.array(call(ctx, "queryWarehouse", Map.of()), "list")) {
            out.add(new OwhData.Warehouse(OwhHttp.first(n, "warehouseCode", "warehouseID"), OwhHttp.first(n, "warehouseName"),
                    OwhHttp.first(n, "countryCode", "country")));
        }
        return out;
    }

    @Override
    public List<OwhData.Product> products(OwhContext ctx, String warehouseCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (StringUtils.hasText(warehouseCode)) {
            body.put("warehouseID", warehouseId(ctx, warehouseCode));
        }
        List<OwhData.Product> out = new ArrayList<>();
        for (JsonNode n : OwhHttp.array(call(ctx, "queryDeliveryWay", body), "list")) {
            out.add(new OwhData.Product(OwhHttp.first(n, "deliveryID", "deliveryWayID", "code"), OwhHttp.first(n, "deliveryWay", "deliveryWayName", "name"),
                    warehouseCode, OwhHttp.first(n, "carrierName", "carrier")));
        }
        return out;
    }

    @Override
    public List<OwhData.Stock> inventory(OwhContext ctx, String warehouseCode) {
        String whId = warehouseId(ctx, warehouseCode);
        List<OwhData.Stock> out = new ArrayList<>();
        for (int page = 1; page <= OwhMaps.MAX_PAGES; page++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("warehouseID", whId);
            body.put("pageNum", page);
            body.put("pageSize", OwhMaps.PAGE_SIZE);
            JsonNode list = OwhHttp.array(call(ctx, "queryWarehouseStorage", body), "list");
            for (JsonNode n : list) {
                out.add(new OwhData.Stock(OwhHttp.first(n, "productCode", "merchandiseCode"), OwhHttp.dec(n, "qtyAvailable"),
                        OwhHttp.dec(n, "qtyOnway"), OwhHttp.dec(n, "qtyReserved")));
            }
            if (list.size() < OwhMaps.PAGE_SIZE) {
                break;
            }
        }
        return out;
    }

    @Override
    public String createInbound(OwhContext ctx, OwhData.InboundDraft d) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sellerOrderNo", d.refNo());
        body.put("destinationWarehouseCode", d.warehouseCode());
        body.put("transportMode", d.transportMode());
        body.put("carrier", d.carrier());
        body.put("trackingNo", d.trackingNo());
        body.put("estimatedArrivalDate", d.etaDate() == null ? null : d.etaDate().toString());
        body.put("remark", d.remark());
        List<Map<String, Object>> packages = new ArrayList<>();
        for (OwhData.Box b : d.boxes()) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("packageNo", b.boxMark());
            p.put("packageLength", b.length());
            p.put("packageWidth", b.width());
            p.put("packageHeight", b.height());
            p.put("packageWeight", b.weight());
            List<Map<String, Object>> items = new ArrayList<>();
            for (OwhData.Line l : b.items()) {
                items.add(Map.of("merchandiseCode", l.sku(), "quantity", l.qty().intValue()));
            }
            p.put("merchandiseList", items);
            packages.add(p);
        }
        body.put("packageList", packages);
        String no = OwhHttp.first(call(ctx, "createInboundOrder", body), "orderNo", "inboundOrderNum");
        if (no == null) {
            throw new BizException("ecom.error.owh.badResponse", NAME, "orderNo");
        }
        return no;
    }

    @Override
    public OwhData.InboundState queryInbound(OwhContext ctx, String owhOrderNo) {
        JsonNode data = call(ctx, "queryInboundOrder", Map.of("orderNo", owhOrderNo));
        String raw = OwhHttp.first(data, "status", "orderStatus");
        String s = raw == null ? "" : raw.toUpperCase(Locale.ROOT);
        OwhInboundStatus st;
        if (s.contains("FINISH") || s.contains("COMPLETE") || s.equals("FN") || s.contains("完成")) { // i18n-ignore 服务商返回的中文状态 / 费用名称关键字
            st = OwhInboundStatus.RECEIVED;
        } else if (s.contains("RECEIV") || s.contains("PUTAWAY") || s.equals("RC") || s.contains("上架") || s.contains("收货")) { // i18n-ignore 服务商返回的中文状态 / 费用名称关键字
            st = OwhInboundStatus.RECEIVING;
        } else if (s.contains("VO") || s.contains("CANCEL") || s.contains("作废")) { // i18n-ignore 服务商返回的中文状态 / 费用名称关键字
            st = OwhInboundStatus.CANCELLED;
        } else {
            st = OwhInboundStatus.PUSHED;
        }
        Map<String, BigDecimal> received = new LinkedHashMap<>();
        for (JsonNode n : OwhHttp.array(data, "merchandiseList", "list")) {
            received.merge(OwhHttp.first(n, "merchandiseCode", "productCode"), OwhHttp.dec(n, "receivedQuantity", "putawayQuantity"), BigDecimal::add);
        }
        return new OwhData.InboundState(st, raw, received, OwhHttp.dateTime(OwhHttp.first(data, "completeDate", "updateDate")));
    }

    @Override
    public OwhData.OutboundResult createOutbound(OwhContext ctx, OwhData.OutboundDraft d) {
        return outbound(ctx, d, false);
    }

    /** 调拨出库：同出库单接口，orderType = B2B（整箱发往另一个仓库） */
    @Override
    public OwhData.OutboundResult createTransferOut(OwhContext ctx, OwhData.OutboundDraft d) {
        return outbound(ctx, d, true);
    }

    private OwhData.OutboundResult outbound(OwhContext ctx, OwhData.OutboundDraft d, boolean b2b) {
        OwhData.Receiver r = d.receiver();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("warehouseID", warehouseId(ctx, d.warehouseCode()));
        if (b2b) {
            body.put("orderType", "B2B");
        }
        body.put("deliveryWayID", d.productCode());
        body.put("sellerOrderNo", d.refNo());
        body.put("eBayOrderID", d.platformOrderNo());
        body.put("recipientName", r.name());
        body.put("phoneNum", r.phone());
        body.put("emailAddress", r.email());
        body.put("region", r.country());
        body.put("state", r.state());
        body.put("city", r.city());
        body.put("address1", r.address());
        body.put("zipCode", r.postcode());
        body.put("isShareOrder", "N");
        body.put("insuranceTypeID", "1000000");
        List<Map<String, Object>> items = new ArrayList<>();
        for (OwhData.Line l : d.items()) {
            items.add(Map.of("productCode", l.sku(), "productNum", l.qty().intValue()));
        }
        body.put("productList", items);
        JsonNode data = call(ctx, "createOutboundOrder", body);
        String no = OwhHttp.first(data, "outboundOrderNum", "orderNo");
        if (no == null) {
            throw new BizException("ecom.error.owh.badResponse", NAME, "outboundOrderNum");
        }
        BigDecimal fee = OwhHttp.dec(data, "totalFee", "estimateFee");
        return new OwhData.OutboundResult(no, OwhHttp.first(data, "trackingNum"), fee.signum() > 0 ? fee : null, OwhHttp.first(data, "currency"));
    }

    @Override
    public OwhData.OutboundState queryOutbound(OwhContext ctx, String owhOrderNo) {
        JsonNode data = call(ctx, "queryOutboundOrder", Map.of("outboundOrderNum", owhOrderNo));
        JsonNode list = OwhHttp.array(data, "list");
        JsonNode order = list.isEmpty() ? data : list.get(0);
        String raw = OwhHttp.first(order, "status", "orderStatus");
        String s = raw == null ? "" : raw.toUpperCase(Locale.ROOT);
        OwhOutboundStatus st;
        if (s.contains("INTERCEPT") || s.contains("截单")) { // i18n-ignore 服务商返回的中文状态 / 费用名称关键字
            st = OwhOutboundStatus.INTERCEPTING;
        } else if (s.equals("DLC") || s.contains("SHIP") || s.contains("OUTBOUND") || s.contains("出库") || s.contains("发货")) { // i18n-ignore 服务商返回的中文状态 / 费用名称关键字
            st = OwhOutboundStatus.OUTBOUND;
        } else if (s.equals("VO") || s.contains("VOID") || s.contains("CANCEL") || s.contains("作废")) { // i18n-ignore 服务商返回的中文状态 / 费用名称关键字
            st = OwhOutboundStatus.CANCELLED;
        } else {
            st = OwhOutboundStatus.PUSHED;
        }
        return new OwhData.OutboundState(st, raw, OwhHttp.first(order, "trackingNum", "trackingNo"), OwhHttp.first(order, "carrierName", "deliveryWay"),
                OwhHttp.dateTime(OwhHttp.first(order, "dateShipped", "outboundDate")));
    }

    @Override
    public boolean cancelOutbound(OwhContext ctx, String owhOrderNo, String reason) {
        JsonNode data = call(ctx, "voidOutboundOrder", Map.of("outboundOrderNum", owhOrderNo));
        String st = OwhHttp.first(data, "status", "result");
        return st == null || !st.toUpperCase(Locale.ROOT).contains("INTERCEPT");
    }

    @Override
    public List<OwhData.Fee> fees(OwhContext ctx, LocalDate begin, LocalDate end) {
        List<OwhData.Fee> out = new ArrayList<>();
        for (int page = 1; page <= OwhMaps.MAX_PAGES; page++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("startDate", begin.toString());
            body.put("endDate", end.toString());
            body.put("pageNum", page);
            body.put("pageSize", OwhMaps.PAGE_SIZE);
            JsonNode list = OwhHttp.array(call(ctx, "queryBillDetail", body), "list");
            for (JsonNode n : list) {
                String typeName = OwhHttp.first(n, "feeName", "feeType");
                LocalDate date = OwhHttp.date(OwhHttp.first(n, "chargeDate", "createDate"));
                out.add(new OwhData.Fee(OwhHttp.first(n, "billDetailNo", "id"), OwhMaps.feeType(typeName), date == null ? end : date,
                        OwhHttp.first(n, "orderNo", "businessNo"), OwhHttp.first(n, "warehouseCode"), OwhHttp.first(n, "productCode", "merchandiseCode"),
                        OwhHttp.first(n, "currency", "currencyCode"), OwhHttp.dec(n, "amount", "fee").abs(), typeName));
            }
            if (list.size() < OwhMaps.PAGE_SIZE) {
                break;
            }
        }
        return out;
    }

    @Override
    public String createReturn(OwhContext ctx, OwhData.ReturnDraft d) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sellerReturnNo", d.refNo());
        body.put("warehouseCode", d.warehouseCode());
        body.put("sellerOrderNo", d.platformOrderNo());
        body.put("carrier", d.carrier());
        body.put("trackingNo", d.trackingNo());
        body.put("buyerName", d.buyerName());
        body.put("remark", d.remark());
        List<Map<String, Object>> items = new ArrayList<>();
        for (OwhData.Line l : d.lines()) {
            items.add(Map.of("productCode", l.sku(), "qty", l.qty().intValue()));
        }
        body.put("productList", items);
        String no = OwhHttp.first(call(ctx, "createReturnOrder", body), "returnOrderNo", "orderNo");
        if (no == null) {
            throw new BizException("ecom.error.owh.badResponse", NAME, "returnOrderNo");
        }
        return no;
    }

    @Override
    public OwhData.ReturnState queryReturn(OwhContext ctx, String owhOrderNo) {
        JsonNode data = call(ctx, "queryReturnOrder", Map.of("returnOrderNo", owhOrderNo));
        String raw = OwhHttp.first(data, "status", "orderStatus");
        String s = raw == null ? "" : raw.toUpperCase(Locale.ROOT);
        OwhReturnStatus st = s.contains("FINISH") || s.contains("COMPLETE") || s.equals("FN") ? OwhReturnStatus.RECEIVED
                : s.equals("VO") || s.contains("CANCEL") ? OwhReturnStatus.CANCELLED : OwhReturnStatus.PUSHED;
        Map<String, BigDecimal> good = new LinkedHashMap<>();
        Map<String, BigDecimal> bad = new LinkedHashMap<>();
        for (JsonNode n : OwhHttp.array(data, "productList", "list")) {
            String sku = OwhHttp.first(n, "productCode", "merchandiseCode");
            good.merge(sku, OwhHttp.dec(n, "qualifiedQty"), BigDecimal::add);
            bad.merge(sku, OwhHttp.dec(n, "unqualifiedQty"), BigDecimal::add);
        }
        return new OwhData.ReturnState(st, raw, good, bad, OwhHttp.dateTime(OwhHttp.first(data, "completeDate", "updateDate")));
    }

    /** 作废入库单 voidInboundOrder（未到仓的入库单）；退件单没有作废接口，使用默认实现（提示到万邑通后台作废后标记已作废） */
    @Override
    public void cancelInbound(OwhContext ctx, String owhOrderNo) {
        call(ctx, "voidInboundOrder", Map.of("orderNo", owhOrderNo));
    }

    /** 签名原文：token action{action} app_key{appKey} data{data} format{json} platform{platform} sign_method{md5} timestamp{ts} version{1.0} token，MD5 大写 */
    public static String sign(String token, String action, String appKey, String data, String platform, String timestamp) {
        return CarrierSigns.md5Lower(token + "action" + action + "app_key" + appKey + "data" + data + "format" + "json" + "platform" + platform
                + "sign_method" + "md5" + "timestamp" + timestamp + "version" + VERSION + token).toUpperCase(Locale.ROOT);
    }

    /** 仓库代码换取仓库 ID（服务商返回中没有 ID 时直接使用代码） */
    private String warehouseId(OwhContext ctx, String warehouseCode) {
        for (JsonNode n : OwhHttp.array(call(ctx, "queryWarehouse", Map.of()), "list")) {
            if (warehouseCode != null && warehouseCode.equals(OwhHttp.first(n, "warehouseCode"))) {
                String id = OwhHttp.first(n, "warehouseID");
                return id == null ? warehouseCode : id;
            }
        }
        return warehouseCode;
    }

    private JsonNode call(OwhContext ctx, String action, Object payload) {
        String appKey = ctx.require(INFO, "appKey");
        String token = ctx.require(INFO, "token");
        String platform = ctx.optional("platform") == null ? "" : ctx.optional("platform");
        String data = OwhHttp.write(payload);
        String ts = LocalDateTime.now().format(TS);
        // 按签名使用的 data 原文拼接请求体，保证与签名一致
        String body = "{\"action\":" + OwhHttp.write(action) + ",\"app_key\":" + OwhHttp.write(appKey) + ",\"data\":" + data
                + ",\"format\":\"json\",\"language\":\"zh_CN\",\"platform\":" + OwhHttp.write(platform)
                + ",\"sign\":" + OwhHttp.write(sign(token, action, appKey, data, platform, ts)) + ",\"sign_method\":\"md5\""
                + ",\"timestamp\":" + OwhHttp.write(ts) + ",\"version\":\"" + VERSION + "\"}";
        JsonNode resp = OwhHttp.postJson(ctx.endpoint(INFO), Map.of(), body, NAME);
        if (!CODE_OK.equals(OwhHttp.text(resp, "code"))) {
            String msg = OwhHttp.first(resp, "msg", "message");
            throw new BizException("ecom.error.owh.rejected", NAME, OwhHttp.abbreviate((OwhHttp.text(resp, "code") == null ? "" : OwhHttp.text(resp, "code") + " ")
                    + (msg == null ? String.valueOf(resp) : msg)));
        }
        JsonNode d = resp.get("data");
        return d == null || d.isNull() ? OwhHttp.MAPPER.createObjectNode() : d;
    }
}
