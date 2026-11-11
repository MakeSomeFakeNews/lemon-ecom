package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.InboundStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 头程货件：从本地仓发往平台仓 / 海外仓的一批货（亚马逊 FBA 货件等）。发运时生成在途调拨单，签收时调拨收货，
 * 头程运费、关税等按数量分摊到每件（unitLanded），作为平台配送订单的物流成本
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_inbound")
public class EcomInbound extends BaseEntity {

    private String docNo;
    private Long storeId;
    /** 平台货件号（FBA Shipment ID 等） */
    private String shipmentId;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private Long channelId;
    private String carrier;
    private String trackingNo;
    private LocalDate shipDate;
    /** 预计到达 */
    private LocalDate etaDate;
    private LocalDate receiveDate;
    private Integer boxCount;
    /** 毛重（kg） */
    private BigDecimal weight;
    /** 体积（m³） */
    private BigDecimal volume;
    /** 头程运费（本位币） */
    private BigDecimal freight;
    /** 关税（本位币） */
    private BigDecimal duty;
    /** 其他费用（本位币） */
    private BigDecimal otherCost;
    private BigDecimal totalQty;
    private InboundStatus status;
    /** 生成的调拨单 */
    private Long transferDocId;
    private String transferDocNo;
    private String flowInstanceId;
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
    private String channelName;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private List<EcomInboundItem> items;
}
