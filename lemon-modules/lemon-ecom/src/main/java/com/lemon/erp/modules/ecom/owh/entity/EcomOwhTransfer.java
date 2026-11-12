package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhTransferStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 海外仓调拨单：海外仓之间、海外仓与本地仓之间的调拨。调出仓是海外仓时向服务商推送调拨出库单，出库后生成 ERP 在途调拨单；
 * 调入仓是海外仓时由在途调拨单生成并推送海外仓入库单，收货后在途转可用；调入仓是本地仓时确认收货
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_transfer")
public class EcomOwhTransfer extends BaseEntity {

    private String transferNo;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private Long fromMappingId;
    private Long toMappingId;
    private OwhTransferStatus status;
    private String productCode;
    private String owhOutNo;
    private String owhStatus;
    private String carrier;
    private String trackingNo;
    private Long transferDocId;
    private String transferDocNo;
    private Long inboundId;
    private BigDecimal totalQty;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pushTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;
    private String errorMsg;
    private String remark;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String fromWarehouseName;
    @TableField(exist = false)
    private String toWarehouseName;
    @TableField(exist = false)
    private String fromOwhCode;
    @TableField(exist = false)
    private String toOwhCode;
    @TableField(exist = false)
    private String inboundNo;
    @TableField(exist = false)
    private OwhInboundStatus inboundStatus;
    @TableField(exist = false)
    private List<EcomOwhTransferItem> items;
}
