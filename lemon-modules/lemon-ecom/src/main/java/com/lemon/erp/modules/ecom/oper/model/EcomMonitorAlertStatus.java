package com.lemon.erp.modules.ecom.oper.model;

/**
 * 监控预警的处理状态（字典 ecom_monitor_alert_status）
 */
public enum EcomMonitorAlertStatus {
    /** 未处理 */
    NEW,
    /** 已处理 */
    HANDLED,
    /** 已忽略 */
    IGNORED
}
