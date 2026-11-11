package com.lemon.erp.modules.ecom.model;

/**
 * 电商平台（字典 ecom_platform）：店铺所属的平台，决定订单同步使用的渠道接口
 */
public enum EcomPlatform {
    /** 亚马逊 */
    AMAZON,
    /** 速卖通 */
    ALIEXPRESS,
    /** eBay */
    EBAY,
    /** Shopee */
    SHOPEE,
    /** Lazada */
    LAZADA,
    /** Shopify 独立站 */
    SHOPIFY,
    /** TikTok Shop */
    TIKTOK,
    /** Temu */
    TEMU,
    /** 沃尔玛 */
    WALMART,
    /** Ozon */
    OZON,
    /** 其他 */
    OTHER
}
