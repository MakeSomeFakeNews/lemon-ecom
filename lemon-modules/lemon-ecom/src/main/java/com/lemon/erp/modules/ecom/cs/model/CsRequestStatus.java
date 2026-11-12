package com.lemon.erp.modules.ecom.cs.model;

/**
 * 请求评价（亚马逊 Solicitations）的结果
 */
public enum CsRequestStatus {
    /** 已发送 */
    SENT,
    /** 平台判定不符合条件（不在可请求的时间窗口、已请求过等） */
    NOT_ELIGIBLE,
    /** 调用失败 */
    FAILED
}
