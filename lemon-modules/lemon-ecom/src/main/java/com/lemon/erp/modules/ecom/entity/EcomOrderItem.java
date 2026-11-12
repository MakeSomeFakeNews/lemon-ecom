package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 平台订单明细：一行平台 SKU；materialId / materialQty 为按在线商品映射换算的本地物料和数量（数量 × 多件装数量）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_order_item")
public class EcomOrderItem extends BaseEntity {

    private Long orderId;
    /** 平台订单行 ID（亚马逊 OrderItemId 等），回传发货时使用 */
    private String platformLineId;
    private String sellerSku;
    private String platformItemId;
    private String title;
    private BigDecimal qty;
    /** 单价（订单币种） */
    private BigDecimal price;
    /** 商品金额（订单币种） */
    private BigDecimal amount;
    private BigDecimal shipping;
    private BigDecimal tax;
    private BigDecimal discount;
    private Long materialId;
    private BigDecimal materialQty;
    /** 成本（本位币） */
    private BigDecimal cost;
    /** 是否组合商品 1是：物料见 parts（materialId / materialQty 为空） */
    private Integer bundle;

    /** 组合商品展开的物料（ecom_order_item_part） */
    @TableField(exist = false)
    private java.util.List<EcomOrderItemPart> parts;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialSpec;
    @TableField(exist = false)
    private String unit;
    /** 详情：已分配到未取消包裹的件数 */
    @TableField(exist = false)
    private BigDecimal allocatedQty;
    /** 详情：其中已打单 / 已发货（拆分时不能再分配）的件数 */
    @TableField(exist = false)
    private BigDecimal lockedQty;
}
