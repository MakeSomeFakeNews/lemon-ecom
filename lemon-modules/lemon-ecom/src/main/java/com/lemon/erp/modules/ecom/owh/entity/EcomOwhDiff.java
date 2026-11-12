package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.owh.model.OwhDiffSource;
import com.lemon.erp.modules.ecom.owh.model.OwhDiffStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收货差异：入库单 / 退件单 / 调拨单的实收与单据数量不一致的一个物料。ERP 只入实收（短少的同时差异出库、退件按实收生成销售退货），
 * 差异记录原因并处理：短少确认损耗或找回补收，溢收确认
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_diff")
public class EcomOwhDiff extends BaseEntity {

    private OwhDiffSource sourceType;
    private Long sourceId;
    private String sourceNo;
    /** 关联单据（头程货件号 / 调拨单号 / 退货单号） */
    private String refNo;
    private Long ecomInboundId;
    private Long warehouseId;
    private Long materialId;
    private String sku;
    private BigDecimal expectedQty;
    private BigDecimal receivedQty;
    /** 差异 = 单据 − 实收（正数为短少、负数为溢收） */
    private BigDecimal diffQty;
    private OwhDiffStatus status;
    private String reason;
    /** 收货时生成的差异出库 / 溢收入库单号 */
    private String diffDocNo;
    /** 处理时生成的补收入库单号 */
    private String handleDocNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;
    private Long handleBy;

    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
}
