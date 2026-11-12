package com.lemon.erp.modules.ecom.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 销量预测参数（每个租户一行，没有时取默认值）：Holt 的 α / β、移动平均窗口天数、星期系数取最近几周，
 * 以及断货日的处理（补货建议的加权日均也按它排除断货日）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_forecast_setting")
public class EcomForecastSetting extends BaseEntity {

    @NotNull(message = "{ecom.valid.analytics.alphaRange}")
    @DecimalMin(value = "0.01", message = "{ecom.valid.analytics.alphaRange}")
    @DecimalMax(value = "1", message = "{ecom.valid.analytics.alphaRange}")
    private BigDecimal alpha;

    @NotNull(message = "{ecom.valid.analytics.betaRange}")
    @DecimalMin(value = "0", message = "{ecom.valid.analytics.betaRange}")
    @DecimalMax(value = "1", message = "{ecom.valid.analytics.betaRange}")
    private BigDecimal beta;

    @NotNull(message = "{ecom.valid.analytics.windowRange}")
    @Min(value = 7, message = "{ecom.valid.analytics.windowRange}")
    @Max(value = 90, message = "{ecom.valid.analytics.windowRange}")
    private Integer windowDays;

    @NotNull(message = "{ecom.valid.analytics.seasonWeeksRange}")
    @Min(value = 2, message = "{ecom.valid.analytics.seasonWeeksRange}")
    @Max(value = 16, message = "{ecom.valid.analytics.seasonWeeksRange}")
    private Integer seasonWeeks;

    /** 排除（补偿）断货日：预测时断货日的销量按前后正常日的日均补偿，补货加权日均不计断货日；为空按开启 */
    private Boolean excludeStockout;

    /** 断货阈值：当天快照的可售数不大于它记为断货日（默认 0） */
    @DecimalMin(value = "0", message = "{ecom.valid.fix.stockoutThreshold}")
    @DecimalMax(value = "100000", message = "{ecom.valid.fix.stockoutThreshold}")
    private BigDecimal stockoutThreshold;
}
