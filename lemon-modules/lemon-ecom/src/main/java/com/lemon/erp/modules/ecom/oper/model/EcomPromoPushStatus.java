package com.lemon.erp.modules.ecom.oper.model;

/**
 * 促销活动推送平台的状态（字典 ecom_promo_push_status）
 */
public enum EcomPromoPushStatus {
    /** 未推送（手工维护或平台不支持） */
    NONE,
    /** 已推送 */
    PUSHED,
    /** 推送失败 */
    FAILED
}
