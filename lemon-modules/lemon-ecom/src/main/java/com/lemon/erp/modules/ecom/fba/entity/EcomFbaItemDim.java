package com.lemon.erp.modules.ecom.fba.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.lemon.erp.modules.ecom.fba.model.FbaDataSource;

import java.math.BigDecimal;

/**
 * FBA 商品尺寸重量（店铺 × SKU）：计算应收配送费、仓储体积；手工维护或由预估 FBA 费用报告带入
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_fba_item_dim")
public class EcomFbaItemDim extends BaseEntity {

    private Long storeId;
    private String sellerSku;
    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private FbaDataSource source;
    private String platformTier;
    private BigDecimal platformFee;

    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String marketplace;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
}
