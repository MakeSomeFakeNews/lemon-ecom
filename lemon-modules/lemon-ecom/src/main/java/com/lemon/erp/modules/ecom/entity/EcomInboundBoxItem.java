package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 箱内的一个平台 SKU（没有平台 SKU 的货件明细按物料编码）与件数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_inbound_box_item")
public class EcomInboundBoxItem extends BaseEntity {

    private Long inboundId;
    /** 箱号 */
    private Integer boxNo;
    private String sellerSku;
    private BigDecimal qty;
}
