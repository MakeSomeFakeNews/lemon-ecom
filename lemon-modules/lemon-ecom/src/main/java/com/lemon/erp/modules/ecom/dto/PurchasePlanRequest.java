package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改采购计划（草稿）
 *
 * @param warehouseId 收货仓库，选填（生成的采购订单的默认收货仓）
 */
public record PurchasePlanRequest(
        @NotNull(message = "{ecom.valid.purchase.planDateRequired}") LocalDate planDate,
        Long warehouseId,
        @Size(max = 255, message = "{ecom.valid.purchase.remarkTooLong}") String remark,
        @NotEmpty(message = "{ecom.valid.purchase.itemsRequired}") @Size(max = 500, message = "{ecom.valid.purchase.tooManyItems}") List<@Valid @NotNull Item> items) {

    /**
     * @param suggestQty 生成时的建议采购量（只用于显示）
     * @param price      含税单价，为空时生成订单时取供应商价格 / 默认采购价
     * @param taxRate    税率（%），为空取默认
     * @param demand     需求构成（JSON，生成计划时由系统填写，修改时原样传回）
     */
    public record Item(
            @NotNull(message = "{ecom.valid.purchase.materialRequired}") Long materialId,
            @NotNull(message = "{ecom.valid.purchase.qtyRequired}") @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.purchase.qtyPositive}")
            @Digits(integer = 14, fraction = 4, message = "{ecom.valid.purchase.qtyPositive}") BigDecimal qty,
            BigDecimal suggestQty,
            Long supplierId,
            @DecimalMin(value = "0", message = "{ecom.valid.purchase.priceNegative}") @Digits(integer = 14, fraction = 4, message = "{ecom.valid.purchase.priceNegative}") BigDecimal price,
            @DecimalMin(value = "0", message = "{ecom.valid.purchase.taxRateRange}") @DecimalMax(value = "100", message = "{ecom.valid.purchase.taxRateRange}") BigDecimal taxRate,
            LocalDate arrivalDate,
            @Size(max = 2000) String demand,
            @Size(max = 255, message = "{ecom.valid.purchase.remarkTooLong}") String remark) {
    }
}
