package com.lemon.erp.modules.ecom.model;

/**
 * 广告规则的指标：比率类在统计窗口内按合计计算（ACOS = 花费 / 销售额 × 100，ROAS = 销售额 / 花费，CTR = 点击 / 曝光 × 100，
 * CPC = 花费 / 点击，CVR = 订单 / 点击 × 100）
 */
public enum EcomAdMetric {
    /** 花费 */
    SPEND,
    /** 广告销售额 */
    SALES,
    /** 点击 */
    CLICKS,
    /** 曝光 */
    IMPRESSIONS,
    /** 广告订单 */
    ORDERS,
    /** ACOS（%） */
    ACOS,
    /** ROAS */
    ROAS,
    /** 点击率（%） */
    CTR,
    /** 单次点击成本 */
    CPC,
    /** 转化率（%） */
    CVR
}
