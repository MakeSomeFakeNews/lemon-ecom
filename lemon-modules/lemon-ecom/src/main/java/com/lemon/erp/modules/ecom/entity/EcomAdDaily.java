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

    @TableField(exist = false)
    private String storeName;
}
