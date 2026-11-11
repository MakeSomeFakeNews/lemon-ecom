package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.SettlementItemType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 结算报告导入的一行：同一店铺 + 结算编号的行合并为一张结算单，收入为正、费用为负（店铺币种）
 */
@Data
public class SettlementImportRow {

    @NotBlank(message = "{ecom.valid.storeCodeRequired}")
    @Size(max = 32, message = "{ecom.valid.storeCodeCannotExceedCharacters}")
    private String storeCode;

    @NotBlank(message = "{ecom.valid.settlementNoRequired}")
    @Size(max = 64, message = "{ecom.valid.settlementNoCannotExceedCharacters}")
    private String settlementNo;

    @NotNull(message = "{ecom.valid.periodRequired}")
    private LocalDate periodStart;

    @NotNull(message = "{ecom.valid.periodRequired}")
    private LocalDate periodEnd;

    @Size(max = 8)
    private String currency;

    private LocalDate postedDate;

    @NotNull(message = "{ecom.valid.selectItemType}")
    private SettlementItemType itemType;

    @Size(max = 64, message = "{ecom.valid.platformOrderNoCannotExceedCharacters}")
    private String platformOrderNo;

    @Size(max = 64, message = "{ecom.valid.sellerSkuCannotExceedCharacters}")
    private String sellerSku;

    @NotNull(message = "{ecom.valid.enterAmount}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}")
    private BigDecimal amount;

    @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}")
    private String description;
}
