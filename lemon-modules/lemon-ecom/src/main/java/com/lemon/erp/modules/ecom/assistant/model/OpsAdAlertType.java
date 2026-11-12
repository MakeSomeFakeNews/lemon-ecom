package com.lemon.erp.modules.ecom.assistant.model;

/** 运营日报识别的广告异常（字典 ecom_ops_ad_alert_type） */
public enum OpsAdAlertType {
    /** 当天花费达到或超过日预算 */
    OVER_BUDGET,
    /** ACOS 明显高于近 7 天 */
    ACOS_HIGH,
    /** 花费较多但没有订单 */
    NO_ORDER
}
