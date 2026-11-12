package com.lemon.erp.modules.ecom.model;

/**
 * 按款式批量映射的配对规则：按变体属性（在线商品的变体属性 ↔ SKU 的规格值）或按 SKU 编码（平台 SKU 去掉前后缀 = 物料编码）
 */
public enum StyleMatchRule {
    /** 按变体属性 */
    ATTRIBUTE,
    /** 按 SKU 编码 */
    SKU
}
