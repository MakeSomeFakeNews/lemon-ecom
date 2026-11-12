package com.lemon.erp.modules.ecom.model;

/**
 * 发货计划状态（字典 ecom_shipment_plan_status）：草稿 → 已确认 → 已生成货件（计划数量全部生成头程货件）；草稿、已确认可取消
 */
public enum ShipmentPlanStatus {
    /** 草稿 */
    DRAFT,
    /** 已确认 */
    CONFIRMED,
    /** 已生成货件 */
    GENERATED,
    /** 已取消 */
    CANCELLED
}
