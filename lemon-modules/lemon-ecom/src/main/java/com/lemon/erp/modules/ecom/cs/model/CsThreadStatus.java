package com.lemon.erp.modules.ecom.cs.model;

/**
 * 买家消息会话的状态（字典 ecom_cs_thread_status）
 */
public enum CsThreadStatus {
    /** 待回复：最近有未回复的买家消息 */
    PENDING,
    /** 已回复 */
    REPLIED,
    /** 已关闭（买家再发消息时重新变为待回复） */
    CLOSED
}
