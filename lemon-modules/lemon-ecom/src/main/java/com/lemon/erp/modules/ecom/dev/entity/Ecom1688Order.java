package com.lemon.erp.modules.ecom.dev.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.dev.model.Ali1688OrderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 1688 采购单：由采购订单一键下单生成（按卖家拆单），同步 1688 的交易状态、实付金额（含运费）、物流单号与轨迹
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_1688_order")
public class Ecom1688Order extends BaseEntity {

    private Long accountId;
    private Long scmOrderId;
    private String aliOrderId;
    private String sellerLoginId;
    private String sellerName;
    private Ali1688OrderStatus status;
    private String aliStatus;
    private BigDecimal productAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private BigDecimal totalQty;
    private String logisticsCompany;
    private String logisticsNo;
    /** 物流轨迹 JSON，只在详情返回 */
    private String trace;
    private String message;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime aliCreateTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;
    private String syncMessage;
    private Long createBy;

    @TableField(exist = false)
    private String accountName;
    @TableField(exist = false)
    private String scmOrderNo;
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String createByName;
    @TableField(exist = false)
    private Integer itemCount;
    @TableField(exist = false)
    private List<Ecom1688OrderItem> items;
}
