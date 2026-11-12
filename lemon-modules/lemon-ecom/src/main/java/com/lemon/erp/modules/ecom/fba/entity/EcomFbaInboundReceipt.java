package com.lemon.erp.modules.ecom.fba.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.lemon.erp.modules.ecom.fba.model.FbaDataSource;

import java.math.BigDecimal;

/**
 * 头程货件明细的亚马逊实收数量（货件对账）：发货件数大于实收件数的差额为入库短少
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_fba_inbound_receipt")
public class EcomFbaInboundReceipt extends BaseEntity {

    private Long inboundItemId;
    private Long inboundId;
    private BigDecimal shippedQty;
    private BigDecimal receivedQty;
    private FbaDataSource source;
}
