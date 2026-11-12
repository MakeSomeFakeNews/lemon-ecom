package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.SettlementItemType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 海外仓费用的入账明细：计入订单物流成本（order_id 不为空，金额为正）或分摊到店铺 SKU（利润报表的仓储费 / 其他费用，金额为负）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_fee_alloc")
public class EcomOwhFeeAlloc extends BaseEntity {

    private Long feeId;
    private Long storeId;
    private String sellerSku;
    private Long orderId;
    private SettlementItemType itemType;
    private LocalDate allocDate;
    private BigDecimal basisValue;
    private BigDecimal amountBase;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String orderNo;
}
