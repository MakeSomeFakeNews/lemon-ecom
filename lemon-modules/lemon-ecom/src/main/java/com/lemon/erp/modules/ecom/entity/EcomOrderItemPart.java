package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 组合商品订单行的展开结果：匹配时按在线商品的组成记下每个物料、每件的数量和收入分摊比例，之后的库存检查、包裹、出库、
 * 退货都按它处理（组合的组成之后再修改也不影响已匹配的订单）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_order_item_part")
public class EcomOrderItemPart extends BaseEntity {

    private Long orderId;
    private Long orderItemId;
    private Long materialId;
    /** 每件平台 SKU 含的物料数量 */
    private BigDecimal unitQty;
    /** 物料数量 = 件数 × unitQty */
    private BigDecimal materialQty;
    /** 收入分摊比例（各组件合计为 1），出库时按它拆分本行金额 */
    private BigDecimal share;
    /** 出库成本（本位币） */
    private BigDecimal cost;
    private Integer sort;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialSpec;
    @TableField(exist = false)
    private String unit;
}
