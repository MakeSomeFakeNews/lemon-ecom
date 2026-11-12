package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.PayoutSource;
import com.lemon.erp.modules.ecom.model.PayoutStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 平台回款：结算单导入时按净额生成一条「未到账」记录（应到账），导入银行 / 收款平台流水或手工登记后变为已到账 / 有差异；
 * 没有匹配到结算单的流水单独一条（有差异，settlementId 为空），可手工匹配
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_payout")
public class EcomPayout extends BaseEntity {

    private Long storeId;
    private Long settlementId;
    private PayoutSource source;
    /** 结算币种 */
    private String currency;
    private BigDecimal expectedAmount;
    private BigDecimal expectedBase;
    private LocalDate expectedDate;

    private LocalDate payoutDate;
    /** 到账金额（到账币种） */
    private BigDecimal amount;
    /** 到账币种：与结算币种相同，或收款平台结汇后为本位币 */
    private String payCurrency;
    private Long accountId;
    /** 收款平台（业务字典 ecom_payout_channel） */
    private String channel;
    private String bankRef;
    /** 到账汇率：1 结算币种 = exchangeRate 本位币 */
    private BigDecimal exchangeRate;
    private BigDecimal amountBase;
    /** 汇兑差额（本位币，正数为收益） */
    private BigDecimal fxDiff;
    /** 金额差额（本位币，负数通常为收款平台手续费） */
    private BigDecimal amountDiff;
    private PayoutStatus status;
    private Long voucherId;
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private EcomPlatform platform;
    @TableField(exist = false)
    private String settlementNo;
    @TableField(exist = false)
    private LocalDate periodStart;
    @TableField(exist = false)
    private BigDecimal settlementRate;
    /** 结算确认时生成的收款单 */
    @TableField(exist = false)
    private Long finSettlementId;
    @TableField(exist = false)
    private String accountName;
    @TableField(exist = false)
    private String voucherNo;
}
