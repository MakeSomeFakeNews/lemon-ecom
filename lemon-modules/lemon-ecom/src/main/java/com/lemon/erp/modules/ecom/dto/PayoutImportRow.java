package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 银行 / 收款平台（PingPong、连连、Payoneer、万里汇等）到账流水的一行：通用列 日期、金额、币种、备注、流水号，可选汇率、店铺编码
 */
@Data
public class PayoutImportRow {

    @NotNull(message = "{ecom.valid.payout.dateRequired}")
    private LocalDate payoutDate;

    @NotNull(message = "{ecom.valid.enterAmount}")
    @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.payout.amountPositive}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}")
    private BigDecimal amount;

    @NotBlank(message = "{ecom.valid.payout.currencyRequired}")
    @Size(max = 8, message = "{ecom.valid.payout.currencyRequired}")
    private String currency;

    @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}")
    private String remark;

    @Size(max = 64, message = "{ecom.valid.payout.bankRefTooLong}")
    private String bankRef;

    /** 到账汇率（1 结算币种 = 本位币），为空按到账日期所在月的汇率 */
    @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.payout.ratePositive}")
    @Digits(integer = 12, fraction = 6, message = "{ecom.valid.payout.ratePositive}")
    private BigDecimal exchangeRate;

    /** 店铺编码（可空：为空时按导入时选择的店铺或全部店铺匹配） */
    @Size(max = 32, message = "{ecom.valid.storeCodeCannotExceedCharacters}")
    private String storeCode;
}
