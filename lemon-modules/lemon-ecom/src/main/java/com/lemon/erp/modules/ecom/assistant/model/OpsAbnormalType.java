package com.lemon.erp.modules.ecom.assistant.model;

/** 运营日报识别的 SKU 异常（字典 ecom_ops_abnormal_type） */
public enum OpsAbnormalType {
    /** 销量骤降：当天件数不到前 7 天日均的一半 */
    SALES_DROP,
    /** 利润下滑：当天利润不到前 7 天日均的一半（或为负） */
    PROFIT_DROP,
    /** 退货率升高：近 7 天退货率明显高于此前 30 天 */
    RETURN_RATE_UP
}
