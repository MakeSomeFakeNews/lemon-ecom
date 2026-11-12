package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundSource;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhProvider;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 海外仓入库单（ASN）：由头程货件或在途调拨单生成，按箱 × SKU 推送到服务商，同步收货数量；收货完成后 ERP 签收来源单据（在途转可用）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_inbound")
public class EcomOwhInbound extends BaseEntity {

    private String inboundNo;
    private Long accountId;
    private Long mappingId;
    private Long warehouseId;
    private OwhInboundSource sourceType;
    private Long sourceId;
    private String sourceNo;
    private String owhOrderNo;
    private OwhInboundStatus status;
    private String owhStatus;
    private String transportMode;
    private String carrier;
    private String trackingNo;
    private LocalDate etaDate;
    private Integer boxCount;
    private BigDecimal totalQty;
    private BigDecimal receivedQty;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pushTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receiveTime;
    /** ERP 已签收 1是 */
    private Integer erpReceived;
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
    private List<EcomOwhInboundBox> boxes;
    @TableField(exist = false)
    private List<EcomOwhInboundItem> items;
}
