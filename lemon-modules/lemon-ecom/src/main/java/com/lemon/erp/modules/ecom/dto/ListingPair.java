package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 一条配对：在线商品 → 物料（单品）与多件装数量（为空按 1）
 */
public record ListingPair(
        @NotNull(message = "{ecom.valid.catalog.listingRequired}") Long listingId,
        @NotNull(message = "{ecom.valid.selectMaterial}") Long materialId,
        @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.packQtyMustGreater}")
        @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}") BigDecimal packQty) {
}
