package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.FulfillmentType;
import jakarta.validation.constraints.Size;

/**
 * 修改平台订单的收件信息、配送方式与备注（未发货时）
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
        @Size(max = 255, message = "{ecom.valid.remarkCannotExceedCharacters}") String remark) {
}
