package com.lemon.erp.modules.ecom.publish.model;

/**
 * 改价结果（字典 ecom_price_change_status）
 */
public enum PriceChangeStatus {
    /** 已推送到平台 */
    SUCCESS,
    /** 推送失败（本地价格不变） */
    FAILED,
    /** 只修改了本地（不推送或平台不支持） */
    LOCAL
}
