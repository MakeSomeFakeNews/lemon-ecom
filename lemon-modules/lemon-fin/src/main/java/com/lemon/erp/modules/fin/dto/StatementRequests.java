package com.lemon.erp.modules.fin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
}
