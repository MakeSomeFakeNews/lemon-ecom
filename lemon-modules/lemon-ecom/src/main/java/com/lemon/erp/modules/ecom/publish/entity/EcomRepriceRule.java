package com.lemon.erp.modules.ecom.publish.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.publish.model.PriceAdjustMode;
import com.lemon.erp.modules.ecom.publish.model.RepriceFloorMode;
import com.lemon.erp.modules.ecom.publish.model.RepriceStrategy;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自动调价规则：适用店铺 / SKU、策略（跟随最低价、低于最低价 X、跟随购物车价、按目标利润率、按库存 / 销量）、
 * 底价（固定或按成本 + 最低利润率推算）与最高价、执行间隔；可设为「仅建议不推送」
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_reprice_rule")
public class EcomRepriceRule extends BaseEntity {

    @NotBlank(message = "{ecom.valid.pub.nameRequired}")
    @Size(max = 64, message = "{ecom.valid.pub.nameTooLong}")
    private String name;

    private EnableStatus status;

    /** 适用店铺 ID（逗号分隔，为空全部） */
    @Size(max = 500, message = "{ecom.valid.pub.filterTooLong}")
    private String storeIds;

    /** 适用平台 SKU（逗号分隔，为空为店铺的全部在售商品） */
    @Size(max = 1000, message = "{ecom.valid.pub.filterTooLong}")
    private String skus;

    @NotNull(message = "{ecom.valid.pub.strategyRequired}")
    private RepriceStrategy strategy;

    /** 低于最低价、按库存的调整方式：AMOUNT 金额 / PERCENT 百分比 */
    private PriceAdjustMode adjustMode;

    @DecimalMin(value = "0", message = "{ecom.valid.pub.amountNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.pub.amountDecimals}")
    private BigDecimal adjustValue;

    @DecimalMin(value = "-50", message = "{ecom.valid.pub.marginRange}")
    @DecimalMax(value = "90", message = "{ecom.valid.pub.marginRange}")
    @Digits(integer = 2, fraction = 2, message = "{ecom.valid.pub.marginRange}")
    private BigDecimal targetMargin;

    @NotNull(message = "{ecom.valid.pub.floorModeRequired}")
    private RepriceFloorMode floorMode;

    @DecimalMin(value = "0", message = "{ecom.valid.pub.amountNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.pub.amountDecimals}")
    private BigDecimal minPrice;

    @DecimalMin(value = "0", message = "{ecom.valid.pub.amountNegative}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.pub.amountDecimals}")
    private BigDecimal maxPrice;

    @DecimalMin(value = "-50", message = "{ecom.valid.pub.marginRange}")
    @DecimalMax(value = "90", message = "{ecom.valid.pub.marginRange}")
    @Digits(integer = 2, fraction = 2, message = "{ecom.valid.pub.marginRange}")
    private BigDecimal minMargin;

    private Long priceTemplateId;

    @Min(value = 1, message = "{ecom.valid.pub.daysRange}")
    @Max(value = 999, message = "{ecom.valid.pub.daysRange}")
    private Integer stockHighDays;

    @Min(value = 0, message = "{ecom.valid.pub.daysRange}")
    @Max(value = 999, message = "{ecom.valid.pub.daysRange}")
    private Integer stockLowDays;

    /** 执行间隔（分钟，15 – 1440） */
    @NotNull(message = "{ecom.valid.pub.intervalRange}")
    @Min(value = 15, message = "{ecom.valid.pub.intervalRange}")
    @Max(value = 1440, message = "{ecom.valid.pub.intervalRange}")
    private Integer intervalMinutes;

    /** 仅建议不推送 1 是 */
    private Integer suggestOnly;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime lastRunTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String lastRunMessage;

    @Size(max = 255, message = "{ecom.valid.pub.remarkTooLong}")
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String priceTemplateName;
    /** 待处理的建议数 */
    @TableField(exist = false)
    private Integer suggestedCount;
}
