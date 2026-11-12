package com.lemon.erp.modules.ecom.analytics.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 促销 / 节假日的预测系数：日期区间内（含首尾）的预测量 × 系数；店铺为空表示全部店铺，多个事件重叠时系数相乘。
 * 计算历史基线时，这些日期的实际销量先 ÷ 系数，避免促销拉高日常预测
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_forecast_event")
public class EcomForecastEvent extends BaseEntity {

    @NotBlank(message = "{ecom.valid.analytics.eventNameRequired}")
    @Size(max = 64, message = "{ecom.valid.analytics.eventNameTooLong}")
    private String name;

    @NotNull(message = "{ecom.valid.analytics.eventDateRequired}")
    private LocalDate startDate;

    @NotNull(message = "{ecom.valid.analytics.eventDateRequired}")
    private LocalDate endDate;

    /** 店铺，为空表示全部店铺 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long storeId;

    @NotNull(message = "{ecom.valid.analytics.factorRequired}")
    @DecimalMin(value = "0.01", message = "{ecom.valid.analytics.factorRange}")
    @DecimalMax(value = "20", message = "{ecom.valid.analytics.factorRange}")
    @Digits(integer = 4, fraction = 4, message = "{ecom.valid.analytics.factorRange}")
    private BigDecimal factor;

    @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}")
    private String remark;

    @TableField(exist = false)
    private String storeName;
}
