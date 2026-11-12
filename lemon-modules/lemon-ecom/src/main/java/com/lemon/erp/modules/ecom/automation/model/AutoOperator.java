package com.lemon.erp.modules.ecom.automation.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;

/**
 * 条件运算符（参照工作流的 ConditionOperator）：两边都是数字时按数值比较，否则按文本比较（不区分大小写）；
 * 字段值是列表（如订单的 SKU、标签）时，肯定类运算符任一元素满足即成立，否定类（不等于、不属于、不包含）全部元素都不满足才成立。
 * 新增运算符：在这里加一个枚举值并在 {@link #supports} 中声明适用的字段类型，前端常量 OPERATORS 同步增加
 */
public enum AutoOperator {

    EQ((a, e) -> compare(a, e) == 0),
    NE((a, e) -> compare(a, e) == 0),
    GT((a, e) -> compare(a, e) > 0),
    GE((a, e) -> compare(a, e) >= 0),
    LT((a, e) -> compare(a, e) < 0),
    LE((a, e) -> compare(a, e) <= 0),
    /** 属于其中之一，多个值用逗号分隔 */
    IN((a, e) -> split(e).stream().anyMatch(v -> compare(a, v) == 0)),
    NOT_IN((a, e) -> split(e).stream().anyMatch(v -> compare(a, v) == 0)),
    /** 文本包含 */
    CONTAINS((a, e) -> text(a).contains(e.trim().toLowerCase(Locale.ROOT))),
    NOT_CONTAINS((a, e) -> text(a).contains(e.trim().toLowerCase(Locale.ROOT))),
    /** 为空（不需要比较值） */
    EMPTY(null),
    NOT_EMPTY(null);

    /** 否定类运算符的 predicate 是对应肯定运算的判断，结果取反 */
    private final BiPredicate<Object, String> predicate;

    AutoOperator(BiPredicate<Object, String> predicate) {
        this.predicate = predicate;
    }

    /** 否定类：不等于、不属于、不包含 */
    public boolean negative() {
        return this == NE || this == NOT_IN || this == NOT_CONTAINS;
    }

    /** 是否需要填写比较值 */
    public boolean needsValue() {
        return this != EMPTY && this != NOT_EMPTY;
    }

    /** 适用的字段类型 */
    public boolean supports(AutoFieldType type) {
        return switch (type) {
            case NUMBER -> this == EQ || this == NE || this == GT || this == GE || this == LT || this == LE || this == EMPTY || this == NOT_EMPTY;
            case TEXT -> this == EQ || this == NE || this == IN || this == NOT_IN || this == CONTAINS || this == NOT_CONTAINS || this == EMPTY
                    || this == NOT_EMPTY;
            case ENUM -> this == EQ || this == NE || this == IN || this == NOT_IN || this == EMPTY || this == NOT_EMPTY;
            case BOOLEAN -> this == EQ;
        };
    }

    /**
     * @param actual   事件字段的值（可以是列表）；为空（null、空文本、空列表）时只有 EMPTY 和否定类成立
     * @param expected 规则中填写的比较值
     */
    public boolean test(Object actual, String expected) {
        boolean empty = isEmpty(actual);
        if (this == EMPTY) {
            return empty;
        }
        if (this == NOT_EMPTY) {
            return !empty;
        }
        if (expected == null || expected.isBlank()) {
            return false;
        }
        if (empty) {
            return negative();
        }
        List<?> values = actual instanceof Collection<?> c ? List.copyOf(c) : List.of(actual);
        boolean any = values.stream().anyMatch(v -> v != null && predicate.test(v, expected.trim()));
        return negative() != any;
    }

    private static boolean isEmpty(Object v) {
        return v == null || v instanceof String s && s.isBlank() || v instanceof Collection<?> c && c.isEmpty();
    }

    private static List<String> split(String e) {
        return Arrays.stream(e.split("[,，]")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String text(Object v) {
        return v.toString().toLowerCase(Locale.ROOT);
    }

    private static int compare(Object actual, String expected) {
        if (actual instanceof Boolean b) {
            return Boolean.toString(b).equalsIgnoreCase(expected.trim()) || (b ? "1" : "0").equals(expected.trim()) ? 0 : 1;
        }
        BigDecimal left = toNumber(actual);
        BigDecimal right = toNumber(expected);
        if (left != null && right != null) {
            return left.compareTo(right);
        }
        return actual.toString().trim().compareToIgnoreCase(expected.trim());
    }

    static BigDecimal toNumber(Object value) {
        if (value instanceof Boolean) {
            return null;
        }
        if (value instanceof BigDecimal d) {
            return d;
        }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
