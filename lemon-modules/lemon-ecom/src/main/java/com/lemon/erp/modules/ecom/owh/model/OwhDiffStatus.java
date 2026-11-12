package com.lemon.erp.modules.ecom.owh.model;

/**
 * 收货差异的处理状态（字典 ecom_owh_diff_status）：短少的确认损耗或找回补收，溢收的确认溢收
 */
public enum OwhDiffStatus {
    /** 待处理 */
    PENDING,
    /** 确认损耗：差异出库保留（调拨为调出方损耗，入库关联头程货件） */
    LOSS,
    /** 找回补收：补收数量其他入库到收货仓 */
    FOUND,
    /** 确认溢收：溢收数量已入库 */
    ACCEPTED
}
