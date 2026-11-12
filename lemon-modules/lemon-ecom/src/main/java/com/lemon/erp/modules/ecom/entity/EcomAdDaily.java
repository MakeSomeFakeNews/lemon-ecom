package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 广告日报：店铺 × 日期 × 广告活动 × 平台 SKU 的曝光、点击、花费、广告销售额与订单数（店铺币种），
 * 由广告报告导入，用于 ACOS、点击率、单次点击成本分析和 SKU 利润的广告分摊
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_ad_daily")
public class EcomAdDaily extends BaseEntity {

    private Long storeId;
    private LocalDate adDate;
    private String campaign;
    private String sellerSku;
    private String platformItemId;
    private Long impressions;
    private Long clicks;
    private BigDecimal spend;
    private String currency;
    private BigDecimal adSales;
    private Integer adOrders;

    /** 广告活动（ecom_ad_campaign），导入时按店铺 + 活动名称关联，没有时自动建立 */
    private Long campaignId;
    /** 广告组（推广商品报告带广告组时关联；同一 SKU 在多个广告组时合并为一行，广告组为空） */
    private Long adGroupId;
    /** 粒度：PRODUCT 活动 × SKU（默认）、CAMPAIGN 只有活动级数据（导入推广商品数据后删除） */
    private com.lemon.erp.modules.ecom.model.EcomAdDailyLevel adLevel;

    @TableField(exist = false)
    private String storeName;
}
