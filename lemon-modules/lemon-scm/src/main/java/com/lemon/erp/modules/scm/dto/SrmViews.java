package com.lemon.erp.modules.scm.dto;

import java.math.BigDecimal;

/**
 * 采购合同的查询结果
 */
public final class SrmViews {

    private SrmViews() {
    }

    /**
     * 合同物料的执行情况：非草稿的引用订单（关闭的订单按已入库数量）
     *
     * @param receivedQty 已入库（扣除退货）
     */
    public record ContractUsage(Long materialId, BigDecimal orderedQty, BigDecimal orderedAmount, BigDecimal receivedQty) {
    }
}
