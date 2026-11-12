package com.lemon.erp.modules.ecom.fba.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.lemon.erp.modules.ecom.fba.model.FbaSizeCategory;
import com.lemon.erp.modules.ecom.fba.model.FbaStorageFeeType;
import com.lemon.erp.modules.ecom.fba.model.FbaVolumeUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * FBA 仓储费费率：月度仓储费按 尺寸类别 + 月份，超龄附加费按 尺寸类别 + 库龄段，每单位体积的费用
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_fba_storage_rate")
public class EcomFbaStorageRate extends BaseEntity {

    @NotBlank(message = "{ecom.valid.selectMarketplace}")
    @Size(max = 16)
    private String marketplace;
    @NotNull(message = "{ecom.valid.fba.feeTypeRequired}")
    private FbaStorageFeeType feeType;
    @NotNull(message = "{ecom.valid.fba.sizeCategoryRequired}")
    private FbaSizeCategory sizeCategory;
    @Min(value = 1, message = "{ecom.valid.fba.monthRange}")
    @Max(value = 12, message = "{ecom.valid.fba.monthRange}")
    private Integer monthFrom;
    @Min(value = 1, message = "{ecom.valid.fba.monthRange}")
    @Max(value = 12, message = "{ecom.valid.fba.monthRange}")
    private Integer monthTo;
    @Min(value = 0, message = "{ecom.valid.fba.ageRange}")
    private Integer ageFrom;
    @Min(value = 0, message = "{ecom.valid.fba.ageRange}")
    private Integer ageTo;
    @NotNull(message = "{ecom.valid.fba.rateRequired}")
    @DecimalMin(value = "0", message = "{ecom.valid.fba.rateRequired}")
    private BigDecimal rate;
    @NotNull(message = "{ecom.valid.fba.volumeUnitRequired}")
    private FbaVolumeUnit volumeUnit;
    @NotBlank(message = "{ecom.valid.selectCurrency}")
    @Size(max = 8)
    private String currency;
    private Integer builtin;
    @Size(max = 255)
    private String remark;
}
