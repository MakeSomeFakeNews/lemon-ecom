package com.lemon.erp.modules.ecom.dev.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 平台佣金费率：平台 × 站点（空为全部站点）× 类目（空为默认），利润试算按「站点 + 类目 → 站点 → 全部站点 + 类目 → 全部站点」匹配
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_commission_rate")
public class EcomCommissionRate extends BaseEntity {

    @NotNull(message = "{ecom.valid.dev.platformRequired}")
    private EcomPlatform platform;
    @Size(max = 16, message = "{ecom.valid.dev.marketplaceTooLong}")
    private String marketplace;
    @Size(max = 64, message = "{ecom.valid.dev.categoryTooLong}")
    private String category;
    @NotNull(message = "{ecom.valid.dev.rateRequired}")
    @DecimalMin(value = "0", message = "{ecom.valid.dev.rateRange}")
    @DecimalMax(value = "100", message = "{ecom.valid.dev.rateRange}")
    private BigDecimal rate;
    private BigDecimal minFee;
    private BigDecimal fixedFee;
    @Size(max = 8, message = "{ecom.valid.dev.currencyTooLong}")
    private String currency;
    @DecimalMin(value = "0", message = "{ecom.valid.dev.rateRange}")
    @DecimalMax(value = "100", message = "{ecom.valid.dev.rateRange}")
    private BigDecimal vatRate;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer builtin;
    private EnableStatus status;
    @Size(max = 255, message = "{ecom.valid.dev.remarkTooLong}")
    private String remark;
}
