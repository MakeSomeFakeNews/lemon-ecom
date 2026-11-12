package com.lemon.erp.modules.ecom.fba.model;

/**
 * 移除订单状态（字典 ecom_fba_removal_status）
 */
public enum FbaRemovalStatus {
    /** 草稿（计划） */
    DRAFT,
    /** 已提交（已在亚马逊创建，填写了移除订单号） */
    SUBMITTED,
    /** 处理中（亚马逊部分发出 / 弃置） */
    PROCESSING,
    /** 亚马逊已完成 */
    COMPLETED,
    /** 本地已收货（退回类型全部收完） */
    RECEIVED,
    /** 已取消 */
    CANCELLED
}
