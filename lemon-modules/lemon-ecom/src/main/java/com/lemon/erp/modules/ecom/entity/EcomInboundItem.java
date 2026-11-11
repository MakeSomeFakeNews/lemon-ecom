package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 头程货件明细：物料、平台 SKU、数量；unitLanded 为发运时按数量分摊的单件头程成本（运费 + 关税 + 其他）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_inbound_item")
public class EcomInboundItem extends BaseEntity {

    private Long inboundId;
    private Long materialId;
    private String sellerSku;
    private BigDecimal qty;
    private BigDecimal receivedQty;
    private BigDecimal unitLanded;
    private String remark;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialSpec;
    @TableField(exist = false)
    private String unit;
}
