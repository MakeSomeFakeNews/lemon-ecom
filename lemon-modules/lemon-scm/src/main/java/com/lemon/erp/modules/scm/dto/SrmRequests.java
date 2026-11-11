package com.lemon.erp.modules.scm.dto;

import com.lemon.erp.modules.scm.model.ContractType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购合同的请求
 */
public final class SrmRequests {

    private SrmRequests() {
    }

    /**
     * 采购合同
     *
     * @param amountLimit 金额上限（价税合计），为空不限
     * @param paymentDays 付款天数：引用合同的采购订单生成应付单时按此计算到期日（未填按供应商账期）
     */
    public record Contract(
            @NotBlank(message = "{scm.valid.enterContractName}") @Size(max = 128, message = "{scm.valid.contractNameCannotExceedCharacters}") String name,
            @NotNull(message = "{scm.valid.selectSupplier}") Long supplierId,
            @NotNull(message = "{scm.valid.selectContractType}") ContractType contractType,
            @NotNull(message = "{scm.valid.selectSigningDate}") LocalDate signDate,
            @NotNull(message = "{scm.valid.selectEffectiveDate}") LocalDate startDate,
            @NotNull(message = "{scm.valid.selectExpiryDate}") LocalDate endDate,
            @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.amountLimitMustGreater}") @Digits(integer = 16, fraction = 2) BigDecimal amountLimit,
            @Min(value = 0, message = "{scm.valid.paymentDaysCannotLess}") @Max(value = 3650, message = "{scm.valid.paymentDaysCannotExceed}") Integer paymentDays,
            @Size(max = 255, message = "{scm.valid.paymentTermsCannotExceedCharacters}") String paymentTerms,
            @Size(max = 500, message = "{scm.valid.remarkCannotExceedCharacters2}") String remark,
            @NotEmpty(message = "{scm.valid.addContractMaterials}") @Size(max = 500, message = "{scm.valid.lineItemsCannotExceed}") List<@Valid @NotNull ContractItem> items) {
    }

    public record ContractItem(
            @NotNull(message = "{scm.valid.selectMaterial}") Long materialId,
            @NotNull(message = "{scm.valid.enterContractUnitPrice}") @DecimalMin(value = "0", message = "{scm.valid.unitPriceCannotLess}") @Digits(integer = 14, fraction = 4) BigDecimal price,
            @DecimalMin(value = "0", message = "{scm.valid.taxRateCannotNegative}") @DecimalMax(value = "100", message = "{scm.valid.taxRateCannotExceed2}") BigDecimal taxRate,
            @DecimalMin(value = "0", inclusive = false, message = "{scm.valid.quantityLimitMustGreater}") @Digits(integer = 14, fraction = 4) BigDecimal qtyLimit,
            @Size(max = 255) String remark) {
    }

    public record Close(@NotBlank(message = "{scm.valid.enterCloseReason}") @Size(max = 255, message = "{scm.valid.closeReasonCannotExceedCharacters}") String reason) {
    }
}
