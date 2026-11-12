package com.lemon.erp.modules.ecom.model;

/**
 * 利润报表的口径
 */
public enum ProfitBasis {
    /** 订单口径：按下单时间，费用为订单上的估算或已回写的实际值（实时） */
    ORDER,
    /** 结算口径：按结算明细的入账日期，收入与费用取结算报告的实际金额 */
    SETTLEMENT
}
