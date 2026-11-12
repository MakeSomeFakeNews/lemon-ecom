package com.lemon.erp.modules.ecom.cs.model;

/**
 * 评价 / 店铺反馈的处理状态（字典 ecom_cs_review_status）：低分（不超过 3 星）的新评价自动为待处理
 */
public enum CsReviewStatus {
    /** 无需处理 */
    NONE,
    /** 待处理 */
    PENDING,
    /** 处理中 */
    PROCESSING,
    /** 已处理 */
    RESOLVED
}
