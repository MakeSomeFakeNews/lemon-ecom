package com.lemon.erp.modules.ecom.publish.model;

/**
 * 竞品价格快照的来源（字典 ecom_competitor_source）
 */
public enum CompetitorSource {
    /** 平台接口（亚马逊 Product Pricing API） */
    API,
    /** 手工录入 */
    MANUAL,
    /** Excel 导入 */
    IMPORT
}
