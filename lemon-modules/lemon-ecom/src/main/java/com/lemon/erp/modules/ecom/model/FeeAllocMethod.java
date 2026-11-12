package com.lemon.erp.modules.ecom.model;

/**
 * 不挂订单的平台费用分摊到 SKU 的方式（字典 ecom_fee_alloc_method）
 */
public enum FeeAllocMethod {
    /** 结算报告带 SKU：直接归属该 SKU */
    SKU,
    /** 按结算周期内各 SKU 的销量（件数） */
    QTY,
    /** 按结算周期内各 SKU 的本位币销售额 */
    SALES,
    /** 结算周期内店铺没有销量：留在店铺，不分到 SKU */
    STORE
}
