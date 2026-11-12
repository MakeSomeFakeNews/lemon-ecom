package com.lemon.erp.modules.ecom.carrier;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.model.EcomCarrier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 云途物流 YunExpress（OMS API v1）：认证头 {@code Authorization: Basic Base64(客户编号&ApiSecret)}，JSON 报文；
 * 返回 {@code {"Code":"0000","Message":"...","Item":...}}，Code 为 0000 时成功。
 * <ul>
 *   <li>下单 POST WayBill/CreateOrder（订单数组，CustomerOrderNumber 为包裹号、ShippingMethodCode 为运输方式代码）：
 *   每单返回 Success（1 成功）、WayBillNumber（云途单号）、TrackingNumber（跟踪号，可能稍后才分配，没有时用云途单号）</li>
 *   <li>面单 POST Label/Print（单号数组）：返回面单地址 Item[0].Url，再下载 PDF</li>
 *   <li>取消 POST WayBill/Delete（OrderType 1 = 云途单号）</li>
 *   <li>轨迹 GET Tracking/GetTrackInfo?OrderNumber=</li>
 * </ul>
 * 授权：customerCode（客户编号）、apiSecret；测试环境 omsapi.uat.yunexpress.com。
 * <p>
 * 说明：按云途 OMS API 公开文档实现，尚未用真实账号联调；各运输方式要求的申报字段有差异，接入时请先在测试环境验证
 */
@Component
public class YunExpressConnector implements CarrierConnector {

    static final String NAME = "YunExpress";
    static final String PROD_URL = "http://oms.api.yunexpress.com/api";
    static final String TEST_URL = "http://omsapi.uat.yunexpress.com/api";
    /** 返回 Code 为 0000 表示成功 */
    private static final String CODE_OK = "0000";
    /** 单个订单的 Success 为 1 表示下单成功 */
    private static final String ITEM_OK = "1";
    /** 取消时按云途单号 */
    private static final String ORDER_TYPE_WAYBILL = "1";
    private static final CarrierInfo INFO = new CarrierInfo(EcomCarrier.YUNEXPRESS, "ecom.label.ops.carrier.yunexpress", NAME, List.of(
            CarrierField.text("customerCode", "ecom.label.ops.cred.customerCode", true),
            CarrierField.secret("apiSecret", "ecom.label.ops.cred.apiSecret")),
            PROD_URL, TEST_URL, "ecom.label.ops.carrier.yunexpressService", "https://www.yunexpress.com/");

    @Override
    public EcomCarrier carrier() {
        return EcomCarrier.YUNEXPRESS;
    }

    @Override
    public CarrierInfo info() {
        return INFO;
    }

    @Override
    public CarrierWaybill createOrder(CarrierContext ctx, CarrierShipment s) {
        CarrierShipment.Receiver r = s.receiver();
        Map<String, Object> receiver = new LinkedHashMap<>();
        receiver.put("CountryCode", r.country());
        receiver.put("FirstName", r.firstName());
        receiver.put("LastName", r.lastName());
        receiver.put("Street", r.address());
        receiver.put("City", r.city());
        receiver.put("State", r.state());
        receiver.put("Zip", r.postcode());
        receiver.put("Phone", r.phone());
        receiver.put("Email", r.email());
        List<Map<String, Object>> parcels = new ArrayList<>();
        for (CarrierShipment.Item it : s.items()) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("EName", it.nameEn());
            p.put("CName", it.nameCn());
            p.put("SKU", it.sku());
            p.put("Quantity", it.qty());
            p.put("UnitPrice", it.unitPrice().setScale(2, RoundingMode.HALF_UP));
            p.put("UnitWeight", it.unitWeight().setScale(3, RoundingMode.HALF_UP));
            p.put("CurrencyCode", s.currency());
            parcels.add(p);
        }
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("CustomerOrderNumber", s.refNo());
        order.put("ShippingMethodCode", ctx.requireService());
        order.put("PackageCount", 1);
        order.put("Weight", s.weightKg().setScale(3, RoundingMode.HALF_UP));
        order.put("Receiver", receiver);
        order.put("Parcels", parcels);
        order.put("SourceCode", "API");

        JsonNode item = call(ctx, "/WayBill/CreateOrder", List.of(order));
        JsonNode first = item != null && item.isArray() && !item.isEmpty() ? item.get(0) : null;
        if (first == null) {
            throw new BizException("ecom.error.ops.carrierBadResponse", NAME, CarrierHttp.abbreviate(String.valueOf(item)));
        }
        if (!ITEM_OK.equals(CarrierHttp.text(first, "Success"))) {
            throw new BizException("ecom.error.ops.carrierRejected", NAME, CarrierHttp.abbreviate(CarrierHttp.text(first, "Remark")));
        }
        String waybill = CarrierHttp.text(first, "WayBillNumber");
        String tracking = CarrierHttp.text(first, "TrackingNumber");
        if (!StringUtils.hasText(waybill)) {
            throw new BizException("ecom.error.ops.carrierBadResponse", NAME, CarrierHttp.abbreviate(String.valueOf(first)));
        }
        return new CarrierWaybill(waybill, StringUtils.hasText(tracking) ? tracking : waybill, null);
    }

    @Override
    public byte[] label(CarrierContext ctx, CarrierWaybill waybill) {
        JsonNode item = call(ctx, "/Label/Print", List.of(waybill.carrierOrderNo()));
        JsonNode first = item != null && item.isArray() && !item.isEmpty() ? item.get(0) : null;
        String url = CarrierHttp.text(first, "Url");
        if (!StringUtils.hasText(url)) {
            throw new BizException("ecom.error.ops.carrierLabelInvalid", NAME);
        }
        return CarrierHttp.download(url, NAME);
    }

    @Override
    public void cancel(CarrierContext ctx, CarrierWaybill waybill) {
        call(ctx, "/WayBill/Delete", Map.of("OrderType", ORDER_TYPE_WAYBILL, "OrderNumber", waybill.carrierOrderNo()));
    }

    @Override
    public List<CarrierTrackEvent> track(CarrierContext ctx, String trackingNo) {
        JsonNode resp = CarrierHttp.get(ctx.endpoint(INFO) + "/Tracking/GetTrackInfo?" + CarrierSigns.query(Map.of("OrderNumber", trackingNo)),
                headers(ctx), NAME);
        JsonNode item = check(resp);
        List<CarrierTrackEvent> out = new ArrayList<>();
        JsonNode list = item == null ? null : item.get("OrderTrackingDetails");
        if (list != null && list.isArray()) {
            for (JsonNode n : list) {
                out.add(new CarrierTrackEvent(CarrierHttp.text(n, "ProcessDate"), CarrierHttp.text(n, "ProcessLocation"),
                        CarrierHttp.text(n, "ProcessContent")));
            }
        }
        return out;
    }

    /** 认证头：Basic Base64(客户编号&ApiSecret) */
    static String authorization(String customerCode, String apiSecret) {
        return CarrierSigns.basic(customerCode, apiSecret);
    }

    private Map<String, String> headers(CarrierContext ctx) {
        return Map.of("Authorization", authorization(ctx.require(INFO, "customerCode"), ctx.require(INFO, "apiSecret")),
                "Accept-Language", "zh-cn");
    }

    private JsonNode call(CarrierContext ctx, String path, Object body) {
        return check(CarrierHttp.postJson(ctx.endpoint(INFO) + path, headers(ctx), CarrierHttp.write(body), NAME));
    }

    private static JsonNode check(JsonNode resp) {
        if (!CODE_OK.equals(CarrierHttp.text(resp, "Code"))) {
            String msg = CarrierHttp.text(resp, "Message");
            throw new BizException("ecom.error.ops.carrierRejected", NAME, CarrierHttp.abbreviate(msg == null ? String.valueOf(resp) : msg));
        }
        return resp.get("Item");
    }
}
