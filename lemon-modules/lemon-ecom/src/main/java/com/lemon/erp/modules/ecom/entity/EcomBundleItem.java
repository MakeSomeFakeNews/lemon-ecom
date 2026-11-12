package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 组合商品的组成：一个平台 SKU（组成方式为组合的在线商品）包含的物料与数量，如 A × 1 + B × 2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_bundle_item")
public class EcomBundleItem extends BaseEntity {

    private Long listingId;

    @NotNull(message = "{ecom.valid.catalog.bundleMaterialRequired}")
    private Long materialId;

    /** 一件平台 SKU 包含的物料数量 */
    @NotNull(message = "{ecom.valid.catalog.bundleQtyRequired}")
    @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.catalog.bundleQtyPositive}")
    @Digits(integer = 14, fraction = 4, message = "{ecom.valid.qtyDecimals}")
    private BigDecimal qty;

    private Integer sort;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialSpec;
    @TableField(exist = false)
    private String unit;
    /** 物料的参考成本（订单展开时按它分摊收入） */
    @TableField(exist = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private BigDecimal refCost;
}
