package com.lemon.erp.modules.ecom.model;

/**
 * 包裹状态（字典 ecom_package_status）：待打单 →（申请运单号 / 手工填写运单号）→ 待发货 →（扫描发货）→ 已发货；未发货的可取消
 */
public enum EcomPackageStatus {
    /** 待打单：还没有运单号 */
    WAIT_LABEL,
    /** 待发货：已有运单号（面单） */
    LABELED,
    /** 已发货：已出库并回传平台 */
    SHIPPED,
    /** 已取消 */
    CANCELLED
}
