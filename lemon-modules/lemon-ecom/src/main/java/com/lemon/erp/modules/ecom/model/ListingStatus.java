package com.lemon.erp.modules.ecom.model;

/**
 * 在线商品状态（字典 ecom_listing_status）
 */
public enum ListingStatus {
    /** 在售 */
    ACTIVE,
    /** 下架 */
    INACTIVE,
    /** 信息不完整 */
    INCOMPLETE,
    /** 已删除 */
    DELETED
}
