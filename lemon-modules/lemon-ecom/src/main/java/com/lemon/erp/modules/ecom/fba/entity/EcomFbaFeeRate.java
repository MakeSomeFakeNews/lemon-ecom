package com.lemon.erp.modules.ecom.fba.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * FBA 配送费费率：分段 × 发货重量段（下限不含、上限含），费用 = 固定费 + 超出续重起点的部分每续重单位加收
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_fba_fee_rate")
public class EcomFbaFeeRate extends BaseEntity {

    private String marketplace;
    private String tierCode;
    @NotNull(message = "{ecom.valid.fba.weightRequired}")
    @DecimalMin(value = "0", message = "{ecom.valid.fba.weightRequired}")
    private BigDecimal weightFrom;
    private BigDecimal weightTo;
    @NotNull(message = "{ecom.valid.fba.feeRequired}")
    @DecimalMin(value = "0", message = "{ecom.valid.fba.feeRequired}")
    private BigDecimal fee;
    private BigDecimal extraFrom;
    private BigDecimal extraUnit;
    private BigDecimal extraFee;
    private String currency;
    private Integer builtin;
}
