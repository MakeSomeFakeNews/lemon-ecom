package com.lemon.erp.modules.ecom.model;

/**
 * 广告日报（ecom_ad_daily）一行的粒度：利润分析按 ecom_ad_daily 汇总花费，同一店铺、日期、活动的花费只记一次
 */
public enum EcomAdDailyLevel {
    /** 活动 × 推广商品（平台 SKU）：广告报告导入、推广商品报告、旧版导入 */
    PRODUCT,
    /** 只有活动级数据（活动报告）：同一活动、日期导入推广商品数据后删除，避免重复计算花费 */
    CAMPAIGN
}
