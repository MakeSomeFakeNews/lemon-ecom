package com.lemon.erp.modules.ecom.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.entity.EcomPackageTrack;
import com.lemon.erp.modules.ecom.model.EcomTrackAlert;
import com.lemon.erp.modules.ecom.model.EcomTrackStatus;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单增强的请求与视图：手工建单、补发、打标签、内部备注、批量指定仓库 / 物流、地址校验与利润预估结果、物流跟踪
 */
public final class OrderEnhanceViews {

    private OrderEnhanceViews() {
    }

    // ---------------------------------------------------------------- 手工建单 / 补发

    /**
     * 手工创建订单（线下 / 站外订单）：选店铺、买家、收件地址、商品（平台 SKU 按店铺的在线商品映射到物料）
     *
     * @param platformOrderNo 订单号，为空自动生成（MO + 时间）
     * @param currency        币种，为空取店铺币种
     */
    public record ManualOrder(
            @NotNull(message = "{ecom.valid.selectStore}") Long storeId,
            @Size(max = 64, message = "{ecom.valid.ord.orderNoTooLong}") String platformOrderNo,
            FulfillmentType fulfillment,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime orderTime,
            @Size(max = 64, message = "{ecom.valid.nameCannotExceedCharacters}") String buyerName,
            @Email(message = "{ecom.valid.ord.emailInvalid}") @Size(max = 128, message = "{ecom.valid.ord.emailInvalid}") String buyerEmail,
            @NotBlank(message = "{ecom.valid.ord.shipNameRequired}") @Size(max = 64, message = "{ecom.valid.nameCannotExceedCharacters}") String shipName,
            @Size(max = 32, message = "{ecom.valid.phoneCannotExceedCharacters}") String shipPhone,
            @NotBlank(message = "{ecom.valid.ord.countryRequired}") @Size(max = 8, message = "{ecom.valid.countryCannotExceedCharacters}") String shipCountry,
            @Size(max = 64, message = "{ecom.valid.addressPartCannotExceedCharacters}") String shipState,
            @Size(max = 64, message = "{ecom.valid.addressPartCannotExceedCharacters}") String shipCity,
            @Size(max = 16, message = "{ecom.valid.postcodeCannotExceedCharacters}") String shipPostcode,
            @Size(max = 255, message = "{ecom.valid.addressCannotExceedCharacters}") String shipAddress,
            @Size(max = 8, message = "{ecom.valid.ord.currencyInvalid}") String currency,
            @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
            @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}") BigDecimal shippingAmount,
            @DecimalMin(value = "0", message = "{ecom.valid.weightCannotNegative}")
            @Digits(integer = 12, fraction = 3, message = "{ecom.valid.weightDecimals}") BigDecimal weight,
            @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}") String remark,
            @NotEmpty(message = "{ecom.valid.addLineItems}") @Size(max = 200, message = "{ecom.valid.lineItemsCannotExceed}")
            List<@Valid @NotNull ManualLine> lines) {
    }

    public record ManualLine(
            @NotBlank(message = "{ecom.valid.ord.skuRequired}") @Size(max = 64, message = "{ecom.valid.ord.skuTooLong}") String sellerSku,
            @Size(max = 255, message = "{ecom.valid.ord.titleTooLong}") String title,
            @NotNull(message = "{ecom.valid.enterQuantity}")
            @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}") BigDecimal qty,
            @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
            @Digits(integer = 16, fraction = 4, message = "{ecom.valid.amountDecimals}") BigDecimal price) {
    }

    /**
     * 从原订单创建补发 / 重发单：选择补发的行与件数（不超过原件数），可关联售后工单、退货退款单；
     * 收件信息为空时取原订单的
     */
    public record Resend(
            @NotEmpty(message = "{ecom.valid.addLineItems}") List<@Valid @NotNull ResendLine> lines,
            Long ticketId,
            Long returnId,
            @Size(max = 64, message = "{ecom.valid.nameCannotExceedCharacters}") String shipName,
            @Size(max = 32, message = "{ecom.valid.phoneCannotExceedCharacters}") String shipPhone,
            @Size(max = 8, message = "{ecom.valid.countryCannotExceedCharacters}") String shipCountry,
            @Size(max = 64, message = "{ecom.valid.addressPartCannotExceedCharacters}") String shipState,
            @Size(max = 64, message = "{ecom.valid.addressPartCannotExceedCharacters}") String shipCity,
            @Size(max = 16, message = "{ecom.valid.postcodeCannotExceedCharacters}") String shipPostcode,
            @Size(max = 255, message = "{ecom.valid.addressCannotExceedCharacters}") String shipAddress,
            @Size(max = 200, message = "{ecom.valid.remarkCannotExceedCharacters}") String reason) {
    }

    public record ResendLine(
            @NotNull(message = "{ecom.valid.selectOrderLine}") Long orderItemId,
            @NotNull(message = "{ecom.valid.enterQuantity}")
            @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.quantityMustGreater}")
            @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}") BigDecimal qty) {
    }

    /** 补发单与售后成本：原订单的补发单列表、合计售后成本（补发单的出库成本 + 运费，本位币） */
    public record ResendSummary(List<EcomOrder> resends, BigDecimal afterSaleCost) {
    }

    // ---------------------------------------------------------------- 标签 / 备注 / 批量

    /** 批量打标 / 去标 */
    public record TagRequest(
            @NotEmpty(message = "{ecom.valid.ord.selectOrders}") @Size(max = 500, message = "{ecom.valid.ord.tooManyOrders}") List<Long> ids,
            @NotEmpty(message = "{ecom.valid.ord.selectTags}") List<Long> tagIds) {
    }

    public record NoteRequest(@NotBlank(message = "{ecom.valid.ord.noteRequired}") @Size(max = 500, message = "{ecom.valid.ord.noteTooLong}") String content) {
    }

    /** 批量指定发货仓 / 物流渠道（未打单的自发货订单，已生成的待打单包裹一并修改） */
    public record AssignRequest(
            @NotEmpty(message = "{ecom.valid.ord.selectOrders}") @Size(max = 500, message = "{ecom.valid.ord.tooManyOrders}") List<Long> ids,
            Long warehouseId,
            Long channelId) {
    }

    /** 修改收件地址（地址校验不通过后在订单里修改），保存后重新校验 */
    public record AddressRequest(
            @Size(max = 64, message = "{ecom.valid.nameCannotExceedCharacters}") String shipName,
            @Size(max = 32, message = "{ecom.valid.phoneCannotExceedCharacters}") String shipPhone,
            @Size(max = 8, message = "{ecom.valid.countryCannotExceedCharacters}") String shipCountry,
            @Size(max = 64, message = "{ecom.valid.addressPartCannotExceedCharacters}") String shipState,
            @Size(max = 64, message = "{ecom.valid.addressPartCannotExceedCharacters}") String shipCity,
            @Size(max = 16, message = "{ecom.valid.postcodeCannotExceedCharacters}") String shipPostcode,
            @Size(max = 255, message = "{ecom.valid.addressCannotExceedCharacters}") String shipAddress) {
    }

    /**
     * 地址校验与利润预估的结果（订单详情显示，审单规则按它匹配）
     *
     * @param issues        地址问题（含提示类，如偏远地区）
     * @param invalid       是否不通过（有错误类问题）
     * @param blacklistId   命中的黑名单
     * @param blacklistDesc 命中的黑名单说明
     */
    public record CheckResult(List<AddressIssue> issues, boolean invalid, boolean remote, Long blacklistId, String blacklistDesc,
                              Long estChannelId, String estChannelName, BigDecimal estWeight, BigDecimal estShipping, BigDecimal estFee,
                              BigDecimal estCost, BigDecimal estProfit, BigDecimal estProfitRate) {
    }

    /**
     * 地址的一个问题
     *
     * @param code    问题代码（MISSING_FIELD、POSTCODE_FORMAT、PO_BOX、PHONE_FORMAT、REMOTE_AREA、STATE_MISSING、PHONE_MISSING、ADDRESS_HIDDEN）
     * @param error   true 为错误（不通过），false 为提示
     * @param message 说明（已按当前语言）
     */
    public record AddressIssue(String code, boolean error, String message) {
    }

    // ---------------------------------------------------------------- 物流跟踪

    /** 物流跟踪列表的条件 */
    @Data
    public static class TrackQuery {
        private Long storeId;
        private Long channelId;
        private String country;
        private EcomTrackStatus trackStatus;
        private EcomTrackAlert alert;
        /** 只看有预警的 */
        private Boolean alertOnly;
        /** 包裹号 / 运单号 / 收件人 / 平台订单号 */
        private String keyword;
        /** 发货日期 */
        private LocalDate beginDate;
        private LocalDate endDate;

        public LocalDateTime getBeginTime() {
            return beginDate == null ? null : beginDate.atStartOfDay();
        }

        public LocalDateTime getEndExclusive() {
            return endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        }
    }

    /** 包裹的轨迹：包裹（含汇总状态）与时间线（新的在前） */
    public record PackageTracks(EcomPackage pkg, List<EcomPackageTrack> events) {
    }

    /**
     * 订单作业设置
     *
     * @param trackKey 17TRACK 密钥：为空不修改，传空格清除
     */
    public record SettingRequest(
            @NotNull(message = "{ecom.valid.ord.settingRequired}") Integer addressCheck,
            @NotNull(message = "{ecom.valid.ord.settingRequired}") Integer trackEnabled,
            @Size(max = 128, message = "{ecom.valid.ord.trackKeyTooLong}") String trackKey,
            @Size(max = 255, message = "{ecom.valid.ord.trackKeyTooLong}") String trackApiUrl,
            @NotNull(message = "{ecom.valid.ord.settingRequired}") @Min(value = 1, message = "{ecom.valid.ord.hoursRange}")
            @Max(value = 720, message = "{ecom.valid.ord.hoursRange}") Integer noScanHours,
            @NotNull(message = "{ecom.valid.ord.settingRequired}") @Min(value = 1, message = "{ecom.valid.ord.daysRange}")
            @Max(value = 180, message = "{ecom.valid.ord.daysRange}") Integer transitDays,
            @NotNull(message = "{ecom.valid.ord.settingRequired}") @Min(value = 1, message = "{ecom.valid.ord.daysRange}")
            @Max(value = 180, message = "{ecom.valid.ord.daysRange}") Integer staleDays) {
    }
}
