package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomAdMatchType;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 投放：广告组下的关键词 / 商品投放 / 自动投放 / 否定关键词。
 * 竞价 bid 是当前在平台上生效的竞价；基准竞价 base_bid 是用户（或规则）设定的竞价，分时调价按 基准竞价 × 当前时段系数 计算 bid，
 * 不在 bid 上累乘；没有分时策略时两者相同
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_ad_target")
public class EcomAdTarget extends BaseEntity {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long storeId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long campaignId;

    @NotNull(message = "{ecom.valid.ads.adGroupRequired}")
    private Long adGroupId;

    @NotNull(message = "{ecom.valid.ads.targetTypeRequired}")
    private EcomAdTargetType targetType;

    @NotBlank(message = "{ecom.valid.ads.targetTextRequired}")
    @Size(max = 255, message = "{ecom.valid.ads.targetTextTooLong}")
    private String targetText;

    private EcomAdMatchType matchType;

    @Size(max = 64, message = "{ecom.valid.ads.platformIdTooLong}")
    private String platformTargetId;

    private EcomAdStatus status;

    @DecimalMin(value = "0", message = "{ecom.valid.ads.amountNegative}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.ads.amountDecimals}")
    private BigDecimal bid;

    /** 基准竞价（分时调价、规则按它计算） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal baseBid;

    /** 当前生效的分时系数（没有分时策略为空） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal scheduleFactor;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime syncedTime;

    @TableField(exist = false)
    private String campaignName;
    @TableField(exist = false)
    private String adGroupName;
}
