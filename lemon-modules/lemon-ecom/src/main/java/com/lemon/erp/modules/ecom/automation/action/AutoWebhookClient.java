package com.lemon.erp.modules.ecom.automation.action;

import com.lemon.erp.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Webhook 发送：HTTP POST JSON（UTF-8），连接超时 5 秒、请求超时 10 秒；网络错误、5xx、429 时重试，最多 3 次（间隔 1、2 秒），
 * 4xx 不重试。配置了密钥时带签名请求头：{@code X-Lemon-Timestamp}（毫秒时间戳）与
 * {@code X-Lemon-Signature: sha256=<hex(HMAC-SHA256(密钥, 时间戳 + "." + 请求体))>}，接收方据此校验来源与报文完整性。
 * 只能在事务外调用
 */
@Slf4j
@Component
public class AutoWebhookClient {

    static final int MAX_ATTEMPTS = 3;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /** @param attempts 实际请求次数 */
    public record Result(int status, int attempts, String body) {
    }

    public Result post(String url, String secret, String body) {
        String lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                long ts = System.currentTimeMillis();
                HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT)
                        .header("Content-Type", "application/json; charset=utf-8")
                        .header("User-Agent", "LemonERP-Automation/1.0")
                        .header("X-Lemon-Timestamp", String.valueOf(ts))
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
                if (StringUtils.hasText(secret)) {
                    req.header("X-Lemon-Signature", "sha256=" + sign(secret, ts + "." + body));
                }
                HttpResponse<String> resp = CLIENT.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = resp.statusCode();
                String text = resp.body() == null ? "" : resp.body();
                if (status >= 200 && status < 300) {
                    return new Result(status, attempt, cut(text));
                }
                lastError = "HTTP " + status + " " + cut(text);
                if (status < 500 && status != 429) {
                    break;
                }
            } catch (IOException e) {
                lastError = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                lastError = "interrupted";
                break;
            } catch (IllegalArgumentException e) {
                throw new BizException("ecom.error.auto.urlInvalid");
            }
            if (attempt < MAX_ATTEMPTS) {
                sleep(1000L * attempt);
            }
        }
        throw new BizException("ecom.error.auto.webhookFailed", lastError);
    }

    /** HMAC-SHA256 签名（十六进制小写） */
    public static String sign(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String cut(String s) {
        return s.length() > 200 ? s.substring(0, 200) : s;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
