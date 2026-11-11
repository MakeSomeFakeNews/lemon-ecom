package com.lemon.erp.modules.ecom.model;

/**
 * 头程货件状态（字典 ecom_inbound_status）：草稿 → （审批中）→ 已发运（生成在途调拨）→ 已签收（调拨收货，平台仓库存增加）
 */
public enum InboundStatus {
    /** 草稿 */
    DRAFT,
    /** 审批中 */
    APPROVING,
    /** 已发运（在途） */
    SHIPPED,
    /** 已签收 */
    RECEIVED,
    /** 已取消 */
    CANCELLED
}
