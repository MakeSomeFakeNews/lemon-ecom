package com.lemon.erp.modules.ecom.publish.model;

/**
 * 改价历史的来源（字典 ecom_price_change_source）
 */
public enum PriceChangeSource {
    /** 手工修改单个 */
    MANUAL,
    /** 批量修改 */
    BATCH,
    /** 自动调价 */
    REPRICER,
    /** 刊登 */
    PUBLISH,
    /** 自动化规则 */
    AUTOMATION
}
