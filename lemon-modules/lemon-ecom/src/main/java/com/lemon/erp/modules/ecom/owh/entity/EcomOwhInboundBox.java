package com.lemon.erp.modules.ecom.owh.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 海外仓入库单的一箱：箱号、箱唛号、外箱尺寸（cm）与毛重（kg）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_owh_inbound_box")
public class EcomOwhInboundBox extends BaseEntity {

    private Long inboundId;
    private Integer boxNo;
    private String boxMark;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal weight;
}
