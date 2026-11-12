package com.lemon.erp.modules.ecom.model;

/**
 * SKU 配对推荐的依据：编码相同、去掉前后缀后相同、标题相似；按款式批量映射时还有变体属性相同
 */
public enum PairingMatchType {
    /** 平台 SKU 与物料编码相同 */
    EXACT,
    /** 去掉前后缀后与物料编码相同 */
    STRIPPED,
    /** 标题与物料名称相似 */
    TITLE,
    /** 变体属性与 SKU 规格相同（按款式映射） */
    ATTRIBUTE
}
