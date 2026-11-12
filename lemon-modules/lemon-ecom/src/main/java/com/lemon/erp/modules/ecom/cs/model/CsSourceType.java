package com.lemon.erp.modules.ecom.cs.model;

/**
 * 售后工单的来源
 */
public enum CsSourceType {
    /** 买家消息会话 */
    THREAD,
    /** 商品评价 */
    REVIEW,
    /** 店铺反馈 */
    FEEDBACK
}
