package com.lemon.erp.modules.ecom.dev.dto;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 新建 / 修改开发项目（图片单独上传）
 */
public record DevProjectRequest(
        @NotBlank(message = "{ecom.valid.dev.nameRequired}") @Size(max = 128, message = "{ecom.valid.dev.nameTooLong}") String name,
        @Size(max = 64, message = "{ecom.valid.dev.categoryTooLong}") String category,
        @Size(max = 64, message = "{ecom.valid.dev.brandTooLong}") String brand,
        @Size(max = 32, message = "{ecom.valid.dev.sourceTooLong}") String source,
        EcomPlatform platform,
        @Size(max = 16, message = "{ecom.valid.dev.marketplaceTooLong}") String marketplace,
        @Size(max = 8, message = "{ecom.valid.dev.currencyTooLong}") String currency,
        @Size(max = 500, message = "{ecom.valid.dev.urlTooLong}") String competitorUrl,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal competitorPrice,
        @Min(value = 0, message = "{ecom.valid.dev.nonNegative}") Integer competitorReviews,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.ratingRange}") @DecimalMax(value = "5", message = "{ecom.valid.dev.ratingRange}") BigDecimal competitorRating,
        @Min(value = 0, message = "{ecom.valid.dev.nonNegative}") Integer estMonthlySales,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal targetPrice,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal estPurchasePrice,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal weightKg,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal lengthCm,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal widthCm,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal heightCm,
        Long ownerId,
        @Size(max = 1000, message = "{ecom.valid.dev.sellingPointsTooLong}") String sellingPoints,
        @Size(max = 2000, message = "{ecom.valid.dev.descriptionTooLong}") String description,
        @Size(max = 255, message = "{ecom.valid.dev.remarkTooLong}") String remark) {
}
