package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.FulfillmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 修改平台订单的收件信息、配送方式、预估重量与备注（未发货、未打单时）
 *
 * @param weight 预估重量（kg），审单规则按它匹配重量区间，也是包裹的默认重量
 */
public record OrderEditRequest(
        @Size(max = 64, message = "{ecom.valid.nameCannotExceedCharacters}") String shipName,
        @Size(max = 32, message = "{ecom.valid.phoneCannotExceedCharacters}") String shipPhone,
        @Size(max = 8, message = "{ecom.valid.countryCannotExceedCharacters}") String shipCountry,
        @Size(max = 64, message = "{ecom.valid.addressPartCannotExceedCharacters}") String shipState,
        @Size(max = 64, message = "{ecom.valid.addressPartCannotExceedCharacters}") String shipCity,
        @Size(max = 16, message = "{ecom.valid.postcodeCannotExceedCharacters}") String shipPostcode,
        @Size(max = 255, message = "{ecom.valid.addressCannotExceedCharacters}") String shipAddress,
        FulfillmentType fulfillment,
        @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}") String remark,
        @DecimalMin(value = "0", message = "{ecom.valid.weightCannotNegative}")
        @Digits(integer = 12, fraction = 3, message = "{ecom.valid.weightDecimals}") BigDecimal weight) {
}
