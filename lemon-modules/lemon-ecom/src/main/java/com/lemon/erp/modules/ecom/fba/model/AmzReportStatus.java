package com.lemon.erp.modules.ecom.fba.model;

/**
 * 亚马逊报告任务状态（字典 ecom_amz_report_status）
 */
public enum AmzReportStatus {
    /** 已提交（已调用 createReport，等待生成） */
    SUBMITTED,
    /** 生成中 */
    PROCESSING,
    /** 已下载并处理 */
    DONE,
    /** 失败（亚马逊返回 FATAL、下载或解析出错） */
    FAILED,
    /** 已取消（亚马逊返回 CANCELLED，通常是没有数据） */
    CANCELLED
}
