package com.lemon.erp.modules.ecom.fba.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimStatus;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * FBA 索赔：系统识别或手工登记的可索赔项，跟踪 Case 编号、状态和赔付
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_fba_claim")
public class EcomFbaClaim extends BaseEntity {

    private String claimNo;
    private Long storeId;
    private FbaClaimType claimType;
    private String sourceKey;
    private String sourceRef;
    private String sellerSku;
    private String fnsku;
    private BigDecimal qty;
    private BigDecimal unitValue;
    private BigDecimal estAmount;
    private String currency;
    private LocalDate eventDate;
    private LocalDate deadline;
    private FbaClaimStatus status;
    private String caseNo;
    private LocalDate submitDate;
    private BigDecimal paidAmount;
    private BigDecimal paidQty;
    private BigDecimal paidBase;
    private LocalDate paidDate;
    private String detail;
    private String remark;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    /** 距截止日期的天数（负数为已过期） */
    @TableField(exist = false)
    private Integer daysLeft;
}
