package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomAdAction;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.model.EcomAdLogSource;
import com.lemon.erp.modules.ecom.model.EcomAdLogStatus;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 广告执行日志 / 建议：规则执行时每个命中的对象一行（待处理、已执行、失败、仅提醒），页面手工修改竞价 / 预算 / 状态也记一行
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_ad_rule_log")
public class EcomAdRuleLog extends BaseEntity {

    private Long ruleId;
    private EcomAdLogSource source;
    private LocalDateTime runTime;
    private Long storeId;
    private EcomAdLevel level;
    private Long entityId;
    private Long campaignId;
    private String entityName;
    private EcomAdAction action;
    private BigDecimal oldValue;
    private BigDecimal newValue;
    private EcomAdStatus oldStatus;
    private EcomAdStatus newStatus;
    /** 命中时的指标快照（JSON） */
    private String metrics;
    private EcomAdLogStatus status;
    private String message;
    private LocalDateTime appliedTime;
    private Long appliedBy;

    @TableField(exist = false)
    private String ruleName;
    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String campaignName;
    @TableField(exist = false)
    private String appliedByName;
}
