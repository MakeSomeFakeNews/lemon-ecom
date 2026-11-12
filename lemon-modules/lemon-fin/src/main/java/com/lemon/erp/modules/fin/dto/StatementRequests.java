package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.lemon.erp.modules.fin.model.StatementStatus;

import java.time.LocalDate;

/**
 * 供应商对账单的请求
 */
public final class StatementRequests {

    private StatementRequests() {
    }

    /** 生成对账单：期间内还没有对过账的应付单 */
    public record Generate(
            @NotNull(message = "{fin.valid.selectSupplier}") Long supplierId,
            @NotNull(message = "{fin.valid.selectStatementPeriod}") LocalDate beginDate,
            @NotNull(message = "{fin.valid.selectStatementPeriod}") LocalDate endDate,
            @Size(max = 255, message = "{fin.valid.remarkCannotExceedCharacters}") String remark) {
    }

    /**
     * 登记供应商的对账结果（对账单发送 / 导出给供应商后，由内部人员按供应商的回复登记）
     *
     * @param status         CONFIRMED 确认 / DISPUTED 有异议
     * @param supplierRemark 供应商意见（有异议时必填）
     * @param confirmDate    供应商确认（回复）日期，为空取今天
     * @param invoiceNo      供应商开具的发票号（确认时可填）
     */
    public record Result(
            @NotNull(message = "{fin.valid.chooseConfirmDispute}") StatementStatus status,
            @Size(max = 500, message = "{fin.valid.remarkCannotExceedCharacters}") String supplierRemark,
            LocalDate confirmDate,
            @Size(max = 64, message = "{fin.valid.remarkCannotExceedCharacters}") String invoiceNo,
            java.math.BigDecimal invoiceAmount,
            LocalDate invoiceDate) {
    }
}
