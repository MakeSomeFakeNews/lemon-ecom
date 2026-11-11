package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.common.exception.BizException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * 渠道接口的 HTTP + JSON 工具：超时、非 2xx 转为业务异常（包含平台返回的前 300 个字符，便于排查授权问题）
 */
final class HttpJson {

    static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private HttpJson() {
    }

    static JsonNode get(String url, Map<String, String> headers, String platform) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET();
        headers.forEach(b::header);
        return send(b.build(), platform);
    }

    static JsonNode postJson(String url, Map<String, String> headers, Object body, String platform) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)));
        headers.forEach(b::header);
        return send(b.build(), platform);
    }

    static JsonNode postForm(String url, Map<String, String> headers, String form, String platform) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(form));
        headers.forEach(b::header);
        return send(b.build(), platform);
    }

    private static JsonNode send(HttpRequest request, String platform) {
        try {
            HttpResponse<String> resp = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new BizException("ecom.error.connectorHttpError", platform, resp.statusCode(), abbreviate(resp.body()));
            }
            return resp.body() == null || resp.body().isBlank() ? MAPPER.createObjectNode() : MAPPER.readTree(resp.body());
        } catch (IOException e) {
            throw new BizException("ecom.error.connectorIoError", platform, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("ecom.error.connectorIoError", platform, e.getMessage());
        }
    }

    static String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 300 ? s : s.substring(0, 300) + "...";
    }

    static String text(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return v == null || v.isNull() ? null : v.asString();
    }

    static BigDecimal decimal(JsonNode node, String... path) {
        JsonNode v = node;
        for (String p : path) {
            v = v == null ? null : v.get(p);
        }
        if (v == null || v.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(v.asString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
