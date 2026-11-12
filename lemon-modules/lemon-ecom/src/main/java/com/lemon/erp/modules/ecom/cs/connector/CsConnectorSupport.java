package com.lemon.erp.modules.ecom.cs.connector;

import com.lemon.erp.modules.ecom.connector.ConnectorContext;
import com.lemon.erp.modules.ecom.connector.ConnectorJson;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 客服连接器的公共工具：接口地址（自定义地址 &gt; 沙箱 / 正式）、各种精度的时间戳换算
 */
final class CsConnectorSupport {

    /** 单次拉取的分页上限，防止平台返回异常时死循环 */
    static final int MAX_PAGES = 50;

    private CsConnectorSupport() {
    }

    /** 接口根地址：店铺填写了自定义地址时用它，否则按沙箱开关取默认地址 */
    static String baseUrl(ConnectorContext ctx, String production, String sandbox) {
        if (ctx.baseUrlOverride() != null) {
            return ctx.baseUrlOverride();
        }
        return ctx.sandbox() && sandbox != null ? sandbox : production;
    }

    /**
     * Unix 时间戳 → 服务器时区的本地时间，按数值大小判断精度：秒、毫秒、微秒、纳秒；0 或缺失为 null
     */
    static LocalDateTime epoch(JsonNode v) {
        if (v == null || v.isNull() || v.isMissingNode()) {
            return null;
        }
        long n = ConnectorJson.parseLong(v.asString(), 0);
        return epoch(n);
    }

    static LocalDateTime epoch(long n) {
        if (n <= 0) {
            return null;
        }
        long millis;
        if (n >= 100_000_000_000_000_000L) {
            millis = n / 1_000_000;
        } else if (n >= 100_000_000_000_000L) {
            millis = n / 1_000;
        } else if (n >= 1_000_000_000_000L) {
            millis = n;
        } else {
            millis = n * 1000;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }

    /** 本地时间 → Unix 秒 */
    static long toSeconds(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
