package com.lemon.erp.modules.scm.model;

/**
 * 订单来源（字典 scm_order_source）
 */
public enum OrderSource {
    /** 手工 */
    MANUAL,
    /** 寄售结算 */
    CONSIGN,
    /** 外部导入 */
    IMPORT,
    /** 直接开单：不选订单直接开销售出库 / 采购入库（或退货）时系统生成并直接审核的隐式订单，不能修改、变更 */
    DIRECT,
    /** 电商采购计划：由补货建议生成的采购计划按供应商拆单生成（草稿） */
    PURCHASE_PLAN
}
