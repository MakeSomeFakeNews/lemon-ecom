package com.lemon.erp.modules.ecom.model;

/**
 * 渠道同步类型（字典 ecom_sync_type）
 */
public enum SyncType {
    /** 拉取订单 */
    ORDERS,
    /** 回传发货 */
    SHIP_CONFIRM,
    /** 回传库存 */
    INVENTORY_PUSH,
    /** 拉取在线商品 */
    LISTINGS,
    /** 平台仓库存 */
    PLATFORM_STOCK,
    /** 授权 */
    AUTH,
    /** 买家消息（客服） */
    MESSAGES,
    /** 评价（客服） */
    REVIEWS,
    /** 请求评价（客服，亚马逊 Solicitations） */
    REVIEW_REQUEST
}
