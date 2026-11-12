package com.lemon.erp.modules.ecom.fba.model;

/**
 * 库存健康的处理建议（字典 ecom_fba_advice）
 */
public enum FbaHealthAdvice {
    /** 健康 */
    HEALTHY,
    /** 冗余：站外推广 / 加大广告 */
    PROMOTE,
    /** 冗余且已超龄：降价清仓 */
    PRICE_DOWN,
    /** 长期滞销或超过一年：移除 / 弃置 */
    REMOVE
}
