package com.lemon.erp.modules.ecom.owh.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.carrier.CarrierField;
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
import java.util.Map;

/**
 * 谷仓海外仓（Goodcang OMS 开放接口 v1，{@code /public_open/<模块>/<方法>}）：POST JSON，请求头 {@code app-token}、{@code app-key}
 * （OMS 后台「API 对接」中生成），返回 {@code {"ask":"Success|Failure","message":"...","Error":{...},"data":...}}，ask 为 Success 时成功。
 * <ul>
 *   <li>仓库 base_data/get_warehouse；物流产品 base_data/get_shipping_method（按仓库）</li>
 *   <li>库存 inventory/get_product_inventory（分页 page / pageSize，sellable 可售、onway + pending 在途 / 待上架、reserved 预留）</li>
 *   <li>入库单 inbound_order/create_grn（reference_no、箱号 box_no × SKU），查询 inbound_order/get_grn_detail（receiving_status、明细 received_quantity）</li>
 *   <li>出库单 order/create_order（reference_no = 包裹号、shipping_method、收件人、items），查询 order/get_order_by_code
 *   （order_status：C 待审核、W 待发货、D 已发货、H 暂存、N 异常、P 问题件、X 已废弃；tracking_no），取消 order/cancel_order
 *   （已开始拣货的返回截单中）</li>
 *   <li>费用 finance/get_cost_flow（按费用日期分页，cost_type / cost_name 识别类型）</li>
 *   <li>退件 return_order/create_return_order（reference_no、tracking_number、items），查询 return_order/get_return_order_detail
 *   （ro_status，明细 sellable_quantity 良品、unsellable_quantity 不良品）</li>
 *   <li>取消：入库单 inbound_order/cancel_grn、退件单 return_order/cancel_return_order</li>
 *   <li>调拨出库（海外仓之间 / 发往本地仓）：order/create_order 加 order_type = B2B，收件人为调入仓地址，查询同出库单</li>
 * </ul>
 * 测试环境地址为谷仓提供的沙箱。
 * <p>
 * 说明：按谷仓开放接口公开文档实现，尚未用真实账号联调。需要用真实账号核对的点：沙箱地址；入库单状态码（下面按 0–2 待到仓、3–4 收货 / 上架中、
 * 5 完成、6 / -1 废弃 归一）；费用流水接口的路径与字段名；取消接口返回截单中的字段（cancel_status = 2）；
 * 退件单接口的路径、状态码（按 3 / C 完成、X / -1 作废归一）与良品 / 不良品字段；B2B 出库的 order_type 取值
 */
@Component
public class GoodcangConnector implements OwhConnector {

    static final String NAME = "Goodcang";
    static final String PROD_URL = "https://oms.goodcang.net/public_open";
    static final String TEST_URL = "https://sandbox-oms.goodcang.net/public_open";
    private static final String ASK_OK = "Success";
    private static final OwhInfo INFO = new OwhInfo(OwhProvider.GOODCANG, "ecom.label.owh.provider.goodcang", List.of(
            CarrierField.text("appKey", "ecom.label.owh.cred.appKey", true),
            CarrierField.secret("appToken", "ecom.label.owh.cred.appToken")),
            PROD_URL, TEST_URL, EnumSet.allOf(OwhCapability.class), "https://open.goodcang.com/");

    @Override
    public OwhProvider provider() {
        return OwhProvider.GOODCANG;
    }

    @Override
    public OwhInfo info() {
        return INFO;
    }

    @Override
    public List<OwhData.Warehouse> warehouses(OwhContext ctx) {
        List<OwhData.Warehouse> out = new ArrayList<>();
        for (JsonNode n : OwhHttp.array(call(ctx, "base_data/get_warehouse", Map.of()))) {
            out.add(new OwhData.Warehouse(OwhHttp.text(n, "warehouse_code"), OwhHttp.first(n, "warehouse_name", "warehouse_desc"),
                    OwhHttp.first(n, "country_code", "country")));
        }
        return out;
    }

    @Override
    public List<OwhData.Product> products(OwhContext ctx, String warehouseCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (StringUtils.hasText(warehouseCode)) {
            body.put("warehouse_code", warehouseCode);
        }
        List<OwhData.Product> out = new ArrayList<>();
        for (JsonNode n : OwhHttp.array(call(ctx, "base_data/get_shipping_method", body))) {
            out.add(new OwhData.Product(OwhHttp.first(n, "code", "sm_code"), OwhHttp.first(n, "name", "cn_name", "sm_name"),
                    OwhHttp.first(n, "warehouse_code") == null ? warehouseCode : OwhHttp.first(n, "warehouse_code"), OwhHttp.first(n, "carrier_name", "carrier")));
        }
        return out;
    }

    @Override
    public List<OwhData.Stock> inventory(OwhContext ctx, String warehouseCode) {
        List<OwhData.Stock> out = new ArrayList<>();
        for (int page = 1; page <= OwhMaps.MAX_PAGES; page++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("pageSize", OwhMaps.PAGE_SIZE);
            body.put("page", page);
            body.put("warehouse_code", warehouseCode);
            JsonNode resp = callRaw(ctx, "inventory/get_product_inventory", body);
            JsonNode list = OwhHttp.array(resp.get("data"));
            for (JsonNode n : list) {
                out.add(new OwhData.Stock(OwhHttp.text(n, "product_sku"), OwhHttp.dec(n, "sellable"),
                        OwhHttp.dec(n, "onway").add(OwhHttp.dec(n, "pending")), OwhHttp.dec(n, "reserved")));
            }
            if (list.size() < OwhMaps.PAGE_SIZE || "false".equalsIgnoreCase(OwhHttp.text(resp, "nextPage"))) {
                break;
            }
        }
        return out;
    }

    @Override
    public String createInbound(OwhContext ctx, OwhData.InboundDraft d) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reference_no", d.refNo());
        body.put("warehouse_code", d.warehouseCode());
        // 0 海运散货、1 海运整柜、2 空运、3 快递（按运输方式粗略对应）
        body.put("receiving_shipping_type", shippingType(d.transportMode()));
        body.put("tracking_number", d.trackingNo());
        body.put("shipping_company", d.carrier());
        body.put("eta_date", d.etaDate() == null ? null : d.etaDate().toString());
        body.put("receiving_desc", d.remark());
        List<Map<String, Object>> boxes = new ArrayList<>();
        for (OwhData.Box b : d.boxes()) {
            Map<String, Object> box = new LinkedHashMap<>();
            box.put("box_no", b.boxNo());
            box.put("box_mark", b.boxMark());
            box.put("length", b.length());
            box.put("width", b.width());
            box.put("height", b.height());
            box.put("weight", b.weight());
            List<Map<String, Object>> items = new ArrayList<>();
            for (OwhData.Line l : b.items()) {
                items.add(Map.of("product_sku", l.sku(), "quantity", l.qty().intValue()));
            }
            box.put("details", items);
            boxes.add(box);
        }
        body.put("items", boxes);
        String no = OwhHttp.first(call(ctx, "inbound_order/create_grn", body), "receiving_code");
        if (no == null) {
            throw new BizException("ecom.error.owh.badResponse", NAME, "receiving_code");
        }
        return no;
    }

    @Override
    public OwhData.InboundState queryInbound(OwhContext ctx, String owhOrderNo) {
        JsonNode data = call(ctx, "inbound_order/get_grn_detail", Map.of("receiving_code", owhOrderNo));
        String raw = OwhHttp.first(data, "receiving_status");
        OwhInboundStatus st;
        if (OwhMaps.in(raw, "5")) {
            st = OwhInboundStatus.RECEIVED;
        } else if (OwhMaps.in(raw, "3", "4")) {
            st = OwhInboundStatus.RECEIVING;
        } else if (OwhMaps.in(raw, "6", "-1")) {
            st = OwhInboundStatus.CANCELLED;
        } else {
            st = OwhInboundStatus.PUSHED;
        }
        Map<String, BigDecimal> received = new LinkedHashMap<>();
        for (JsonNode n : OwhHttp.array(data, "items", "details")) {
            received.merge(OwhHttp.text(n, "product_sku"), OwhHttp.dec(n, "received_quantity", "putaway_quantity"), BigDecimal::add);
        }
        return new OwhData.InboundState(st, raw, received, OwhHttp.dateTime(OwhHttp.first(data, "receiving_finish_time", "update_time")));
    }

    @Override
    public OwhData.OutboundResult createOutbound(OwhContext ctx, OwhData.OutboundDraft d) {
        return outbound(ctx, d, false);
    }

    /** 调拨出库：同出库单接口，order_type = B2B（发往另一个仓库的整箱 / 批量出库） */
    @Override
    public OwhData.OutboundResult createTransferOut(OwhContext ctx, OwhData.OutboundDraft d) {
        return outbound(ctx, d, true);
    }

    private OwhData.OutboundResult outbound(OwhContext ctx, OwhData.OutboundDraft d, boolean b2b) {
        OwhData.Receiver r = d.receiver();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reference_no", d.refNo());
        body.put("platform", "OTHER");
        if (b2b) {
            body.put("order_type", "B2B");
        }
        body.put("shipping_method", d.productCode());
        body.put("warehouse_code", d.warehouseCode());
        body.put("country_code", r.country());
        body.put("province", r.state());
        body.put("city", r.city());
        body.put("address1", r.address());
        body.put("zipcode", r.postcode());
        body.put("name", r.name());
        body.put("phone", r.phone());
        body.put("email", r.email());
        body.put("order_desc", d.remark());
        body.put("platform_order_code", d.platformOrderNo());
        List<Map<String, Object>> items = new ArrayList<>();
        for (OwhData.Line l : d.items()) {
            items.add(Map.of("product_sku", l.sku(), "quantity", l.qty().intValue()));
        }
        body.put("items", items);
        JsonNode data = call(ctx, "order/create_order", body);
        String no = OwhHttp.first(data, "order_code");
        if (no == null) {
            throw new BizException("ecom.error.owh.badResponse", NAME, "order_code");
        }
        return new OwhData.OutboundResult(no, OwhHttp.first(data, "tracking_no"), null, null);
    }

    @Override
    public OwhData.OutboundState queryOutbound(OwhContext ctx, String owhOrderNo) {
        JsonNode data = call(ctx, "order/get_order_by_code", Map.of("order_code", owhOrderNo));
        String raw = OwhHttp.first(data, "order_status");
        OwhOutboundStatus st;
        if (OwhMaps.in(raw, "D")) {
            st = OwhOutboundStatus.OUTBOUND;
        } else if (OwhMaps.in(raw, "X")) {
            st = OwhOutboundStatus.CANCELLED;
        } else if ("1".equals(OwhHttp.first(data, "intercept_status"))) {
            st = OwhOutboundStatus.INTERCEPTING;
        } else {
            st = OwhOutboundStatus.PUSHED;
        }
        return new OwhData.OutboundState(st, raw, OwhHttp.first(data, "tracking_no", "tracking_number"),
                OwhHttp.first(data, "carrier_name", "shipping_method"), OwhHttp.dateTime(OwhHttp.first(data, "date_shipping", "ship_time")));
    }

    @Override
    public boolean cancelOutbound(OwhContext ctx, String owhOrderNo, String reason) {
        JsonNode data = call(ctx, "order/cancel_order", Map.of("order_code", owhOrderNo, "reason", reason == null ? "cancel" : reason));
        return !"2".equals(OwhHttp.first(data, "cancel_status"));
    }

    @Override
    public List<OwhData.Fee> fees(OwhContext ctx, LocalDate begin, LocalDate end) {
        List<OwhData.Fee> out = new ArrayList<>();
        for (int page = 1; page <= OwhMaps.MAX_PAGES; page++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("start_time", begin + " 00:00:00");
            body.put("end_time", end + " 23:59:59");
            body.put("page", page);
            body.put("pageSize", OwhMaps.PAGE_SIZE);
            JsonNode list = OwhHttp.array(call(ctx, "finance/get_cost_flow", body));
            for (JsonNode n : list) {
                String typeName = OwhHttp.first(n, "cost_name", "cost_type");
                LocalDate date = OwhHttp.date(OwhHttp.first(n, "cost_time", "add_time"));
                out.add(new OwhData.Fee(OwhHttp.first(n, "flow_no", "cost_id"), OwhMaps.feeType(typeName), date == null ? end : date,
                        OwhHttp.first(n, "order_code", "reference_no", "receiving_code"), OwhHttp.first(n, "warehouse_code"), OwhHttp.first(n, "product_sku"),
                        OwhHttp.first(n, "currency_code", "currency"), OwhHttp.dec(n, "cost_amount", "amount").abs(), typeName));
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
        body.put("reference_no", d.refNo());
        body.put("warehouse_code", d.warehouseCode());
        body.put("platform_order_code", d.platformOrderNo());
        body.put("shipping_company", d.carrier());
        body.put("tracking_number", d.trackingNo());
        body.put("buyer_name", d.buyerName());
        body.put("country_code", d.country());
        body.put("ro_desc", d.remark());
        List<Map<String, Object>> items = new ArrayList<>();
        for (OwhData.Line l : d.lines()) {
            items.add(Map.of("product_sku", l.sku(), "quantity", l.qty().intValue()));
        }
        body.put("items", items);
        String no = OwhHttp.first(call(ctx, "return_order/create_return_order", body), "ro_code");
        if (no == null) {
            throw new BizException("ecom.error.owh.badResponse", NAME, "ro_code");
        }
        return no;
    }

    @Override
    public OwhData.ReturnState queryReturn(OwhContext ctx, String owhOrderNo) {
        JsonNode data = call(ctx, "return_order/get_return_order_detail", Map.of("ro_code", owhOrderNo));
        String raw = OwhHttp.first(data, "ro_status");
        OwhReturnStatus st = OwhMaps.in(raw, "3", "C", "COMPLETED") ? OwhReturnStatus.RECEIVED
                : OwhMaps.in(raw, "X", "-1", "CANCELLED") ? OwhReturnStatus.CANCELLED : OwhReturnStatus.PUSHED;
        Map<String, BigDecimal> good = new LinkedHashMap<>();
        Map<String, BigDecimal> bad = new LinkedHashMap<>();
        for (JsonNode n : OwhHttp.array(data, "items")) {
            String sku = OwhHttp.text(n, "product_sku");
            good.merge(sku, OwhHttp.dec(n, "sellable_quantity"), BigDecimal::add);
            bad.merge(sku, OwhHttp.dec(n, "unsellable_quantity"), BigDecimal::add);
        }
        return new OwhData.ReturnState(st, raw, good, bad, OwhHttp.dateTime(OwhHttp.first(data, "receiving_time", "update_time")));
    }

    /** 废弃入库单 inbound_order/cancel_grn（未到仓的入库单） */
    @Override
    public void cancelInbound(OwhContext ctx, String owhOrderNo) {
        call(ctx, "inbound_order/cancel_grn", Map.of("receiving_code", owhOrderNo));
    }

    /** 作废退件单 return_order/cancel_return_order（未收货的退件单） */
    @Override
    public void cancelReturn(OwhContext ctx, String owhOrderNo) {
        call(ctx, "return_order/cancel_return_order", Map.of("ro_code", owhOrderNo));
    }

    private static int shippingType(String transportMode) {
        if (transportMode == null) {
            return 0;
        }
        return switch (transportMode) {
            case "AIR" -> 2;
            case "EXPRESS" -> 3;
            default -> 0;
        };
    }

    private JsonNode call(OwhContext ctx, String path, Object payload) {
        JsonNode data = callRaw(ctx, path, payload).get("data");
        return data == null || data.isNull() ? OwhHttp.MAPPER.createObjectNode() : data;
    }

    private JsonNode callRaw(OwhContext ctx, String path, Object payload) {
        Map<String, String> headers = Map.of("app-key", ctx.require(INFO, "appKey"), "app-token", ctx.require(INFO, "appToken"));
        JsonNode resp = OwhHttp.postJson(ctx.endpoint(INFO) + "/" + path, headers, OwhHttp.write(payload), NAME);
        if (!ASK_OK.equalsIgnoreCase(OwhHttp.text(resp, "ask"))) {
            JsonNode err = resp.get("Error");
            String msg = OwhHttp.first(resp, "message");
            String detail = OwhHttp.first(err, "errMessage");
            throw new BizException("ecom.error.owh.rejected", NAME, OwhHttp.abbreviate(
                    (msg == null ? "" : msg) + (detail == null ? "" : " " + detail) + (msg == null && detail == null ? String.valueOf(resp) : "")));
        }
        return resp;
    }
}
