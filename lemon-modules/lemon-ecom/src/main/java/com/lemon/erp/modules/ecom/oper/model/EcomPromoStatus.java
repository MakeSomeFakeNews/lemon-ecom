package com.lemon.erp.modules.ecom.oper.model;

/**
 * 促销活动的状态（字典 ecom_promo_status），已生效的活动的进行阶段见 EcomPromoPhase
 */
public enum EcomPromoStatus {
    /** 草稿：可修改、删除 */
    DRAFT,
    /** 已生效：计入促销日历和效果分析，可推送平台 */
    ACTIVE,
    /** 已取消 */
    CANCELLED
}
