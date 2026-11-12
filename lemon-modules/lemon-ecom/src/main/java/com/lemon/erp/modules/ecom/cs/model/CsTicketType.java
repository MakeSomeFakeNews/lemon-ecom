package com.lemon.erp.modules.ecom.cs.model;

/**
 * 售后类型（字典 ecom_cs_ticket_type）：退货退款 / 仅退款生成退货退款单，补发生成一张手工补发订单
 */
public enum CsTicketType {
    /** 退货退款 */
    RETURN_REFUND,
    /** 仅退款 */
    REFUND_ONLY,
    /** 补发 */
    RESEND
}
