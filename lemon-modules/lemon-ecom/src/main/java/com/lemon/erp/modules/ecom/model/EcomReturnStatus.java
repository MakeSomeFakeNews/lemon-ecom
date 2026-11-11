package com.lemon.erp.modules.ecom.model;

/**
 * 退货退款状态（字典 ecom_return_status）：待处理 → 已入库（退货）→ 已退款；仅退款直接退款
 */
public enum EcomReturnStatus {
    /** 待处理 */
    PENDING,
    /** 已入库 */
    RECEIVED,
    /** 已退款 */
    REFUNDED,
    /** 已关闭（取消） */
    CLOSED
}
