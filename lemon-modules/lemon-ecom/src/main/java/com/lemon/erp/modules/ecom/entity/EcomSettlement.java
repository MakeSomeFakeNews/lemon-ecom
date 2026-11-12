package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FeeAllocMethod;
import com.lemon.erp.modules.ecom.model.SettlementStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 平台结算单：平台一个结算周期的款项汇总（店铺币种）。导入结算报告后按明细类型汇总；确认时把佣金、配送费回写到订单，
 * 并可按净额生成收款单核销店铺客户的应收（平台费用作为折让）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_settlement")
public class EcomSettlement extends BaseEntity {

    private Long storeId;
    private String settlementNo;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String currency;
    /** 订单收入 */
    private BigDecimal salesAmount;
    /** 退款（负数） */
    private BigDecimal refundAmount;
    /** 平台佣金（负数） */
    private BigDecimal commissionFee;
    /** 配送费（负数） */
    private BigDecimal fulfillmentFee;
    /** 仓储费（负数） */
    private BigDecimal storageFee;
    /** 广告费（负数） */
    private BigDecimal adFee;
    /** 其他费用（负数） */
    private BigDecimal otherFee;
    /** 调整 */
    private BigDecimal adjustment;
    /** 结算净额 = 以上合计 */
    private BigDecimal netAmount;
    private BigDecimal exchangeRate;
    /** 结算净额（本位币） */
    private BigDecimal netBase;
    private SettlementStatus status;
    /** 确认时生成的收款单 */
    private Long finSettlementId;
    /** 不挂订单费用的分摊方式（QTY / SALES），为空按类型默认 */
    private FeeAllocMethod allocMethod;
    private String remark;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private EcomPlatform platform;
    @TableField(exist = false)
    private Integer itemCount;
    @TableField(exist = false)
    private List<EcomSettlementItem> items;
}
