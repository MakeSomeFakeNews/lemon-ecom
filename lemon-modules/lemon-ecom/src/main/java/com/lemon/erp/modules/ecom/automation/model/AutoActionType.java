package com.lemon.erp.modules.ecom.automation.model;

/**
 * 自动化动作（字典 ecom_auto_action）。required 为动作需要事件关联的对象，为空表示任何触发器都可以使用。
 * 新增动作：加一个枚举值并实现 {@code automation.action.AutoActionHandler}，前端动作编辑器、字典、语言包同步增加
 */
public enum AutoActionType {
    /** 站内消息（已接入钉钉 / 企业微信 / 飞书应用消息的同步推送） */
    NOTIFY(null),
    /** 钉钉 / 企业微信 / 飞书群机器人 */
    IM_ROBOT(null),
    /** Webhook（HTTP POST JSON，带签名） */
    WEBHOOK(null),
    /** 创建待办提醒 */
    TODO(null),
    /** 订单挂起 */
    ORDER_HOLD(AutoRef.ORDER),
    /** 订单打标签 */
    ORDER_TAG(AutoRef.ORDER),
    /** 指定发货仓 / 物流渠道 */
    ORDER_ASSIGN(AutoRef.ORDER),
    /** 在线商品调价 */
    LISTING_REPRICE(AutoRef.LISTING),
    /** 生成采购计划草稿 */
    PURCHASE_PLAN(AutoRef.MATERIAL),
    /** 建售后工单（评价、买家消息） */
    CS_TICKET(null),
    /** 暂停广告活动 */
    AD_PAUSE(AutoRef.CAMPAIGN),
    /** 调整广告竞价 / 预算 */
    AD_ADJUST(AutoRef.CAMPAIGN);

    private final AutoRef required;

    AutoActionType(AutoRef required) {
        this.required = required;
    }

    public AutoRef required() {
        return required;
    }

    /** 能否用于该触发器 */
    public boolean supports(AutoTrigger trigger) {
        if (this == CS_TICKET) {
            return trigger.refs().contains(AutoRef.REVIEW) || trigger.refs().contains(AutoRef.THREAD);
        }
        return required == null || trigger.refs().contains(required);
    }

    public String label() {
        return "ecom.enum.auto.action." + name();
    }
}
