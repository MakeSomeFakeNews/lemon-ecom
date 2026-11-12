package com.lemon.erp.modules.ecom.owh.model;

/**
 * 收货差异的来源（字典 ecom_owh_diff_source）
 */
public enum OwhDiffSource {
    /** 海外仓入库单（头程货件、在途调拨、海外仓调拨的调入） */
    INBOUND,
    /** 海外仓退件单 */
    RETURN,
    /** 海外仓调拨单（调入本地仓的确认收货） */
    TRANSFER
}
