package com.lemon.erp.modules.ecom.model;

/**
 * 广告执行日志 / 建议的状态（字典 ecom_ad_log_status）
 */
public enum EcomAdLogStatus {
    /** 待处理的建议（未自动执行，或店铺没有广告接口） */
    PENDING,
    /** 已通过广告接口执行 */
    APPLIED,
    /** 已手工执行（在平台后台修改后标记，只更新本地数据） */
    MANUAL,
    /** 执行失败 */
    FAILED,
    /** 仅提醒 */
    NOTIFIED,
    /** 已忽略 */
    IGNORED
}
