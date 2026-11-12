package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomPackageStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhOutboundStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhProvider;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 海外仓出库单（一件代发）：一个包裹推送到海外仓生成一张；拿到运单号后包裹按现有流程发货（销售出库、回传平台）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_outbound")
public class EcomOwhOutbound extends BaseEntity {

    private String outboundNo;
    private Long packageId;
    private Long storeId;
    private Long accountId;
    private Long mappingId;
    private Long warehouseId;
    private String productCode;
    private String owhOrderNo;
    private OwhOutboundStatus status;
    private String owhStatus;
    private String carrierName;
    private String trackingNo;
    private BigDecimal estimateFee;
    private String feeCurrency;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pushTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime outboundTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;
    private Integer pushCount;
    private String errorMsg;
    private String remark;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String packageNo;
    @TableField(exist = false)
    private EcomPackageStatus packageStatus;
    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String accountName;
    @TableField(exist = false)
    private OwhProvider provider;
    @TableField(exist = false)
    private String owhCode;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String productName;
    /** 包裹中的平台订单号（逗号分隔） */
    @TableField(exist = false)
    private String orderNos;
    @TableField(exist = false)
    private String shipName;
    @TableField(exist = false)
    private String shipCountry;
    /** 已同步的费用合计（本位币） */
    @TableField(exist = false)
    private BigDecimal feeBase;
}
