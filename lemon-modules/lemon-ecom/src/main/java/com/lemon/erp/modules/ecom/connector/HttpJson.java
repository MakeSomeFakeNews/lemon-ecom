package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 渠道接口的 HTTP + JSON 工具（各平台连接器共用）：
 * <ul>
 *   <li>超时：连接 15 秒、请求 60 秒</li>
 *   <li>非 2xx 抛出 {@link ConnectorHttpException}（消息键 ecom.error.connectorHttpError，带状态码、响应头、响应体前 300 个字符，
 *       其中的令牌、密钥已遮盖）</li>
 *   <li>429（限流）/ 503（暂不可用）自动重试：按 Retry-After（秒数或 HTTP 日期），没有时指数退避（1、2、4 秒），
 *       默认最多重试 3 次、总等待不超过 30 秒，超过时直接抛出；策略见 {@link RetryPolicy}，可用配置 lemon.ecom.connector.http-retry.* 关闭</li>
 *   <li>{@link #requestRaw} 按原样发送字符串请求体（签名包含请求体的平台，如 TikTok Shop，需要签名与发送的报文逐字一致）</li>
 * </ul>
 * JSON 取值、时间换算等工具在 {@link ConnectorJson}
 */
@Slf4j
public final class HttpJson {

    public static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final String JSON = "application/json";
    private static final String FORM = "application/x-www-form-urlencoded;charset=utf-8";
    private static final int SUMMARY_MAX = 300;
    /** 遮盖响应 / 错误信息中的令牌与密钥（JSON 键值、查询串） */
    private static final Pattern SECRETS = Pattern.compile(
            "(?i)((?<![A-Za-z0-9_])\"?(?:access_token|refresh_token|accessToken|refreshToken|client_secret|clientSecret|app_secret|appSecret|partner_key|partnerKey"
                    + "|api_key|apiKey|session|sign|token|app_token|appToken|api_token|apiToken|password|secret|secret_key|secretKey|access_key|accessKey)\"?\\s*[:=]\\s*\"?)([^\"&,\\s}]+)");

    /**
     * 429 / 503 的自动重试策略
     *
     * @param maxRetries   最多重试次数（不含第一次请求）
     * @param maxTotalWait 一次调用累计等待的上限，下一次等待会超过时不再重试
     * @param baseDelay    没有 Retry-After 时第一次重试前的等待，之后每次翻倍
     */
    public record RetryPolicy(boolean enabled, int maxRetries, Duration maxTotalWait, Duration baseDelay) {
        public static final RetryPolicy DEFAULT = new RetryPolicy(true, 3, Duration.ofSeconds(30), Duration.ofSeconds(1));
        public static final RetryPolicy DISABLED = new RetryPolicy(false, 0, Duration.ZERO, Duration.ZERO);
    }

    /** 等待（测试中替换为记录等待时间而不真正休眠） */
    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private static volatile RetryPolicy retryPolicy = RetryPolicy.DEFAULT;
    static volatile Sleeper sleeper = Thread::sleep;

    private HttpJson() {
    }

    public static RetryPolicy retryPolicy() {
        return retryPolicy;
    }

    /** 修改自动重试策略（由配置 lemon.ecom.connector.http-retry.* 在启动时设置） */
    public static void retryPolicy(RetryPolicy policy) {
        retryPolicy = policy == null ? RetryPolicy.DISABLED : policy;
    }

    // ---------------------------------------------------------------- 请求

    public static JsonNode get(String url, Map<String, String> headers, String platform) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET();
        headers.forEach(b::header);
        return send(b.build(), platform);
    }

    public static JsonNode postJson(String url, Map<String, String> headers, Object body, String platform) {
        return requestRaw("POST", url, headers, MAPPER.writeValueAsString(body), JSON, platform);
    }

    public static JsonNode putJson(String url, Map<String, String> headers, Object body, String platform) {
        return requestRaw("PUT", url, headers, MAPPER.writeValueAsString(body), JSON, platform);
    }

    /** 任意方法（PATCH、DELETE 等），body 为空时不带请求体 */
    public static JsonNode request(String method, String url, Map<String, String> headers, Object body, String platform) {
        return requestRaw(method, url, headers, body == null ? null : MAPPER.writeValueAsString(body), JSON, platform);
    }

    public static JsonNode postForm(String url, Map<String, String> headers, String form, String platform) {
        return requestRaw("POST", url, headers, form, FORM, platform);
    }

    /**
     * 按原样发送字符串请求体（不再经过 JSON 序列化），签名包含请求体时用它保证签名原文与发送的报文逐字一致
     *
     * @param body        请求体原文，为空时不带请求体
     * @param contentType 请求体类型，为空时按 JSON；headers 中已有 Content-Type 时以 headers 为准
     */
    public static JsonNode requestRaw(String method, String url, Map<String, String> headers, String body, String contentType, String platform) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT)
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        boolean hasType = headers.keySet().stream().anyMatch("Content-Type"::equalsIgnoreCase);
        if (body != null && !hasType) {
            b.header("Content-Type", contentType == null ? JSON : contentType);
        }
        headers.forEach(b::header);
        return send(b.build(), platform);
    }

    /** POST 原样的 JSON 字符串 */
    public static JsonNode postRaw(String url, Map<String, String> headers, String json, String platform) {
        return requestRaw("POST", url, headers, json, JSON, platform);
    }

    private static JsonNode send(HttpRequest request, String platform) {
        RetryPolicy policy = retryPolicy;
        long waited = 0;
        try {
            for (int attempt = 0; ; attempt++) {
                HttpResponse<String> resp = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                int status = resp.statusCode();
                if (status >= 200 && status < 300) {
                    return resp.body() == null || resp.body().isBlank() ? MAPPER.createObjectNode() : MAPPER.readTree(resp.body());
                }
                Map<String, List<String>> headers = lowerCase(resp.headers().map());
                if (policy.enabled() && retryable(status) && attempt < policy.maxRetries()) {
                    long delay = retryDelayMillis(headers, attempt, policy);
                    if (delay >= 0 && waited + delay <= policy.maxTotalWait().toMillis()) {
                        log.warn("{} returned {} for {} {}, retry {} in {} ms", platform, status, request.method(), request.uri().getPath(),
                                attempt + 1, delay);
                        sleeper.sleep(delay);
                        waited += delay;
                        continue;
                    }
                }
                throw new ConnectorHttpException(platform, status, headers, abbreviate(resp.body()));
            }
        } catch (IOException e) {
            throw new BizException("ecom.error.connectorIoError", platform, abbreviate(e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("ecom.error.connectorIoError", platform, e.getMessage());
        }
    }

    static boolean retryable(int status) {
        return status == 429 || status == 503;
    }

    /**
     * 下一次重试前的等待（毫秒）：Retry-After 为秒数或 HTTP 日期时按它，否则 baseDelay × 2^attempt；
     * Retry-After 无法解析时按指数退避
     */
    static long retryDelayMillis(Map<String, List<String>> headers, int attempt, RetryPolicy policy) {
        List<String> values = headers.get("retry-after");
        String v = values == null || values.isEmpty() || values.get(0) == null ? null : values.get(0).trim();
        if (v != null && !v.isEmpty()) {
            if (v.chars().allMatch(Character::isDigit)) {
                try {
                    return Long.parseLong(v) * 1000;
                } catch (NumberFormatException ignored) {
                    return Long.MAX_VALUE;
                }
            }
            try {
                long millis = Duration.between(ZonedDateTime.now(), ZonedDateTime.parse(v, DateTimeFormatter.RFC_1123_DATE_TIME)).toMillis();
                return Math.max(0, millis);
            } catch (RuntimeException ignored) {
                // 无法解析时按指数退避
            }
        }
        return policy.baseDelay().toMillis() << Math.min(attempt, 20);
    }

    private static Map<String, List<String>> lowerCase(Map<String, List<String>> headers) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        headers.forEach((k, v) -> map.put(k.toLowerCase(), v));
        return map;
    }

    // ---------------------------------------------------------------- 工具

    /** 前 300 个字符，令牌、密钥等敏感值遮盖为 *** */
    public static String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        String masked = mask(s);
        return masked.length() <= SUMMARY_MAX ? masked : masked.substring(0, SUMMARY_MAX) + "...";
    }

    /** 遮盖文本中 access_token、refresh_token、client_secret、sign 等键的值 */
    public static String mask(String s) {
        return s == null ? null : SECRETS.matcher(s).replaceAll("$1***");
    }

    public static String text(JsonNode node, String field) {
        JsonNode v = node == null ? null : node.get(field);
        return v == null || v.isNull() ? null : v.asString();
    }

    public static BigDecimal decimal(JsonNode node, String... path) {
        JsonNode v = node;
        for (String p : path) {
            v = v == null ? null : v.get(p);
        }
        if (v == null || v.isNull()) {
            return null;
        }
        try {
            return new BigDecimal(v.asString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
