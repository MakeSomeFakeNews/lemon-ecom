package com.lemon.erp.modules.ecom.fba.dto;

import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimStatus;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimType;
import com.lemon.erp.modules.ecom.fba.model.FbaDisposition;
import com.lemon.erp.modules.ecom.fba.model.FbaRemovalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * FBA 运营的请求
 */
public final class FbaRequests {

    private FbaRequests() {
    }

    /** 创建亚马逊报告任务（按日期的报告不填日期时取最近 30 天） */
    public record ReportCreate(@NotNull(message = "{ecom.valid.selectStore}") Long storeId,
                               @NotNull(message = "{ecom.valid.fba.reportTypeRequired}") AmzReportType reportType,
                               LocalDate dataStart, LocalDate dataEnd) {
    }

    /** 上传报告文件（制表符分隔的文本，前端读取文件后提交） */
    public record ReportUpload(@NotNull(message = "{ecom.valid.selectStore}") Long storeId,
                               @NotNull(message = "{ecom.valid.fba.reportTypeRequired}") AmzReportType reportType,
                               @NotBlank(message = "{ecom.valid.fba.reportContentRequired}") String content,
                               LocalDate snapshotDate) {
    }

    /** 移除订单明细 */
    public record RemovalLine(@NotBlank(message = "{ecom.valid.fba.skuRequired}") @Size(max = 64) String sellerSku,
                              FbaDisposition disposition,
                              @NotNull(message = "{ecom.valid.fba.qtyRequired}") @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.fba.qtyRequired}") BigDecimal qty) {
    }

    /** 新增 / 修改移除订单（移除计划） */
    public record RemovalRequest(@NotNull(message = "{ecom.valid.selectStore}") Long storeId,
                                 @NotNull(message = "{ecom.valid.fba.removalTypeRequired}") FbaRemovalType removalType,
                                 @Size(max = 64) String platformOrderId,
                                 Long warehouseId, Long defectiveWarehouseId,
                                 @Size(max = 255) String shipAddress,
                                 LocalDate requestDate,
                                 @Size(max = 255) String remark,
                                 @NotEmpty(message = "{ecom.valid.fba.linesRequired}") @Size(max = 500) List<@Valid @NotNull RemovalLine> items) {
    }

    /** 提交（已在亚马逊卖家平台创建移除订单）：填写亚马逊移除订单号 */
    public record RemovalSubmit(@NotBlank(message = "{ecom.valid.fba.platformOrderIdRequired}") @Size(max = 64) String platformOrderId) {
    }

    /** 收货的一行：良品、不良品件数 */
    public record ReceiveLine(@NotNull Long itemId, BigDecimal goodQty, BigDecimal defectiveQty) {
    }

    /** 本地仓收货 */
    public record RemovalReceive(LocalDate receiveDate, Long warehouseId, Long defectiveWarehouseId,
                                 @Size(max = 64) String carrier, @Size(max = 255) String trackingNo,
                                 @NotEmpty(message = "{ecom.valid.fba.linesRequired}") List<@Valid @NotNull ReceiveLine> lines) {
    }

    /** 手工新增索赔 */
    public record ClaimCreate(@NotNull(message = "{ecom.valid.selectStore}") Long storeId,
                              @NotNull(message = "{ecom.valid.fba.claimTypeRequired}") FbaClaimType claimType,
                              @Size(max = 64) String sourceRef, @Size(max = 64) String sellerSku,
                              @NotNull(message = "{ecom.valid.fba.qtyRequired}") @DecimalMin(value = "0", message = "{ecom.valid.fba.qtyRequired}") BigDecimal qty,
                              BigDecimal unitValue, BigDecimal estAmount, LocalDate eventDate, LocalDate deadline,
                              @Size(max = 255) String remark) {
    }

    /** 索赔处理：改状态、Case 编号、提交日期、估算、备注（已赔付只能由赔偿报告匹配或手工登记赔付金额） */
    public record ClaimUpdate(@NotNull(message = "{ecom.valid.fba.claimStatusRequired}") FbaClaimStatus status,
                              @Size(max = 64) String caseNo, LocalDate submitDate,
                              BigDecimal qty, BigDecimal unitValue, LocalDate deadline,
                              BigDecimal paidAmount, LocalDate paidDate,
                              @Size(max = 255) String remark) {
    }

    /** 货件对账：填写亚马逊实收件数 */
    public record ReceiptSave(@NotNull Long inboundItemId,
                              @NotNull(message = "{ecom.valid.fba.qtyRequired}") @DecimalMin(value = "0", message = "{ecom.valid.fba.qtyRequired}") BigDecimal receivedQty) {
    }

    /** 维护商品尺寸重量（cm / kg） */
    public record DimSave(@NotNull(message = "{ecom.valid.selectStore}") Long storeId,
                          @NotBlank(message = "{ecom.valid.fba.skuRequired}") @Size(max = 64) String sellerSku,
                          @NotNull(message = "{ecom.valid.fba.dimRequired}") @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.fba.dimRequired}") BigDecimal lengthCm,
                          @NotNull(message = "{ecom.valid.fba.dimRequired}") @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.fba.dimRequired}") BigDecimal widthCm,
                          @NotNull(message = "{ecom.valid.fba.dimRequired}") @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.fba.dimRequired}") BigDecimal heightCm,
                          @NotNull(message = "{ecom.valid.fba.weightRequired}") @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.fba.weightRequired}") BigDecimal weightKg) {
    }

    /** 费用核查生成索赔 */
    public record AuditClaims(Long storeId, LocalDate beginDate, LocalDate endDate,
                              @NotEmpty(message = "{ecom.valid.fba.linesRequired}") List<AuditKey> items) {
    }

    /** 费用核查的店铺 + SKU */
    public record AuditKey(@NotNull Long storeId, @NotBlank String sellerSku) {
    }
}
