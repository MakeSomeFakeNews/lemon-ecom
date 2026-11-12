package com.lemon.erp.modules.ecom.assistant.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.assistant.model.OpsReportAiStatus;
import com.lemon.erp.modules.ecom.assistant.model.OpsReportSource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 运营日报：同一天同一店铺范围一份。列表字段（指标、计数、摘要）单独成列，结构化内容（JSON）与 AI 建议只在详情中返回
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_ops_report")
public class EcomOpsReport extends BaseEntity {

    private LocalDate reportDate;
    private String scopeKey;
    private String scopeName;
    private Integer storeCount;
    private OpsReportSource source;
    private Integer orders;
    private BigDecimal sales;
    private BigDecimal profit;
    /** 销售额环比（%） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal salesChange;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal profitChange;
    private Integer abnormalCount;
    private Integer badReviewCount;
    private Integer stockRiskCount;
    private Integer adAlertCount;
    private Integer todoCount;
    private String summary;
    private String content;
    private OpsReportAiStatus aiStatus;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String aiAdvice;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String aiError;
    private Integer aiSample;
    private Integer pushCount;
    private LocalDateTime pushedTime;
    private Long createBy;

    @TableField(exist = false)
    private String createByName;
}
