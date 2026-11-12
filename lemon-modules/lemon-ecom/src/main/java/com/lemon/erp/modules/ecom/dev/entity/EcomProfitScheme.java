package com.lemon.erp.modules.ecom.dev.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 利润试算方案：试算参数（JSON）与结果摘要，可在试算器中载入；在开发项目中保存时回写项目的试算利润与利润率
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_profit_scheme")
public class EcomProfitScheme extends BaseEntity {

    private String name;
    private Long projectId;
    private EcomPlatform platform;
    private String marketplace;
    private String currency;
    private BigDecimal salePrice;
    private BigDecimal profit;
    private BigDecimal margin;
    private BigDecimal roi;
    /** 试算参数 JSON（ProfitCalcRequest），列表不返回 */
    private String params;
    private Long createBy;

    @TableField(exist = false)
    private String projectName;
    @TableField(exist = false)
    private String createByName;
}
