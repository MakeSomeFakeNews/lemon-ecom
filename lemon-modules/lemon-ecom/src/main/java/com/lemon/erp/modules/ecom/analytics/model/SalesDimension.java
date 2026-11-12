package com.lemon.erp.modules.ecom.analytics.model;

/**
 * 销量统计的行维度
 */
public enum SalesDimension {
    /** 店铺 × 平台 SKU */
    SKU,
    /** 店铺 × 父商品（父 ASIN，没有父商品的按 SKU 自身） */
    SPU,
    /** 本地物料（组合商品按组件展开，未匹配物料的订单行不计入） */
    MATERIAL,
    /** 店铺 */
    STORE,
    /** 平台 */
    PLATFORM,
    /** 收件国家 / 地区 */
    COUNTRY
}
