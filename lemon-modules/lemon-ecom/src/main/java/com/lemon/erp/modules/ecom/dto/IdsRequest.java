package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量操作的订单 / 包裹 ID
 */
public record IdsRequest(@NotEmpty(message = "{ecom.valid.ops.selectRecords}") @Size(max = 500, message = "{ecom.valid.ops.tooManyRecords}") List<Long> ids) {
}
