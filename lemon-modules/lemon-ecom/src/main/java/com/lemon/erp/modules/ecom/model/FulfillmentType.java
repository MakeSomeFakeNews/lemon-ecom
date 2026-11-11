package com.lemon.erp.modules.ecom.model;

/**
 * 配送方式（字典 ecom_fulfillment）：平台仓配送（如亚马逊 FBA）从店铺的平台仓出库，自发货从店铺的默认发货仓出库
 */
public enum FulfillmentType {
    /** 平台仓配送（FBA / 平台海外仓） */
    PLATFORM,
    /** 自发货（FBM / 商家自行发货） */
    SELLER
}
