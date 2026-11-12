package com.lemon.erp.modules.ecom.dev.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 物料关联 1688 商品：offerId（或粘贴的链接）+ specId。有已授权的 1688 账号时保存后自动同步标题、规格、价格、起批量和库存，
 * 没有账号时按填写的标题、价格等保存
 *
 * @param qtyRatio 1 个 1688 单位 = 多少物料单位，默认 1
 */
public record Ali1688OfferRequest(
        @NotNull(message = "{ecom.valid.dev.materialRequired}") Long materialId,
        @NotBlank(message = "{ecom.valid.dev.offerRequired}") @Size(max = 500, message = "{ecom.valid.dev.urlTooLong}") String offer,
        @Size(max = 64, message = "{ecom.valid.dev.specIdTooLong}") String specId,
        Long supplierId,
        @DecimalMin(value = "0.0001", message = "{ecom.valid.dev.positive}") BigDecimal qtyRatio,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal alertThreshold,
        @Size(max = 255, message = "{ecom.valid.dev.titleTooLong}") String title,
        @Size(max = 255, message = "{ecom.valid.dev.titleTooLong}") String specText,
        @Size(max = 64, message = "{ecom.valid.dev.sellerTooLong}") String sellerLoginId,
        @Size(max = 128, message = "{ecom.valid.dev.sellerTooLong}") String sellerName,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal price,
        @Min(value = 0, message = "{ecom.valid.dev.nonNegative}") Integer minOrderQty,
        @Size(max = 16, message = "{ecom.valid.dev.unitTooLong}") String unit,
        @Size(max = 255, message = "{ecom.valid.dev.remarkTooLong}") String remark) {
}
