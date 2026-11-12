package com.lemon.erp.modules.ecom.publish.connector;

/**
 * 刊登连接器的能力：页面据此显示类目 / 属性是从平台选择还是手工填写，能否发布、下架、改价、拉取竞品价格
 */
public enum PublishCapability {
    /** 查询平台类目（亚马逊 Product Type、虾皮 get_category、TikTok categories、eBay taxonomy、Lazada category tree） */
    CATEGORIES,
    /** 查询类目的属性（必填项、可选值） */
    ATTRIBUTES,
    /** 发布商品 */
    PUBLISH,
    /** 下架 */
    OFFLINE,
    /** 推送价格 */
    PRICE,
    /** 拉取竞品价格（购物车价、最低价） */
    COMPETITIVE_PRICE
}
