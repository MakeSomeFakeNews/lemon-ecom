package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 退货退款明细：退回的订单行与数量（平台 SKU 数量）、本行退款金额
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_return_item")
public class EcomReturnItem extends BaseEntity {

    private Long returnId;
    private Long orderItemId;
    private Long materialId;
    private String sellerSku;
    private BigDecimal qty;
    private BigDecimal refundAmount;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String title;
}
