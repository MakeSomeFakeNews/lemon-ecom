package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.ListingComposeType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 发货计划明细：一个平台 SKU（在线商品）与计划件数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_shipment_plan_item")
public class EcomShipmentPlanItem extends BaseEntity {

    private Long planId;
    private Long listingId;
    private String sellerSku;
    /** 计划件数（平台 SKU） */
    private BigDecimal qty;
    private String remark;

    @TableField(exist = false)
    private String title;
    @TableField(exist = false)
    private String fnsku;
    @TableField(exist = false)
    private ListingComposeType composeType;
    @TableField(exist = false)
    private Long materialId;
    @TableField(exist = false)
    private BigDecimal packQty;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    /** 已生成货件的件数（未取消的货件） */
    @TableField(exist = false)
    private BigDecimal shippedQty;
}
