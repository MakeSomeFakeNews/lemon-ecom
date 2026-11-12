package com.lemon.erp.modules.ecom.dev.dto;

import com.lemon.erp.modules.ecom.dev.model.DevHeadPricing;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.TaxType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 利润试算参数。金额：采购价、其他成本、头程单价为本位币；售价、配送费、站点其他费用、最低佣金、固定费为站点币种。
 * 比率都是百分数。留空的佣金率、币种、汇率、税率按「佣金费率」「币种汇率」「税务」中的设置自动带出
 *
 * @param salePrice          售价（站点币种）；为空且填写了目标利润率时反算建议售价
 * @param targetMargin       目标利润率（%），填写后同时返回建议售价
 * @param purchasePrice      采购价（本位币，含税）
 * @param otherCost          其他成本（本位币：包材、贴标、质检等）
 * @param transportMode      头程运输方式（业务字典 ecom_transport_mode），SEA 时按实重计费，其他按 max(实重, 体积重 ÷ 6000)
 * @param headPricing        头程计费方式；为空时使用 headFee
 * @param logisticsChannelId 物流渠道（headPricing = CHANNEL）
 * @param headUnitPrice      头程单价（本位币 / kg 或 / m³）
 * @param headFee            手填的单件头程费用（本位币）
 * @param dutyRate           进口关税率（%，按采购价计）
 * @param fbaAuto            按尺寸自动估算亚马逊美国站 FBA 配送费
 * @param fulfillmentFee     手填的配送费 / 尾程运费（站点币种）
 * @param otherSiteFee       站点其他费用（站点币种：仓储费、入库配置费等）
 * @param taxType            税种：VAT / GST 为价内税从售价中扣除，销售税由平台代收不计成本
 * @param projectId          开发项目（保存方案时关联）
 */
public record ProfitCalcRequest(
        @NotNull(message = "{ecom.valid.dev.platformRequired}") EcomPlatform platform,
        @Size(max = 16, message = "{ecom.valid.dev.marketplaceTooLong}") String marketplace,
        @Size(max = 64, message = "{ecom.valid.dev.categoryTooLong}") String category,
        @Size(max = 8, message = "{ecom.valid.dev.currencyTooLong}") String currency,
        @DecimalMin(value = "0", inclusive = false, message = "{ecom.valid.dev.positive}") BigDecimal exchangeRate,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal salePrice,
        @DecimalMax(value = "99", message = "{ecom.valid.dev.marginRange}") BigDecimal targetMargin,
        @NotNull(message = "{ecom.valid.dev.purchasePriceRequired}") @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal purchasePrice,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal otherCost,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal weightKg,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal lengthCm,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal widthCm,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal heightCm,
        String transportMode,
        DevHeadPricing headPricing,
        Long logisticsChannelId,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal headUnitPrice,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal headFee,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.rateRange}") @DecimalMax(value = "100", message = "{ecom.valid.dev.rateRange}") BigDecimal dutyRate,
        Boolean fbaAuto,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal fulfillmentFee,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.rateRange}") @DecimalMax(value = "100", message = "{ecom.valid.dev.rateRange}") BigDecimal commissionRate,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal minCommission,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal fixedFee,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.rateRange}") @DecimalMax(value = "100", message = "{ecom.valid.dev.rateRange}") BigDecimal adRate,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.rateRange}") @DecimalMax(value = "100", message = "{ecom.valid.dev.rateRange}") BigDecimal returnRate,
        TaxType taxType,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.rateRange}") @DecimalMax(value = "100", message = "{ecom.valid.dev.rateRange}") BigDecimal taxRate,
        @DecimalMin(value = "0", message = "{ecom.valid.dev.nonNegative}") BigDecimal otherSiteFee,
        Long projectId) {

    /** 换成另一个平台 / 站点（多平台对比）：清空随平台变化的佣金、币种、汇率、税率，售价按对比项；FBA 自动估算只对亚马逊生效 */
    public ProfitCalcRequest forTarget(EcomPlatform p, String mkt, String cat, BigDecimal price, BigDecimal margin, BigDecimal fulfillment) {
        return new ProfitCalcRequest(p, mkt, cat, null, null, price, margin, purchasePrice, otherCost, weightKg, lengthCm, widthCm,
                heightCm, transportMode, headPricing == DevHeadPricing.CHANNEL ? null : headPricing, null, headUnitPrice,
                headPricing == DevHeadPricing.CHANNEL ? null : headFee, dutyRate, Boolean.TRUE.equals(fbaAuto) && p == EcomPlatform.AMAZON, fulfillment, null, null, null, adRate, returnRate,
                null, null, otherSiteFee, projectId);
    }
}
