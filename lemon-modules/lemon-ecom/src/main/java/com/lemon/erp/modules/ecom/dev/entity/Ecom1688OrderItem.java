package com.lemon.erp.modules.ecom.dev.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 1688 采购单明细：采购订单明细 → 1688 商品规格与下单数量
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_1688_order_item")
public class Ecom1688OrderItem extends BaseEntity {

    private Long orderId;
    private Long scmOrderItemId;
    private Long materialId;
    private String offerId;
    private String specId;
    private BigDecimal qty;
    private BigDecimal materialQty;
    private BigDecimal price;
    private BigDecimal amount;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String title;
    @TableField(exist = false)
    private String specText;
    @TableField(exist = false)
    private String imageUrl;
}
