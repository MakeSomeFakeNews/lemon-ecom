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
    /** 在线商品（由发货计划生成，或按 店铺 + 平台 SKU 识别），没有时为空 */
    private Long listingId;
    /** 本行对应的平台 SKU 件数（多件装 = 数量 ÷ 多件装数量；组合商品各组件行相同），装箱、FNSKU 标签按它计 */
    private BigDecimal skuQty;
    /** 是否计入平台 SKU 件数 1是（组合商品只有第一个组件行） */
    private Integer partMain;
    /** 来源发货计划明细 */
    private Long planItemId;

    @TableField(exist = false)
    private String fnsku;
    @TableField(exist = false)
    private String title;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialSpec;
    @TableField(exist = false)
    private String unit;
}
