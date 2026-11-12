package com.lemon.erp.modules.ecom.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 发货计划生成头程货件：每个 shipment 生成一张货件草稿（可拆成多个货件）；shipments 为空时把全部未生成的件数生成一张货件
 */
public record GenerateInboundRequest(
        @Size(max = 20, message = "{ecom.valid.catalog.tooManyShipments}") List<@Valid @NotNull Shipment> shipments) {

    /**
     * @param shipmentId 平台货件号（可空，之后在货件上填写）
     */
    public record Shipment(
            @Size(max = 64, message = "{ecom.valid.shipmentIdCannotExceedCharacters}") String shipmentId,
            Long channelId,
            @NotEmpty(message = "{ecom.valid.addLineItems}") @Size(max = 500, message = "{ecom.valid.lineItemsCannotExceed}") List<@Valid @NotNull Line> lines) {
    }

    public record Line(
            @NotNull(message = "{ecom.valid.catalog.planItemRequired}") Long planItemId,
            @NotNull(message = "{ecom.valid.enterQuantity}")
            @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}") BigDecimal qty) {
    }
}
