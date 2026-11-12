package com.lemon.erp.modules.ecom.model;

/**
 * 电商税种（字典 ecom_tax_type）：VAT / GST 为价内税（含税价 ÷ (1 + 税率) 拆出），销售税为价外税（取订单的税费）
 */
public enum TaxType {
    /** 增值税 */
    VAT,
    /** 商品及服务税 */
    GST,
    /** 销售税（美国等，价外税） */
    SALES_TAX;

    /** 价内税：订单没有单独的税费时按含税价拆出 */
    public boolean inclusive() {
        return this != SALES_TAX;
    }
}
