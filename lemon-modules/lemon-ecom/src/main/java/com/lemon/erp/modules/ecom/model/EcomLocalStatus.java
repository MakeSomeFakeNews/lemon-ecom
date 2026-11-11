package com.lemon.erp.modules.ecom.model;

/**
 * 订单处理状态（字典 ecom_local_status）：本系统内的处理进度
 * 待处理 → 已出库（生成销售出库，库存减少、应收生成）→ 已发货（填写物流并回传平台）；取消 / 退款的订单为已关闭，SKU 未匹配等为异常
 */
public enum EcomLocalStatus {
    /** 待处理 */
    PENDING,
    /** 异常：SKU 未匹配本地物料、库存不足等，处理后可重新出库 */
    EXCEPTION,
    /** 已出库 */
    OUTBOUND,
    /** 已发货 */
    SHIPPED,
    /** 已关闭 */
    CLOSED
}
