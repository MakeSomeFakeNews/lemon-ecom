package com.lemon.erp.modules.ecom.carrier;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.model.EcomCarrier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 递四方 4PX 开放平台（open.4px.com）：统一网关 {@code /router/api/service}，公共参数 method、app_key、v、timestamp（毫秒）、format=json、sign
 * 放在查询串中，业务参数为 JSON 请求体。签名：MD5（小写 32 位）of
 * {@code "app_key" + appKey + "format" + "json" + "method" + method + "timestamp" + timestamp + "v" + v + 请求体 + appSecret}。
 * 返回 {@code {"result":"1","msg":"...","errors":[...],"data":{...}}}，result 为 1 时成功。
 * <ul>
 *   <li>下单 ds.xms.order.create（v1.1.0，直发委托单 business_type=BDS）：返回 4PX 单号 4px_tracking_no、委托单号 ds_consignment_no、
 *   尾程跟踪号 logistics_channel_no（有尾程号时作为运单号）</li>
 *   <li>面单 ds.xms.label.get（v1.1.0，PDF，100 × 100）：返回面单地址 label_url_info.logistics_label，再下载 PDF</li>
 *   <li>取消 ds.xms.order.cancel（v1.0.0）</li>
 *   <li>轨迹 tr.order.tracking.get（v1.0.0）：trackingList 的 occurDatetime / occurLocation / trackingContent；状态按最新一条描述归一
 *       （{@link TrackStatusNormalizer}），4PX 的轨迹节点代码（businessLinkCode）需用真实账号核对后再映射</li>
 * </ul>
 * 授权：appKey、appSecret（开放平台创建应用获得），可选 accessToken（第三方应用授权）、发件人信息（部分产品要求）。
 * 测试环境 open-test.4px.com。
 * <p>
 * 说明：按 4PX 开放平台公开文档实现，尚未用真实账号联调；不同产品要求的报文字段（发件人、申报信息、电池等）有差异，接入时请先在测试环境验证
 */
@Component
public class FourPxConnector implements CarrierConnector {

    static final String NAME = "4PX";
    static final String PROD_URL = "https://open.4px.com/router/api/service";
    static final String TEST_URL = "https://open-test.4px.com/router/api/service";
    /** 返回 result 为 1 表示成功 */
    private static final String RESULT_OK = "1";
    private static final CarrierInfo INFO = new CarrierInfo(EcomCarrier.FOURPX, "ecom.label.ops.carrier.fourpx", NAME, List.of(
            CarrierField.text("appKey", "ecom.label.ops.cred.appKey", true),
            CarrierField.secret("appSecret", "ecom.label.ops.cred.appSecret"),
            CarrierField.text("accessToken", "ecom.label.ops.cred.accessToken", false),
            CarrierField.text("senderName", "ecom.label.ops.cred.senderName", false),
            CarrierField.text("senderPhone", "ecom.label.ops.cred.senderPhone", false),
            CarrierField.text("senderCountry", "ecom.label.ops.cred.senderCountry", false),
            CarrierField.text("senderState", "ecom.label.ops.cred.senderState", false),
            CarrierField.text("senderCity", "ecom.label.ops.cred.senderCity", false),
            CarrierField.text("senderAddress", "ecom.label.ops.cred.senderAddress", false),
            CarrierField.text("senderPostcode", "ecom.label.ops.cred.senderPostcode", false)),
            PROD_URL, TEST_URL, "ecom.label.ops.carrier.fourpxService", "https://open.4px.com/");

    @Override
    public EcomCarrier carrier() {
        return EcomCarrier.FOURPX;
    }

    @Override
    public CarrierInfo info() {
        return INFO;
    }

    @Override
    public CarrierWaybill createOrder(CarrierContext ctx, CarrierShipment s) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ref_no", s.refNo());
        body.put("business_type", "BDS");
        body.put("duty_type", "U");
        body.put("cargo_type", "5");
        body.put("logistics_service_info", Map.of("logistics_product_code", ctx.requireService()));
        Map<String, Object> parcel = new LinkedHashMap<>();
        parcel.put("weight", s.weightKg().multiply(BigDecimal.valueOf(1000)).setScale(0, RoundingMode.CEILING).intValue());
        parcel.put("parcel_value", s.totalValue().setScale(2, RoundingMode.HALF_UP));
        parcel.put("currency", s.currency());
        parcel.put("include_battery", "N");
        List<Map<String, Object>> products = new ArrayList<>();
        List<Map<String, Object>> declares = new ArrayList<>();
        for (CarrierShipment.Item it : s.items()) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("sku_code", it.sku());
            p.put("product_name", it.nameEn());
            p.put("qty", it.qty());
            p.put("product_unit_price", it.unitPrice().setScale(2, RoundingMode.HALF_UP));
            p.put("currency", s.currency());
            products.add(p);
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("declare_product_code_qty", it.qty());
            d.put("declare_product_name_en", it.nameEn());
            d.put("declare_product_name_cn", it.nameCn());
            d.put("declare_unit_price_export", it.unitPrice().setScale(2, RoundingMode.HALF_UP));
            d.put("currency_export", s.currency());
            d.put("declare_unit_price_import", it.unitPrice().setScale(2, RoundingMode.HALF_UP));
            d.put("currency_import", s.currency());
            d.put("brand_export", "none");
            d.put("brand_import", "none");
            declares.add(d);
        }
        parcel.put("product_list", products);
        parcel.put("declare_product_info", declares);
        body.put("parcel_list", List.of(parcel));
        body.put("is_insure", "N");
        if (ctx.optional("senderName") != null) {
            Map<String, Object> sender = new LinkedHashMap<>();
            sender.put("first_name", ctx.optional("senderName"));
            sender.put("phone", ctx.optional("senderPhone"));
            sender.put("country", ctx.optional("senderCountry") == null ? "CN" : ctx.optional("senderCountry"));
            sender.put("state", ctx.optional("senderState"));
            sender.put("city", ctx.optional("senderCity"));
            sender.put("street", ctx.optional("senderAddress"));
            sender.put("post_code", ctx.optional("senderPostcode"));
            body.put("sender", sender);
        }
        CarrierShipment.Receiver r = s.receiver();
        Map<String, Object> recipient = new LinkedHashMap<>();
        recipient.put("first_name", r.firstName());
        recipient.put("last_name", r.lastName());
        recipient.put("phone", r.phone());
        recipient.put("email", r.email());
        recipient.put("post_code", r.postcode());
        recipient.put("country", r.country());
        recipient.put("state", r.state());
        recipient.put("city", r.city());
        recipient.put("street", r.address());
        body.put("recipient_info", recipient);
        body.put("deliver_type_info", Map.of("deliver_type", "1"));

        JsonNode data = call(ctx, "ds.xms.order.create", "1.1.0", body);
        String fourPxNo = CarrierHttp.text(data, "4px_tracking_no");
        String consignment = CarrierHttp.text(data, "ds_consignment_no");
        String channelNo = CarrierHttp.text(data, "logistics_channel_no");
        String orderNo = StringUtils.hasText(consignment) ? consignment : fourPxNo;
        String tracking = StringUtils.hasText(channelNo) ? channelNo : fourPxNo;
        if (!StringUtils.hasText(orderNo) || !StringUtils.hasText(tracking)) {
            throw new BizException("ecom.error.ops.carrierBadResponse", NAME, CarrierHttp.abbreviate(String.valueOf(data)));
        }
        return new CarrierWaybill(orderNo, tracking, null);
    }

    @Override
    public byte[] label(CarrierContext ctx, CarrierWaybill waybill) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("request_no", waybill.carrierOrderNo());
        body.put("response_label_format", "PDF");
        body.put("label_size", "label_100x100");
        body.put("is_print_declaration_list", "N");
        body.put("is_print_customer_weight", "N");
        JsonNode data = call(ctx, "ds.xms.label.get", "1.1.0", body);
        JsonNode info = data.get("label_url_info");
        String url = CarrierHttp.text(info, "logistics_label");
        if (!StringUtils.hasText(url)) {
            throw new BizException("ecom.error.ops.carrierLabelInvalid", NAME);
        }
        return CarrierHttp.download(url, NAME);
    }

    @Override
    public void cancel(CarrierContext ctx, CarrierWaybill waybill) {
        call(ctx, "ds.xms.order.cancel", "1.0.0", Map.of("request_no", waybill.carrierOrderNo(), "cancel_reason", "cancel"));
    }

    @Override
    public List<CarrierTrackEvent> track(CarrierContext ctx, String trackingNo) {
        JsonNode data = call(ctx, "tr.order.tracking.get", "1.0.0", Map.of("deliveryOrderNo", trackingNo));
        List<CarrierTrackEvent> out = new ArrayList<>();
        JsonNode list = data.get("trackingList");
        if (list != null && list.isArray()) {
            for (JsonNode n : list) {
                out.add(new CarrierTrackEvent(CarrierHttp.text(n, "occurDatetime"), CarrierHttp.text(n, "occurLocation"),
                        CarrierHttp.text(n, "trackingContent")));
            }
        }
        return out;
    }

    /** 签名原文：app_key{appKey}format{json}method{method}timestamp{ts}v{v}{body}{appSecret} */
    static String sign(String appKey, String method, String timestamp, String version, String body, String appSecret) {
        return CarrierSigns.md5Lower("app_key" + appKey + "format" + "json" + "method" + method + "timestamp" + timestamp + "v" + version + body + appSecret);
    }

    private JsonNode call(CarrierContext ctx, String method, String version, Object payload) {
        String appKey = ctx.require(INFO, "appKey");
        String appSecret = ctx.require(INFO, "appSecret");
        String body = CarrierHttp.write(payload);
        String ts = String.valueOf(System.currentTimeMillis());
        Map<String, String> params = new LinkedHashMap<>();
        params.put("method", method);
        params.put("app_key", appKey);
        params.put("v", version);
        params.put("timestamp", ts);
        params.put("format", "json");
        params.put("access_token", ctx.optional("accessToken"));
        params.put("sign", sign(appKey, method, ts, version, body, appSecret));
        JsonNode resp = CarrierHttp.postJson(ctx.endpoint(INFO) + "?" + CarrierSigns.query(params), Map.of(), body, NAME);
        if (!RESULT_OK.equals(CarrierHttp.text(resp, "result"))) {
            throw new BizException("ecom.error.ops.carrierRejected", NAME, errorOf(resp));
        }
        JsonNode data = resp.get("data");
        return data == null || data.isNull() ? CarrierHttp.MAPPER.createObjectNode() : data;
    }

    private static String errorOf(JsonNode resp) {
        StringBuilder sb = new StringBuilder();
        String msg = CarrierHttp.text(resp, "msg");
        if (msg != null) {
            sb.append(msg);
        }
        JsonNode errors = resp.get("errors");
        if (errors != null && errors.isArray()) {
            for (JsonNode e : errors) {
                String m = CarrierHttp.text(e, "error_msg");
                if (m != null) {
                    sb.append(sb.isEmpty() ? "" : "; ").append(CarrierHttp.text(e, "error_code") == null ? "" : CarrierHttp.text(e, "error_code") + " ").append(m);
                }
            }
        }
        return CarrierHttp.abbreviate(sb.isEmpty() ? String.valueOf(resp) : sb.toString());
    }
}
