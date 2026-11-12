package com.lemon.erp.modules.ecom.fba.model;

/**
 * 索赔类型（字典 ecom_fba_claim_type）
 */
public enum FbaClaimType {
    /** 仓库丢失（库存流水调整原因 M / 5，扣除找到 F） */
    LOST,
    /** 仓库损坏（调整原因 E / 6 / 7） */
    DAMAGED,
    /** 入库短少（货件发货数大于亚马逊实收数） */
    INBOUND_SHORTAGE,
    /** 退款后超过 45 天未退回库存 */
    RETURN_NOT_RECEIVED,
    /** 多收配送费（费用核查） */
    FEE_OVERCHARGE,
    /** 其他（手工登记） */
    OTHER
}
