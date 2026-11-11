package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 平台库存报告导入的一行：店铺 + 平台 SKU 只保留最近一次快照
 */
@Data
public class PlatformStockImportRow {

    @NotBlank(message = "{ecom.valid.storeCodeRequired}")
    @Size(max = 32, message = "{ecom.valid.storeCodeCannotExceedCharacters}")
    private String storeCode;

    @NotBlank(message = "{ecom.valid.sellerSkuRequired}")
    @Size(max = 64, message = "{ecom.valid.sellerSkuCannotExceedCharacters}")
    private String sellerSku;

    @Size(max = 64, message = "{ecom.valid.platformItemIdCannotExceedCharacters}")
    private String platformItemId;

    @NotNull(message = "{ecom.valid.enterQuantity}")
    @DecimalMin(value = "0", message = "{ecom.valid.countCannotNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}")
    private BigDecimal fulfillableQty;

    @DecimalMin(value = "0", message = "{ecom.valid.countCannotNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}")
    private BigDecimal inboundQty;

    @DecimalMin(value = "0", message = "{ecom.valid.countCannotNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}")
    private BigDecimal reservedQty;

    @DecimalMin(value = "0", message = "{ecom.valid.countCannotNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}")
    private BigDecimal unfulfillableQty;

    private LocalDate snapshotDate;
}
