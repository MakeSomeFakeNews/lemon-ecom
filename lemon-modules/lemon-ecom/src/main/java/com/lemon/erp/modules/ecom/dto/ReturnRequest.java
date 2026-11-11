package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.ReturnType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增退货退款：按订单行填写退回数量和退款金额（订单币种），退货退款须选择退回仓库
 *
 * @param refundAmount 退款总额，为空按明细合计
 */
public record ReturnRequest(
        @NotNull(message = "{ecom.valid.selectOrder}") Long orderId,
        @NotNull(message = "{ecom.valid.selectReturnType}") ReturnType returnType,
        @Size(max = 32) String reason,
        @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
        @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}") BigDecimal refundAmount,
        Long warehouseId,
        @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}") String remark,
        @NotEmpty(message = "{ecom.valid.addLineItems}") @Size(max = 200, message = "{ecom.valid.lineItemsCannotExceed}") List<@Valid @NotNull Item> items) {

    public record Item(
            @NotNull(message = "{ecom.valid.selectOrderLine}") Long orderItemId,
            @NotNull(message = "{ecom.valid.enterQuantity}")
            @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}") BigDecimal qty,
            @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
            @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}") BigDecimal refundAmount) {
    }
}
