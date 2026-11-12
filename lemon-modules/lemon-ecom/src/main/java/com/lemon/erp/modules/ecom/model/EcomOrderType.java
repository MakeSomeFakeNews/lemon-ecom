package com.lemon.erp.modules.ecom.model;

/**
 * 订单类型（字典 ecom_order_type）：平台同步 / 导入的订单、手工创建的线下 / 站外订单、从原订单创建的补发单
 */
public enum EcomOrderType {
    /** 平台订单（渠道同步、Excel 导入） */
    PLATFORM,
    /** 手工订单（线下、站外、独立站等，不回传平台） */
    MANUAL,
    /** 补发 / 重发单：金额为 0（不计收入），出库成本与运费计入售后成本，关联原订单与售后工单 / 退货 */
    RESEND
}
