package com.lemon.erp.modules.ecom.model;

/**
 * 关键词匹配方式（字典 ecom_ad_match_type），商品投放、自动投放为空
 */
public enum EcomAdMatchType {
    /** 精准 */
    EXACT,
    /** 词组 */
    PHRASE,
    /** 广泛 */
    BROAD,
    /** 否定精准 */
    NEGATIVE_EXACT,
    /** 否定词组 */
    NEGATIVE_PHRASE
}
