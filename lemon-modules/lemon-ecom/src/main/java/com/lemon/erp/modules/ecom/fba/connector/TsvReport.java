package com.lemon.erp.modules.ecom.fba.connector;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 亚马逊平面文件报告（制表符分隔，第一行为表头）的解析：表头统一为小写、空格与下划线换成连字符
 * （如 "Event Type" → event-type、"afn_fulfillable_quantity" → afn-fulfillable-quantity），便于兼容报告的新旧表头；
 * 去掉 UTF-8 BOM，忽略空行，值去掉首尾空格与引号，空串为 null
 */
public final class TsvReport {

    private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final DateTimeFormatter DOT_DATE = DateTimeFormatter.ofPattern("d.M.yyyy");

    private final List<String> headers;
    private final List<Row> rows;

    private TsvReport(List<String> headers, List<Row> rows) {
        this.headers = headers;
        this.rows = rows;
    }

    public static TsvReport parse(String text) {
        List<String> headers = new ArrayList<>();
        List<Row> rows = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return new TsvReport(headers, rows);
        }
        String s = text.charAt(0) == '\uFEFF' ? text.substring(1) : text;
        String[] lines = s.split("\r?\n", -1);
        int i = 0;
        while (i < lines.length && lines[i].isBlank()) {
            i++;
        }
        if (i >= lines.length) {
            return new TsvReport(headers, rows);
        }
        for (String h : lines[i].split("\t", -1)) {
            headers.add(normalize(h));
        }
        for (int n = i + 1; n < lines.length; n++) {
            if (lines[n].isBlank()) {
                continue;
            }
            String[] cells = lines[n].split("\t", -1);
            Map<String, String> values = new LinkedHashMap<>();
            for (int c = 0; c < headers.size() && c < cells.length; c++) {
                String v = clean(cells[c]);
                if (v != null) {
                    values.put(headers.get(c), v);
                }
            }
            rows.add(new Row(n + 1, values));
        }
        return new TsvReport(headers, rows);
    }

    public List<String> headers() {
        return headers;
    }

    public List<Row> rows() {
        return rows;
    }

    /** 表头规范化：小写，空格 / 下划线换成连字符，去掉引号与首尾空格 */
    public static String normalize(String header) {
        String h = clean(header);
        return h == null ? "" : h.toLowerCase(Locale.ROOT).replace('_', '-').replaceAll("\\s+", "-");
    }

    private static String clean(String v) {
        if (v == null) {
            return null;
        }
        String s = v.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s.isEmpty() ? null : s;
    }

    /**
     * 报告的一行：按规范化后的列名取值，可以给多个候选列名（兼容不同版本的表头），取第一个有值的
     *
     * @param line 文件中的行号（从 1 开始，含表头行）
     */
    public record Row(int line, Map<String, String> values) {

        public String text(String... names) {
            for (String n : names) {
                String v = values.get(normalize(n));
                if (v != null) {
                    return v;
                }
            }
            return null;
        }

        /** 数值（去掉千分位逗号和货币符号），没有或无法解析时为 null */
        public BigDecimal decimal(String... names) {
            return number(text(names));
        }

        /** 数值，没有时为 0 */
        public BigDecimal qty(String... names) {
            BigDecimal v = decimal(names);
            return v == null ? BigDecimal.ZERO : v;
        }

        public LocalDate date(String... names) {
            return TsvReport.date(text(names));
        }
    }

    public static BigDecimal number(String s) {
        if (s == null) {
            return null;
        }
        String v = s.replace(",", "").replace("$", "").replace("£", "").replace("€", "").trim();
        if (v.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 日期：ISO 日期、ISO 日期时间（带或不带时区，取日期部分）、M/d/yyyy（美国站）、d.M.yyyy（欧洲站） */
    public static LocalDate date(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String v = s.trim();
        try {
            if (v.length() > 10 && (v.charAt(10) == 'T' || v.charAt(10) == ' ') && Character.isDigit(v.charAt(0)) && v.charAt(4) == '-') {
                try {
                    return OffsetDateTime.parse(v.replace(' ', 'T')).toLocalDate();
                } catch (DateTimeParseException e) {
                    return LocalDate.parse(v.substring(0, 10));
                }
            }
            if (v.length() >= 10 && v.charAt(4) == '-') {
                return LocalDate.parse(v.substring(0, 10));
            }
            String head = v.split("[ T]")[0];
            if (head.contains("/")) {
                return LocalDate.parse(head, US_DATE);
            }
            if (head.contains(".")) {
                return LocalDate.parse(head, DOT_DATE);
            }
        } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
            return null;
        }
        return null;
    }
}
