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
 * 由补货建议（本地仓采购）勾选的物料生成采购计划：后端按同样的参数重新计算，带出默认供应商、单价、到货日期和各店铺的需求构成
 *
 * @param warehouseId 收货仓库，为空取需求店铺中最多的默认发货仓
 * @param w7          日均加权（与补货建议页面所选一致），全部为空取默认
 * @param forecast    是否按销量预测计算日均（与补货建议页面所选一致）
 */
public record PurchasePlanFromReplenish(
        Long warehouseId,
        @Size(max = 255, message = "{ecom.valid.purchase.remarkTooLong}") String remark,
        BigDecimal w7, BigDecimal w14, BigDecimal w30, BigDecimal w60,
        @NotEmpty(message = "{ecom.valid.purchase.itemsRequired}") @Size(max = 500, message = "{ecom.valid.purchase.tooManyItems}") List<@Valid @NotNull Item> items,
        Boolean forecast) {

    public PurchasePlanFromReplenish(Long warehouseId, String remark, BigDecimal w7, BigDecimal w14, BigDecimal w30, BigDecimal w60, List<Item> items) {
        this(warehouseId, remark, w7, w14, w30, w60, items, null);
    }

    /** @param qty 计划采购量，为空取建议采购量 */
    public record Item(@NotNull(message = "{ecom.valid.purchase.materialRequired}") Long materialId,
                       @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.purchase.qtyPositive}")
                       @Digits(integer = 14, fraction = 4, message = "{ecom.valid.purchase.qtyPositive}") BigDecimal qty) {
    }
}
