package com.lemon.erp.modules.ecom.connector;

/**
 * 店铺授权方式
 */
public enum AuthType {
    /** 卖家在平台授权页面同意后回调换取令牌（虾皮、TikTok Shop、Lazada、eBay 等） */
    OAUTH,
    /** 直接填写平台后台生成的密钥 / 令牌（Temu、沃尔玛、Ozon、Shopify 自定义应用等） */
    KEYS
}
