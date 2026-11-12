package com.lemon.erp.modules.ecom.carrier;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * 物流商接口的 HTTP 工具：JSON 请求、面单下载；超时、非 2xx、非 JSON 响应转为业务异常（带物流商返回的前 300 个字符）
 */
public final class CarrierHttp {

    public static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    /** 面单文件上限 10MB */
    private static final int MAX_LABEL_BYTES = 10 * 1024 * 1024;

    private CarrierHttp() {
    }

    /** POST JSON 字符串（签名的物流商需要发送与签名完全相同的报文，所以由调用方序列化） */
    public static JsonNode postJson(String url, Map<String, String> headers, String body, String carrier) {
        HttpRequest.Builder b = HttpRequest.newBuilder(uri(url, carrier)).timeout(TIMEOUT)
                .header("Content-Type", "application/json;charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        headers.forEach(b::header);
        return json(send(b.build(), carrier), carrier);
    }

    public static JsonNode get(String url, Map<String, String> headers, String carrier) {
        HttpRequest.Builder b = HttpRequest.newBuilder(uri(url, carrier)).timeout(TIMEOUT).header("Accept", "application/json").GET();
        headers.forEach(b::header);
        return json(send(b.build(), carrier), carrier);
    }

    /** 下载面单 PDF（只接受 http / https，内容必须以 %PDF 开头） */
    public static byte[] download(String url, String carrier) {
        HttpRequest req = HttpRequest.newBuilder(uri(url, carrier)).timeout(TIMEOUT).GET().build();
        byte[] data;
        try {
            HttpResponse<byte[]> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new BizException("ecom.error.ops.carrierHttpError", carrier, resp.statusCode(), "");
            }
            data = resp.body();
        } catch (IOException e) {
            throw new BizException("ecom.error.ops.carrierIoError", carrier, HttpJson.abbreviate(e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("ecom.error.ops.carrierIoError", carrier, HttpJson.abbreviate(e.getMessage()));
        }
        if (!isPdf(data) || data.length > MAX_LABEL_BYTES) {
            throw new BizException("ecom.error.ops.carrierLabelInvalid", carrier);
        }
        return data;
    }

    public static boolean isPdf(byte[] data) {
        return data != null && data.length > 4 && data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F';
    }

    public static String write(Object body) {
        return MAPPER.writeValueAsString(body);
    }

    public static String text(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return v == null || v.isNull() || v.isMissingNode() ? null : v.asString();
    }

    /** 前 300 个字符，令牌、密钥等敏感值遮盖为 ***（规则同 HttpJson） */
    public static String abbreviate(String s) {
        return HttpJson.abbreviate(s);
    }

    private static URI uri(String url, String carrier) {
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new BizException("ecom.error.ops.carrierUrlInvalid", carrier);
        }
        try {
            return URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new BizException("ecom.error.ops.carrierUrlInvalid", carrier);
        }
    }

    private static String send(HttpRequest request, String carrier) {
        try {
            HttpResponse<String> resp = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new BizException("ecom.error.ops.carrierHttpError", carrier, resp.statusCode(), abbreviate(resp.body()));
            }
            return resp.body();
        } catch (IOException e) {
            throw new BizException("ecom.error.ops.carrierIoError", carrier, HttpJson.abbreviate(e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("ecom.error.ops.carrierIoError", carrier, HttpJson.abbreviate(e.getMessage()));
        }
    }

    private static JsonNode json(String body, String carrier) {
        if (body == null || body.isBlank()) {
            return MAPPER.createObjectNode();
        }
        try {
            return MAPPER.readTree(body);
        } catch (JacksonException e) {
            throw new BizException("ecom.error.ops.carrierBadResponse", carrier, abbreviate(body));
        }
    }
}
