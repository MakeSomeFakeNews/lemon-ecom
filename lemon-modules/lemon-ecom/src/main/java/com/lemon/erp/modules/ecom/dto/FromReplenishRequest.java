package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 由补货建议勾选生成发货计划：店铺、目标仓（为空取店铺的平台仓）、发货仓（为空取店铺默认发货仓）与勾选的在线商品、件数
 */
public record FromReplenishRequest(
        @NotNull(message = "{ecom.valid.selectStore}") Long storeId,
        Long toWarehouseId,
        Long fromWarehouseId,
        @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{ecom.valid.addLineItems}") @Size(max = 500, message = "{ecom.valid.lineItemsCannotExceed}") List<@Valid @NotNull Line> items) {

    public record Line(
            @NotNull(message = "{ecom.valid.catalog.listingRequired}") Long listingId,
            @NotNull(message = "{ecom.valid.enterQuantity}")
            @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}") BigDecimal qty) {
    }
}
