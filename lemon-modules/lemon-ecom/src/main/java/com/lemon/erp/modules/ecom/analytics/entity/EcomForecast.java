package com.lemon.erp.modules.ecom.analytics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.modules.ecom.analytics.model.ForecastMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每个 SKU 每天的预测销量（平台 SKU 件数，只存所选方法的结果）：定时任务每天从今天起生成 90 天
 */
@Data
@TableName("ecom_forecast")
public class EcomForecast {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long listingId;
    private Long storeId;
    private LocalDate forecastDate;
    private BigDecimal qty;
    private ForecastMethod method;
    private LocalDateTime generatedTime;
}
