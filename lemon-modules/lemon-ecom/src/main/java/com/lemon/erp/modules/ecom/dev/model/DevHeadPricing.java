package com.lemon.erp.modules.ecom.dev.model;

/**
 * 利润试算的头程计费方式：按物流渠道费率（运费试算）、手填每公斤单价、手填每立方米单价（海运）
 */
public enum DevHeadPricing {
    /** 物流渠道费率（ecom_logistics_rate，按目的国与计费重） */
    CHANNEL,
    /** 每公斤单价 × 计费重 */
    PER_KG,
    /** 每立方米单价 × 体积 */
    PER_CBM
}
