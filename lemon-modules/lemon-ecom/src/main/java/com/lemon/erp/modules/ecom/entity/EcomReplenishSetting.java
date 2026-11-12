package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.ReplenishScope;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 补货设置：在线商品级（listingId）或物料级（materialId），为空的项不覆盖。
 * 补货周期、安全天数用于平台仓补货；采购周期、物料级的日均覆盖用于本地仓采购；系数乘在按销量计算的日均上（手工日均不乘）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_replenish_setting")
public class EcomReplenishSetting extends BaseEntity {

    private ReplenishScope scope;
    private Long listingId;
    private Long materialId;
    /** 平台仓补货周期（天），覆盖店铺设置 */
    private Integer leadDays;
    /** 安全天数，覆盖店铺设置 */
    private Integer safetyDays;
    /** 采购周期（天），覆盖默认供应商的提前期 */
    private Integer purchaseDays;
    /** 手工日均销量（在线商品级为平台 SKU 件数，物料级为物料数量） */
    private BigDecimal dailySales;
    /** 季节 / 活动系数，乘在按销量计算的日均上 */
    private BigDecimal factor;
    /** 1 不补货 */
    private Integer disabled;
    private String remark;

    @TableField(exist = false)
    private String sellerSku;
    @TableField(exist = false)
    private String storeName;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
}
