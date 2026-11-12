package com.lemon.erp.modules.ecom.dev.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.modules.ecom.dev.model.DevSampleResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 开发项目的样品记录：供应商、样品费、到样日期、评估结论
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_dev_sample")
public class EcomDevSample extends BaseEntity {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long projectId;
    private Long supplierId;
    private String supplierName;
    private String sampleUrl;
    private Integer sampleQty;
    /** 样品费（本位币，含运费） */
    private BigDecimal sampleFee;
    /** 大货报价（本位币） */
    private BigDecimal quotePrice;
    private LocalDate requestDate;
    private LocalDate arrivalDate;
    private DevSampleResult result;
    private String evaluation;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long createBy;

    /** 供应商档案名称（supplierId 不为空时） */
    @TableField(exist = false)
    private String supplierRecordName;
    @TableField(exist = false)
    private String createByName;
}
