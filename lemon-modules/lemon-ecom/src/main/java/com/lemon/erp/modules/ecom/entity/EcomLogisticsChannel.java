package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.modules.ecom.model.EcomCarrier;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

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

    // ---------------------------------------------------------------- 物流商面单对接（为空表示未对接，包裹手工填写运单号）

    /** 对接的物流商 */
    private EcomCarrier carrier;
    /** 物流商的产品 / 运输方式代码（4PX logistics_product_code、云途 ShippingMethodCode） */
    @Size(max = 64, message = "{ecom.valid.ops.carrierServiceTooLong}")
    private String carrierService;
    /** 物流商授权（JSON，CredentialCipher 加密保存），只写不读 */
    @JsonIgnore
    private String carrierCredentials;
    /** 物流商接口环境 1测试 0正式 */
    private Integer carrierSandbox;
    /** 自定义接口地址（私有网关、测试用模拟服务器），为空按环境取默认地址 */
    @Size(max = 255, message = "{ecom.valid.ops.carrierApiUrlTooLong}")
    private String carrierApiUrl;

    /** 保存时提交的授权字段（与已保存的合并，为空的字段不修改） */
    @TableField(exist = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Map<String, String> carrierAuth;
    /** 是否已保存物流商授权 */
    @TableField(exist = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean hasCarrierCredentials;

    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private Integer rateCount;
    @TableField(exist = false)
    private List<EcomLogisticsRate> rates;
}
