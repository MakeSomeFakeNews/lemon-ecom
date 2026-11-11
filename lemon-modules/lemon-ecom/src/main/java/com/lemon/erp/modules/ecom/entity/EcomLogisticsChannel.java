package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 国际物流渠道：物流商的一条线路（如 某货代 美国空派、UPS 快递），按国家 / 重量段的费率试算运费，头程货件和自发货订单选择渠道记录运费
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_logistics_channel")
public class EcomLogisticsChannel extends BaseEntity {

    @NotBlank(message = "{ecom.valid.channelCodeRequired}")
    @Size(max = 32, message = "{ecom.valid.channelCodeCannotExceedCharacters}")
    private String code;

    @NotBlank(message = "{ecom.valid.channelNameRequired}")
    @Size(max = 64, message = "{ecom.valid.channelNameCannotExceedCharacters}")
    private String name;

    /** 物流商 / 货代名称 */
    @Size(max = 64, message = "{ecom.valid.providerCannotExceedCharacters}")
    private String provider;

    /** 运输方式（业务字典 ecom_transport_mode） */
    @Size(max = 16)
    private String transportMode;

    /** 对应的供应商（结算运费的往来单位），可空 */
    private Long supplierId;

    /** 参考时效（天） */
    @Min(value = 0, message = "{ecom.valid.daysRange}")
    @Max(value = 365, message = "{ecom.valid.daysRange}")
    private Integer daysMin;
    @Min(value = 0, message = "{ecom.valid.daysRange}")
    @Max(value = 365, message = "{ecom.valid.daysRange}")
    private Integer daysMax;

    private EnableStatus status;

    @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}")
    private String remark;

    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private Integer rateCount;
    @TableField(exist = false)
    private List<EcomLogisticsRate> rates;
}
