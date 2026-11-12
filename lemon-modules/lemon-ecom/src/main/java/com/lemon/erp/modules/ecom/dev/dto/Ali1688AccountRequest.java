package com.lemon.erp.modules.ecom.dev.dto;

import com.lemon.erp.common.enums.EnableStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 1688 账号：AppSecret 修改时留空表示不变；收货地址用于一键下单
 */
public record Ali1688AccountRequest(
        @NotBlank(message = "{ecom.valid.dev.accountNameRequired}") @Size(max = 64, message = "{ecom.valid.dev.accountNameTooLong}") String name,
        @NotBlank(message = "{ecom.valid.dev.appKeyRequired}") @Size(max = 64, message = "{ecom.valid.dev.appKeyTooLong}") String appKey,
        @Size(max = 128, message = "{ecom.valid.dev.appSecretTooLong}") String appSecret,
        @Size(max = 255, message = "{ecom.valid.dev.urlTooLong}") @Pattern(regexp = "^(https?://.+)?$", message = "{ecom.valid.dev.apiUrlInvalid}") String apiUrl,
        @Size(max = 64, message = "{ecom.valid.dev.receiverTooLong}") String receiverName,
        @Size(max = 32, message = "{ecom.valid.dev.receiverTooLong}") String receiverMobile,
        @Size(max = 32, message = "{ecom.valid.dev.receiverTooLong}") String province,
        @Size(max = 32, message = "{ecom.valid.dev.receiverTooLong}") String city,
        @Size(max = 32, message = "{ecom.valid.dev.receiverTooLong}") String district,
        @Size(max = 32, message = "{ecom.valid.dev.receiverTooLong}") String town,
        @Size(max = 255, message = "{ecom.valid.dev.receiverTooLong}") String address,
        @Size(max = 16, message = "{ecom.valid.dev.receiverTooLong}") String postCode,
        Integer isDefault,
        EnableStatus status,
        @Size(max = 255, message = "{ecom.valid.dev.remarkTooLong}") String remark) {
}
