package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量申请运单号：包裹没有物流渠道 / 重量时用这里填写的
 */
public record LabelRequest(
        @NotEmpty(message = "{ecom.valid.ops.selectRecords}") @Size(max = 200, message = "{ecom.valid.ops.tooManyRecords}") List<Long> ids,
        Long channelId,
        @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.ops.weightPositive}")
        @Digits(integer = 12, fraction = 3, message = "{ecom.valid.weightDecimals}") BigDecimal weight) {
}
