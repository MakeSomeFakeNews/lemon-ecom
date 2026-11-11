package com.lemon.erp.modules.ecom.model;

/**
 * 平台订单状态（字典 ecom_order_status）：平台侧的状态，由导入 / 同步带入
 */
public enum EcomOrderStatus {
    /** 待付款 */
    PENDING,
    /** 待发货 */
    UNSHIPPED,
    /** 已发货 */
    SHIPPED,
    /** 已取消 */
    CANCELLED,
    /** 已退款 */
    REFUNDED
}
