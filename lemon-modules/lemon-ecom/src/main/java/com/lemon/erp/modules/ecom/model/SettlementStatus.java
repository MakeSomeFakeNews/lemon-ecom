package com.lemon.erp.modules.ecom.model;

/**
 * 平台结算单状态（字典 ecom_settlement_status）
 */
public enum SettlementStatus {
    /** 已导入 */
    IMPORTED,
    /** 已确认：费用已回写订单，可生成收款单核销应收 */
    CONFIRMED
}
