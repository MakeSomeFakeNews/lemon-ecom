package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 审单规则：订单写入后按优先级（数字小的先）逐条匹配，条件都满足（为空的条件不限）时执行动作。
 * 多条规则命中时，发货仓、物流渠道、截单时间取第一条设置了它的规则；任何一条要求人工审核即挂起；没有挂起且有规则要求自动审核时自动审核。
 * 多值条件（店铺、平台、国家、SKU、物料）用英文逗号分隔
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_order_rule")
public class EcomOrderRule extends BaseEntity {

    @NotBlank(message = "{ecom.valid.ops.ruleNameRequired}")
    @Size(max = 64, message = "{ecom.valid.ops.ruleNameTooLong}")
    private String name;

    /** 优先级，数字小的先匹配 */
    @NotNull(message = "{ecom.valid.ops.rulePriorityRequired}")
    @Min(value = 0, message = "{ecom.valid.ops.rulePriorityRange}")
    @Max(value = 9999, message = "{ecom.valid.ops.rulePriorityRange}")
    private Integer priority;

    private EnableStatus status;

    // ---------------------------------------------------------------- 条件

    /** 店铺 ID（逗号分隔） */
    @Size(max = 500, message = "{ecom.valid.ops.ruleConditionTooLong}")
    private String storeIds;
    /** 平台（EcomPlatform，逗号分隔） */
    @Size(max = 255, message = "{ecom.valid.ops.ruleConditionTooLong}")
    private String platforms;
    /** 配送方式 */
    private FulfillmentType fulfillment;
    /** 收件国家 / 地区代码（逗号分隔） */
    @Size(max = 500, message = "{ecom.valid.ops.ruleConditionTooLong}")
    private String countries;
    /** 平台 SKU（逗号分隔，订单任一行命中即可） */
    @Size(max = 1000, message = "{ecom.valid.ops.ruleConditionTooLong}")
    private String skus;
    /** 本地物料 ID（逗号分隔，订单任一行命中即可） */
    @Size(max = 500, message = "{ecom.valid.ops.ruleConditionTooLong}")
    private String materialIds;
    /** 订单金额区间（本位币，含两端） */
    @DecimalMin(value = "0", message = "{ecom.valid.ops.ruleRangeNegative}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}")
    private BigDecimal amountMin;
    @DecimalMin(value = "0", message = "{ecom.valid.ops.ruleRangeNegative}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}")
    private BigDecimal amountMax;
    /** 重量区间（kg，含两端；订单没有重量时不命中） */
    @DecimalMin(value = "0", message = "{ecom.valid.ops.ruleRangeNegative}")
    @Digits(integer = 12, fraction = 3, message = "{ecom.valid.weightDecimals}")
    private BigDecimal weightMin;
    @DecimalMin(value = "0", message = "{ecom.valid.ops.ruleRangeNegative}")
    @Digits(integer = 12, fraction = 3, message = "{ecom.valid.weightDecimals}")
    private BigDecimal weightMax;

    // ---------------------------------------------------------------- 动作

    /** 指定发货仓 */
    private Long warehouseId;
    /** 指定物流渠道 */
    private Long channelId;
    /** 标记需人工审核（挂起） 1是 0否 */
    private Integer holdOrder;
    /** 挂起原因 */
    @Size(max = 255, message = "{ecom.valid.ops.holdReasonTooLong}")
    private String holdReason;
    /** 自动审核通过 1是 0否 */
    private Integer autoAudit;
    /** 截单时间：付款（没有付款时间按下单）在此之前当天发货，之后次日发货 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime cutoffTime;
    /** 命中后不再匹配后续规则 1是 0否 */
    private Integer stopOnMatch;

    @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}")
    private String remark;

    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String channelName;
}
