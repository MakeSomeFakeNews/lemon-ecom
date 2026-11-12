package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 修改包裹：物流渠道（只能在打单前修改）、重量、运费（本位币）、备注
 */
public record PackageUpdateRequest(
        Long channelId,
        @DecimalMin(value = "0", message = "{ecom.valid.weightCannotNegative}")
        @Digits(integer = 12, fraction = 3, message = "{ecom.valid.weightDecimals}") BigDecimal weight,
        @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
        @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}") BigDecimal shippingCost,
        @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}") String remark) {
}
