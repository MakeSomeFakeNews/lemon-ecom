package com.lemon.erp.modules.ecom.connector;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 连接器共用的 JSON 取值与换算工具。约定：
 * <ul>
 *   <li>ERP 中的时间一律为服务器默认时区的 {@link LocalDateTime}；平台返回的时间戳 / 带时区的时间换算到默认时区，
 *       没有时区的时间按平台所在时区（由调用方传入）换算</li>
 *   <li>取不到、为 null、为空串的值返回 null，不抛异常</li>
 * </ul>
 */
public final class ConnectorJson {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConnectorJson.class);

    /** 大于该值的时间戳视为毫秒（10^12 毫秒约为 2001 年，秒级时间戳到 33658 年才会超过） */
    private static final long MILLIS_THRESHOLD = 1_000_000_000_000L;

    private ConnectorJson() {
    }

    // ---------------------------------------------------------------- 取值

    /** 按路径取文本（对象字段名，或数组下标的数字串），缺失 / null / 空串为 null；数字、布尔按文本返回 */
    public static String str(JsonNode node, String... path) {
        JsonNode v = at(node, path);
        if (v == null || v.isNull() || v.isMissingNode() || v.isContainer()) {
            return null;
        }
        String s = v.asString();
        return s == null || s.isEmpty() ? null : s;
    }

    /** 按路径取节点，缺失为 null */
    public static JsonNode at(JsonNode node, String... path) {
        JsonNode v = node;
        for (String p : path) {
            if (v == null || v.isNull() || v.isMissingNode()) {
                return null;
            }
            if (v.isArray() && !p.isEmpty() && p.chars().allMatch(Character::isDigit)) {
                v = v.get(Integer.parseInt(p));
            } else {
                v = v.get(p);
            }
        }
        return v == null || v.isMissingNode() ? null : v;
    }

    /** 按路径取金额 / 数量：数字或数字字符串（允许千分位逗号与首尾空格），无法解析为 null */
    public static BigDecimal decimal(JsonNode node, String... path) {
        JsonNode v = at(node, path);
        if (v == null || v.isNull() || v.isContainer()) {
            return null;
        }
        return decimal(v.asString());
    }

    public static BigDecimal decimal(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 两个可空金额相加，都为空时为 null */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return a == null ? b : b == null ? a : a.add(b);
    }

    public static long parseLong(String s, long def) {
        if (s == null || s.isBlank()) {
            return def;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** 去掉值为空的键，返回不可修改的 Map（保持顺序），null 为空 Map */
    public static Map<String, String> compact(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return Map.of();
        }
        Map<String, String> m = new LinkedHashMap<>();
        map.forEach((k, v) -> {
            if (k != null && v != null && !v.isBlank()) {
                m.put(k, v.trim());
            }
        });
        return m.isEmpty() ? Map.of() : Collections.unmodifiableMap(m);
    }

    // ---------------------------------------------------------------- 文本

    /** 第一个非空白的值（去掉首尾空格），都为空时为 null */
    public static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    /** 非空白的部分用空格连接（地址行、姓名），都为空时为 null */
    public static String join(String... parts) {
        return joinWith(" ", parts);
    }

    public static String joinWith(String separator, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(separator);
                }
                sb.append(p.trim());
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    // ---------------------------------------------------------------- 分页

    /**
     * 分页达到连接器的上限时记录警告（防止平台返回异常时死循环；达到上限说明这次没有取全，需要缩短同步间隔或手工分段同步）
     */
    public static void warnPageLimit(String platform, String what, int maxPages) {
        log.warn("{} {} reached the page limit {}, the remaining pages were not fetched", platform, what, maxPages);
    }

    // ---------------------------------------------------------------- 时间

    /** Unix 秒 → 默认时区，0 / 负数 / 缺失为 null */
    public static LocalDateTime unixSeconds(long seconds) {
        return seconds <= 0 ? null : LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault());
    }

    /** Unix 毫秒 → 默认时区，0 / 负数为 null */
    public static LocalDateTime unixMillis(long millis) {
        return millis <= 0 ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }

    /** 节点中的 Unix 秒（数字或数字字符串） */
    public static LocalDateTime unixSeconds(JsonNode v) {
        return unixSeconds(longValue(v));
    }

    /** 节点中的 Unix 毫秒（数字或数字字符串） */
    public static LocalDateTime unixMillis(JsonNode v) {
        return unixMillis(longValue(v));
    }

    /** 节点中的时间戳，按大小自动区分秒与毫秒 */
    public static LocalDateTime unixAuto(JsonNode v) {
        long n = longValue(v);
        return n > MILLIS_THRESHOLD ? unixMillis(n) : unixSeconds(n);
    }

    private static long longValue(JsonNode v) {
        if (v == null || v.isNull() || v.isMissingNode() || v.isContainer()) {
            return 0;
        }
        if (v.isNumber()) {
            return v.asLong();
        }
        return parseLong(v.asString(), 0);
    }

    /** 默认时区的本地时间 → Unix 秒 */
    public static long toUnixSeconds(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    /** 默认时区的本地时间 → UTC 的 ISO 8601（如 2026-11-01T02:00:00Z，去掉毫秒） */
    public static String toUtcIso(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).withNano(0).toOffsetDateTime()
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /** 默认时区的本地时间 → 指定时区的本地时间 */
    public static LocalDateTime toZone(LocalDateTime t, ZoneId zone) {
        return t.atZone(ZoneId.systemDefault()).withZoneSameInstant(zone).toLocalDateTime();
    }

    /**
     * ISO 8601 时间 → 默认时区：带偏移（2026-11-01T10:00:00+08:00、...Z）按偏移换算；没有偏移的（2026-11-01T10:00:00）视为 UTC
     */
    public static LocalDateTime isoTime(String s) {
        return isoTime(s, ZoneOffset.UTC);
    }

    /**
     * ISO 8601 时间 → 默认时区，没有偏移的按 zoneIfMissing 理解；也接受「yyyy-MM-dd HH:mm:ss」（空格分隔）。无法解析为 null
     */
    public static LocalDateTime isoTime(String s, ZoneId zoneIfMissing) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String v = s.trim();
        try {
            return OffsetDateTime.parse(v).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (RuntimeException ignored) {
            // 继续尝试其他格式
        }
        try {
            return ZonedDateTime.parse(v).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (RuntimeException ignored) {
            // 继续尝试其他格式
        }
        try {
            LocalDateTime local = LocalDateTime.parse(v.replace(' ', 'T'));
            return local.atZone(zoneIfMissing).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** 按格式解析平台时区的时间并换算到默认时区，无法解析为 null */
    public static LocalDateTime localTime(String s, DateTimeFormatter format, ZoneId platformZone) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(s.trim(), format).atZone(platformZone).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
