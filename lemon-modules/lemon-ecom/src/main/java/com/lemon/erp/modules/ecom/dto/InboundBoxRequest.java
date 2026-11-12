package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 头程货件装箱（整体替换）：各箱的尺寸、重量与箱内平台 SKU 件数；箱号按顺序从 1 编起
 */
public record InboundBoxRequest(@NotNull @Size(max = 500, message = "{ecom.valid.catalog.tooManyBoxes}") List<@Valid @NotNull Box> boxes) {

    public record Box(
            @DecimalMin(value = "0", message = "{ecom.valid.weightCannotNegative}")
            @Digits(integer = 8, fraction = 2, message = "{ecom.valid.catalog.sizeDecimals}") BigDecimal length,
            @DecimalMin(value = "0", message = "{ecom.valid.weightCannotNegative}")
            @Digits(integer = 8, fraction = 2, message = "{ecom.valid.catalog.sizeDecimals}") BigDecimal width,
            @DecimalMin(value = "0", message = "{ecom.valid.weightCannotNegative}")
            @Digits(integer = 8, fraction = 2, message = "{ecom.valid.catalog.sizeDecimals}") BigDecimal height,
            @DecimalMin(value = "0", message = "{ecom.valid.weightCannotNegative}")
            @Digits(integer = 12, fraction = 3, message = "{ecom.valid.weightDecimals}") BigDecimal weight,
            @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}") String remark,
            @NotNull @Size(max = 200, message = "{ecom.valid.lineItemsCannotExceed}") List<@Valid @NotNull Item> items) {
    }

    public record Item(
            @NotBlank(message = "{ecom.valid.sellerSkuRequired}") @Size(max = 64, message = "{ecom.valid.sellerSkuCannotExceedCharacters}") String sellerSku,
            @NotNull(message = "{ecom.valid.enterQuantity}")
            @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}") BigDecimal qty) {
    }
}
