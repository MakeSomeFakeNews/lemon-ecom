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
 * 销量预测参数（每个租户一行，没有时取默认值）：Holt 的 α / β、移动平均窗口天数、星期系数取最近几周
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
}
