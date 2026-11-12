package com.lemon.erp.modules.ecom.automation.trigger;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 取数结果（Map 行）的取值工具
 */
public final class AutoRows {

    private AutoRows() {
    }

    public static Long id(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? null : ((Number) v).longValue();
    }

    public static String str(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? null : v.toString();
    }

    public static BigDecimal num(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v == null) {
            return null;
        }
        return v instanceof BigDecimal d ? d : new BigDecimal(v.toString());
    }

    public static Integer integer(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? null : ((Number) v).intValue();
    }

    public static boolean flag(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v instanceof Boolean b ? b : v instanceof Number n && n.intValue() == 1;
    }

    public static LocalDate date(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof LocalDate d) {
            return d;
        }
        if (v instanceof Date d) {
            return d.toLocalDate();
        }
        if (v instanceof LocalDateTime t) {
            return t.toLocalDate();
        }
        if (v instanceof Timestamp t) {
            return t.toLocalDateTime().toLocalDate();
        }
        return v == null ? null : LocalDate.parse(v.toString().substring(0, 10));
    }

    public static LocalDateTime time(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof LocalDateTime t) {
            return t;
        }
        if (v instanceof Timestamp t) {
            return t.toLocalDateTime();
        }
        return v == null ? null : LocalDateTime.parse(v.toString().replace(' ', 'T'));
    }

    public static EcomPlatform platform(Map<String, Object> row, String key) {
        String v = str(row, key);
        return StringUtils.hasText(v) ? EcomPlatform.valueOf(v) : null;
    }

    /** 逗号分隔的标签 */
    public static List<String> tags(String v) {
        return StringUtils.hasText(v) ? Arrays.stream(v.split("[,，]")).map(String::trim).filter(s -> !s.isEmpty()).toList() : List.of();
    }

    /** a / b × 100，保留 2 位；b 为 0 或空时为空 */
    public static BigDecimal percent(BigDecimal a, BigDecimal b) {
        if (a == null || b == null || b.signum() == 0) {
            return null;
        }
        return a.multiply(BigDecimal.valueOf(100)).divide(b, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal scale(BigDecimal v, int scale) {
        return v == null ? null : v.setScale(scale, RoundingMode.HALF_UP);
    }
}
