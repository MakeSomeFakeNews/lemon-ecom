package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 批量导入配对的一行：店铺编码 + 平台 SKU → 物料编码 × 数量（多件装，为空按 1）；在线商品必须已存在
 */
@Data
public class PairingImportRow {

    @NotBlank(message = "{ecom.valid.storeCodeRequired}")
    @Size(max = 32, message = "{ecom.valid.storeCodeCannotExceedCharacters}")
    private String storeCode;

    @NotBlank(message = "{ecom.valid.sellerSkuRequired}")
    @Size(max = 64, message = "{ecom.valid.sellerSkuCannotExceedCharacters}")
    private String sellerSku;

    @NotBlank(message = "{ecom.valid.catalog.materialCodeRequired}")
    @Size(max = 64, message = "{ecom.valid.catalog.materialCodeCannotExceedCharacters}")
    private String materialCode;

    @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.packQtyMustGreater}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}")
    private BigDecimal qty;
}
