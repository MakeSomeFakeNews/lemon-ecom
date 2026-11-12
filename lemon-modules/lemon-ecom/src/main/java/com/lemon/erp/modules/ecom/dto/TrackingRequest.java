package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 手工填写包裹的运单号（渠道没有对接物流商时，在物流商后台下单后填写）
 *
 * @param shippingCost 运费（本位币），为空时按渠道费率和重量试算
 */
public record TrackingRequest(
        @NotBlank(message = "{ecom.valid.carrierRequired}") @Size(max = 64, message = "{ecom.valid.carrierCannotExceedCharacters}") String carrier,
        @NotBlank(message = "{ecom.valid.trackingNoRequired}") @Size(max = 64, message = "{ecom.valid.trackingNoCannotExceedCharacters}") String trackingNo,
        Long channelId,
        @DecimalMin(value = "0", message = "{ecom.valid.weightCannotNegative}")
        @Digits(integer = 12, fraction = 3, message = "{ecom.valid.weightDecimals}") BigDecimal weight,
        @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
        @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}") BigDecimal shippingCost) {
}
