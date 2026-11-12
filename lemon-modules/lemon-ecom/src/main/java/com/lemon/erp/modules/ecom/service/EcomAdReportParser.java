package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.ads.AdsReportRow;
import com.lemon.erp.modules.ecom.model.EcomAdMatchType;
import com.lemon.erp.modules.ecom.model.EcomAdReportType;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 亚马逊广告后台报告文件的解析（英文表头）：表头去掉空格、标点并转小写，再去掉「7 Day」「14 Day」等归因窗口前缀后按名称对应字段，
 * 例如 "7 Day Total Sales ($)" → totalsales、"Customer Search Term" → customersearchterm。
 * 金额去掉货币符号与千分位；日期支持 2026-11-01、Nov 01, 2026、11/01/2026、2026/11/01、01.11.2026、20261101。
 * CTR、CPC、ACOS 等比率列忽略（由系统按合计重新计算）
 */
public final class EcomAdReportParser {

    /** 最多返回的错误数 */
    static final int MAX_ERRORS = 20;

    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        alias("date", "date", "day", "reportdate", "startdate");
        alias("campaignName", "campaignname", "campaign");
        alias("campaignId", "campaignid");
        alias("campaignStatus", "campaignstatus", "status", "state");
        alias("budget", "budget", "campaignbudget", "campaignbudgetamount", "dailybudget");
        alias("adGroupName", "adgroupname", "adgroup");
        alias("adGroupId", "adgroupid");
        alias("targeting", "targeting", "keyword", "keywordtext", "target", "targetingexpression");
        alias("targetId", "keywordid", "targetid", "targetingid");
        alias("matchType", "matchtype");
        alias("searchTerm", "customersearchterm", "searchterm");
        alias("sku", "advertisedsku", "sku");
        alias("asin", "advertisedasin", "asin");
        alias("impressions", "impressions");
        alias("clicks", "clicks");
        alias("spend", "spend", "cost", "totalcost");
        alias("sales", "totalsales", "sales", "attributedsales");
        alias("orders", "totalorders", "orders", "totalordersno", "purchases");
        alias("units", "totalunits", "units", "totalunitsno", "unitssold");
        alias("currency", "currency");
    }

    private static final List<DateTimeFormatter> DATES = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.BASIC_ISO_DATE);

    private static final Set<String> AUTO_TARGETS = Set.of("close-match", "loose-match", "complements", "substitutes", "*");

    private EcomAdReportParser() {
    }

    private static void alias(String field, String... names) {
        for (String n : names) {
            ALIASES.put(n, field);
        }
    }

    /** 解析结果：报告类型、行、错误（有错误时不导入） */
    public record Result(EcomAdReportType type, List<AdsReportRow> rows, List<String> errors) {
    }

    /** 表头规范化：小写、去掉非字母数字、去掉开头的 N day 归因窗口 */
    static String normalize(String header) {
        if (header == null) {
            return "";
        }
        String s = header.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return s.replaceFirst("^\\d+day", "");
    }

    /** 表头 → 字段的列号（同一字段取第一列） */
    static Map<String, Integer> columns(List<String> headers) {
        Map<String, Integer> cols = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String field = ALIASES.get(normalize(headers.get(i)));
            if (field != null) {
                cols.putIfAbsent(field, i);
            }
        }
        return cols;
    }

    /** 按表头判断报告类型 */
    public static EcomAdReportType detect(List<String> headers) {
        Map<String, Integer> cols = columns(headers);
        if (cols.containsKey("searchTerm")) {
            return EcomAdReportType.SEARCH_TERM;
        }
        if (cols.containsKey("sku") || cols.containsKey("asin")) {
            return EcomAdReportType.PRODUCT;
        }
        if (cols.containsKey("targeting") || cols.containsKey("targetId")) {
            return EcomAdReportType.TARGETING;
        }
        return EcomAdReportType.CAMPAIGN;
    }

    public static Result parse(EcomAdReportType forced, List<String> headers, List<List<String>> data, int firstRowNum) {
        EcomAdReportType type = forced == null ? detect(headers) : forced;
        Map<String, Integer> cols = columns(headers);
        List<String> errors = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String f : required(type)) {
            if (!cols.containsKey(f)) {
                missing.add(f);
            }
        }
        if (!missing.isEmpty()) {
            errors.add(Messages.get("ecom.error.ads.reportColumnsMissing", String.join(", ", missing)));
            return new Result(type, List.of(), errors);
        }
        List<AdsReportRow> rows = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            List<String> r = data.get(i);
            int rowNum = firstRowNum + i;
            if (r == null || r.stream().allMatch(v -> v == null || v.isBlank())) {
                continue;
            }
            try {
                rows.add(row(type, cols, r, rowNum));
            } catch (IllegalArgumentException e) {
                if (errors.size() < MAX_ERRORS) {
                    errors.add(Messages.get("ecom.error.ads.reportRowError", rowNum, e.getMessage()));
                }
            }
        }
        return new Result(type, rows, errors);
    }

    private static List<String> required(EcomAdReportType type) {
        return switch (type) {
            case CAMPAIGN -> List.of("date", "campaignName", "spend");
            case PRODUCT -> List.of("date", "campaignName", "sku", "spend");
            case TARGETING -> List.of("date", "campaignName", "adGroupName", "targeting", "spend");
            case SEARCH_TERM -> List.of("date", "campaignName", "adGroupName", "searchTerm", "spend");
        };
    }

    private static AdsReportRow row(EcomAdReportType type, Map<String, Integer> cols, List<String> r, int rowNum) {
        AdsReportRow row = new AdsReportRow();
        row.setRowNum(rowNum);
        String dateText = get(cols, r, "date");
        LocalDate date = date(dateText);
        if (date == null) {
            throw new IllegalArgumentException(Messages.get("ecom.error.ads.reportDateInvalid", dateText == null ? "" : dateText));
        }
        row.setDate(date);
        row.setCampaignName(get(cols, r, "campaignName"));
        row.setCampaignPlatformId(get(cols, r, "campaignId"));
        if (row.getCampaignName() == null && row.getCampaignPlatformId() == null) {
            throw new IllegalArgumentException(Messages.get("ecom.error.ads.reportCampaignMissing"));
        }
        row.setImpressions(longValue(get(cols, r, "impressions")));
        row.setClicks(longValue(get(cols, r, "clicks")));
        row.setSpend(money(get(cols, r, "spend")));
        row.setSales(money(get(cols, r, "sales")));
        row.setOrders((int) longValue(get(cols, r, "orders")));
        row.setUnits((int) longValue(get(cols, r, "units")));
        String currency = get(cols, r, "currency");
        row.setCurrency(currency == null ? null : currency.trim().toUpperCase(Locale.ROOT));
        if (type == EcomAdReportType.CAMPAIGN) {
            row.setCampaignStatus(status(get(cols, r, "campaignStatus")));
            String budget = get(cols, r, "budget");
            row.setCampaignBudget(budget == null ? null : money(budget));
            return row;
        }
        row.setAdGroupName(get(cols, r, "adGroupName"));
        row.setAdGroupPlatformId(get(cols, r, "adGroupId"));
        if (type == EcomAdReportType.PRODUCT) {
            row.setSellerSku(get(cols, r, "sku"));
            row.setAsin(get(cols, r, "asin"));
            if (row.getSellerSku() == null) {
                row.setSellerSku(row.getAsin() == null ? "" : row.getAsin());
            }
            return row;
        }
        String targeting = get(cols, r, "targeting");
        if (targeting == null) {
            throw new IllegalArgumentException(Messages.get("ecom.error.ads.reportTargetMissing"));
        }
        row.setTargetText(targeting);
        row.setTargetPlatformId(get(cols, r, "targetId"));
        EcomAdMatchType match = matchType(get(cols, r, "matchType"));
        EcomAdTargetType tt = match != null ? EcomAdTargetType.KEYWORD
                : AUTO_TARGETS.contains(targeting.toLowerCase(Locale.ROOT)) ? EcomAdTargetType.AUTO : EcomAdTargetType.PRODUCT;
        row.setTargetType(tt);
        row.setMatchType(match);
        if (type == EcomAdReportType.SEARCH_TERM) {
            String term = get(cols, r, "searchTerm");
            if (term == null) {
                throw new IllegalArgumentException(Messages.get("ecom.error.ads.reportSearchTermMissing"));
            }
            row.setSearchTerm(term);
        }
        return row;
    }

    private static String get(Map<String, Integer> cols, List<String> r, String field) {
        Integer i = cols.get(field);
        if (i == null || i >= r.size()) {
            return null;
        }
        String v = r.get(i);
        if (v == null) {
            return null;
        }
        v = v.trim();
        return v.isEmpty() || "-".equals(v) || "--".equals(v) ? null : v;
    }

    static LocalDate date(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        if (v.length() > 10 && v.charAt(4) == '-' && (v.charAt(10) == 'T' || v.charAt(10) == ' ')) {
            v = v.substring(0, 10);
        }
        for (DateTimeFormatter f : DATES) {
            try {
                return LocalDate.parse(v, f);
            } catch (RuntimeException ignored) {
                // 尝试下一种格式
            }
        }
        return null;
    }

    /** 金额：去掉货币符号、空格；同时有 , 和 . 时 , 为千分位；只有 , 且后面恰好 2 位时为小数点 */
    static BigDecimal money(String s) {
        if (s == null) {
            return BigDecimal.ZERO;
        }
        String v = s.replaceAll("[^0-9,.\\-]", "");
        if (v.isEmpty() || "-".equals(v)) {
            return BigDecimal.ZERO;
        }
        if (v.contains(",") && v.contains(".")) {
            v = v.indexOf(',') < v.indexOf('.') ? v.replace(",", "") : v.replace(".", "").replace(',', '.');
        } else if (v.contains(",")) {
            int last = v.lastIndexOf(',');
            v = v.length() - last - 1 == 2 && v.indexOf(',') == last ? v.replace(',', '.') : v.replace(",", "");
        }
        try {
            return new BigDecimal(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(Messages.get("ecom.error.ads.reportNumberInvalid", s));
        }
    }

    static long longValue(String s) {
        if (s == null) {
            return 0;
        }
        return money(s).longValue();
    }

    static EcomAdMatchType matchType(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return switch (v) {
            case "EXACT" -> EcomAdMatchType.EXACT;
            case "PHRASE" -> EcomAdMatchType.PHRASE;
            case "BROAD" -> EcomAdMatchType.BROAD;
            case "NEGATIVE_EXACT", "NEGATIVEEXACT" -> EcomAdMatchType.NEGATIVE_EXACT;
            case "NEGATIVE_PHRASE", "NEGATIVEPHRASE" -> EcomAdMatchType.NEGATIVE_PHRASE;
            default -> null;
        };
    }

    static EcomAdStatus status(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim().toUpperCase(Locale.ROOT);
        if (v.startsWith("PAUSE")) {
            return EcomAdStatus.PAUSED;
        }
        if (v.startsWith("ARCHIV")) {
            return EcomAdStatus.ARCHIVED;
        }
        if (v.startsWith("ENABLE") || v.startsWith("ACTIVE") || v.startsWith("RUNNING") || v.startsWith("DELIVERING")) {
            return EcomAdStatus.ENABLED;
        }
        return null;
    }
}
