package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomReturnStatus;
import com.lemon.erp.modules.ecom.model.ReturnType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 退货退款：平台订单的售后。退货退款在收到货物后生成销售退货（库存增加、红字应收），退款后金额回写订单重算利润
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_return")
public class EcomReturn extends BaseEntity {

    private String docNo;
    private Long storeId;
    private Long orderId;
    private String platformOrderNo;
    private ReturnType returnType;
    /** 退货原因（业务字典 ecom_return_reason） */
    private String reason;
    private String currency;
    private BigDecimal exchangeRate;
    /** 退款金额（订单币种） */
    private BigDecimal refundAmount;
    /** 退回仓库（退货退款） */
    private Long warehouseId;
    private EcomReturnStatus status;
    /** 生成的销售退货单 */
    private Long tradeDocId;
    private String tradeDocNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refundTime;
    private String remark;

    private Long deptId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String buyerName;
    @TableField(exist = false)
    private List<EcomReturnItem> items;
}
