package com.lemon.erp.modules.ecom.automation.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 消息模板与文字：模板中的 ${字段} 替换为事件字段值，另有公共变量 ruleName（规则名称）、triggerName（触发器）、
 * objectLabel（对象）、time（触发时间）、link（页面地址）；没有的变量保留原样，便于发现写错的字段名。
 * 存库的内容（日志说明、消息）按默认语言（中文）生成
 */
public final class AutoText {

    private static final Pattern VAR = Pattern.compile("\\$\\{(\\w+)}");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 公共变量（编辑页「插入变量」列出） */
    public static final String[] COMMON_VARIABLES = {"ruleName", "triggerName", "objectLabel", "time", "link"};

    private AutoText() {
    }

    /** 动态拼接的消息键：前缀 + "." + 后缀（如运算符、渠道名称） */
    public static String key(String prefix, Object suffix) {
        return prefix + "." + suffix;
    }

    /** 按默认语言取消息（存库内容） */
    public static String t(String key, Object... args) {
        return Messages.get(Messages.DEFAULT_LOCALE, key, args);
    }

    public static String render(String template, AutoEvent event, EcomAutoRule rule) {
        return render(template, event, rule, Messages.DEFAULT_LOCALE);
    }

    public static String render(String template, AutoEvent event, EcomAutoRule rule, Locale locale) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        Map<String, Object> vars = variables(event, rule, locale);
        Matcher m = VAR.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String value = vars.containsKey(key) ? format(vars.get(key), locale) : m.group();
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** 模板可用的全部变量：公共变量 + 事件字段 */
    public static Map<String, Object> variables(AutoEvent event, EcomAutoRule rule, Locale locale) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("ruleName", rule == null ? null : rule.getName());
        vars.put("triggerName", event == null ? null : Messages.get(locale, event.trigger().label()));
        vars.put("objectLabel", event == null ? null : event.objectLabel());
        vars.put("time", event == null || event.time() == null ? null : event.time());
        vars.put("link", event == null ? null : event.link());
        if (event != null && event.fields() != null) {
            vars.putAll(event.fields());
        }
        return vars;
    }

    /** 字段值的显示：数字去掉末尾的 0，是否为「是 / 否」，列表用逗号连接，时间到分钟，空为空字符串 */
    public static String format(Object v, Locale locale) {
        if (v == null) {
            return "";
        }
        if (v instanceof BigDecimal d) {
            return d.stripTrailingZeros().toPlainString();
        }
        if (v instanceof Double || v instanceof Float) {
            return BigDecimal.valueOf(((Number) v).doubleValue()).stripTrailingZeros().toPlainString();
        }
        if (v instanceof Boolean b) {
            return Messages.get(locale, b ? "ecom.text.auto.yes" : "ecom.text.auto.no");
        }
        if (v instanceof Collection<?> c) {
            return c.stream().map(x -> format(x, locale)).collect(Collectors.joining(", "));
        }
        if (v instanceof LocalDateTime t) {
            return t.format(TIME);
        }
        if (v instanceof LocalDate d) {
            return d.toString();
        }
        return v.toString();
    }

    public static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    /** 默认的消息标题：自动化规则名称 + 对象 */
    public static String defaultTitle(AutoEvent event, EcomAutoRule rule) {
        return t("ecom.text.auto.defaultTitle", rule.getName(), event.objectLabel() == null ? "" : event.objectLabel());
    }

    /** 默认的消息内容：触发器 + 对象 + 店铺 */
    public static String defaultContent(AutoEvent event) {
        Object store = event.field("storeName");
        return t("ecom.text.auto.defaultContent", t(event.trigger().label()), event.objectLabel() == null ? "" : event.objectLabel(),
                store == null ? "-" : store);
    }
}
