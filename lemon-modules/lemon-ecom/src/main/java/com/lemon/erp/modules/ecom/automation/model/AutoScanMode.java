package com.lemon.erp.modules.ecom.automation.model;

/**
 * 触发器的产生方式：实时（业务事件提交后立即触发）、频繁扫描（每 15 分钟）、每日扫描（每天一次）
 */
public enum AutoScanMode {
    REALTIME,
    FREQUENT,
    DAILY
}
