package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 海外仓退件单明细：物料、服务商 SKU、退件数量与质检后的良品 / 不良品数量
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_return_item")
public class EcomOwhReturnItem extends BaseEntity {

    private Long owhReturnId;
    private Long materialId;
    private String sku;
    private BigDecimal qty;
    private BigDecimal goodQty;
    private BigDecimal badQty;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
}
