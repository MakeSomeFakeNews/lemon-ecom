package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
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
 * 广告组：活动下的广告组，默认竞价为店铺币种
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_ad_group")
public class EcomAdGroup extends BaseEntity {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long storeId;

    @NotNull(message = "{ecom.valid.ads.campaignRequired}")
    private Long campaignId;

    @NotBlank(message = "{ecom.valid.ads.adGroupNameRequired}")
    @Size(max = 128, message = "{ecom.valid.ads.nameTooLong}")
    private String name;

    @Size(max = 64, message = "{ecom.valid.ads.platformIdTooLong}")
    private String platformAdGroupId;

    private EcomAdStatus status;

    @DecimalMin(value = "0", message = "{ecom.valid.ads.amountNegative}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.ads.amountDecimals}")
    private BigDecimal defaultBid;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime syncedTime;

    @TableField(exist = false)
    private String campaignName;
    @TableField(exist = false)
    private Integer targetCount;
}
