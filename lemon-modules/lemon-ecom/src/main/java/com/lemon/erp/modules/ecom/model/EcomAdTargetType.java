package com.lemon.erp.modules.ecom.model;

/**
 * 投放类型
 */
public enum EcomAdTargetType {
    /** 关键词投放 */
    KEYWORD,
    /** 商品 / 类目投放（ASIN、类目表达式） */
    PRODUCT,
    /** 自动投放（紧密匹配、宽泛匹配、同类商品、关联商品） */
    AUTO,
    /** 否定关键词（不参与竞价、规则与分时调价） */
    NEGATIVE
}
