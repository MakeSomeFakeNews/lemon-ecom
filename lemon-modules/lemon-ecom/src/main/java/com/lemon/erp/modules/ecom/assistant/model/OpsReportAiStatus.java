package com.lemon.erp.modules.ecom.assistant.model;

/** 运营日报「要点与建议」的生成状态（字典 ecom_ops_report_ai_status） */
public enum OpsReportAiStatus {
    /** 未生成（AI 未配置 / 未开启「AI 写作」） */
    NONE,
    /** 已生成 */
    SUCCESS,
    /** 调用模型失败 */
    FAILED
}
