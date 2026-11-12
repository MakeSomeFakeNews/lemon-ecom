package com.lemon.erp.modules.ecom.cs.connector;

/**
 * 客服连接器的能力
 */
public enum CsCapability {
    /** 拉取买家消息会话与消息 */
    MESSAGES,
    /** 发送回复 */
    SEND_MESSAGE,
    /** 拉取商品评价 */
    REVIEWS,
    /** 回复商品评价 */
    REVIEW_REPLY,
    /** 请求买家留评（亚马逊 Solicitations） */
    REVIEW_REQUEST
}
