package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 广告结构的日统计：活动报告写 CAMPAIGN 级、投放报告写 TARGET 级（店铺 × 日期 × 层级 × 对象唯一，重复导入覆盖）。
 * 只用于层级报表与广告规则；利润分析的广告花费仍取 ecom_ad_daily
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_ad_stat")
public class EcomAdStat extends BaseEntity {

    private Long storeId;
    private LocalDate statDate;
    private EcomAdLevel level;
    private Long entityId;
    private Long campaignId;
    private Long adGroupId;
    private Long impressions;
    private Long clicks;
    private BigDecimal spend;
    private BigDecimal sales;
    private Integer orders;
    private Integer units;
    private String currency;
}
