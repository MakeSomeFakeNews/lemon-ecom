package com.lemon.erp.modules.ecom.publish.model;

/**
 * 调整方式（字典 ecom_price_adjust_mode）：批量改价 / 改库存，以及调价规则的「低于最低价」「按库存」
 */
public enum PriceAdjustMode {
    /** 改为固定值 */
    FIXED,
    /** 按百分比加减（正数加、负数减） */
    PERCENT,
    /** 按金额加减 */
    AMOUNT,
    /** 按价格模板的定价公式计算（只用于价格） */
    FORMULA
}
