package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.owh.model.OwhFeeStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhFeeType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 海外仓费用明细：操作费、尾程运费跟随出库单计入订单物流成本；仓租、入库费按 SKU 分摊到店铺 SKU（利润报表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_fee")
public class EcomOwhFee extends BaseEntity {

    private Long accountId;
    private String externalId;
    private OwhFeeType feeType;
    private LocalDate feeDate;
    private String refNo;
    private String owhCode;
    private Long warehouseId;
    private String sku;
    private Long materialId;
    private Long outboundId;
    private Long inboundId;
    private String currency;
    private BigDecimal amount;
    private BigDecimal exchangeRate;
    private BigDecimal amountBase;
    private OwhFeeStatus status;
    private String postTarget;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime postTime;
    private String remark;

    @TableField(exist = false)
    private String accountName;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String outboundNo;
    @TableField(exist = false)
    private String inboundNo;
    @TableField(exist = false)
    private String packageNo;
}
