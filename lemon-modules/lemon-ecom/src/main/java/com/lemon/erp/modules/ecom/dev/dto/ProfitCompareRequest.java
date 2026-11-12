package com.lemon.erp.modules.ecom.dev.dto;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 多平台对比：同一产品（采购价、尺寸重量、头程、广告占比、退货率等取 base），在多个平台 / 站点按各自的佣金、币种、汇率、税率试算
 */
public record ProfitCompareRequest(
        @NotNull(message = "{ecom.valid.dev.paramsRequired}") @Valid ProfitCalcRequest base,
        @NotEmpty(message = "{ecom.valid.dev.targetsRequired}") @Size(max = 12, message = "{ecom.valid.dev.tooManyTargets}") List<@Valid Target> targets) {

    /**
     * @param salePrice      该平台的售价（站点币种），为空时按 targetMargin 反算
     * @param fulfillmentFee 该平台的配送费（站点币种），亚马逊美国站可按尺寸自动估算
     */
    public record Target(@NotNull(message = "{ecom.valid.dev.platformRequired}") EcomPlatform platform, String marketplace, String category,
                         BigDecimal salePrice, BigDecimal targetMargin, BigDecimal fulfillmentFee) {
    }
}
