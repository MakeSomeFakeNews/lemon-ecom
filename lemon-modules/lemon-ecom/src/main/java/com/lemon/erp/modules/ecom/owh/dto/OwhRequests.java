package com.lemon.erp.modules.ecom.owh.dto;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 海外仓接口的请求体
 */
public final class OwhRequests {

    private OwhRequests() {
    }

    /**
     * 仓库映射
     *
     * @param warehouseId        系统仓库
     * @param owhCode            服务商仓库代码
     * @param defaultProductCode 默认物流产品代码
     * @param autoPush           审核后自动推送 1是
     */
    public record MappingRequest(
            @NotNull(message = "{ecom.valid.owh.accountRequired}") Long accountId,
            @NotNull(message = "{ecom.valid.owh.warehouseRequired}") Long warehouseId,
            @NotBlank(message = "{ecom.valid.owh.owhCodeRequired}") @Size(max = 64, message = "{ecom.valid.owh.owhCodeTooLong}") String owhCode,
            @Size(max = 128, message = "{ecom.valid.owh.owhNameTooLong}") String owhName,
            @Size(max = 8, message = "{ecom.valid.owh.countryTooLong}") String country,
            @Size(max = 64, message = "{ecom.valid.owh.productCodeTooLong}") String defaultProductCode,
            Integer autoPush,
            EnableStatus status,
            @Size(max = 255, message = "{ecom.valid.owh.remarkTooLong}") String remark,
            Long defectWarehouseId,
            @Size(max = 64, message = "{ecom.valid.owh.contactTooLong}") String contact,
            @Size(max = 32, message = "{ecom.valid.owh.phoneTooLong}") String phone,
            @Size(max = 64, message = "{ecom.valid.owh.stateTooLong}") String state,
            @Size(max = 64, message = "{ecom.valid.owh.cityTooLong}") String city,
            @Size(max = 16, message = "{ecom.valid.owh.postcodeTooLong}") String postcode,
            @Size(max = 255, message = "{ecom.valid.owh.addressTooLong}") String address) {

        /** 不填地址与不良品仓 */
        public MappingRequest(Long accountId, Long warehouseId, String owhCode, String owhName, String country, String defaultProductCode, Integer autoPush,
                              EnableStatus status, String remark) {
            this(accountId, warehouseId, owhCode, owhName, country, defaultProductCode, autoPush, status, remark, null, null, null, null, null, null, null);
        }
    }

    /** SKU 映射：物料 ↔ 服务商 SKU */
    public record SkuRequest(@NotNull(message = "{ecom.valid.owh.materialRequired}") Long materialId,
                             @NotBlank(message = "{ecom.valid.owh.skuRequired}") @Size(max = 64, message = "{ecom.valid.owh.skuTooLong}") String owhSku,
                             @Size(max = 255, message = "{ecom.valid.owh.remarkTooLong}") String remark) {
    }

    /** 从 ERP 退货退款单生成海外仓退件单：退件运单号可在买家寄回后补填 */
    public record ReturnCreateRequest(@NotNull(message = "{ecom.valid.owh.sourceRequired}") Long ecomReturnId,
                                      @Size(max = 64, message = "{ecom.valid.owh.carrierTooLong}") String carrier,
                                      @Size(max = 64, message = "{ecom.valid.owh.trackingTooLong}") String trackingNo,
                                      @Size(max = 255, message = "{ecom.valid.owh.remarkTooLong}") String remark) {
    }

    /**
     * 新建 / 修改海外仓调拨单（草稿）：调出、调入仓至少一个是海外仓且不能相同
     *
     * @param productCode 调出仓是海外仓时的物流产品（为空取映射的默认产品）
     */
    public record TransferRequest(@NotNull(message = "{ecom.valid.owh.fromWarehouseRequired}") Long fromWarehouseId,
                                  @NotNull(message = "{ecom.valid.owh.toWarehouseRequired}") Long toWarehouseId,
                                  @Size(max = 64, message = "{ecom.valid.owh.productCodeTooLong}") String productCode,
                                  @Size(max = 255, message = "{ecom.valid.owh.remarkTooLong}") String remark,
                                  @NotEmpty(message = "{ecom.valid.owh.linesRequired}") @Size(max = 500, message = "{ecom.valid.owh.tooMany}")
                                  List<@Valid @NotNull TransferLine> items) {
    }

    public record TransferLine(@NotNull(message = "{ecom.valid.owh.materialRequired}") Long materialId,
                               @NotNull(message = "{ecom.valid.owh.qtyRequired}") @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.owh.qtyRequired}") BigDecimal qty) {
    }

    /**
     * 确认收货（调入仓是本地仓）
     *
     * @param receiveDate 收货日期，为空取当天
     * @param items       各物料实收数量，为空按调拨数量全部收到；与调拨数量不一致的记为收货差异
     * @param reason      差异原因
     */
    public record ReceiveRequest(LocalDate receiveDate, @Size(max = 500, message = "{ecom.valid.owh.tooMany}") List<@Valid @NotNull ReceiveLine> items,
                                 @Size(max = 255, message = "{ecom.valid.owh.remarkTooLong}") String reason) {

        public ReceiveRequest(LocalDate receiveDate) {
            this(receiveDate, null, null);
        }
    }

    /** 一个物料的实收数量（可为 0） */
    public record ReceiveLine(@NotNull(message = "{ecom.valid.owh.materialRequired}") Long materialId,
                              @NotNull(message = "{ecom.valid.owh.qtyRequired}") @DecimalMin(value = "0", message = "{ecom.valid.owh.qtyRequired}") BigDecimal qty) {
    }

    /**
     * 处理收货差异
     *
     * @param action LOSS 确认损耗（短少）/ FOUND 找回补收（短少）/ ACCEPTED 确认溢收（溢收）
     */
    public record DiffHandleRequest(@NotNull(message = "{ecom.valid.owh.diffActionRequired}") com.lemon.erp.modules.ecom.owh.model.OwhDiffStatus action,
                                    @Size(max = 255, message = "{ecom.valid.owh.remarkTooLong}") String reason) {
    }

    /** 标记已作废（服务商后台已作废）：备注作废原因 */
    public record VoidRequest(@Size(max = 255, message = "{ecom.valid.owh.remarkTooLong}") String reason) {
    }

    /** 物流产品对应的物流渠道（为空取消对应） */
    public record ProductChannelRequest(Long channelId, String carrierName) {
    }

    /** 按 ID 批量操作 */
    public record IdsRequest(@NotEmpty(message = "{ecom.valid.owh.selectRecords}") @Size(max = 500, message = "{ecom.valid.owh.tooMany}") List<Long> ids) {
    }

    /**
     * 按海外仓数量调整 ERP 库存
     *
     * @param ids     库存行
     * @param bizDate 业务日期，为空取当天
     */
    public record AdjustRequest(@NotEmpty(message = "{ecom.valid.owh.selectRecords}") @Size(max = 500, message = "{ecom.valid.owh.tooMany}") List<Long> ids,
                                LocalDate bizDate) {
    }

    /** 从头程货件 / 在途调拨单生成入库单 */
    public record InboundCreateRequest(@NotNull(message = "{ecom.valid.owh.sourceRequired}") OwhInboundSource sourceType,
                                       @NotNull(message = "{ecom.valid.owh.sourceRequired}") Long sourceId) {
    }

    /**
     * 修改草稿入库单：头程信息与装箱（箱 × 物料 × 数量，各物料合计须与来源单据一致）
     */
    public record InboundUpdateRequest(
            @Size(max = 16, message = "{ecom.valid.owh.transportModeTooLong}") String transportMode,
            @Size(max = 64, message = "{ecom.valid.owh.carrierTooLong}") String carrier,
            @Size(max = 64, message = "{ecom.valid.owh.trackingTooLong}") String trackingNo,
            LocalDate etaDate,
            @Size(max = 255, message = "{ecom.valid.owh.remarkTooLong}") String remark,
            @NotEmpty(message = "{ecom.valid.owh.boxesRequired}") @Size(max = 500, message = "{ecom.valid.owh.tooMany}") List<@Valid @NotNull BoxRequest> boxes,
            @NotEmpty(message = "{ecom.valid.owh.linesRequired}") @Size(max = 2000, message = "{ecom.valid.owh.tooMany}") List<@Valid @NotNull LineRequest> lines) {
    }

    /** 一箱的尺寸（cm）与毛重（kg） */
    public record BoxRequest(@NotNull(message = "{ecom.valid.owh.boxNoRequired}") @Min(value = 1, message = "{ecom.valid.owh.boxNoRequired}") Integer boxNo,
                             BigDecimal length, BigDecimal width, BigDecimal height, BigDecimal weight) {
    }

    /** 箱内一个物料 */
    public record LineRequest(@NotNull(message = "{ecom.valid.owh.boxNoRequired}") @Min(value = 1, message = "{ecom.valid.owh.boxNoRequired}") Integer boxNo,
                              @NotNull(message = "{ecom.valid.owh.materialRequired}") Long materialId,
                              @NotNull(message = "{ecom.valid.owh.qtyRequired}") @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.owh.qtyRequired}") BigDecimal qty) {
    }

    /**
     * 推送海外仓
     *
     * @param ids         包裹（推送）或推送失败的出库单所在的包裹（重推）
     * @param productCode 物流产品代码；为空时按 包裹物流渠道对应的产品 → 仓库映射的默认产品 取
     */
    public record PushRequest(@NotEmpty(message = "{ecom.valid.owh.selectRecords}") @Size(max = 500, message = "{ecom.valid.owh.tooMany}") List<Long> ids,
                              @Size(max = 64, message = "{ecom.valid.owh.productCodeTooLong}") String productCode) {
    }

    /**
     * 取消 / 截单
     *
     * @param cancelPackage 取消成功后同时取消包裹（订单回到待审核）
     */
    public record CancelRequest(@Size(max = 255, message = "{ecom.valid.owh.remarkTooLong}") String reason, Boolean cancelPackage) {
    }

    /** 同步费用：账号为空时同步全部启用账号；日期为空取最近 7 天 */
    public record FeeSyncRequest(Long accountId, LocalDate beginDate, LocalDate endDate) {
    }
}
