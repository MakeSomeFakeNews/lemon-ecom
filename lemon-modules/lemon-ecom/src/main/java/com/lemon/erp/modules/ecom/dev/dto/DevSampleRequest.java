package com.lemon.erp.modules.ecom.dev.dto;

import com.lemon.erp.modules.ecom.dev.model.DevSampleResult;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 样品记录：供应商档案或名称至少填一个
 */
public record DevSampleRequest(Long supplierId,
                               @Size(max = 128, message = "{ecom.valid.dev.supplierNameTooLong}") String supplierName,
                               @Size(max = 500, message = "{ecom.valid.dev.urlTooLong}") String sampleUrl,
                               @Min(value = 0, message = "{ecom.valid.dev.nonNegative}") Integer sampleQty,
                               @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal sampleFee,
                               @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal quotePrice,
                               LocalDate requestDate,
                               LocalDate arrivalDate,
                               DevSampleResult result,
                               @Size(max = 1000, message = "{ecom.valid.dev.evaluationTooLong}") String evaluation) {
}
