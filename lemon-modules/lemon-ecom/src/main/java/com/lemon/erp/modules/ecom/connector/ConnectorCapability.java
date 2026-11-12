package com.lemon.erp.modules.ecom.connector;

/**
 * 渠道接口支持的能力：店铺页面据此显示可用的操作，同步任务据此决定调用哪些接口
 */
public enum ConnectorCapability {
    /** 拉取订单 */
    ORDERS,
    /** 回传发货（运单号） */
    SHIP_CONFIRM,
    /** 回传可售库存（自发货） */
    INVENTORY_PUSH,
    /** 拉取在线商品 */
    LISTINGS,
    /** 拉取平台仓库存（FBA / 平台海外仓） */
    PLATFORM_STOCK
}
