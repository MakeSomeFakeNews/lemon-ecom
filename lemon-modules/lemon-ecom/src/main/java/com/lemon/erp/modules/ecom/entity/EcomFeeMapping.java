package com.lemon.erp.modules.ecom.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lemon.erp.common.base.BaseEntity;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.SettlementItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 结算费用类型映射：平台结算报告中的原始类型名（如亚马逊的 FBAPerUnitFulfillmentFee）→ 系统的结算明细类型。
 * 平台为 OTHER 的是通用映射，店铺平台没有对应的映射时使用；匹配时忽略首尾空格和大小写（raw_key）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ecom_fee_mapping")
public class EcomFeeMapping extends BaseEntity {

    @NotNull(message = "{ecom.valid.fee.platformRequired}")
    private EcomPlatform platform;

    @NotBlank(message = "{ecom.valid.fee.rawTypeRequired}")
    @Size(max = 128, message = "{ecom.valid.fee.rawTypeTooLong}")
    private String rawType;

    /** 原始类型名去掉首尾空格后的小写，由服务端填写 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String rawKey;

    @NotNull(message = "{ecom.valid.selectItemType}")
    private SettlementItemType itemType;

    /** 内置示例 1 是 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer builtin;

    private EnableStatus status;

    @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}")
    private String remark;
}
