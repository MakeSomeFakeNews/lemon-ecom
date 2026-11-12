package com.lemon.erp.modules.ecom.dev.dto;

import com.lemon.erp.modules.ecom.model.TaxType;

import java.math.BigDecimal;

/**
 * 平台 / 站点 / 类目的默认试算参数（页面切换平台时带出）
 *
 * @param commissionRateId 匹配到的佣金费率，没有时为空
 * @param exchangeRate     当月汇率，没有维护时为空
 */
public record ProfitDefaults(Long commissionRateId, BigDecimal commissionRate, BigDecimal minCommission, BigDecimal fixedFee, String currency,
                             BigDecimal exchangeRate, TaxType taxType, BigDecimal taxRate, String country) {
}
