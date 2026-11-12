package com.lemon.erp.modules.ecom.fba.model;

/**
 * FBA 仓储费类型（字典 ecom_fba_storage_fee_type）
 */
public enum FbaStorageFeeType {
    /** 月度仓储费（按月份淡旺季） */
    MONTHLY,
    /** 超龄库存附加费（按库龄段，即原长期仓储费） */
    AGED
}
