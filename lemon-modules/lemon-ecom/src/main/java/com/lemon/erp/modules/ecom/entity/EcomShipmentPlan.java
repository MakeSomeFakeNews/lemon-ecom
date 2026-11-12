package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.ShipmentPlanStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 发货计划（FBA / 平台仓补货计划）：店铺、发货仓 → 目标平台仓、平台 SKU 与计划数量；确认后生成头程货件（可拆成多个货件）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_shipment_plan")
public class EcomShipmentPlan extends BaseEntity {

    private String planNo;
    private Long storeId;
    /** 发货仓（本地仓） */
    private Long fromWarehouseId;
    /** 目标平台仓 / 海外仓 */
    private Long toWarehouseId;
    /** 计划发货日期 */
    private LocalDate planDate;
    /** 计划数量合计（平台 SKU 件数） */
    private BigDecimal totalQty;
    private ShipmentPlanStatus status;
    private String remark;

    private Long deptId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String fromWarehouseName;
    @TableField(exist = false)
    private String toWarehouseName;
    @TableField(exist = false)
    private String createByName;
    /** 已生成货件的数量合计（未取消的货件） */
    @TableField(exist = false)
    private BigDecimal shippedQty;
    @TableField(exist = false)
    private List<EcomShipmentPlanItem> items;
    /** 由本计划生成的头程货件 */
    @TableField(exist = false)
    private List<EcomInbound> inbounds;
}
