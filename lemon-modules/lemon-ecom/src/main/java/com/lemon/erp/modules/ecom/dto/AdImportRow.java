package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 广告报告导入的一行：店铺 × 日期 × 广告活动 × 平台 SKU，同一组合重复导入时覆盖
 */
@Data
public class AdImportRow {

    @NotBlank(message = "{ecom.valid.storeCodeRequired}")
    @Size(max = 32, message = "{ecom.valid.storeCodeCannotExceedCharacters}")
    private String storeCode;

    @NotNull(message = "{ecom.valid.adDateRequired}")
    private LocalDate adDate;

    @Size(max = 128, message = "{ecom.valid.campaignCannotExceedCharacters}")
    private String campaign;

    @Size(max = 64, message = "{ecom.valid.sellerSkuCannotExceedCharacters}")
    private String sellerSku;

    @Size(max = 64, message = "{ecom.valid.platformItemIdCannotExceedCharacters}")
    private String platformItemId;

    @Min(value = 0, message = "{ecom.valid.countCannotNegative}")
    private Long impressions;

    @Min(value = 0, message = "{ecom.valid.countCannotNegative}")
    private Long clicks;

    @NotNull(message = "{ecom.valid.enterAmount}")
    @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}")
    private BigDecimal spend;

    @Size(max = 8)
    private String currency;

    @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}")
    private BigDecimal adSales;

    @Min(value = 0, message = "{ecom.valid.countCannotNegative}")
    private Integer adOrders;
}
