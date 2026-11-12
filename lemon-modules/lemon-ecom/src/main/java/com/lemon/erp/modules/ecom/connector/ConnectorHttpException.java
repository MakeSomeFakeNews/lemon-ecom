package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.common.exception.BizException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 平台接口返回非 2xx：消息键仍为 ecom.error.connectorHttpError（参数：平台、状态码、响应体摘要），
 * 另外带上状态码、响应头与响应体摘要，连接器据此判断 401（令牌失效）、404、429（限流）等，不必解析消息参数
 */
public class ConnectorHttpException extends BizException {

    private final int statusCode;
    private final transient Map<String, List<String>> headers;
    private final String bodySummary;

    public ConnectorHttpException(String platform, int statusCode, Map<String, List<String>> headers, String bodySummary) {
        super("ecom.error.connectorHttpError", platform, statusCode, bodySummary);
        this.statusCode = statusCode;
        this.headers = headers == null ? Map.of() : Collections.unmodifiableMap(headers);
        this.bodySummary = bodySummary;
    }

    public int statusCode() {
        return statusCode;
    }

    /** 响应头（键为小写） */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /** 响应头的第一个值（名称不区分大小写） */
    public Optional<String> header(String name) {
        List<String> v = headers.get(name.toLowerCase());
        return v == null || v.isEmpty() ? Optional.empty() : Optional.ofNullable(v.get(0));
    }

    /** 响应体前 300 个字符（已遮盖令牌、密钥） */
    public String bodySummary() {
        return bodySummary;
    }

    public boolean isUnauthorized() {
        return statusCode == 401;
    }

    public boolean isNotFound() {
        return statusCode == 404;
    }

    /** 异常是否为指定状态码的 HTTP 错误 */
    public static boolean is(Throwable e, int statusCode) {
        return e instanceof ConnectorHttpException h && h.statusCode == statusCode;
    }
}
