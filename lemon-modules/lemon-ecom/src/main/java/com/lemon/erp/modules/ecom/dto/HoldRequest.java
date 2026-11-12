package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 挂起订单（人工拦截）：原因必填
 */
public record HoldRequest(
        @NotEmpty(message = "{ecom.valid.ops.selectRecords}") @Size(max = 500, message = "{ecom.valid.ops.tooManyRecords}") List<Long> ids,
        @NotBlank(message = "{ecom.valid.ops.holdReasonRequired}") @Size(max = 255, message = "{ecom.valid.ops.holdReasonTooLong}") String reason) {
}
