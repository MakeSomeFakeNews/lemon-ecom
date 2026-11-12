package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 头程货件的箱子：箱号、外箱尺寸（cm）、毛重（kg）与箱内各平台 SKU 的件数（装箱单、箱唛按它打印）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_inbound_box")
public class EcomInboundBox extends BaseEntity {

    private Long inboundId;
    /** 箱号（货件内从 1 开始） */
    private Integer boxNo;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal weight;
    private String remark;

    @TableField(exist = false)
    private List<EcomInboundBoxItem> items;
}
