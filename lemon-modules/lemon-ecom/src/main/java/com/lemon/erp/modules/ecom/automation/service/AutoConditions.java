package com.lemon.erp.modules.ecom.automation.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.model.AutoLogic;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import com.lemon.erp.modules.ecom.automation.trigger.AutoField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * 条件与范围的判断（在 Java 中计算，参照工作流条件分支）：条件组按「全部满足 / 任一满足」组合条件和子组（子组只有一层），
 * 空的条件组视为满足；同时生成判断说明（日志、试运行显示每个条件的实际值与结果）
 */
public final class AutoConditions {

    /** @param detail 判断说明（每个条件一段，分号分隔） */
    public record Match(boolean ok, String detail) {
    }

    private AutoConditions() {
    }

    public static Match evaluate(AutoRuleParts.Group group, AutoEvent event, Map<String, AutoField> fields, Locale locale) {
        return evaluate(group, event, fields, locale, (dict, v) -> v);
    }

    /**
     * @param labeler 枚举字段的取值 → 名称（判断说明中显示字典名称），参数为字典类型和取值
     */
    public static Match evaluate(AutoRuleParts.Group group, AutoEvent event, Map<String, AutoField> fields, Locale locale,
                                 BiFunction<String, String, String> labeler) {
        if (isEmpty(group)) {
            return new Match(true, Messages.get(locale, "ecom.text.auto.condNone"));
        }
        List<String> lines = new ArrayList<>();
        boolean ok = group(group, event, fields, locale, lines, false, labeler);
        return new Match(ok, String.join("; ", lines));
    }

    public static boolean isEmpty(AutoRuleParts.Group g) {
        return g == null || (g.items() == null || g.items().isEmpty()) && (g.groups() == null || g.groups().isEmpty());
    }

    private static boolean group(AutoRuleParts.Group g, AutoEvent event, Map<String, AutoField> fields, Locale locale, List<String> lines,
                                 boolean nested, BiFunction<String, String, String> labeler) {
        boolean or = g.logic() == AutoLogic.OR;
        List<Boolean> results = new ArrayList<>();
        List<String> own = new ArrayList<>();
        if (g.items() != null) {
            for (AutoRuleParts.Condition c : g.items()) {
                Object actual = event.field(c.field());
                boolean r = c.operator().test(actual, c.value());
                results.add(r);
                AutoField f = fields.get(c.field());
                String label = f == null ? c.field() : Messages.get(locale, f.label());
                String expected = c.operator().needsValue() ? c.value() : "";
                String actualText = actual == null ? "-" : AutoText.format(actual, locale);
                if (f != null && f.dict() != null) {
                    expected = names(expected, f.dict(), labeler);
                    actualText = actual == null ? "-" : names(actualText, f.dict(), labeler);
                }
                own.add(Messages.get(locale, "ecom.text.auto.condLine", label, Messages.get(locale, AutoText.key("ecom.enum.auto.operator", c.operator())),
                        expected, actualText,
                        Messages.get(locale, r ? "ecom.text.auto.condYes" : "ecom.text.auto.condNo")));
            }
        }
        if (g.groups() != null && !nested) {
            for (AutoRuleParts.Group sub : g.groups()) {
                if (isEmpty(sub)) {
                    continue;
                }
                List<String> subLines = new ArrayList<>();
                boolean r = group(sub, event, fields, locale, subLines, true, labeler);
                results.add(r);
                own.add("(" + String.join(sub.logic() == AutoLogic.OR ? " | " : " & ", subLines) + ")");
            }
        }
        lines.addAll(own);
        if (results.isEmpty()) {
            return true;
        }
        return or ? results.contains(Boolean.TRUE) : !results.contains(Boolean.FALSE);
    }

    /** 逗号分隔的枚举取值换成名称 */
    private static String names(String values, String dict, BiFunction<String, String, String> labeler) {
        if (values == null || values.isBlank()) {
            return values;
        }
        return Arrays.stream(values.split("[,，]")).map(String::trim).map(v -> labeler.apply(dict, v)).collect(Collectors.joining(","));
    }

    /** 对象是否在规则的范围内：各项为空不限；事件没有该属性（如没有店铺）而规则限定了时不在范围内 */
    public static boolean inScope(AutoRuleParts.Scope s, AutoEvent e) {
        if (s == null) {
            return true;
        }
        if (notEmpty(s.storeIds()) && (e.storeId() == null || !s.storeIds().contains(e.storeId()))) {
            return false;
        }
        if (notEmpty(s.platforms()) && (e.platform() == null || !s.platforms().contains(e.platform()))) {
            return false;
        }
        if (notEmpty(s.sites()) && (e.site() == null || s.sites().stream().noneMatch(x -> x.equalsIgnoreCase(e.site())))) {
            return false;
        }
        if (notEmpty(s.skus()) && (e.skus() == null || e.skus().stream().noneMatch(k -> s.skus().stream().anyMatch(k::equalsIgnoreCase)))) {
            return false;
        }
        return !notEmpty(s.tags()) || e.tags() != null && e.tags().stream().anyMatch(k -> s.tags().stream().anyMatch(k::equalsIgnoreCase));
    }

    private static boolean notEmpty(List<?> l) {
        return l != null && !l.isEmpty();
    }
}
