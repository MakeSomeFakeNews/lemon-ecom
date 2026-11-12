package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRow;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 导入到账流水：行数据 + 到账账户、收款平台、可选店铺；自动匹配的日期窗口（天，默认 7）和金额容差（%，默认 2）
 */
public record PayoutImportRequest(
        @NotEmpty(message = "{framework.valid.noDataImport}")
        @Size(max = ImportExecutor.MAX_ROWS, message = "{framework.valid.upMaxRowsImportedTime}") List<ImportRow<PayoutImportRow>> rows,
        @NotNull(message = "{ecom.valid.payout.accountRequired}") Long accountId,
        @Size(max = 16) String channel,
        Long storeId,
        @Min(value = 0, message = "{ecom.valid.payout.windowRange}") @Max(value = 90, message = "{ecom.valid.payout.windowRange}") Integer windowDays,
        @DecimalMin(value = "0", message = "{ecom.valid.payout.toleranceRange}") @DecimalMax(value = "20", message = "{ecom.valid.payout.toleranceRange}")
        BigDecimal tolerance) {

    public int window() {
        return windowDays == null ? 7 : windowDays;
    }

    public BigDecimal tolerancePct() {
        return tolerance == null ? BigDecimal.valueOf(2) : tolerance;
    }
}
