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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 递四方海外仓（4PX 开放平台 open.4px.com 的订单履约 / 海外仓接口，即 FB4）：与 4PX 物流商接口相同的统一网关 {@code /router/api/service}，
 * 公共参数 method、app_key、v、timestamp（毫秒）、format=json、sign 在查询串中，业务参数为 JSON 请求体。签名：MD5（小写 32 位）of
 * {@code "app_key" + appKey + "format" + "json" + "method" + method + "timestamp" + timestamp + "v" + v + 请求体 + appSecret}。
 * 返回 {@code {"result":"1","msg":"...","errors":[...],"data":...}}，result 为 1 时成功。
 * <ul>
 *   <li>仓库 com.basis.warehouse.getlist（service_code=F 海外仓）；物流产品 com.basis.logisticsproduct.getlist（按仓库）</li>
 *   <li>库存 fu.wms.inventory.get（分页 page_no / page_size；available_stock 可用、onway_stock 在途、pending_stock 待出库）</li>
 *   <li>入库委托 fu.wms.inbound.create（ref_no、箱 box_list）、查询 fu.wms.inbound.getlist（status、sku_list 中 received_qty）</li>
 *   <li>出库委托 fu.wms.outbound.create（ref_no = 包裹号、logistics_product_code、oconsignment_sku）、查询 fu.wms.outbound.getlist
 *   （status、shipping_no）、取消 fu.wms.outbound.cancel</li>
 *   <li>费用 fu.wms.bill.getlist（按费用日期分页）</li>
 *   <li>退件 fu.wms.return.create / fu.wms.return.getlist（sku_list 中 good_qty 良品、bad_qty 不良品）</li>
 *   <li>调拨出库：fu.wms.outbound.create 的 consignment_type = B（B2B），收件人为调入仓地址</li>
 *   <li>取消：入库委托 fu.wms.inbound.cancel、退件 fu.wms.return.cancel</li>
 * </ul>
 * 授权：appKey、appSecret（开放平台创建应用），可选 accessToken（第三方应用授权）。测试环境 open-test.4px.com。
 * <p>
 * 说明：按 4PX 开放平台公开文档实现，尚未用真实账号联调。需要用真实账号核对的点：各接口的版本号（均按 1.0.0）；
 * 入库 / 出库状态码（下面按 C 完成 / 已出库、P / R 处理中、X / E 取消归一）；费用接口的方法名与字段名；
 * 退件接口的方法名、状态码与良品 / 不良品字段；B2B 出库的 consignment_type 取值
 */
@Component
public class FourPxFb4Connector implements OwhConnector {

    static final String NAME = "4PX FB4";
    static final String PROD_URL = "https://open.4px.com/router/api/service";
    static final String TEST_URL = "https://open-test.4px.com/router/api/service";
    private static final String RESULT_OK = "1";
    private static final String V = "1.0.0";
    private static final OwhInfo INFO = new OwhInfo(OwhProvider.FOURPX_FB4, "ecom.label.owh.provider.fourpx", List.of(
            CarrierField.text("appKey", "ecom.label.owh.cred.appKey", true),
            CarrierField.secret("appSecret", "ecom.label.owh.cred.appSecret"),
            CarrierField.text("accessToken", "ecom.label.owh.cred.accessToken", false)),
            PROD_URL, TEST_URL, EnumSet.allOf(OwhCapability.class), "https://open.4px.com/");

    @Override
    public OwhProvider provider() {
        return OwhProvider.FOURPX_FB4;
    }

    @Override
    public OwhInfo info() {
        return INFO;
    }

    @Override
    public List<OwhData.Warehouse> warehouses(OwhContext ctx) {
        List<OwhData.Warehouse> out = new ArrayList<>();
        for (JsonNode n : OwhHttp.array(call(ctx, "com.basis.warehouse.getlist", Map.of("service_code", "F")), "data", "list")) {
            out.add(new OwhData.Warehouse(OwhHttp.text(n, "warehouse_code"), OwhHttp.first(n, "warehouse_name_cn", "warehouse_name_en", "warehouse_name"),
                    OwhHttp.first(n, "country", "country_code")));
        }
        return out;
    }

    @Override
    public List<OwhData.Product> products(OwhContext ctx, String warehouseCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transport_mode", "1");
        if (StringUtils.hasText(warehouseCode)) {
            body.put("source_position_code", warehouseCode);
        }
        List<OwhData.Product> out = new ArrayList<>();
        for (JsonNode n : OwhHttp.array(call(ctx, "com.basis.logisticsproduct.getlist", body), "data", "list")) {
            out.add(new OwhData.Product(OwhHttp.text(n, "logistics_product_code"), OwhHttp.first(n, "logistics_product_name_cn", "logistics_product_name_en"),
                    warehouseCode, OwhHttp.first(n, "carrier_name")));
        }
        return out;
    }

    @Override
    public List<OwhData.Stock> inventory(OwhContext ctx, String warehouseCode) {
        List<OwhData.Stock> out = new ArrayList<>();
        for (int page = 1; page <= OwhMaps.MAX_PAGES; page++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("warehouse_code", warehouseCode);
            body.put("page_no", page);
            body.put("page_size", OwhMaps.PAGE_SIZE);
            JsonNode list = OwhHttp.array(call(ctx, "fu.wms.inventory.get", body), "data", "list");
            for (JsonNode n : list) {
                out.add(new OwhData.Stock(OwhHttp.first(n, "sku_code", "sku"), OwhHttp.dec(n, "available_stock"), OwhHttp.dec(n, "onway_stock"),
                        OwhHttp.dec(n, "pending_stock")));
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
        body.put("ref_no", d.refNo());
        body.put("to_warehouse_code", d.warehouseCode());
        body.put("transport_type", d.transportMode());
        body.put("carrier", d.carrier());
        body.put("tracking_no", d.trackingNo());
        body.put("eta", d.etaDate() == null ? null : d.etaDate().toString());
        body.put("remark", d.remark());
        List<Map<String, Object>> boxes = new ArrayList<>();
        for (OwhData.Box b : d.boxes()) {
            Map<String, Object> box = new LinkedHashMap<>();
            box.put("box_no", b.boxMark());
            box.put("length", b.length());
            box.put("width", b.width());
            box.put("height", b.height());
            box.put("weight", b.weight());
            List<Map<String, Object>> skus = new ArrayList<>();
            for (OwhData.Line l : b.items()) {
                skus.add(Map.of("sku_code", l.sku(), "plan_qty", l.qty().intValue()));
            }
            box.put("sku_list", skus);
            boxes.add(box);
        }
        body.put("box_list", boxes);
        String no = OwhHttp.first(call(ctx, "fu.wms.inbound.create", body), "consignment_no");
        if (no == null) {
            throw new BizException("ecom.error.owh.badResponse", NAME, "consignment_no");
        }
        return no;
    }

    @Override
    public OwhData.InboundState queryInbound(OwhContext ctx, String owhOrderNo) {
        JsonNode data = one(call(ctx, "fu.wms.inbound.getlist", Map.of("consignment_no", owhOrderNo)));
        String raw = OwhHttp.first(data, "status");
        OwhInboundStatus st;
        if (OwhMaps.in(raw, "C", "F")) {
            st = OwhInboundStatus.RECEIVED;
        } else if (OwhMaps.in(raw, "R", "P")) {
            st = OwhInboundStatus.RECEIVING;
        } else if (OwhMaps.in(raw, "X", "E")) {
            st = OwhInboundStatus.CANCELLED;
        } else {
            st = OwhInboundStatus.PUSHED;
        }
        Map<String, BigDecimal> received = new LinkedHashMap<>();
        for (JsonNode n : OwhHttp.array(data, "sku_list")) {
            received.merge(OwhHttp.first(n, "sku_code"), OwhHttp.dec(n, "received_qty", "shelf_qty"), BigDecimal::add);
        }
        return new OwhData.InboundState(st, raw, received, OwhHttp.dateTime(OwhHttp.first(data, "complete_time", "update_time")));
    }

    @Override
    public OwhData.OutboundResult createOutbound(OwhContext ctx, OwhData.OutboundDraft d) {
        return outbound(ctx, d, false);
    }

    /** 调拨出库：同出库委托，consignment_type = B（B2B 出库） */
    @Override
    public OwhData.OutboundResult createTransferOut(OwhContext ctx, OwhData.OutboundDraft d) {
        return outbound(ctx, d, true);
    }

    private OwhData.OutboundResult outbound(OwhContext ctx, OwhData.OutboundDraft d, boolean b2b) {
        OwhData.Receiver r = d.receiver();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ref_no", d.refNo());
        body.put("from_warehouse_code", d.warehouseCode());
        body.put("consignment_type", b2b ? "B" : "S");
        body.put("logistics_service_info", Map.of("logistics_product_code", d.productCode()));
        body.put("platform_order_no", d.platformOrderNo());
        Map<String, Object> recipient = new LinkedHashMap<>();
        recipient.put("first_name", OwhMaps.firstName(r.name()));
        recipient.put("last_name", OwhMaps.lastName(r.name()));
        recipient.put("phone", r.phone());
        recipient.put("email", r.email());
        recipient.put("country", r.country());
        recipient.put("state", r.state());
        recipient.put("city", r.city());
        recipient.put("street", r.address());
        recipient.put("post_code", r.postcode());
        body.put("recipient_info", recipient);
        List<Map<String, Object>> skus = new ArrayList<>();
        for (OwhData.Line l : d.items()) {
            skus.add(Map.of("sku_code", l.sku(), "qty", l.qty().intValue()));
        }
        body.put("oconsignment_sku", skus);
        body.put("remark", d.remark());
        JsonNode data = call(ctx, "fu.wms.outbound.create", body);
        String no = OwhHttp.first(data, "consignment_no");
        if (no == null) {
            throw new BizException("ecom.error.owh.badResponse", NAME, "consignment_no");
        }
        return new OwhData.OutboundResult(no, OwhHttp.first(data, "shipping_no"), null, null);
    }

    @Override
    public OwhData.OutboundState queryOutbound(OwhContext ctx, String owhOrderNo) {
        JsonNode data = one(call(ctx, "fu.wms.outbound.getlist", Map.of("consignment_no", owhOrderNo)));
        String raw = OwhHttp.first(data, "status");
        OwhOutboundStatus st;
        if (OwhMaps.in(raw, "C", "S_OUT")) {
            st = OwhOutboundStatus.OUTBOUND;
        } else if (OwhMaps.in(raw, "X", "E")) {
            st = OwhOutboundStatus.CANCELLED;
        } else if (OwhMaps.in(raw, "I")) {
            st = OwhOutboundStatus.INTERCEPTING;
        } else {
            st = OwhOutboundStatus.PUSHED;
        }
        return new OwhData.OutboundState(st, raw, OwhHttp.first(data, "shipping_no", "tracking_no"), OwhHttp.first(data, "carrier_name", "logistics_product_name"),
                OwhHttp.dateTime(OwhHttp.first(data, "outbound_time", "ship_time")));
    }

    @Override
    public boolean cancelOutbound(OwhContext ctx, String owhOrderNo, String reason) {
        JsonNode data = call(ctx, "fu.wms.outbound.cancel", Map.of("consignment_no", owhOrderNo, "cancel_reason", reason == null ? "cancel" : reason));
        String st = OwhHttp.first(data, "status", "cancel_status");
        return st == null || !st.toUpperCase(Locale.ROOT).startsWith("I");
    }

    @Override
    public List<OwhData.Fee> fees(OwhContext ctx, LocalDate begin, LocalDate end) {
        List<OwhData.Fee> out = new ArrayList<>();
        for (int page = 1; page <= OwhMaps.MAX_PAGES; page++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("start_date", begin.toString());
            body.put("end_date", end.toString());
            body.put("page_no", page);
            body.put("page_size", OwhMaps.PAGE_SIZE);
            JsonNode list = OwhHttp.array(call(ctx, "fu.wms.bill.getlist", body), "data", "list");
            for (JsonNode n : list) {
                String typeName = OwhHttp.first(n, "fee_name", "fee_type");
                LocalDate date = OwhHttp.date(OwhHttp.first(n, "bill_date", "create_time"));
                out.add(new OwhData.Fee(OwhHttp.first(n, "bill_no", "id"), OwhMaps.feeType(typeName), date == null ? end : date,
                        OwhHttp.first(n, "consignment_no", "ref_no"), OwhHttp.first(n, "warehouse_code"), OwhHttp.first(n, "sku_code"),
                        OwhHttp.first(n, "currency"), OwhHttp.dec(n, "amount").abs(), typeName));
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
        body.put("ref_no", d.refNo());
        body.put("warehouse_code", d.warehouseCode());
        body.put("platform_order_no", d.platformOrderNo());
        body.put("carrier", d.carrier());
        body.put("tracking_no", d.trackingNo());
        body.put("remark", d.remark());
        List<Map<String, Object>> skus = new ArrayList<>();
        for (OwhData.Line l : d.lines()) {
            skus.add(Map.of("sku_code", l.sku(), "qty", l.qty().intValue()));
        }
        body.put("sku_list", skus);
        String no = OwhHttp.first(call(ctx, "fu.wms.return.create", body), "consignment_no");
        if (no == null) {
            throw new BizException("ecom.error.owh.badResponse", NAME, "consignment_no");
        }
        return no;
    }

    @Override
    public OwhData.ReturnState queryReturn(OwhContext ctx, String owhOrderNo) {
        JsonNode data = one(call(ctx, "fu.wms.return.getlist", Map.of("consignment_no", owhOrderNo)));
        String raw = OwhHttp.first(data, "status");
        OwhReturnStatus st = OwhMaps.in(raw, "C", "F") ? OwhReturnStatus.RECEIVED : OwhMaps.in(raw, "X", "E") ? OwhReturnStatus.CANCELLED : OwhReturnStatus.PUSHED;
        Map<String, BigDecimal> good = new LinkedHashMap<>();
        Map<String, BigDecimal> bad = new LinkedHashMap<>();
        for (JsonNode n : OwhHttp.array(data, "sku_list")) {
            String sku = OwhHttp.first(n, "sku_code");
            good.merge(sku, OwhHttp.dec(n, "good_qty"), BigDecimal::add);
            bad.merge(sku, OwhHttp.dec(n, "bad_qty"), BigDecimal::add);
        }
        return new OwhData.ReturnState(st, raw, good, bad, OwhHttp.dateTime(OwhHttp.first(data, "complete_time", "update_time")));
    }

    /** 取消入库委托 fu.wms.inbound.cancel（未到仓的入库单） */
    @Override
    public void cancelInbound(OwhContext ctx, String owhOrderNo) {
        call(ctx, "fu.wms.inbound.cancel", Map.of("consignment_no", owhOrderNo));
    }

    /** 取消退件 fu.wms.return.cancel（未收货的退件单） */
    @Override
    public void cancelReturn(OwhContext ctx, String owhOrderNo) {
        call(ctx, "fu.wms.return.cancel", Map.of("consignment_no", owhOrderNo));
    }

    /** 签名原文：app_key{appKey}format{json}method{method}timestamp{ts}v{v}{body}{appSecret}，MD5 小写 */
    public static String sign(String appKey, String method, String timestamp, String version, String body, String appSecret) {
        return CarrierSigns.md5Lower("app_key" + appKey + "format" + "json" + "method" + method + "timestamp" + timestamp + "v" + version + body + appSecret);
    }

    /** 列表接口按单号查询时取第一条 */
    private static JsonNode one(JsonNode data) {
        JsonNode list = OwhHttp.array(data, "data", "list");
        return list.isEmpty() ? data : list.get(0);
    }

    private JsonNode call(OwhContext ctx, String method, Object payload) {
        String appKey = ctx.require(INFO, "appKey");
        String appSecret = ctx.require(INFO, "appSecret");
        String body = OwhHttp.write(payload);
        String ts = String.valueOf(System.currentTimeMillis());
        Map<String, String> params = new LinkedHashMap<>();
        params.put("method", method);
        params.put("app_key", appKey);
        params.put("v", V);
        params.put("timestamp", ts);
        params.put("format", "json");
        params.put("access_token", ctx.optional("accessToken"));
        params.put("sign", sign(appKey, method, ts, V, body, appSecret));
        JsonNode resp = OwhHttp.postJson(ctx.endpoint(INFO) + "?" + CarrierSigns.query(params), Map.of(), body, NAME);
        if (!RESULT_OK.equals(OwhHttp.text(resp, "result"))) {
            StringBuilder sb = new StringBuilder();
            String msg = OwhHttp.text(resp, "msg");
            if (msg != null) {
                sb.append(msg);
            }
            for (JsonNode e : OwhHttp.array(resp, "errors")) {
                String m = OwhHttp.text(e, "error_msg");
                if (m != null) {
                    sb.append(sb.isEmpty() ? "" : "; ").append(m);
                }
            }
            throw new BizException("ecom.error.owh.rejected", NAME, OwhHttp.abbreviate(sb.isEmpty() ? String.valueOf(resp) : sb.toString()));
        }
        JsonNode data = resp.get("data");
        return data == null || data.isNull() ? OwhHttp.MAPPER.createObjectNode() : data;
    }
}
