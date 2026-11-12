package com.lemon.erp.modules.ecom.fba.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.lemon.erp.modules.ecom.fba.model.FbaDisposition;

import java.math.BigDecimal;

/**
 * FBA 移除订单明细（平台 SKU 件数）：亚马逊发出 / 弃置的数量由移除订单报告同步，本地收货分良品与不良品
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_fba_removal_item")
public class EcomFbaRemovalItem extends BaseEntity {

    private Long removalId;
    private String sellerSku;
    private String fnsku;
    private FbaDisposition disposition;
    private Long materialId;
    private BigDecimal packQty;
    private BigDecimal requestedQty;
    private BigDecimal cancelledQty;
    private BigDecimal shippedQty;
    private BigDecimal disposedQty;
    private BigDecimal receivedQty;
    private BigDecimal defectiveQty;
    private BigDecimal scrappedQty;
    private BigDecimal removalFee;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
}
