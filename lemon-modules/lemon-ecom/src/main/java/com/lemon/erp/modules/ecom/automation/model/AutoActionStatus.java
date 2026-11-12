package com.lemon.erp.modules.ecom.automation.model;

/**
 * 单个动作的执行结果（字典 ecom_auto_action_status）
 */
public enum AutoActionStatus {
    /** 成功 */
    SUCCESS,
    /** 失败 */
    FAILED,
    /** 跳过（如对象状态不允许、没有可执行的内容） */
    SKIPPED,
    /** 仅建议（调价只记录建议价，不修改） */
    SUGGESTED
}
