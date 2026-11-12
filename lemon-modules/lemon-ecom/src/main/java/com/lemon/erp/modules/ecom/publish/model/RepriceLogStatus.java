package com.lemon.erp.modules.ecom.publish.model;

/**
 * 自动调价日志的结果（字典 ecom_reprice_log_status）
 */
public enum RepriceLogStatus {
    /** 仅建议（未推送，可在日志中应用或忽略） */
    SUGGESTED,
    /** 已推送到平台 */
    PUSHED,
    /** 推送失败 */
    FAILED,
    /** 平台不支持改价，只修改了本地价格 */
    LOCAL,
    /** 跳过（没有竞品价格、没有成本等） */
    SKIPPED,
    /** 建议已忽略 */
    IGNORED
}
