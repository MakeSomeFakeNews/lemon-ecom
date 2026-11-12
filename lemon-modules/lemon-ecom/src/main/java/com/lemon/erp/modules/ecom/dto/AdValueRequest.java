package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 修改竞价 / 日预算（店铺币种）
 */
public record AdValueRequest(
        @NotNull(message = "{ecom.valid.ads.valueRequired}")
        @DecimalMin(value = "0.01", message = "{ecom.valid.ads.valueRequired}")
        @Digits(integer = 16, fraction = 2, message = "{ecom.valid.ads.amountDecimals}") BigDecimal value) {
}
