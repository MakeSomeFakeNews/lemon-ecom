package com.lemon.erp.modules.ecom.owh.model;

/**
 * 海外仓费用的入账状态（字典 ecom_owh_fee_status）
 */
public enum OwhFeeStatus {
    /** 待入账：关联的包裹还没有发货（发货时计入运费），或还没有处理 */
    PENDING,
    /** 已入账：已计入订单物流成本或分摊到店铺 SKU */
    POSTED,
    /** 无法归属：没有关联单据、SKU 也没有对应的在线商品 */
    UNALLOCATED
}
