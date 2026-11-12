package com.lemon.erp.modules.ecom.cs.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.cs.model.CsSourceType;
import com.lemon.erp.modules.ecom.cs.model.CsTicketType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 售后工单：会话 / 评价 / 店铺反馈转售后的记录，退货退款、仅退款生成退货退款单，补发生成一张补发订单
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_cs_ticket")
public class EcomCsTicket extends BaseEntity {

    private Long storeId;
    private CsSourceType sourceType;
    private Long sourceId;
    private CsTicketType ticketType;
    private Long orderId;
    private String platformOrderNo;
    private Long returnId;
    private String returnNo;
    private Long resendOrderId;
    private String resendOrderNo;
    private String remark;
    private Long deptId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String createByName;
    /** 退货退款单的状态（EcomReturnStatus 名称） */
    @TableField(exist = false)
    private String returnStatus;
    /** 补发订单的处理状态（EcomLocalStatus 名称） */
    @TableField(exist = false)
    private String resendStatus;
}
