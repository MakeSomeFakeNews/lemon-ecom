package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 海外仓入库单明细：箱号 × SKU（物料编码）× 数量；收货数量同步自服务商（同一 SKU 多箱时记在第一行）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_inbound_item")
public class EcomOwhInboundItem extends BaseEntity {

    private Long inboundId;
    private Integer boxNo;
    private Long materialId;
    private String sku;
    private BigDecimal qty;
    private BigDecimal receivedQty;

    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialSpec;
}
