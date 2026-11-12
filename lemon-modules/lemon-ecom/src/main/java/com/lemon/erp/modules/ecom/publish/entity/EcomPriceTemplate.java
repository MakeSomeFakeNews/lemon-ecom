package com.lemon.erp.modules.ecom.publish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 价格模板（定价公式）：售价 = ((成本 + 头程 + 其他成本) ÷ 汇率 + 固定费) ÷ (1 − 佣金率 − 其他费率 − 目标利润率)，再按尾数取整。
 * 成本为物料参考成本 × 多件装（本位币），汇率为店铺币种当月汇率；见 {@code PriceCalculator}
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_price_template")
public class EcomPriceTemplate extends BaseEntity {

    @NotBlank(message = "{ecom.valid.pub.nameRequired}")
    @Size(max = 64, message = "{ecom.valid.pub.nameTooLong}")
    private String name;

    /** 适用平台，为空通用 */
    private EcomPlatform platform;

    /** 头程运费（本位币 / 件） */
    @NotNull(message = "{ecom.valid.pub.amountRequired}")
    @DecimalMin(value = "0", message = "{ecom.valid.pub.amountNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.pub.amountDecimals}")
    private BigDecimal firstLegCost;

    /** 包材等其他成本（本位币 / 件） */
    @NotNull(message = "{ecom.valid.pub.amountRequired}")
    @DecimalMin(value = "0", message = "{ecom.valid.pub.amountNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.pub.amountDecimals}")
    private BigDecimal otherCost;

    /** 平台佣金率（%） */
    @NotNull(message = "{ecom.valid.pub.rateRequired}")
    @DecimalMin(value = "0", message = "{ecom.valid.pub.rateRange}")
    @DecimalMax(value = "99", message = "{ecom.valid.pub.rateRange}")
    @Digits(integer = 2, fraction = 2, message = "{ecom.valid.pub.rateRange}")
    private BigDecimal commissionRate;

    /** 其他费率（广告、退货损耗等，%） */
    @NotNull(message = "{ecom.valid.pub.rateRequired}")
    @DecimalMin(value = "0", message = "{ecom.valid.pub.rateRange}")
    @DecimalMax(value = "99", message = "{ecom.valid.pub.rateRange}")
    @Digits(integer = 2, fraction = 2, message = "{ecom.valid.pub.rateRange}")
    private BigDecimal otherRate;

    /** 固定费（店铺币种 / 件） */
    @NotNull(message = "{ecom.valid.pub.amountRequired}")
    @DecimalMin(value = "0", message = "{ecom.valid.pub.amountNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.pub.amountDecimals}")
    private BigDecimal fixedFee;

    /** 目标利润率（%，利润 ÷ 售价） */
    @NotNull(message = "{ecom.valid.pub.rateRequired}")
    @DecimalMin(value = "-50", message = "{ecom.valid.pub.marginRange}")
    @DecimalMax(value = "90", message = "{ecom.valid.pub.marginRange}")
    @Digits(integer = 2, fraction = 2, message = "{ecom.valid.pub.marginRange}")
    private BigDecimal targetMargin;

    /** 尾数（0 ≤ 尾数 &lt; 1，如 0.99），为空保留两位小数 */
    @DecimalMin(value = "0", message = "{ecom.valid.pub.tailRange}")
    @DecimalMax(value = "0.99", message = "{ecom.valid.pub.tailRange}")
    @Digits(integer = 1, fraction = 2, message = "{ecom.valid.pub.tailRange}")
    private BigDecimal priceTail;

    /** 最低售价（店铺币种），为空不限 */
    @DecimalMin(value = "0", message = "{ecom.valid.pub.amountNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.pub.amountDecimals}")
    private BigDecimal minPrice;

    private EnableStatus status;

    @Size(max = 255, message = "{ecom.valid.pub.remarkTooLong}")
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;
}
