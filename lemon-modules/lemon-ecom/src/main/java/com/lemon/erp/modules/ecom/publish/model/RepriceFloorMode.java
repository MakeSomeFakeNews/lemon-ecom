package com.lemon.erp.modules.ecom.publish.model;

/**
 * 自动调价的底价方式（字典 ecom_reprice_floor_mode）
 */
public enum RepriceFloorMode {
    /** 固定最低价 */
    FIXED,
    /** 按成本 + 最低利润率推算（价格模板的定价公式，目标利润率取最低利润率） */
    COST_MARGIN
}
