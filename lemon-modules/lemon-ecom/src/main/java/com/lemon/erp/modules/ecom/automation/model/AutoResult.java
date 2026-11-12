package com.lemon.erp.modules.ecom.automation.model;

/**
 * 一次规则执行的结果（字典 ecom_auto_result）
 */
public enum AutoResult {
    /** 动作全部成功 */
    SUCCESS,
    /** 部分动作失败 */
    PARTIAL,
    /** 动作全部失败 */
    FAILED,
    /** 条件不满足（不执行） */
    NOT_MATCHED,
    /** 防抖跳过：同一对象在防抖时间内已执行过 */
    DEBOUNCED,
    /** 达到每日执行上限 */
    LIMITED;

    /** 是否执行了动作（计入触发次数、防抖、每日上限） */
    public boolean executed() {
        return this == SUCCESS || this == PARTIAL || this == FAILED;
    }
}
