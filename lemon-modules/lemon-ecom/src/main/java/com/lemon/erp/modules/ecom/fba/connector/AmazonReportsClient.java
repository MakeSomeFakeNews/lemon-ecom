package com.lemon.erp.modules.ecom.fba.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.AmazonSpConnector;
import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorJson;
import com.lemon.erp.modules.ecom.connector.ConnectorSigns;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 亚马逊 Selling Partner API 的报告与入库货件接口（FBA 运营使用），授权复用店铺的渠道授权（{@link AmazonSpConnector}：
 * region、marketplaceId、clientId、clientSecret、refreshToken），每次调用前用 {@link AmazonSpConnector#ensureToken} 换取 / 刷新访问令牌，
 * 请求头 x-amz-access-token；地址按区域（NA / EU / FE）与沙箱开关选择，店铺填写了自定义地址时指向它。
 * <ol>
 *   <li>Reports API 2021-06-30：POST /reports/2021-06-30/reports（createReport，reportType + marketplaceIds，按日期的报告带
 *       dataStartTime / dataEndTime）→ GET /reports/2021-06-30/reports/{reportId}（getReport，processingStatus 为 IN_QUEUE / IN_PROGRESS /
 *       DONE / CANCELLED / FATAL，DONE 时带 reportDocumentId）→ GET /reports/2021-06-30/documents/{reportDocumentId}（getReportDocument，
 *       返回预签名的下载地址 url 和 compressionAlgorithm）→ 直接 GET url 下载（不带令牌），GZIP 时解压，按响应头的字符集解码
 *       （没有时先按 UTF-8 严格解码，失败按 ISO-8859-1，亚马逊部分站点的平面文件为 Windows-1252 / Shift_JIS）</li>
 *   <li>FBA Inbound API v0：GET /fba/inbound/v0/shipments/{shipmentId}/items?MarketplaceId=…（getShipmentItemsByShipmentId，
 *       NextToken 分页），取每个 SKU 的 QuantityShipped / QuantityReceived 做货件对账</li>
 * </ol>
 * 限速：createReport 每分钟约 1 次（突发 15），getReport 每秒 2 次，getReportDocument 每分钟 1 次（突发 15），
 * 429 由 {@link HttpJson} 自动退避重试；报告生成通常需要几分钟到几十分钟，由定时任务轮询，不在请求中等待。
 * <p>
 * 说明：按亚马逊公开文档实现，尚未用真实卖家账号联调。需要用真实账号核对：
 * <ul>
 *   <li>应用需要「库存与订单跟踪」「亚马逊物流」角色，赔偿 / 分类账 / 移除订单报告需要对应的报告权限，未授权时 createReport 返回 403</li>
 *   <li>GET_LEDGER_DETAIL_VIEW_DATA 的日期范围上限（文档为 18 个月），以及按天汇总的报告选项 reportOptions（这里不传，取默认的明细）</li>
 *   <li>GET_FBA_REIMBURSEMENTS_DATA、GET_FBA_FULFILLMENT_REMOVAL_ORDER_DETAIL_DATA 在没有数据时是返回 DONE 的空文件还是 CANCELLED（两种都按完成处理）</li>
 *   <li>各报告的实际表头（解析按规范化后的列名取值，并给出了新旧两种列名）和日期格式（M/d/yyyy、ISO 8601 都支持）</li>
 *   <li>getShipmentItemsByShipmentId 在新版入库接口（Fulfillment Inbound v2024-03-20）创建的货件上是否仍返回 QuantityReceived</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AmazonReportsClient {

    static final String PLATFORM = "Amazon";
    private static final String REPORTS = "/reports/2021-06-30";
    private static final Map<String, String> ENDPOINTS = Map.of(
            "NA", "https://sellingpartnerapi-na.amazon.com",
            "EU", "https://sellingpartnerapi-eu.amazon.com",
            "FE", "https://sellingpartnerapi-fe.amazon.com");
    private static final Map<String, String> SANDBOX_ENDPOINTS = Map.of(
            "NA", "https://sandbox.sellingpartnerapi-na.amazon.com",
            "EU", "https://sandbox.sellingpartnerapi-eu.amazon.com",
            "FE", "https://sandbox.sellingpartnerapi-fe.amazon.com");
    private static final HttpClient DOWNLOADER = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(5);
    private static final int MAX_ITEM_PAGES = 50;

    private final AmazonSpConnector spConnector;

    /** getReport 的结果：亚马逊的处理状态（IN_QUEUE / IN_PROGRESS / DONE / CANCELLED / FATAL）和完成后的文档 ID */
    public record ReportState(String processingStatus, String documentId) {

        public boolean done() {
            return "DONE".equals(processingStatus);
        }

        public boolean cancelled() {
            return "CANCELLED".equals(processingStatus);
        }

        public boolean fatal() {
            return "FATAL".equals(processingStatus);
        }
    }

    /** 货件明细的发货 / 实收数量 */
    public record ShipmentItem(String sellerSku, String fnsku, BigDecimal shippedQty, BigDecimal receivedQty) {
    }

    /** createReport，返回 reportId；按日期的报告传 [start, end] 自然日（按服务器时区换算为 UTC） */
    public String createReport(ConnectorContext ctx, AmzReportType type, LocalDate start, LocalDate end) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reportType", type.getAmazonType());
        body.put("marketplaceIds", List.of(ctx.credentials().require("marketplaceId")));
        if (type.isDated() && start != null) {
            body.put("dataStartTime", ConnectorJson.toUtcIso(start.atStartOfDay()));
        }
        if (type.isDated() && end != null) {
            LocalDateTime endTime = end.plusDays(1).atStartOfDay().minusSeconds(1);
            body.put("dataEndTime", ConnectorJson.toUtcIso(endTime.isAfter(LocalDateTime.now()) ? LocalDateTime.now().minusMinutes(2) : endTime));
        }
        JsonNode resp = HttpJson.postJson(endpoint(ctx) + REPORTS + "/reports", headers(ctx), body, PLATFORM);
        String id = HttpJson.text(resp, "reportId");
        if (id == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(resp.toString()));
        }
        return id;
    }

    public ReportState getReport(ConnectorContext ctx, String reportId) {
        JsonNode resp = HttpJson.get(endpoint(ctx) + REPORTS + "/reports/" + path(reportId), headers(ctx), PLATFORM);
        return new ReportState(HttpJson.text(resp, "processingStatus"), HttpJson.text(resp, "reportDocumentId"));
    }

    /** getReportDocument + 下载（GZIP 解压、按字符集解码），返回报告原文 */
    public String downloadDocument(ConnectorContext ctx, String documentId) {
        JsonNode doc = HttpJson.get(endpoint(ctx) + REPORTS + "/documents/" + path(documentId), headers(ctx), PLATFORM);
        String url = HttpJson.text(doc, "url");
        if (url == null) {
            throw new BizException("ecom.error.connectorCallFailed", PLATFORM, HttpJson.abbreviate(doc.toString()));
        }
        boolean gzip = "GZIP".equalsIgnoreCase(HttpJson.text(doc, "compressionAlgorithm"));
        try {
            HttpResponse<byte[]> resp = DOWNLOADER.send(HttpRequest.newBuilder(URI.create(url)).timeout(DOWNLOAD_TIMEOUT).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new BizException("ecom.error.fba.reportDownloadFailed", resp.statusCode());
            }
            byte[] bytes = resp.body();
            String contentType = resp.headers().firstValue("Content-Type").orElse(null);
            return decode(bytes, gzip, contentType);
        } catch (IOException e) {
            throw new BizException("ecom.error.connectorIoError", PLATFORM, HttpJson.abbreviate(e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("ecom.error.connectorIoError", PLATFORM, e.getMessage());
        }
    }

    /** 报告文件解码：声明了 GZIP 或内容以 GZIP 魔数开头时解压；按 Content-Type 的 charset，没有时 UTF-8 严格解码、失败按 ISO-8859-1 */
    public static String decode(byte[] bytes, boolean gzip, String contentType) throws IOException {
        byte[] data = bytes == null ? new byte[0] : bytes;
        if (gzip || (data.length > 2 && (data[0] & 0xff) == 0x1f && (data[1] & 0xff) == 0x8b)) {
            try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
                data = in.readAllBytes();
            }
        }
        Charset charset = charsetOf(contentType);
        if (charset != null) {
            return new String(data, charset);
        }
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException e) {
            return new String(data, StandardCharsets.ISO_8859_1);
        }
    }

    private static Charset charsetOf(String contentType) {
        if (contentType == null) {
            return null;
        }
        for (String part : contentType.split(";")) {
            String p = part.trim().toLowerCase(Locale.ROOT);
            if (p.startsWith("charset=")) {
                try {
                    return Charset.forName(p.substring(8).replace("\"", "").trim());
                } catch (RuntimeException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /** 货件明细的发货 / 实收数量（getShipmentItemsByShipmentId） */
    public List<ShipmentItem> shipmentItems(ConnectorContext ctx, String shipmentId) {
        String marketplace = ctx.credentials().require("marketplaceId");
        Map<String, String> headers = headers(ctx);
        List<ShipmentItem> items = new ArrayList<>();
        String nextToken = null;
        int pages = 0;
        do {
            String url = endpoint(ctx) + "/fba/inbound/v0/shipments/" + path(shipmentId) + "/items?MarketplaceId=" + ConnectorSigns.enc(marketplace)
                    + (nextToken == null ? "" : "&NextToken=" + ConnectorSigns.enc(nextToken));
            JsonNode payload = HttpJson.get(url, headers, PLATFORM).path("payload");
            for (JsonNode it : payload.path("ItemData")) {
                BigDecimal shipped = HttpJson.decimal(it, "QuantityShipped");
                BigDecimal received = HttpJson.decimal(it, "QuantityReceived");
                items.add(new ShipmentItem(HttpJson.text(it, "SellerSKU"), HttpJson.text(it, "FulfillmentNetworkSKU"),
                        shipped == null ? BigDecimal.ZERO : shipped, received == null ? BigDecimal.ZERO : received));
            }
            nextToken = HttpJson.text(payload, "NextToken");
            pages++;
        } while (nextToken != null && pages < MAX_ITEM_PAGES);
        return items;
    }

    private Map<String, String> headers(ConnectorContext ctx) {
        spConnector.ensureToken(ctx);
        return Map.of("x-amz-access-token", ctx.credentials().require("accessToken"), "Accept", "application/json");
    }

    private static String endpoint(ConnectorContext ctx) {
        if (ctx.baseUrlOverride() != null) {
            return ctx.baseUrlOverride();
        }
        String region = ctx.credentials().get("region") == null ? "NA" : ctx.credentials().get("region").toUpperCase(Locale.ROOT);
        String endpoint = (ctx.sandbox() ? SANDBOX_ENDPOINTS : ENDPOINTS).get(region);
        if (endpoint == null) {
            throw new BizException("ecom.error.connectorRegionInvalid", region);
        }
        return endpoint;
    }

    private static String path(String s) {
        return ConnectorSigns.enc(s).replace("+", "%20");
    }
}
