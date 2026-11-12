package com.lemon.erp.modules.ecom.fba.model;

/**
 * 尺寸、实收数量等数据的来源
 */
public enum FbaDataSource {
    /** 手工维护 */
    MANUAL,
    /** 亚马逊报告 */
    REPORT,
    /** 亚马逊接口 */
    API
}
