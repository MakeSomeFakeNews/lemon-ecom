package com.lemon.erp.modules.ecom.fba.model;

/**
 * 索赔状态（字典 ecom_fba_claim_status）
 */
public enum FbaClaimStatus {
    /** 待提交 */
    PENDING,
    /** 已提交（已开 Case） */
    SUBMITTED,
    /** 已赔付 */
    PAID,
    /** 已驳回 */
    REJECTED,
    /** 已忽略（误报或不再索赔） */
    IGNORED
}
