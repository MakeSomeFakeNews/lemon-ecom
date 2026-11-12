package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.model.EcomAdAction;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.model.EcomAdMetric;
import com.lemon.erp.modules.ecom.model.EcomAdOperator;
import com.lemon.erp.modules.ecom.model.EcomAdRunMode;
import com.lemon.erp.modules.ecom.model.EcomAdType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 广告规则：对象（层级 + 店铺 / 活动名称包含 / 标签 / 广告类型筛选）在统计窗口（近 N 天，截至昨天）内的指标满足全部条件时，
 * 执行动作（调整竞价 / 预算、暂停 / 启用、仅提醒）。执行方式：手动预览执行，或每天在设定的整点执行；
 * 自动执行时通过广告接口修改，否则生成待处理的建议
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_ad_rule")
public class EcomAdRule extends BaseEntity {

    @NotBlank(message = "{ecom.valid.ads.ruleNameRequired}")
    @Size(max = 64, message = "{ecom.valid.ads.ruleNameTooLong}")
    private String name;

    private EnableStatus status;

    @NotNull(message = "{ecom.valid.ads.levelRequired}")
    private EcomAdLevel level;

    /** 店铺 ID（逗号分隔，为空不限） */
    @Size(max = 500, message = "{ecom.valid.ads.filterTooLong}")
    private String storeIds;

    /** 活动名称包含 */
    @Size(max = 128, message = "{ecom.valid.ads.filterTooLong}")
    private String campaignKeyword;

    /** 活动标签（逗号分隔，任一匹配） */
    @Size(max = 255, message = "{ecom.valid.ads.filterTooLong}")
    private String tags;

    private EcomAdType adType;

    /** 统计窗口：近 N 天（截至昨天） */
    @NotNull(message = "{ecom.valid.ads.windowRange}")
    @Min(value = 1, message = "{ecom.valid.ads.windowRange}")
    @Max(value = 90, message = "{ecom.valid.ads.windowRange}")
    private Integer windowDays;

    /** 条件（全部满足），存库为 JSON */
    @TableField(exist = false)
    @Valid
    @NotEmpty(message = "{ecom.valid.ads.conditionRequired}")
    @Size(max = 10, message = "{ecom.valid.ads.tooManyConditions}")
    private List<Condition> conditions = new ArrayList<>();

    @JsonIgnore
    private String conditionsJson;

    @NotNull(message = "{ecom.valid.ads.actionRequired}")
    private EcomAdAction action;

    /** 动作的值：百分比调整为 ±%，固定值为竞价 / 预算 */
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.ads.amountDecimals}")
    private BigDecimal actionValue;

    /** 调整后的下限 / 上限（竞价或预算，为空不限） */
    @DecimalMin(value = "0", message = "{ecom.valid.ads.amountNegative}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.ads.amountDecimals}")
    private BigDecimal minValue;

    @DecimalMin(value = "0", message = "{ecom.valid.ads.amountNegative}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.ads.amountDecimals}")
    private BigDecimal maxValue;

    @NotNull(message = "{ecom.valid.ads.runModeRequired}")
    private EcomAdRunMode runMode;

    /** 每天执行的整点（0–23，服务器时间） */
    @Min(value = 0, message = "{ecom.valid.ads.hourRange}")
    @Max(value = 23, message = "{ecom.valid.ads.hourRange}")
    private Integer runHour;

    /** 自动执行 1 是 0 否（否则只生成建议） */
    private Integer autoApply;

    /** 负责人（接收执行提醒），为空时发给店铺负责人 */
    private Long ownerId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime lastRunTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String lastRunMessage;

    @Size(max = 255, message = "{ecom.valid.ads.remarkTooLong}")
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String ownerName;
    /** 待处理的建议数 */
    @TableField(exist = false)
    private Integer pendingCount;

    /** 一个条件：指标 运算 值（比率类为百分数，如 ACOS 30 表示 30%） */
    @Data
    public static class Condition {
        @NotNull(message = "{ecom.valid.ads.metricRequired}")
        private EcomAdMetric metric;
        @NotNull(message = "{ecom.valid.ads.operatorRequired}")
        private EcomAdOperator operator;
        @NotNull(message = "{ecom.valid.ads.conditionValueRequired}")
        @Digits(integer = 14, fraction = 4, message = "{ecom.valid.ads.amountDecimals}")
        private BigDecimal value;
    }
}
