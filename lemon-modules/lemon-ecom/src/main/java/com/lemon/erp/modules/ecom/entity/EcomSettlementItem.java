package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.SettlementItemType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 结算明细：一笔收入 / 费用，收入为正、费用为负（店铺币种）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_settlement_item")
public class EcomSettlementItem extends BaseEntity {

    private Long settlementId;
    private LocalDate postedDate;
    private SettlementItemType itemType;
    private String platformOrderNo;
    private String sellerSku;
    private BigDecimal amount;
    private String description;
    /** 结算报告中的原始类型名（按费用映射识别类型） */
    private String rawType;
}
