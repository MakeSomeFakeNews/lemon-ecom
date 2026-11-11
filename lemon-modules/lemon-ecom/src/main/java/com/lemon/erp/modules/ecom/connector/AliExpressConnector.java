package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.dto.OrderDraft;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 速卖通开放平台（TOP 协议，网关 gw.api.taobao.com/router/rest）：aliexpress.solution.order.get 分页拉取订单，
 * aliexpress.solution.order.fulfill 回传发货。授权信息：appKey、appSecret、accessToken（授权后得到的 session）。
 * <p>
 * 说明：按开放平台公开文档实现（MD5 签名：secret + 按键排序的参数名值串 + secret，结果大写），尚未用真实店铺联调
 */
@Component
public class AliExpressConnector implements ChannelConnector {

    static final String PLATFORM = "AliExpress";
    private static final String GATEWAY = "https://gw.api.taobao.com/router/rest";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAGE_SIZE = 50;

    @Override
    public EcomPlatform platform() {
        return EcomPlatform.ALIEXPRESS;
    }

    @Override
    public boolean supportsShipmentConfirm() {
        return true;
    }

    @Override
    public List<OrderDraft> fetchOrders(EcomStore store, StoreCredentials c, LocalDateTime since) {
        List<OrderDraft> drafts = new ArrayList<>();
        int page = 1;
        while (page <= 20) {
            String query = HttpJson.MAPPER.writeValueAsString(Map.of("page_size", PAGE_SIZE, "current_page", page,
                    "modified_date_start", since.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("Asia/Shanghai")).format(TS)));
            JsonNode resp = call(c, "aliexpress.solution.order.get", Map.of("param_aeop_order_query", query));
            JsonNode result = resp.path("aliexpress_solution_order_get_response").path("result");
            JsonNode list = result.path("target_list").path("order_dto");
            if (!list.isArray() || list.isEmpty()) {
                break;
            }
            for (JsonNode o : list) {
                OrderDraft d = toDraft(store, o);
                JsonNode detail = call(c, "aliexpress.solution.order.info.get",
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
    public void confirmShipment(EcomStore store, StoreCredentials c, EcomOrder order, List<EcomOrderItem> items) {
        Map<String, String> params = new TreeMap<>();
        params.put("service_name", order.getCarrier());
        params.put("logistics_no", order.getTrackingNo());
        params.put("out_ref", order.getPlatformOrderNo());
        params.put("send_type", "all");
        params.put("description", "shipped");
        JsonNode resp = call(c, "aliexpress.solution.order.fulfill", params);
        JsonNode result = resp.path("aliexpress_solution_order_fulfill_response").path("result");
        if (!result.path("result_success").asBoolean(false)) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(result.toString()));
        }
    }

    /** 调用 TOP 接口：系统参数 + 业务参数，MD5 签名，错误响应转为业务异常 */
    private static JsonNode call(StoreCredentials c, String method, Map<String, String> business) {
        TreeMap<String, String> params = new TreeMap<>(business);
        params.put("method", method);
        params.put("app_key", c.require("appKey"));
        params.put("session", c.require("accessToken"));
        params.put("timestamp", LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(TS));
        params.put("format", "json");
        params.put("v", "2.0");
        params.put("sign_method", "md5");
        params.put("sign", sign(params, c.require("appSecret")));
        StringBuilder form = new StringBuilder();
        params.forEach((k, v) -> {
            if (!form.isEmpty()) form.append('&');
            form.append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        JsonNode resp = HttpJson.postForm(GATEWAY, Map.of(), form.toString(), PLATFORM);
        JsonNode error = resp.path("error_response");
        if (!error.isMissingNode()) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(error.toString()));
        }
        return resp;
    }

    static String sign(TreeMap<String, String> params, String secret) {
        StringBuilder sb = new StringBuilder(secret);
        params.forEach((k, v) -> sb.append(k).append(v));
        sb.append(secret);
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
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
