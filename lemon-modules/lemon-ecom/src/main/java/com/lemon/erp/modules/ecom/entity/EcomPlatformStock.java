package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 平台仓库存快照：平台库存报告中每个平台 SKU 的可售、入库中、预留、不可售数量（只保留最近一次），
 * 与系统中平台仓的库存（物料库存 ÷ 多件装数量）对比差异
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_platform_stock")
public class EcomPlatformStock extends BaseEntity {

    private Long storeId;
    private String sellerSku;
    private String platformItemId;
    private BigDecimal fulfillableQty;
    private BigDecimal inboundQty;
    private BigDecimal reservedQty;
    private BigDecimal unfulfillableQty;
    private LocalDate snapshotDate;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private Long materialId;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private BigDecimal packQty;
    /** 系统中平台仓的库存（按平台 SKU 件数） */
    @TableField(exist = false)
    private BigDecimal systemQty;
    /** 差异 = 平台可售 − 系统库存 */
    @TableField(exist = false)
    private BigDecimal diffQty;
}
