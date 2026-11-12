package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.FeeAllocMethod;
import com.lemon.erp.modules.ecom.model.SettlementItemType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 不挂订单的平台费用（仓储费、订阅费、移除费等）分摊到 SKU 的结果：一条结算明细分成多行，金额合计等于明细金额
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_fee_allocation")
public class EcomFeeAllocation extends BaseEntity {

    private Long settlementId;
    private Long settlementItemId;
    private Long storeId;
    /** 分摊到的平台 SKU，为空表示留在店铺 */
    private String sellerSku;
    private SettlementItemType itemType;
    private FeeAllocMethod method;
    private LocalDate allocDate;
    /** 分摊依据：该 SKU 的销量或本位币销售额 */
    private BigDecimal basisValue;
    /** 店铺币种，费用为负 */
    private BigDecimal amount;
    private BigDecimal amountBase;
}
