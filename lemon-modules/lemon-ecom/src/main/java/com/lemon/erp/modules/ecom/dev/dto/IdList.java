package com.lemon.erp.modules.ecom.dev.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * ID 列表（批量操作）
 */
public record IdList(@NotEmpty(message = "{ecom.valid.dev.idsRequired}") @Size(max = 500, message = "{ecom.valid.dev.tooManyIds}") List<Long> ids) {
}
