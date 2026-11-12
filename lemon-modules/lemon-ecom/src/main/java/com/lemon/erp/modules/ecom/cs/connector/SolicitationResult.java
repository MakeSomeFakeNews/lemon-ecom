package com.lemon.erp.modules.ecom.cs.connector;

import com.lemon.erp.modules.ecom.cs.model.CsRequestStatus;

/**
 * 请求评价的结果：已发送，或平台判定不符合条件（附说明）
 */
public record SolicitationResult(CsRequestStatus status, String message) {

    public static SolicitationResult sent() {
        return new SolicitationResult(CsRequestStatus.SENT, null);
    }

    public static SolicitationResult notEligible(String message) {
        return new SolicitationResult(CsRequestStatus.NOT_ELIGIBLE, message);
    }
}
