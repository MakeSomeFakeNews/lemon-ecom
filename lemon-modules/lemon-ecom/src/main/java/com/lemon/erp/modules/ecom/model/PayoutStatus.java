package com.lemon.erp.modules.ecom.model;

/**
 * 平台回款状态（字典 ecom_payout_status）
 */
public enum PayoutStatus {
    /** 未到账：结算单应到账，还没有匹配到银行 / 收款平台的到账记录 */
    PENDING,
    /** 已到账：到账金额与应到账一致 */
    RECEIVED,
    /** 有差异：到账金额与应到账不一致，或导入的流水没有匹配到结算单 */
    DIFF
}
