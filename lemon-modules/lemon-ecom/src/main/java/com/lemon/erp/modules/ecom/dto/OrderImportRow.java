package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台订单导入的一行（Excel 一行一个订单行，同一订单的多行按店铺 + 平台订单号合并）。
 * 收件信息、币种、状态等取同一订单的第一行；运费、税费、优惠按行填写并汇总到订单
 */
@Data
public class OrderImportRow {

    @NotBlank(message = "{ecom.valid.storeCodeRequired}")
    @Size(max = 32, message = "{ecom.valid.storeCodeCannotExceedCharacters}")
    private String storeCode;

    @NotBlank(message = "{ecom.valid.platformOrderNoRequired}")
    @Size(max = 64, message = "{ecom.valid.platformOrderNoCannotExceedCharacters}")
    private String platformOrderNo;

    @NotNull(message = "{ecom.valid.orderTimeRequired}")
    private LocalDateTime orderTime;

    private LocalDateTime paidTime;

    private EcomOrderStatus status;

    private FulfillmentType fulfillment;

    @Size(max = 64, message = "{ecom.valid.nameCannotExceedCharacters}")
    private String buyerName;
    @Size(max = 128, message = "{ecom.valid.emailCannotExceedCharacters}")
    private String buyerEmail;
    @Size(max = 64, message = "{ecom.valid.nameCannotExceedCharacters}")
    private String shipName;
    @Size(max = 32, message = "{ecom.valid.phoneCannotExceedCharacters}")
    private String shipPhone;
    @Size(max = 8, message = "{ecom.valid.countryCannotExceedCharacters}")
    private String shipCountry;
    @Size(max = 64, message = "{ecom.valid.addressPartCannotExceedCharacters}")
    private String shipState;
    @Size(max = 64, message = "{ecom.valid.addressPartCannotExceedCharacters}")
    private String shipCity;
    @Size(max = 16, message = "{ecom.valid.postcodeCannotExceedCharacters}")
    private String shipPostcode;
    @Size(max = 255, message = "{ecom.valid.addressCannotExceedCharacters}")
    private String shipAddress;

    /** 为空取店铺币种 */
    @Size(max = 8)
    private String currency;

    private LocalDateTime shipBy;

    @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}")
    private String note;

    @Size(max = 64, message = "{ecom.valid.platformLineIdCannotExceedCharacters}")
    private String platformLineId;

    @NotBlank(message = "{ecom.valid.sellerSkuRequired}")
    @Size(max = 64, message = "{ecom.valid.sellerSkuCannotExceedCharacters}")
    private String sellerSku;

    @Size(max = 64, message = "{ecom.valid.platformItemIdCannotExceedCharacters}")
    private String platformItemId;

    @Size(max = 255, message = "{ecom.valid.titleCannotExceedCharacters}")
    private String title;

    @NotNull(message = "{ecom.valid.enterQuantity}")
    @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.quantityMustGreater}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}")
    private BigDecimal qty;

    @NotNull(message = "{ecom.valid.enterPrice}")
    @DecimalMin(value = "0", message = "{ecom.valid.priceCannotNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.priceDecimals}")
    private BigDecimal price;

    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}")
    private BigDecimal shipping;
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}")
    private BigDecimal tax;
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}")
    private BigDecimal discount;
}
