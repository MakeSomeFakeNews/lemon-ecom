package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 物流渠道费率：目的国（* 为通用）× 重量段，运费 = 固定费 + 每公斤费 × 计费重量（本位币）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_logistics_rate")
public class EcomLogisticsRate extends BaseEntity {

    private Long channelId;

    /** 目的国家 / 地区代码，* 为其他国家通用 */
    @NotBlank(message = "{ecom.valid.countryRequired}")
    @Size(max = 8, message = "{ecom.valid.countryCannotExceedCharacters}")
    private String country;

    /** 重量段下限（kg，含） */
    @NotNull(message = "{ecom.valid.enterWeight}")
    @DecimalMin(value = "0", message = "{ecom.valid.weightCannotNegative}")
    @Digits(integer = 12, fraction = 3, message = "{ecom.valid.weightDecimals}")
    private BigDecimal minWeight;

    /** 重量段上限（kg，不含），为空不限 */
    @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.weightCannotNegative}")
    @Digits(integer = 12, fraction = 3, message = "{ecom.valid.weightDecimals}")
    private BigDecimal maxWeight;

    /** 固定费用（本位币） */
    @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}")
    private BigDecimal baseFee;

    /** 每公斤费用（本位币） */
    @DecimalMin(value = "0", message = "{ecom.valid.feeCannotNegative}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.amountDecimals}")
    private BigDecimal perKg;

    @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}")
    private String remark;

    @TableField(exist = false)
    private String channelCode;
    @TableField(exist = false)
    private String channelName;
    @TableField(exist = false)
    private String provider;
    @TableField(exist = false)
    private String transportMode;
    @TableField(exist = false)
    private Integer daysMin;
    @TableField(exist = false)
    private Integer daysMax;
}
