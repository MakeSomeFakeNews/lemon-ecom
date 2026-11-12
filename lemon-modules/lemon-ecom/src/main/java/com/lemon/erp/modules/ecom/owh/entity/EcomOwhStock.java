package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 海外仓库存（每个仓库映射 × SKU 保留最近一次同步），列表与 ERP 账面库存并列显示差异
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_stock")
public class EcomOwhStock extends BaseEntity {

    private Long mappingId;
    private Long warehouseId;
    private String sku;
    private Long materialId;
    private BigDecimal availableQty;
    private BigDecimal transitQty;
    private BigDecimal lockedQty;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;

    @TableField(exist = false)
    private String accountName;
    @TableField(exist = false)
    private String owhCode;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialSpec;
    /** ERP 账面库存（系统仓库中该物料的现存量） */
    @TableField(exist = false)
    private BigDecimal erpQty;
    /** ERP 在途（调入该仓库、还没有收货的在途调拨） */
    @TableField(exist = false)
    private BigDecimal erpTransitQty;
    /** 差异 = 海外仓（可用 + 锁定）− ERP 账面 */
    @TableField(exist = false)
    private BigDecimal diffQty;
}
