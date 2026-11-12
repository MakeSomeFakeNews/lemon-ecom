package com.lemon.erp.modules.ecom.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.analytics.model.ForecastMethod;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SKU 的预测汇总：所选方法（手工指定优先，否则回测 MAPE 最小）、各方法的回测 MAPE（%）、最近 28 天日均、未来 30 / 60 / 90 天合计
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_forecast_sku")
public class EcomForecastSku extends BaseEntity {

    private Long listingId;
    private Long storeId;
    /** 使用的方法 */
    private ForecastMethod method;
    /** 手工指定的方法，为空按回测误差自动选择 */
    private ForecastMethod fixedMethod;
    private BigDecimal mapeMa;
    private BigDecimal mapeWma;
    private BigDecimal mapeHolt;
    private BigDecimal mapeSeasonal;
    /** 最近 28 天实际日均 */
    private BigDecimal historyDaily;
    private BigDecimal forecast30;
    private BigDecimal forecast60;
    private BigDecimal forecast90;
    /** 回测的日误差标准差（预测区间用） */
    private BigDecimal sigma;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedTime;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private EcomPlatform platform;
    @TableField(exist = false)
    private String sellerSku;
    @TableField(exist = false)
    private String title;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
}
