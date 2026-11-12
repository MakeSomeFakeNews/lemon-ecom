package com.lemon.erp.modules.ecom.automation.action;

import com.lemon.erp.modules.ecom.automation.model.AutoActionStatus;

/**
 * 动作的执行结果（失败用异常表示，不用本类）
 *
 * @param message 说明（存库，按默认语言），如「已挂起订单」「已生成采购计划 PP20261112001」
 */
public record AutoOutcome(AutoActionStatus status, String message) {

    public static AutoOutcome success(String message) {
        return new AutoOutcome(AutoActionStatus.SUCCESS, message);
    }

    public static AutoOutcome skipped(String message) {
        return new AutoOutcome(AutoActionStatus.SKIPPED, message);
    }

    public static AutoOutcome suggested(String message) {
        return new AutoOutcome(AutoActionStatus.SUGGESTED, message);
    }
}
