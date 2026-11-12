package com.lemon.erp.modules.ecom.automation.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.automation.dto.AutoViews;
import com.lemon.erp.modules.ecom.automation.model.AutoResult;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 自动化执行日志：一条事件对一条规则的处理结果（条件不满足、防抖跳过、达到上限、执行了动作）及每个动作的结果与耗时。
 * 扫描类触发器只记录执行了动作的事件（避免每次扫描都写大量「条件不满足」）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_auto_log")
public class EcomAutoLog extends BaseEntity {

    private Long ruleId;
    private String ruleName;
    /** 触发器（JSON 中为 trigger） */
    @JsonProperty("trigger")
    private AutoTrigger triggerCode;
    /** 对象标识（防抖按它判断），如 ORDER:123 */
    private String objectKey;
    /** 对象名称（订单号、SKU、活动名称等） */
    private String objectLabel;
    private Long storeId;
    private AutoResult result;
    /** 条件判断说明 */
    private String matchDetail;
    private Integer costMs;
    private String errorMsg;
    private Integer retryCount;

    /** 事件（字段值、关联对象，JSON） */
    @JsonIgnore
    private String eventJson;
    /** 动作结果（JSON） */
    @JsonIgnore
    private String actionsJson;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private List<AutoViews.ActionResult> actions;
    @TableField(exist = false)
    private Map<String, Object> fields;
    @TableField(exist = false)
    private String link;
}
