package com.lemon.erp.modules.ecom.dev.ali1688;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.ConnectorHttpException;
import com.lemon.erp.modules.ecom.connector.ConnectorSigns;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models.Address;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models.Cargo;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models.CreatedOrder;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models.Logistics;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models.OrderView;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models.PriceRange;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models.Product;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models.Sku;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models.Token;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models.TraceStep;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1688 开放平台客户端（按公开文档实现，open.1688.com）。所有业务接口都是
 * {@code POST {网关}/param2/1/{namespace}/{name}/{appKey}}，表单参数 + access_token + _aop_timestamp + _aop_signature（{@link Ali1688Signer}）。
 * <p>
 * 用到的接口：
 * <ul>
 *   <li>授权：{@code https://auth.1688.com/oauth/authorize}（client_id、site=1688、redirect_uri、state）；
 *       授权码换令牌 {@code http/1/system.oauth2/getToken}（grant_type=authorization_code、need_refresh_token=true）；
 *       刷新令牌 {@code param2/1/system.oauth2/getToken}（grant_type=refresh_token）。</li>
 *   <li>商品详情：{@code com.alibaba.product:alibaba.cross.productInfo}（跨境买家查询 offer 的标题、规格、阶梯价、起批量、库存）。</li>
 *   <li>快速下单：{@code com.alibaba.trade:alibaba.trade.fastCreateOrder}（flow=general、addressParam、cargoParamList、message）。</li>
 *   <li>订单详情：{@code com.alibaba.trade:alibaba.trade.get.buyerView}（webSite=1688）；物流轨迹：
 *       {@code com.alibaba.logistics:alibaba.trade.getLogisticsTraceInfo.buyerView}；支付链接：{@code com.alibaba.trade:alibaba.alipay.url.get}；
 *       取消订单：{@code com.alibaba.trade:alibaba.trade.cancel}。</li>
 * </ul>
 * 需要用真实账号核对的点：
 * <ol>
 *   <li>应用需要申请「跨境采购（cross）」与「交易（trade）」相关 API 权限，商品详情接口在不同解决方案中可能是
 *       alibaba.product.simple.get 或 alibaba.cross.productInfo，返回结构（productInfo / result.productInfo）需按实际调整；</li>
 *   <li>fastCreateOrder 返回的 totalSuccessAmount、postFee 单位按文档为「分」，这里除以 100；订单详情的 totalAmount、shippingFee 为「元」；</li>
 *   <li>addressParam 需要 1688 能识别的省市区文本（或 districtCode），cargoParamList 的 offerId 为数字、单规格商品不传 specId；</li>
 *   <li>令牌有效期：access_token 约 10 小时，refresh_token 约半年，refresh_token_timeout 格式为 yyyyMMddHHmmssSSS+0800；</li>
 *   <li>业务错误时 HTTP 状态可能为 400 / 401（error_code、error_message）或 200 且 success=false（errorCode、errorMessage / message）。</li>
 * </ol>
 */
@Component
public class Ali1688Client {

    /** 正式网关 */
    public static final String DEFAULT_GATEWAY = "https://gw.open.1688.com/openapi";
    /** 授权页 */
    public static final String AUTHORIZE_URL = "https://auth.1688.com/oauth/authorize";
    /** 图片地址前缀（商品详情返回相对路径时） */
    static final String IMAGE_HOST = "https://cbu01.alicdn.com/";
    static final String PLATFORM = "1688";
    private static final DateTimeFormatter ALI_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Pattern OFFER_IN_URL = Pattern.compile("(?:offer|offerId=|offerid=)/?(\\d{6,20})");
    private static final Pattern SPEC_IN_URL = Pattern.compile("[?&](?:specId|specid|skuId|skuid)=([0-9A-Za-z_-]{1,64})");
    private static final Pattern DIGITS = Pattern.compile("^\\d{6,20}$");

    // ---------------------------------------------------------------- 授权

    public String authorizeUrl(String appKey, String redirectUri, String state) {
        return AUTHORIZE_URL + "?client_id=" + ConnectorSigns.enc(appKey) + "&site=1688&redirect_uri=" + ConnectorSigns.enc(redirectUri)
                + "&state=" + ConnectorSigns.enc(state);
    }

    /** 授权码换取令牌 */
    public Token exchangeCode(Ali1688Context ctx, String code, String redirectUri) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("need_refresh_token", "true");
        form.put("client_id", ctx.appKey());
        form.put("client_secret", ctx.appSecret());
        form.put("redirect_uri", redirectUri);
        form.put("code", code);
        JsonNode node = post(ctx.gateway() + "/http/1/system.oauth2/getToken/" + ctx.appKey(), form);
        return token(node, null);
    }

    /** 刷新访问令牌（刷新令牌不变时沿用原值） */
    public Token refresh(Ali1688Context ctx, String refreshToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", ctx.appKey());
        form.put("client_secret", ctx.appSecret());
        form.put("refresh_token", refreshToken);
        JsonNode node = post(ctx.gateway() + "/param2/1/system.oauth2/getToken/" + ctx.appKey(), form);
        return token(node, refreshToken);
    }

    private static Token token(JsonNode node, String oldRefresh) {
        String access = HttpJson.text(node, "access_token");
        if (access == null || access.isBlank()) {
            throw new BizException("ecom.error.dev.ali1688Failed", errorMessage(node));
        }
        long expiresIn = parseLong(HttpJson.text(node, "expires_in"), 36000L);
        String refresh = HttpJson.text(node, "refresh_token");
        return new Token(access, refresh == null ? oldRefresh : refresh, LocalDateTime.now().plusSeconds(expiresIn),
                parseTime(HttpJson.text(node, "refresh_token_timeout")), HttpJson.text(node, "memberId"), HttpJson.text(node, "resource_owner"));
    }

    // ---------------------------------------------------------------- 商品

    /** 商品详情：标题、主图、单位、起批量、阶梯价、规格（价格、库存）、卖家 */
    public Product product(Ali1688Context ctx, String offerId) {
        JsonNode resp = call(ctx, "com.alibaba.product", "alibaba.cross.productInfo", Map.of("productId", offerId));
        JsonNode root = resp.has("productInfo") ? resp : resp.path("result");
        JsonNode p = root.path("productInfo");
        if (p.isMissingNode() || p.isNull() || p.isEmpty()) {
            throw new BizException("ecom.error.dev.ali1688OfferNotFound", offerId);
        }
        JsonNode sale = p.path("saleInfo");
        List<PriceRange> ranges = new ArrayList<>();
        for (JsonNode r : sale.path("priceRanges")) {
            ranges.add(new PriceRange(intOrNull(r.get("startQuantity")), HttpJson.decimal(r, "price")));
        }
        List<Sku> skus = new ArrayList<>();
        for (JsonNode s : p.path("skuInfos")) {
            StringJoiner text = new StringJoiner(" / ");
            for (JsonNode a : s.path("attributes")) {
                String v = firstText(a, "attributeValue", "skuImageUrl");
                if (v != null) {
                    text.add(v);
                }
            }
            BigDecimal price = HttpJson.decimal(s, "price");
            if (price == null) {
                price = HttpJson.decimal(s, "consignPrice");
            }
            skus.add(new Sku(HttpJson.text(s, "specId"), HttpJson.text(s, "skuId"), text.toString(), price, intOrNull(s.get("amountOnSale"))));
        }
        String image = null;
        JsonNode images = p.path("image").path("images");
        if (images.isArray() && !images.isEmpty()) {
            image = images.get(0).asString();
            if (image != null && !image.startsWith("http")) {
                image = IMAGE_HOST + image;
            }
        }
        String unit = firstText(sale, "unit");
        if (unit == null) {
            unit = HttpJson.text(p.path("shippingInfo"), "unit");
        }
        String id = firstText(p, "productID", "productId");
        return new Product(id == null ? offerId : id, HttpJson.text(p, "subject"), image, unit, intOrNull(sale.get("minOrderQuantity")),
                intOrNull(sale.get("amountOnSale")), ranges, skus, firstText(p, "sellerLoginId", "supplierLoginId"),
                firstText(p, "sellerName", "companyName"), offerUrl(offerId));
    }

    /** 1688 商品链接 */
    public static String offerUrl(String offerId) {
        return "https://detail.1688.com/offer/" + offerId + ".html";
    }

    /**
     * 解析 1688 商品链接（或直接粘贴的 offerId）：detail.1688.com/offer/{offerId}.html、m.1688.com/offer/{offerId}.html、
     * ...?offerId=xxx；链接中带 specId / skuId 参数时一并返回
     *
     * @return [offerId, specId]，specId 可能为空；无法识别时抛出异常
     */
    public static String[] parseOfferUrl(String text) {
        if (text == null || text.isBlank()) {
            throw new BizException("ecom.error.dev.ali1688UrlInvalid");
        }
        String s = text.trim();
        if (DIGITS.matcher(s).matches()) {
            return new String[]{s, null};
        }
        Matcher m = OFFER_IN_URL.matcher(s);
        if (!s.contains("1688.com") || !m.find()) {
            throw new BizException("ecom.error.dev.ali1688UrlInvalid");
        }
        Matcher spec = SPEC_IN_URL.matcher(s);
        return new String[]{m.group(1), spec.find() ? spec.group(1) : null};
    }

    // ---------------------------------------------------------------- 交易

    /** 快速下单（创建待付款订单），一个卖家一单 */
    public CreatedOrder createOrder(Ali1688Context ctx, Address address, List<Cargo> cargos, String message, String outOrderId) {
        Map<String, Object> addr = new LinkedHashMap<>();
        addr.put("fullName", address.fullName());
        addr.put("mobile", address.mobile());
        addr.put("phone", address.mobile());
        addr.put("postCode", address.postCode());
        addr.put("provinceText", address.province());
        addr.put("cityText", address.city());
        addr.put("areaText", address.area());
        addr.put("townText", address.town());
        addr.put("address", address.address());
        List<Map<String, Object>> cargoList = new ArrayList<>();
        for (Cargo c : cargos) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("offerId", Long.valueOf(c.offerId()));
            if (c.specId() != null && !c.specId().isBlank()) {
                m.put("specId", c.specId());
            }
            m.put("quantity", c.quantity());
            cargoList.add(m);
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("flow", "general");
        params.put("addressParam", json(addr));
        params.put("cargoParamList", json(cargoList));
        if (message != null && !message.isBlank()) {
            params.put("message", message);
        }
        if (outOrderId != null) {
            params.put("outOrderId", outOrderId);
        }
        JsonNode resp = call(ctx, "com.alibaba.trade", "alibaba.trade.fastCreateOrder", params);
        JsonNode r = resp.path("result");
        String orderId = HttpJson.text(r, "orderId");
        if (orderId == null || orderId.isBlank()) {
            throw new BizException("ecom.error.dev.ali1688Failed", errorMessage(resp));
        }
        return new CreatedOrder(orderId, fen(HttpJson.decimal(r, "totalSuccessAmount")), fen(HttpJson.decimal(r, "postFee")));
    }

    /** 订单详情（买家视角）：状态、金额、时间、物流单 */
    public OrderView order(Ali1688Context ctx, String orderId) {
        JsonNode resp = call(ctx, "com.alibaba.trade", "alibaba.trade.get.buyerView", Map.of("webSite", "1688", "orderId", orderId));
        JsonNode r = resp.path("result");
        JsonNode base = r.path("baseInfo");
        if (base.isMissingNode() || base.isEmpty()) {
            throw new BizException("ecom.error.dev.ali1688Failed", errorMessage(resp));
        }
        List<Logistics> logistics = new ArrayList<>();
        for (JsonNode l : r.path("nativeLogistics").path("logisticsItems")) {
            String bill = HttpJson.text(l, "logisticsBillNo");
            if (bill != null && !bill.isBlank()) {
                logistics.add(new Logistics(HttpJson.text(l, "logisticsCompanyName"), bill));
            }
        }
        BigDecimal total = HttpJson.decimal(base, "totalAmount");
        BigDecimal shipping = HttpJson.decimal(base, "shippingFee");
        BigDecimal product = HttpJson.decimal(base, "sumProductPayment");
        if (product == null && total != null) {
            product = total.subtract(shipping == null ? BigDecimal.ZERO : shipping);
        }
        String id = firstText(base, "idOfStr", "id");
        return new OrderView(id == null ? orderId : id, HttpJson.text(base, "status"), total, shipping, product,
                parseTime(HttpJson.text(base, "createTime")), parseTime(HttpJson.text(base, "payTime")),
                parseTime(HttpJson.text(base, "allDeliveredTime")), parseTime(HttpJson.text(base, "receivingTime")),
                HttpJson.text(base, "sellerLoginId"), HttpJson.text(base.path("sellerContact"), "companyName"), logistics);
    }

    /** 物流轨迹（多个运单时合并，按接口顺序） */
    public List<TraceStep> trace(Ali1688Context ctx, String orderId) {
        JsonNode resp = call(ctx, "com.alibaba.logistics", "alibaba.trade.getLogisticsTraceInfo.buyerView",
                Map.of("webSite", "1688", "orderId", orderId));
        List<TraceStep> steps = new ArrayList<>();
        for (JsonNode t : resp.path("logisticsTrace")) {
            for (JsonNode s : t.path("logisticsSteps")) {
                steps.add(new TraceStep(HttpJson.text(s, "acceptTime"), HttpJson.text(s, "remark")));
            }
        }
        return steps;
    }

    /** 批量支付链接（在 1688 收银台付款） */
    public String payUrl(Ali1688Context ctx, List<String> orderIds) {
        JsonNode resp = call(ctx, "com.alibaba.trade", "alibaba.alipay.url.get", Map.of("orderIdList", "[" + String.join(",", orderIds) + "]"));
        String url = HttpJson.text(resp, "payUrl");
        if (url == null || url.isBlank()) {
            throw new BizException("ecom.error.dev.ali1688Failed", errorMessage(resp));
        }
        return url;
    }

    /** 取消未付款的订单 */
    public void cancel(Ali1688Context ctx, String orderId, String remark) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("webSite", "1688");
        params.put("tradeID", orderId);
        params.put("cancelReason", "buyerCancel");
        if (remark != null) {
            params.put("remark", remark);
        }
        call(ctx, "com.alibaba.trade", "alibaba.trade.cancel", params);
    }

    // ---------------------------------------------------------------- 通用

    /** 调用业务接口：签名后 POST 表单，success=false 或带错误码时抛出 */
    JsonNode call(Ali1688Context ctx, String namespace, String name, Map<String, String> params) {
        if (ctx.accessToken() == null || ctx.accessToken().isBlank()) {
            throw new BizException("ecom.error.dev.ali1688NotAuthorized");
        }
        String urlPath = "param2/1/" + namespace + "/" + name + "/" + ctx.appKey();
        Map<String, String> all = new LinkedHashMap<>(params);
        all.put("access_token", ctx.accessToken());
        all.put("_aop_timestamp", String.valueOf(System.currentTimeMillis()));
        all.put("_aop_signature", Ali1688Signer.sign(urlPath, all, ctx.appSecret()));
        JsonNode resp = post(ctx.gateway() + "/" + urlPath, all);
        if (failed(resp)) {
            throw new BizException("ecom.error.dev.ali1688Failed", errorMessage(resp));
        }
        return resp;
    }

    private static JsonNode post(String url, Map<String, String> form) {
        try {
            return HttpJson.postForm(url, Map.of(), ConnectorSigns.query(form), PLATFORM);
        } catch (ConnectorHttpException e) {
            throw new BizException("ecom.error.dev.ali1688Failed", e.statusCode() + " " + e.bodySummary());
        }
    }

    /** success 为 false，或带 error_code / errorCode（且不是成功码） */
    static boolean failed(JsonNode resp) {
        JsonNode success = resp.get("success");
        if (success == null) {
            success = resp.path("result").get("success");
        }
        if (success != null && !success.isNull() && "false".equalsIgnoreCase(success.asString())) {
            return true;
        }
        String code = firstText(resp, "error_code", "errorCode");
        return code != null && !code.isBlank() && !"0".equals(code) && !"200".equals(code);
    }

    static String errorMessage(JsonNode resp) {
        String msg = firstText(resp, "error_message", "errorMessage", "errorMsg", "message");
        if (msg == null) {
            msg = firstText(resp.path("result"), "errorMessage", "message", "errorMsg");
        }
        String code = firstText(resp, "error_code", "errorCode", "code");
        String text = (code == null ? "" : code + " ") + (msg == null ? "" : msg);
        return text.isBlank() ? HttpJson.abbreviate(resp.toString()) : text.trim();
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String f : fields) {
            String v = HttpJson.text(node, f);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static Integer intOrNull(JsonNode v) {
        if (v == null || v.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(v.asString().trim()).intValue();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long parseLong(String s, long def) {
        try {
            return s == null ? def : Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** 分 → 元 */
    private static BigDecimal fen(BigDecimal v) {
        return v == null ? null : v.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** 1688 时间：yyyyMMddHHmmssSSS+0800（取前 17 位，按北京时间），解析失败为 null */
    static LocalDateTime parseTime(String s) {
        if (s == null || s.length() < 14) {
            return null;
        }
        String digits = s.length() >= 17 ? s.substring(0, 17) : (s.substring(0, 14) + "000");
        try {
            return LocalDateTime.parse(digits, ALI_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String json(Object value) {
        return HttpJson.MAPPER.writeValueAsString(value);
    }
}
