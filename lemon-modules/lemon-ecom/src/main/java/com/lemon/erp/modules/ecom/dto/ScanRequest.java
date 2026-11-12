package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 扫描发货：运单号或包裹号
 */
public record ScanRequest(@NotBlank(message = "{ecom.valid.ops.scanCodeRequired}") @Size(max = 64, message = "{ecom.valid.trackingNoCannotExceedCharacters}") String code) {
}
