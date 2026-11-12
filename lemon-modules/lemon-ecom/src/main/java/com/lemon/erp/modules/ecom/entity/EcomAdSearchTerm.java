package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 搜索词报告：投放 × 客户搜索词 × 日期。同一店铺、日期范围、活动重新导入时整体替换
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_ad_search_term")
public class EcomAdSearchTerm extends BaseEntity {

    private Long storeId;
    private LocalDate statDate;
    private Long campaignId;
    private Long adGroupId;
    private Long targetId;
    /** 触发的投放（关键词或表达式） */
    private String keywordText;
    private String matchType;
    private String searchTerm;
    private Long impressions;
    private Long clicks;
    private BigDecimal spend;
    private BigDecimal sales;
    private Integer orders;
    private String currency;
}
