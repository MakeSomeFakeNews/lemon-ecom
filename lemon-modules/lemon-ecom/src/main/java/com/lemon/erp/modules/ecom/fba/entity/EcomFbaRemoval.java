package com.lemon.erp.modules.ecom.fba.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.ecom.fba.model.FbaRemovalStatus;
import com.lemon.erp.modules.ecom.fba.model.FbaRemovalType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * FBA 移除订单：退回本地仓（收货时从平台仓调拨到本地仓，不良品进不良品仓）或弃置（弃置出库，从平台仓报废出库）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_fba_removal")
public class EcomFbaRemoval extends BaseEntity {

    private String docNo;
    private Long storeId;
    private FbaRemovalType removalType;
    private String platformOrderId;
    private FbaRemovalStatus status;
    private Long warehouseId;
    private Long defectiveWarehouseId;
    private String shipAddress;
    private LocalDate requestDate;
    private LocalDate completeDate;
    private String carrier;
    private String trackingNo;
    private BigDecimal totalQty;
    private BigDecimal removalFee;
    private String currency;
    private BigDecimal feeBase;
    private String stockDocNos;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSyncTime;
    private String remark;
    private Long deptId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String defectiveWarehouseName;
    @TableField(exist = false)
    private BigDecimal shippedQty;
    @TableField(exist = false)
    private BigDecimal disposedQty;
    @TableField(exist = false)
    private BigDecimal receivedQty;
    @TableField(exist = false)
    private List<EcomFbaRemovalItem> items;
}
