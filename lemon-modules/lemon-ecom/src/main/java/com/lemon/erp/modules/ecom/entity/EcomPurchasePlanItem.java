package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购计划明细：一个物料一行。demand 为生成时各店铺的需求构成（JSON 数组：storeId、storeName、sellerSku、qty），只用于显示；
 * 生成采购订单后记录 orderId / orderNo，已到货数量取该订单中本物料明细的已入库数量
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_purchase_plan_item")
public class EcomPurchasePlanItem extends BaseEntity {

    private Long planId;
    private Long materialId;
    /** 生成计划时的建议采购量 */
    private BigDecimal suggestQty;
    /** 计划采购量 */
    private BigDecimal qty;
    private Long supplierId;
    /** 含税单价，为空时生成订单时取供应商价格 / 默认采购价 */
    private BigDecimal price;
    /** 税率（%），为空取默认 */
    private BigDecimal taxRate;
    /** 要求到货日期 */
    private LocalDate arrivalDate;
    private String demand;
    private Long orderId;
    private String orderNo;
    private String remark;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialSpec;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private String supplierName;
    /** 生成的采购订单状态（订单被删除时为空） */
    @TableField(exist = false)
    private String orderStatus;
    /** 已到货数量（采购订单明细的已入库数量） */
    @TableField(exist = false)
    private BigDecimal arrivedQty;
}
