package com.lemon.erp.modules.ecom.fba.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.lemon.erp.modules.ecom.fba.model.FbaSizeCategory;
import com.lemon.erp.modules.ecom.fba.model.FbaUnitSystem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * FBA 配送费尺寸分段：按 sort 依次匹配第一个各边、长度 + 围长、三边合计、单件实重都不超过上限的分段
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_fba_size_tier")
public class EcomFbaSizeTier extends BaseEntity {

    @NotBlank(message = "{ecom.valid.selectMarketplace}")
    @Size(max = 16)
    private String marketplace;
    @NotBlank(message = "{ecom.valid.fba.tierCodeRequired}")
    @Size(max = 32)
    private String code;
    @NotBlank(message = "{ecom.valid.fba.tierNameRequired}")
    @Size(max = 64)
    private String name;
    @NotNull(message = "{ecom.valid.fba.sizeCategoryRequired}")
    private FbaSizeCategory sizeCategory;
    @NotNull(message = "{ecom.valid.fba.unitSystemRequired}")
    private FbaUnitSystem unitSystem;
    private BigDecimal maxLong;
    private BigDecimal maxMedian;
    private BigDecimal maxShort;
    private BigDecimal maxGirth;
    /** 三边合计上限（日本站按三辺合計分段） */
    private BigDecimal maxSidesSum;
    /** 单件实重上限 */
    private BigDecimal maxWeight;
    private BigDecimal dimDivisor;
    private Integer sort;
    private Integer builtin;

    /** 本分段的重量段费率（保存时整体替换） */
    @TableField(exist = false)
    private List<EcomFbaFeeRate> rates;
}
