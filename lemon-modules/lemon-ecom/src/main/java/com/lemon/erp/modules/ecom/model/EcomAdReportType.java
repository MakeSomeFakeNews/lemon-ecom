package com.lemon.erp.modules.ecom.model;

/**
 * 广告报告类型：文件导入与广告接口拉取共用
 */
public enum EcomAdReportType {
    /** 广告活动报告（活动 × 日期） */
    CAMPAIGN,
    /** 推广商品报告（活动 × 广告组 × SKU × 日期），写入广告日报，利润分析按它扣除广告花费 */
    PRODUCT,
    /** 投放报告（关键词 / 商品投放 × 日期） */
    TARGETING,
    /** 搜索词报告（投放 × 客户搜索词 × 日期） */
    SEARCH_TERM
}
