package com.lemon.erp.modules.ecom.automation.model;

/**
 * 自动化触发器的分类（字典 ecom_auto_category），规则编辑页按分类分组显示触发器
 */
public enum AutoCategory {
    /** 订单 */
    ORDER,
    /** 库存与补货 */
    STOCK,
    /** 客服与评价 */
    CS,
    /** 商品监控 */
    LISTING,
    /** 店铺与渠道 */
    STORE,
    /** 头程物流 */
    LOGISTICS,
    /** 广告 */
    ADS,
    /** FBA */
    FBA,
    /** 价格 */
    PRICE
}
