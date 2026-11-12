package com.lemon.erp.modules.ecom.dev.dto;

import com.lemon.erp.modules.ecom.dev.model.DevFbaSizeTier;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.TaxType;

import java.math.BigDecimal;
import java.util.List;

/**
 * 利润试算结果：本位币金额（base）与站点币种金额（site）并列
 *
 * @param revenue        销售收入（本位币）= 售价 × 汇率
 * @param lines          成本明细（key：purchase 采购、head 头程、duty 关税、other 其他成本、commission 佣金、fulfillment 配送费、
 *                       tax 税费、ad 广告、returns 退货损失、siteOther 站点其他费用）
 * @param profit         单件利润（本位币）
 * @param profitSite     单件利润（站点币种）
 * @param margin         利润率（%）= 利润 ÷ 收入
 * @param roi            投资回报率（%）= 利润 ÷（采购 + 头程 + 关税 + 其他成本）
 * @param breakevenPrice 保本售价（站点币种），无法保本时为空
 * @param suggestPrice   目标利润率下的建议售价（站点币种），没有填写目标利润率或无法达到时为空
 * @param fbaTier        内置美国站估算的尺寸分段（按「FBA 费率」中维护的分段计算时为空）
 * @param fbaTierName    FBA 尺寸分段名称（按维护的分段计算时）
 * @param fbaShippingWeight FBA 计费的发货重量，单位见 fbaWeightUnit（lb / kg）
 * @param chargeWeightKg 头程计费重（kg）
 * @param warnings       提示（已按当前语言）
 */
public record ProfitResult(EcomPlatform platform, String marketplace, String category, String currency, BigDecimal exchangeRate,
                           BigDecimal salePrice, BigDecimal revenue, BigDecimal commissionRate, BigDecimal minCommission, BigDecimal fixedFee,
                           TaxType taxType, BigDecimal taxRate, DevFbaSizeTier fbaTier, String fbaTierName, BigDecimal fbaShippingWeight,
                           String fbaWeightUnit, BigDecimal chargeWeightKg,
                           BigDecimal volumeCbm, List<CostLine> lines, BigDecimal totalCost, BigDecimal profit, BigDecimal profitSite,
                           BigDecimal margin, BigDecimal roi, BigDecimal breakevenPrice, BigDecimal targetMargin, BigDecimal suggestPrice,
                           List<String> warnings) {

    /**
     * @param site 站点币种金额（本位币成本为空）
     * @param base 本位币金额
     */
    public record CostLine(String key, BigDecimal site, BigDecimal base) {
    }
}
