package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改发货计划（草稿）：目标仓为空时取店铺的平台仓，发货仓为空时取店铺默认发货仓
 */
public record ShipmentPlanRequest(
        @NotNull(message = "{ecom.valid.selectStore}") Long storeId,
        Long fromWarehouseId,
        Long toWarehouseId,
        LocalDate planDate,
        @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{ecom.valid.addLineItems}") @Size(max = 500, message = "{ecom.valid.lineItemsCannotExceed}") List<@Valid @NotNull Item> items) {

    /** 明细：在线商品（平台 SKU）与计划件数 */
    public record Item(
            @NotNull(message = "{ecom.valid.catalog.listingRequired}") Long listingId,
            @NotNull(message = "{ecom.valid.enterQuantity}")
            @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}") BigDecimal qty,
            @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}") String remark) {
    }
}
