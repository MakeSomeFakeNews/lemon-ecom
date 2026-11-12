package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 包裹明细：平台订单的一行（或其中一部分数量）；发货时按订单生成销售出库，记录出库单与成本
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_package_item")
public class EcomPackageItem extends BaseEntity {

    private Long packageId;
    private Long orderId;
    private Long orderItemId;
    private Long materialId;
    /** 件数（平台 SKU） */
    private BigDecimal qty;
    /** 物料数量 = 件数 × 多件装 */
    private BigDecimal materialQty;
    /** 出库成本（本位币） */
    private BigDecimal cost;
    /** 发货时生成的销售出库单 */
    private Long tradeDocId;
    private String tradeDocNo;

    @TableField(exist = false)
    private String platformOrderNo;
    @TableField(exist = false)
    private String sellerSku;
    @TableField(exist = false)
    private String title;
    /** 订单行单价（订单币种），申报价值用 */
    @TableField(exist = false)
    private BigDecimal price;
    @TableField(exist = false)
    private String currency;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialSpec;
    @TableField(exist = false)
    private String unit;
}
