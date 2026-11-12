package com.lemon.erp.modules.ecom.oper.dto;

import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.oper.model.EcomMonitorAlertStatus;
import com.lemon.erp.modules.ecom.oper.model.EcomOpsTargetType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 运营监控与团队的请求体
 */
public final class OperRequests {

    private OperRequests() {
    }

    /** 手工录入一天的监控快照（为空的指标表示没有取到，比较时沿用前一次） */
    public record SnapshotRequest(
            @NotNull(message = "{ecom.valid.oper.snapshotDateRequired}") LocalDate snapshotDate,
            @Size(max = 128) String category,
            @Min(value = 1, message = "{ecom.valid.oper.rankInvalid}") Integer rankMain,
            @Size(max = 128) String subCategory,
            @Min(value = 1, message = "{ecom.valid.oper.rankInvalid}") Integer rankSub,
            @DecimalMin(value = "0", message = "{ecom.valid.oper.ratingInvalid}") @DecimalMax(value = "5", message = "{ecom.valid.oper.ratingInvalid}") BigDecimal rating,
            @Min(value = 0, message = "{ecom.valid.oper.qtyInvalid}") Integer reviewCount,
            @Min(value = 0, message = "{ecom.valid.oper.qtyInvalid}") Integer sellerCount,
            @DecimalMin(value = "0", message = "{ecom.valid.oper.amountInvalid}") BigDecimal price,
            @Size(max = 500, message = "{ecom.valid.oper.titleTooLong}") String title,
            @Size(max = 500, message = "{ecom.valid.oper.imageTooLong}") String mainImage,
            @Size(max = 4000, message = "{ecom.valid.oper.bulletsTooLong}") String bullets,
            ListingStatus listingStatus) {
    }

    /** 处理预警：已处理 / 已忽略 */
    public record AlertHandleRequest(@NotEmpty(message = "{ecom.valid.oper.alertRequired}") List<Long> ids,
                                     @NotNull(message = "{ecom.valid.oper.alertStatusRequired}") EcomMonitorAlertStatus status,
                                     @Size(max = 255, message = "{ecom.valid.oper.remarkTooLong}") String note) {
    }

    /** 店铺授权的一个成员 */
    public record Member(@NotNull(message = "{ecom.valid.oper.userRequired}") Long userId,
                         @NotBlank(message = "{ecom.valid.oper.memberRoleRequired}") @Size(max = 16) String memberRole,
                         @Size(max = 255, message = "{ecom.valid.oper.remarkTooLong}") String remark) {
    }

    /** 保存店铺的授权成员（整体替换） */
    public record MembersRequest(@Valid List<Member> members) {
    }

    /** 按用户授权店铺（整体替换该用户被授权的店铺） */
    public record UserStoresRequest(@NotNull(message = "{ecom.valid.oper.userRequired}") Long userId,
                                    List<Long> storeIds,
                                    @NotBlank(message = "{ecom.valid.oper.memberRoleRequired}") @Size(max = 16) String memberRole) {
    }

    /** 一个对象的月度目标 */
    public record TargetRow(@NotNull(message = "{ecom.valid.oper.targetRefRequired}") Long refId,
                            @DecimalMin(value = "0", message = "{ecom.valid.oper.amountInvalid}") BigDecimal salesTarget,
                            BigDecimal profitTarget,
                            @Min(value = 0, message = "{ecom.valid.oper.qtyInvalid}") Integer orderTarget,
                            @Size(max = 255, message = "{ecom.valid.oper.remarkTooLong}") String remark) {
    }

    /** 保存一个月的目标（只保存提交的对象，三项都为 0 / 空的删除） */
    public record TargetSaveRequest(
            @NotBlank(message = "{ecom.valid.oper.monthRequired}") @Pattern(regexp = "\\d{4}-\\d{2}", message = "{ecom.valid.oper.monthRequired}") String month,
            @NotNull(message = "{ecom.valid.oper.targetTypeRequired}") EcomOpsTargetType targetType,
            @Valid List<TargetRow> rows) {
    }

    /** 复制上月目标到本月（已设置的不覆盖） */
    public record TargetCopyRequest(
            @NotBlank(message = "{ecom.valid.oper.monthRequired}") @Pattern(regexp = "\\d{4}-\\d{2}", message = "{ecom.valid.oper.monthRequired}") String month,
            @NotNull(message = "{ecom.valid.oper.targetTypeRequired}") EcomOpsTargetType targetType) {
    }
}
