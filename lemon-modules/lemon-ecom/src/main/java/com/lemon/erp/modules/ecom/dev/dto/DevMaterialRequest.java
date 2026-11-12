package com.lemon.erp.modules.ecom.dev.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 生成物料的选项：编码留空按编码规则生成，单位留空为「个」，规格可选
 */
public record DevMaterialRequest(@Pattern(regexp = "^([A-Za-z0-9_.-]{1,64})?$", message = "{ecom.valid.dev.materialCodeInvalid}") String code,
                                 @Size(max = 16, message = "{ecom.valid.dev.unitTooLong}") String unit,
                                 @Size(max = 128, message = "{ecom.valid.dev.specTooLong}") String spec) {
}
