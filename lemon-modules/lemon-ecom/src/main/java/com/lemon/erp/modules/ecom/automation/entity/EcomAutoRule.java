package com.lemon.erp.modules.ecom.automation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.model.AutoResult;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 自动化规则：当（触发器）发生、对象在范围内且满足条件时，按顺序执行动作。
 * 同一规则同一对象在防抖时间内只执行一次，每天最多执行 dailyLimit 次（0 不限）；动作以规则创建人的身份执行
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_auto_rule")
public class EcomAutoRule extends BaseEntity {

    @NotBlank(message = "{ecom.valid.auto.nameRequired}")
    @Size(max = 64, message = "{ecom.valid.auto.nameTooLong}")
    private String name;

    @NotNull(message = "{ecom.valid.auto.triggerRequired}")
    /** 触发器（JSON 中为 trigger） */
    @JsonProperty("trigger")
    private AutoTrigger triggerCode;

    private EnableStatus status;

    /** 对象范围（存库为 JSON） */
    @Valid
    @TableField(exist = false)
    private AutoRuleParts.Scope scope;

    /** 条件（存库为 JSON） */
    @Valid
    @TableField(exist = false)
    private AutoRuleParts.Group condition;

    /** 动作（按顺序执行，存库为 JSON） */
    @Valid
    @NotEmpty(message = "{ecom.valid.auto.actionRequired}")
    @Size(max = 10, message = "{ecom.valid.auto.tooManyActions}")
    @TableField(exist = false)
    private List<AutoRuleParts.Action> actions = new ArrayList<>();

    @JsonIgnore
    private String scopeJson;
    @JsonIgnore
    private String conditionJson;
    @JsonIgnore
    private String actionsJson;

    /** 防抖（小时）：同一对象在这段时间内只执行一次，0 不防抖 */
    @Min(value = 0, message = "{ecom.valid.auto.debounceRange}")
    @Max(value = 720, message = "{ecom.valid.auto.debounceRange}")
    private Integer debounceHours;

    /** 每日执行上限，0 不限 */
    @Min(value = 0, message = "{ecom.valid.auto.limitRange}")
    @Max(value = 10000, message = "{ecom.valid.auto.limitRange}")
    private Integer dailyLimit;

    /** 来源模板 */
    @Size(max = 32)
    private String templateCode;

    /** 执行次数（执行了动作的次数） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer triggerCount;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTriggerTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private AutoResult lastResult;

    @Size(max = 255, message = "{ecom.valid.auto.remarkTooLong}")
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String createByName;

    /** 动作中指定用户的昵称（编辑页回显） */
    @TableField(exist = false)
    private java.util.Map<Long, String> userNames;

    /** 近 7 天执行失败的次数（列表显示） */
    @TableField(exist = false)
    private Integer failedCount;
}
