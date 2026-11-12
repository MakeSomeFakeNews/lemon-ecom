package com.lemon.erp.modules.ecom.owh.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 海外仓接口的 HTTP 工具：JSON 请求；超时、非 2xx、非 JSON 响应转为业务异常（带服务商返回的前 300 个字符）；以及读取 JSON 字段的辅助方法
 */
public final class OwhHttp {

    public static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private OwhHttp() {
    }

    /** POST JSON 字符串（签名的服务商需要发送与签名完全相同的报文，所以由调用方序列化） */
    public static JsonNode postJson(String url, Map<String, String> headers, String body, String provider) {
        HttpRequest.Builder b = HttpRequest.newBuilder(uri(url, provider)).timeout(TIMEOUT)
                .header("Content-Type", "application/json;charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        headers.forEach(b::header);
        return json(send(b.build(), provider), provider);
    }

    public static String write(Object body) {
        return MAPPER.writeValueAsString(body);
    }

    public static String text(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        if (v == null || v.isNull() || v.isMissingNode()) {
            return null;
        }
        String s = v.asString();
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    /** 依次取第一个有值的字段 */
    public static String first(JsonNode node, String... fields) {
        for (String f : fields) {
            String v = text(node, f);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    public static BigDecimal dec(JsonNode node, String... fields) {
        String v = first(node, fields);
        if (v == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /** 数组：本身是数组时直接返回，否则取第一个是数组的字段，没有时返回空数组 */
    public static JsonNode array(JsonNode node, String... fields) {
        if (node != null) {
            if (node.isArray()) {
                return node;
            }
            for (String f : fields) {
                JsonNode v = node.get(f);
                if (v != null && v.isArray()) {
                    return v;
                }
            }
        }
        return MAPPER.createArrayNode();
    }

    /** 解析 yyyy-MM-dd[ HH:mm:ss] / ISO 时间，无法识别时为 null */
    public static LocalDateTime dateTime(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String v = s.trim();
        try {
            if (v.length() == 10) {
                return LocalDate.parse(v).atStartOfDay();
            }
            if (v.contains("T")) {
                return LocalDateTime.parse(v.length() > 19 ? v.substring(0, 19) : v);
            }
            return LocalDateTime.parse(v.length() > 19 ? v.substring(0, 19) : v, DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static LocalDate date(String s) {
        LocalDateTime t = dateTime(s);
        return t == null ? null : t.toLocalDate();
    }

    /** 前 300 个字符，令牌、密钥等敏感值遮盖为 ***（规则同 HttpJson） */
    public static String abbreviate(String s) {
        return HttpJson.abbreviate(s);
    }

    private static URI uri(String url, String provider) {
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new BizException("ecom.error.owh.urlInvalid", provider);
        }
        try {
            return URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new BizException("ecom.error.owh.urlInvalid", provider);
        }
    }

    private static String send(HttpRequest request, String provider) {
        try {
            HttpResponse<String> resp = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new BizException("ecom.error.owh.httpError", provider, resp.statusCode(), abbreviate(resp.body()));
            }
            return resp.body();
        } catch (IOException e) {
            throw new BizException("ecom.error.owh.ioError", provider, HttpJson.abbreviate(e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("ecom.error.owh.ioError", provider, HttpJson.abbreviate(e.getMessage()));
        }
    }

    private static JsonNode json(String body, String provider) {
        if (body == null || body.isBlank()) {
            return MAPPER.createObjectNode();
        }
        try {
            return MAPPER.readTree(body);
        } catch (JacksonException e) {
            throw new BizException("ecom.error.owh.badResponse", provider, abbreviate(body));
        }
    }
}
