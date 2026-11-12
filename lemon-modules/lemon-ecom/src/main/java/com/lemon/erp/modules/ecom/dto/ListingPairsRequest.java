package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 批量配对（推荐一键确认、按款式批量映射）
 */
public record ListingPairsRequest(
        @NotEmpty(message = "{ecom.valid.ops.selectRecords}") @Size(max = 500, message = "{ecom.valid.ops.tooManyRecords}")
        List<@Valid @NotNull ListingPair> pairs) {
}
