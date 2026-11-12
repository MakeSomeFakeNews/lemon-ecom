package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.owh.model.OwhProvider;
import com.lemon.erp.modules.ecom.owh.model.OwhReturnStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 海外仓退件单：由退回仓是海外仓的 ERP 退货退款单生成，推送到服务商，同步收货质检结果后 ERP 退货入库（良品留在海外仓，不良品转不良品仓或报废）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_return")
public class EcomOwhReturn extends BaseEntity {

    private String returnNo;
    private Long accountId;
    private Long mappingId;
    private Long warehouseId;
    private Long storeId;
    private Long ecomReturnId;
    private String ecomReturnNo;
    private String platformOrderNo;
    private String owhOrderNo;
    private OwhReturnStatus status;
    private String owhStatus;
    private String carrier;
    private String trackingNo;
    private BigDecimal totalQty;
    private BigDecimal goodQty;
    private BigDecimal badQty;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pushTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveTime;
    private Integer erpReceived;
    private String defectDocNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;
    private String errorMsg;
    private String remark;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    @TableField(exist = false)
    private String accountName;
    @TableField(exist = false)
    private OwhProvider provider;
    @TableField(exist = false)
    private String owhCode;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private List<EcomOwhReturnItem> items;
}
