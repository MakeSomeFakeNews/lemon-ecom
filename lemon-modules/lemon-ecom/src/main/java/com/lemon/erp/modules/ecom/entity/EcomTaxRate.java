package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.model.TaxType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 电商税率：收件国家（+ 可选的店铺站点）→ 税种、税率、是否平台代扣代缴。订单写入时按 收件国家 + 店铺站点 匹配
 * （站点相同的优先，其次为全部站点），记到订单上；之后修改税率不影响已有订单，可在「税务」中按日期范围重算
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_tax_rate")
public class EcomTaxRate extends BaseEntity {

    @NotBlank(message = "{ecom.valid.tax.countryRequired}")
    @Size(max = 8, message = "{ecom.valid.tax.countryTooLong}")
    private String country;

    /** 店铺站点（业务字典 ecom_marketplace），空为全部站点 */
    @Size(max = 16)
    private String marketplace;

    @NotNull(message = "{ecom.valid.tax.taxTypeRequired}")
    private TaxType taxType;

    @NotNull(message = "{ecom.valid.tax.rateRequired}")
    @DecimalMin(value = "0", message = "{ecom.valid.tax.rateRange}")
    @DecimalMax(value = "100", message = "{ecom.valid.tax.rateRange}")
    @Digits(integer = 3, fraction = 2, message = "{ecom.valid.tax.rateRange}")
    private BigDecimal rate;

    /** 平台代扣代缴（Marketplace Facilitator）1 是 0 否 */
    private Integer facilitator;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer builtin;

    private EnableStatus status;

    @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}")
    private String remark;
}
