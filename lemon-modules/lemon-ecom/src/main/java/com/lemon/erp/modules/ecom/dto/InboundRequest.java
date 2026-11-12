package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 新增 / 修改头程货件（草稿）
 */
public record InboundRequest(
        @NotNull(message = "{ecom.valid.selectStore}") Long storeId,
        @Size(max = 64, message = "{ecom.valid.shipmentIdCannotExceedCharacters}") String shipmentId,
        @NotNull(message = "{ecom.valid.selectFromWarehouse}") Long fromWarehouseId,
        @NotNull(message = "{ecom.valid.selectToWarehouse}") Long toWarehouseId,
        Long channelId,
        @Size(max = 64, message = "{ecom.valid.carrierCannotExceedCharacters}") String carrier,
        @Size(max = 64, message = "{ecom.valid.trackingNoCannotExceedCharacters}") String trackingNo,
        LocalDate shipDate,
        LocalDate etaDate,
        @Min(value = 0, message = "{ecom.valid.boxCountRange}") @Max(value = 99999, message = "{ecom.valid.boxCountRange}") Integer boxCount,
        @DecimalMin(value = "0", message = "{ecom.valid.weightCannotNegative}")
        @Digits(integer = 12, fraction = 3, message = "{ecom.valid.weightDecimals}") BigDecimal weight,
        @DecimalMin(value = "0", message = "{ecom.valid.weightCannotNegative}")
        @Digits(integer = 12, fraction = 4, message = "{ecom.valid.qtyDecimals}") BigDecimal volume,
        @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
        @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}") BigDecimal freight,
        @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
        @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}") BigDecimal duty,
        @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
        @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}") BigDecimal otherCost,
        @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{ecom.valid.addLineItems}") @Size(max = 500, message = "{ecom.valid.lineItemsCannotExceed}") List<@Valid @NotNull Item> items,
        @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
        @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}") BigDecimal importVat) {

    /** 不带进口增值税（发货计划生成货件等） */
    public InboundRequest(Long storeId, String shipmentId, Long fromWarehouseId, Long toWarehouseId, Long channelId, String carrier, String trackingNo,
                          LocalDate shipDate, LocalDate etaDate, Integer boxCount, BigDecimal weight, BigDecimal volume, BigDecimal freight, BigDecimal duty,
                          BigDecimal otherCost, String remark, List<Item> items) {
        this(storeId, shipmentId, fromWarehouseId, toWarehouseId, channelId, carrier, trackingNo, shipDate, etaDate, boxCount, weight, volume, freight, duty,
                otherCost, remark, items, null);
    }

    /**
     * 明细：物料（必填）与平台 SKU（可空，用于对照平台货件）；listingId（可空，没有时按 店铺 + 平台 SKU 识别）用于按平台 SKU 计件数，
     * planItemId 为来源发货计划明细（由发货计划生成时带入，修改草稿时原样提交）
     */
    public record Item(
            @NotNull(message = "{ecom.valid.selectMaterial}") Long materialId,
            @Size(max = 64, message = "{ecom.valid.sellerSkuCannotExceedCharacters}") String sellerSku,
            @NotNull(message = "{ecom.valid.enterQuantity}")
            @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}") BigDecimal qty,
            @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}") String remark,
            Long listingId,
            Long planItemId) {

        public Item(Long materialId, String sellerSku, BigDecimal qty, String remark) {
            this(materialId, sellerSku, qty, remark, null, null);
        }
    }
}
