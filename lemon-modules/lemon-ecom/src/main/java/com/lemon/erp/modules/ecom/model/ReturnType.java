package com.lemon.erp.modules.ecom.model;

/**
 * 退货退款类型（字典 ecom_return_type）
 */
public enum ReturnType {
    /** 仅退款：不退货 */
    REFUND_ONLY,
    /** 退货退款：货物退回仓库后退款 */
    RETURN_REFUND
}
