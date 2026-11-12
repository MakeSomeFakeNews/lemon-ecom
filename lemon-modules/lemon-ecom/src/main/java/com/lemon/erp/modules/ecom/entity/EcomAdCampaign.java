package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 广告活动：店铺下的一个广告活动（亚马逊 Campaign）。来源：广告接口同步（有平台活动 ID）、报告导入（按名称自动建立）或手工维护。
 * 日预算为店铺币种
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_ad_campaign")
public class EcomAdCampaign extends BaseEntity {

    /** 店铺（新建时必填，修改时不能更换） */
    private Long storeId;

    private EcomAdType adType;

    @NotBlank(message = "{ecom.valid.ads.campaignNameRequired}")
    @Size(max = 128, message = "{ecom.valid.ads.nameTooLong}")
    private String name;

    @Size(max = 64, message = "{ecom.valid.ads.platformIdTooLong}")
    private String platformCampaignId;

    private EcomAdStatus status;

    @DecimalMin(value = "0", message = "{ecom.valid.ads.amountNegative}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.ads.amountDecimals}")
    private BigDecimal dailyBudget;

    /** 竞价策略（平台取值，如 LEGACY_FOR_SALES、AUTO_FOR_SALES、MANUAL） */
    @Size(max = 32, message = "{ecom.valid.ads.strategyTooLong}")
    private String biddingStrategy;

    /** 投放方式 AUTO / MANUAL（亚马逊） */
    @Size(max = 16, message = "{ecom.valid.ads.strategyTooLong}")
    private String targetingType;

    private LocalDate startDate;
    private LocalDate endDate;

    /** 标签（逗号分隔），广告规则可按标签筛选对象 */
    @Size(max = 255, message = "{ecom.valid.ads.tagsTooLong}")
    private String tags;

    @Size(max = 255, message = "{ecom.valid.ads.remarkTooLong}")
    private String remark;

    /** 最近一次从广告接口同步的时间 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime syncedTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String currency;
    @TableField(exist = false)
    private Integer adGroupCount;
    @TableField(exist = false)
    private Integer targetCount;
    /** 是否设置了启用的分时策略 */
    @TableField(exist = false)
    private Boolean hasSchedule;
}
