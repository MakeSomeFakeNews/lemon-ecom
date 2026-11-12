package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 海外仓调拨单明细
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_transfer_item")
public class EcomOwhTransferItem extends BaseEntity {

    private Long transferId;
    private Long materialId;
    private BigDecimal qty;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialSpec;
}
