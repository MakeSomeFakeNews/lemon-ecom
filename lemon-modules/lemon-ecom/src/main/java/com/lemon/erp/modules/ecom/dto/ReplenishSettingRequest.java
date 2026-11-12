package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.ReplenishScope;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 保存补货设置（按级别 + 在线商品 / 物料新增或覆盖）；为空的项不覆盖默认值
 *
 * @param listingId    在线商品级时必填
 * @param materialId   物料级时必填
 * @param purchaseDays 采购周期，只用于物料级
 * @param dailySales   手工日均（在线商品级为平台 SKU 件数，物料级为物料数量）
 * @param factor       系数，乘在按销量计算的日均上
 */
public record ReplenishSettingRequest(
        @NotNull(message = "{ecom.valid.purchase.scopeRequired}") ReplenishScope scope,
        Long listingId,
        Long materialId,
        @Min(value = 0, message = "{ecom.valid.purchase.daysRange}") @Max(value = 365, message = "{ecom.valid.purchase.daysRange}") Integer leadDays,
        @Min(value = 0, message = "{ecom.valid.purchase.daysRange}") @Max(value = 365, message = "{ecom.valid.purchase.daysRange}") Integer safetyDays,
        @Min(value = 0, message = "{ecom.valid.purchase.daysRange}") @Max(value = 365, message = "{ecom.valid.purchase.daysRange}") Integer purchaseDays,
        @DecimalMin(value = "0", message = "{ecom.valid.purchase.dailyNegative}") @Digits(integer = 12, fraction = 4, message = "{ecom.valid.purchase.dailyNegative}") BigDecimal dailySales,
        @DecimalMin(value = "0", message = "{ecom.valid.purchase.factorRange}") @DecimalMax(value = "100", message = "{ecom.valid.purchase.factorRange}")
        @Digits(integer = 4, fraction = 4, message = "{ecom.valid.purchase.factorRange}") BigDecimal factor,
        Boolean disabled,
        @Size(max = 255, message = "{ecom.valid.purchase.remarkTooLong}") String remark) {
}
