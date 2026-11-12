package com.lemon.erp.modules.ecom.fba.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.lemon.erp.modules.ecom.fba.model.FbaAgeSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * FBA 库存健康快照（店铺 × SKU 只保留最近一次）：数量与库龄分段来自库存规划 / 库存报告，或按头程签收先进先出推算
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_fba_inventory")
public class EcomFbaInventory extends BaseEntity {

    private Long storeId;
    private String sellerSku;
    private String fnsku;
    private String asin;
    private String title;
    private LocalDate snapshotDate;
    private BigDecimal availableQty;
    private BigDecimal inboundQty;
    private BigDecimal reservedQty;
    private BigDecimal unfulfillableQty;
    private BigDecimal researchingQty;
    @TableField("age_0_90")
    private BigDecimal age0To90;
    @TableField("age_91_180")
    private BigDecimal age91To180;
    @TableField("age_181_270")
    private BigDecimal age181To270;
    @TableField("age_271_365")
    private BigDecimal age271To365;
    @TableField("age_365_plus")
    private BigDecimal age365Plus;
    private FbaAgeSource ageSource;
    @TableField("units_shipped_30")
    private BigDecimal unitsShipped30;
    private BigDecimal reportStorageFee;
    private String currency;
}
