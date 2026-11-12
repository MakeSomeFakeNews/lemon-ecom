package com.lemon.erp.modules.ecom.oper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.oper.model.EcomPromoPhase;
import com.lemon.erp.modules.ecom.oper.model.EcomPromoPushStatus;
import com.lemon.erp.modules.ecom.oper.model.EcomPromoStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 促销活动：草稿可修改，生效后计入促销日历和效果分析，平台支持时可推送（虾皮折扣活动、TikTok 促销），其余平台手工维护。
 * 活动费用（秒杀费、优惠券费等）按开始日所在月汇率折算本位币，效果分析中从活动期利润扣除
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_promotion")
public class EcomPromotion extends BaseEntity {

    @NotBlank(message = "{ecom.valid.oper.promoNameRequired}")
    @Size(max = 128, message = "{ecom.valid.oper.promoNameTooLong}")
    private String name;
    @NotNull(message = "{ecom.valid.selectStore}")
    private Long storeId;
    @NotBlank(message = "{ecom.valid.oper.promoTypeRequired}")
    @Size(max = 32)
    private String promoType;
    @NotNull(message = "{ecom.valid.oper.promoTimeRequired}")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @NotNull(message = "{ecom.valid.oper.promoTimeRequired}")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    @DecimalMin(value = "0", message = "{ecom.valid.oper.discountInvalid}")
    @DecimalMax(value = "100", message = "{ecom.valid.oper.discountInvalid}")
    private BigDecimal discountPercent;
    @DecimalMin(value = "0", message = "{ecom.valid.oper.amountInvalid}")
    private BigDecimal thresholdAmount;
    @DecimalMin(value = "0", message = "{ecom.valid.oper.amountInvalid}")
    private BigDecimal reduceAmount;
    private String currency;
    @DecimalMin(value = "0", message = "{ecom.valid.oper.amountInvalid}")
    private BigDecimal feeAmount;
    private BigDecimal feeBase;
    @DecimalMin(value = "0", message = "{ecom.valid.oper.amountInvalid}")
    private BigDecimal budget;
    private EcomPromoStatus status;
    private EcomPromoPushStatus pushStatus;
    private String platformPromoId;
    private String pushMessage;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pushTime;
    private Long ownerId;
    @Size(max = 255, message = "{ecom.valid.oper.remarkTooLong}")
    private String remark;
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private EcomPlatform platform;
    @TableField(exist = false)
    private String marketplace;
    @TableField(exist = false)
    private String ownerName;
    @TableField(exist = false)
    private Integer itemCount;
    /** 已生效活动按当前时间计算的阶段（草稿、已取消为空） */
    @TableField(exist = false)
    private EcomPromoPhase phase;
    /** 店铺平台是否支持推送 */
    @TableField(exist = false)
    private Boolean canPush;
    @Valid
    @TableField(exist = false)
    private List<EcomPromotionItem> items;
}
