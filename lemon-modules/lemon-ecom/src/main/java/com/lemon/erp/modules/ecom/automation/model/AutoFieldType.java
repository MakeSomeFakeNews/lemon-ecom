package com.lemon.erp.modules.ecom.automation.model;

/**
 * 触发器字段的类型：决定条件可用的运算符和值的输入方式
 */
public enum AutoFieldType {
    /** 数字 */
    NUMBER,
    /** 文本 */
    TEXT,
    /** 枚举（取值来自字典） */
    ENUM,
    /** 是 / 否 */
    BOOLEAN
}
