package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单发货：填写承运商与运单号，可选择物流渠道并按重量试算运费（或直接填写运费，本位币）
 *
 * @param shipTime 发货时间，为空取当前时间
 */
public record ShipRequest(
        @NotBlank(message = "{ecom.valid.carrierRequired}") @Size(max = 64, message = "{ecom.valid.carrierCannotExceedCharacters}") String carrier,
        @NotBlank(message = "{ecom.valid.trackingNoRequired}") @Size(max = 64, message = "{ecom.valid.trackingNoCannotExceedCharacters}") String trackingNo,
        LocalDateTime shipTime,
        Long channelId,
        @DecimalMin(value = "0", message = "{ecom.valid.weightCannotNegative}")
        @Digits(integer = 12, fraction = 3, message = "{ecom.valid.weightDecimals}") BigDecimal weight,
        @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
        @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}") BigDecimal shippingCost) {
}
