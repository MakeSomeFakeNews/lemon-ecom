package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 手工登记到账：到账日期、金额与币种（结算币种或本位币）、到账账户、收款平台、流水号、到账汇率（为空按到账日期所在月）
 */
public record PayoutReceiveRequest(
        @NotNull(message = "{ecom.valid.payout.dateRequired}") LocalDate payoutDate,
        @NotNull(message = "{ecom.valid.enterAmount}")
        @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.payout.amountPositive}")
        @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}") BigDecimal amount,
        @Size(max = 8) String payCurrency,
        @NotNull(message = "{ecom.valid.payout.accountRequired}") Long accountId,
        @Size(max = 16) String channel,
        @Size(max = 64, message = "{ecom.valid.payout.bankRefTooLong}") String bankRef,
        @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.payout.ratePositive}")
        @Digits(integer = 12, fraction = 6, message = "{ecom.valid.payout.ratePositive}") BigDecimal exchangeRate,
        @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}") String remark) {
}
