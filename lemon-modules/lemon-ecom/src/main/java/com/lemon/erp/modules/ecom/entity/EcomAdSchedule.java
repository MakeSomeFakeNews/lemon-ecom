package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分时策略（活动级）：星期（周一 = 0）× 小时（0–23）的竞价系数表，每小时由定时任务按店铺时区的当前时段
 * 把活动下启用投放的竞价设为 基准竞价 × 系数（限制在最低 / 最高竞价之间），通过广告接口修改
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_ad_schedule")
public class EcomAdSchedule extends BaseEntity {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long storeId;

    /** 广告活动（按接口路径中的活动保存） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long campaignId;

    private EnableStatus status;

    /** 7 × 24 系数表（每个 0.1～3，两位小数），存库为 JSON */
    @TableField(exist = false)
    @NotNull(message = "{ecom.valid.ads.factorsInvalid}")
    @Size(min = 7, max = 7, message = "{ecom.valid.ads.factorsInvalid}")
    private List<List<BigDecimal>> factors;

    @JsonIgnore
    private String factorsJson;

    @DecimalMin(value = "0", message = "{ecom.valid.ads.amountNegative}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.ads.amountDecimals}")
    private BigDecimal minBid;

    @DecimalMin(value = "0", message = "{ecom.valid.ads.amountNegative}")
    @Digits(integer = 16, fraction = 2, message = "{ecom.valid.ads.amountDecimals}")
    private BigDecimal maxBid;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime lastAppliedTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private BigDecimal lastFactor;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String lastMessage;

    @Size(max = 255, message = "{ecom.valid.ads.remarkTooLong}")
    private String remark;

    @TableField(exist = false)
    private String campaignName;
    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String timezone;
}
