package com.lemon.erp.modules.ecom.dev.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 保存试算方案：名称 + 试算参数（服务端重新试算后保存结果摘要）
 */
public record ProfitSchemeRequest(
        @NotBlank(message = "{ecom.valid.dev.schemeNameRequired}") @Size(max = 64, message = "{ecom.valid.dev.schemeNameTooLong}") String name,
        @NotNull(message = "{ecom.valid.dev.paramsRequired}") @Valid ProfitCalcRequest params) {
}
