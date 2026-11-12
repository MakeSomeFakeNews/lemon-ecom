package com.lemon.erp.modules.ecom.model;

/**
 * 广告活动 / 广告组 / 投放的状态（字典 ecom_ad_status）
 */
public enum EcomAdStatus {
    /** 启用 */
    ENABLED,
    /** 暂停 */
    PAUSED,
    /** 已归档（平台上不能再修改） */
    ARCHIVED
}
