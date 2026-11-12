package com.lemon.erp.modules.ecom.model;

/**
 * 采购计划状态（字典 ecom_purchase_plan_status）：草稿 → 已审核 → 已下单（全部明细都已生成采购订单）；已审核、已下单的计划可以关闭
 */
public enum PurchasePlanStatus {
    /** 草稿：可修改数量、供应商、到货日期 */
    DRAFT,
    /** 已审核：可生成采购订单 */
    APPROVED,
    /** 已下单 */
    ORDERED,
    /** 已关闭 */
    CLOSED
}
