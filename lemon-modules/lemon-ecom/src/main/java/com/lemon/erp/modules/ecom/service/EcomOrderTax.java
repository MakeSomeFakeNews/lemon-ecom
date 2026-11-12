package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomTaxRate;
import com.lemon.erp.modules.ecom.model.TaxType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单税额拆分（订单币种）。订单上记录写入时匹配到的税种、税率、是否平台代扣代缴（{@link #assign}），
 * 每次重算利润前按订单金额和退款重新计算税额（{@link #apply}）：
 * <ul>
 *     <li>税额：价外税（销售税）取订单的税费；价内税（VAT / GST）有税费时取税费，没有时按 买家实付 × 税率 ÷ (100 + 税率) 拆出</li>
 *     <li>按退款比例冲减：税额 × (实付 − 退款) ÷ 实付，不为负</li>
 *     <li>平台代扣代缴的记为 withheldTax（不计收入、不需申报），否则记为 vatAmount（需卖家申报的销项税，利润按不含税收入计算）</li>
 * </ul>
 * 没有税种（没有配置或没有匹配到启用的税率）时两者都为 0，利润与不拆税时一致
 */
public final class EcomOrderTax {

    private EcomOrderTax() {
    }

    /** 记录订单适用的税率；rate 为空时清除（不拆税） */
    public static void assign(EcomOrder o, EcomTaxRate rate) {
        if (rate == null) {
            o.setTaxType(null);
            o.setVatRate(null);
            o.setTaxFacilitator(0);
        } else {
            o.setTaxType(rate.getTaxType());
            o.setVatRate(Decimals.nz(rate.getRate()));
            o.setTaxFacilitator(rate.getFacilitator() != null && rate.getFacilitator() == 1 ? 1 : 0);
        }
    }

    /** 按订单上的税率计算 vatAmount / withheldTax（写回订单） */
    public static void apply(EcomOrder o) {
        BigDecimal tax = taxOf(o);
        boolean withheld = o.getTaxFacilitator() != null && o.getTaxFacilitator() == 1;
        o.setVatAmount(withheld ? BigDecimal.ZERO : tax);
        o.setWithheldTax(withheld ? tax : BigDecimal.ZERO);
    }

    static BigDecimal taxOf(EcomOrder o) {
        TaxType type = o.getTaxType();
        BigDecimal total = Decimals.nz(o.getTotalAmount());
        if (type == null || total.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal taxAmount = Decimals.nz(o.getTaxAmount());
        BigDecimal rate = Decimals.nz(o.getVatRate());
        BigDecimal tax;
        if (taxAmount.signum() > 0) {
            tax = taxAmount;
        } else if (type.inclusive() && rate.signum() > 0) {
            tax = total.multiply(rate).divide(Decimals.HUNDRED.add(rate), 6, RoundingMode.HALF_UP);
        } else {
            return BigDecimal.ZERO;
        }
        BigDecimal kept = total.subtract(Decimals.nz(o.getRefundAmount())).max(BigDecimal.ZERO);
        return Decimals.money(tax.multiply(kept).divide(total, 6, RoundingMode.HALF_UP));
    }
}
