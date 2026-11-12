package com.lemon.erp.modules.ecom.oper.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 监控快照导入的一行：平台（EcomPlatform 取值）+ 站点 + 商品 ID 定位监控（必须已建立），日期，各项指标（为空表示没有取到）；
 * 上下架填 ACTIVE / INACTIVE（或在售 / 下架）。同一监控同一天重复导入时覆盖
 */
@Data
public class SnapshotImportRow {

    @NotBlank(message = "{ecom.valid.selectPlatform}")
    @Size(max = 16)
    private String platform;

    @NotBlank(message = "{ecom.valid.selectMarketplace}")
    @Size(max = 16)
    private String marketplace;

    @NotBlank(message = "{ecom.valid.oper.itemIdRequired}")
    @Size(max = 64, message = "{ecom.valid.oper.itemIdTooLong}")
    private String itemId;

    @NotNull(message = "{ecom.valid.oper.snapshotDateRequired}")
    private LocalDate snapshotDate;

    @Size(max = 128)
    private String category;

    @Min(value = 1, message = "{ecom.valid.oper.rankInvalid}")
    private Integer rankMain;

    @Size(max = 128)
    private String subCategory;

    @Min(value = 1, message = "{ecom.valid.oper.rankInvalid}")
    private Integer rankSub;

    @DecimalMin(value = "0", message = "{ecom.valid.oper.ratingInvalid}")
    @DecimalMax(value = "5", message = "{ecom.valid.oper.ratingInvalid}")
    private BigDecimal rating;

    @Min(value = 0, message = "{ecom.valid.oper.qtyInvalid}")
    private Integer reviewCount;

    @Min(value = 0, message = "{ecom.valid.oper.qtyInvalid}")
    private Integer sellerCount;

    @DecimalMin(value = "0", message = "{ecom.valid.oper.amountInvalid}")
    private BigDecimal price;

    @Size(max = 500, message = "{ecom.valid.oper.titleTooLong}")
    private String title;

    @Size(max = 500, message = "{ecom.valid.oper.imageTooLong}")
    private String mainImage;

    @Size(max = 16)
    private String listingStatus;
}
