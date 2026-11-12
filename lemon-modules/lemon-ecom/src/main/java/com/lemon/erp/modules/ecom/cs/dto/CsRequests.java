package com.lemon.erp.modules.ecom.cs.dto;

import com.lemon.erp.modules.ecom.cs.model.CsReviewStatus;
import com.lemon.erp.modules.ecom.cs.model.CsSourceType;
import com.lemon.erp.modules.ecom.cs.model.CsTicketType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客服与评价的请求参数
 */
public final class CsRequests {

    private CsRequests() {
    }

    /**
     * 回复买家
     *
     * @param templateId 使用的模板（只做记录）
     * @param recordOnly 只登记（已在平台后台回复过，或平台没有发送接口时），不调用平台接口
     */
    public record Reply(@NotBlank(message = "{ecom.valid.cs.replyRequired}") @Size(max = 2000, message = "{ecom.valid.cs.replyTooLong}") String content,
                        Long templateId, Boolean recordOnly) {
    }

    /** 修改会话：负责人、标签（逗号分隔）、备注 */
    public record ThreadUpdate(Long ownerId, @Size(max = 255, message = "{ecom.valid.cs.tagsTooLong}") String tags,
                               @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}") String remark) {
    }

    /** 关联订单：店铺内的平台订单号，为空表示取消关联 */
    public record LinkOrder(@Size(max = 64) String platformOrderNo) {
    }

    /**
     * 回复评价
     *
     * @param recordOnly 只登记回复内容（已在平台后台回复，或平台没有回复接口时）
     */
    public record ReviewReply(@NotBlank(message = "{ecom.valid.cs.replyRequired}") @Size(max = 2000, message = "{ecom.valid.cs.replyTooLong}") String content,
                              Boolean recordOnly) {
    }

    /** 处理评价 / 店铺反馈：处理状态、处理人、说明 */
    public record Handle(@NotNull(message = "{ecom.valid.cs.handleStatusRequired}") CsReviewStatus handleStatus, Long handlerId,
                         @Size(max = 500, message = "{ecom.valid.cs.handleNoteTooLong}") String handleNote) {
    }

    /** 请求评价：平台订单 ID 列表 */
    public record ReviewRequest(@NotEmpty(message = "{ecom.valid.cs.selectOrders}") @Size(max = 200, message = "{ecom.valid.cs.tooManyOrders}") List<Long> orderIds) {
    }

    /**
     * 转售后：退货退款 / 仅退款按明细的件数与退款金额生成退货退款单（原因、退回仓库、退款总额同退货退款单）；补发按明细的件数生成补发订单
     *
     * @param orderId 原平台订单，为空取来源（会话、评价）关联的订单
     */
    public record Ticket(@NotNull(message = "{ecom.valid.cs.sourceRequired}") CsSourceType sourceType,
                         @NotNull(message = "{ecom.valid.cs.sourceRequired}") Long sourceId,
                         @NotNull(message = "{ecom.valid.cs.ticketTypeRequired}") CsTicketType ticketType,
                         Long orderId,
                         @Size(max = 32) String reason,
                         @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
                         @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}") BigDecimal refundAmount,
                         Long warehouseId,
                         @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}") String remark,
                         @NotEmpty(message = "{ecom.valid.addLineItems}") @Size(max = 200, message = "{ecom.valid.lineItemsCannotExceed}")
                         List<@Valid @NotNull TicketLine> items) {
    }

    public record TicketLine(@NotNull(message = "{ecom.valid.selectOrderLine}") Long orderItemId,
                             @NotNull(message = "{ecom.valid.enterQuantity}")
                             @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.quantityMustGreater}")
                             @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}") BigDecimal qty,
                             @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
                             @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}") BigDecimal refundAmount) {
    }
}
