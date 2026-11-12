package com.lemon.erp.modules.ecom.model;

/**
 * 补货设置的级别：在线商品（店铺 × 平台 SKU）或物料。同一项在线商品级优先，没有设置的项取物料级，再取店铺 / 供应商的默认值
 */
public enum ReplenishScope {
    /** 在线商品 */
    LISTING,
    /** 物料 */
    MATERIAL
}
