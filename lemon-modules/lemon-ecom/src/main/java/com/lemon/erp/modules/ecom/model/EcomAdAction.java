package com.lemon.erp.modules.ecom.model;

/**
 * 广告规则的动作（字典 ecom_ad_rule_action）：竞价类用于广告组（默认竞价）和投放，预算类只用于活动
 */
public enum EcomAdAction {
    /** 竞价按百分比调整（正数提高、负数降低） */
    BID_PERCENT,
    /** 竞价调整到固定值 */
    BID_SET,
    /** 日预算按百分比调整 */
    BUDGET_PERCENT,
    /** 日预算调整到固定值 */
    BUDGET_SET,
    /** 暂停 */
    PAUSE,
    /** 启用 */
    ENABLE,
    /** 仅提醒 */
    NOTIFY;

    public boolean isBid() {
        return this == BID_PERCENT || this == BID_SET;
    }

    public boolean isBudget() {
        return this == BUDGET_PERCENT || this == BUDGET_SET;
    }
}
