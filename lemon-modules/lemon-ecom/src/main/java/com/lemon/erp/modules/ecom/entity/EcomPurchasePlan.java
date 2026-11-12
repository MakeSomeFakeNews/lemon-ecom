package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.PurchasePlanStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购计划：由补货建议（本地仓采购）勾选生成，按物料汇总多店铺的需求；审核后按供应商拆单生成采购订单（草稿），
 * 采购订单入库后明细显示已到货数量
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_purchase_plan")
public class EcomPurchasePlan extends BaseEntity {

    private String docNo;
    private LocalDate planDate;
    private PurchasePlanStatus status;
    /** 收货仓库（生成的采购订单的默认收货仓） */
    private Long warehouseId;
    private BigDecimal totalQty;
    private String remark;
    private Long deptId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;
    private Long approveBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approveTime;

    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private String approveByName;
    /** 已生成采购订单的数量 */
    @TableField(exist = false)
    private BigDecimal orderedQty;
    /** 已到货（采购订单已入库）数量 */
    @TableField(exist = false)
    private BigDecimal arrivedQty;
    /** 明细行数 */
    @TableField(exist = false)
    private Integer itemCount;
    @TableField(exist = false)
    private List<EcomPurchasePlanItem> items;
}
