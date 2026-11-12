package com.lemon.erp.modules.ecom.cs.model;

/**
 * 卖家回复的发送状态（字典 ecom_cs_send_status）；买家消息为空
 */
public enum CsSendStatus {
    /** 发送中（已保存，正在调用平台接口） */
    SENDING,
    /** 已发送 */
    SENT,
    /** 发送失败，可重新发送 */
    FAILED
}
