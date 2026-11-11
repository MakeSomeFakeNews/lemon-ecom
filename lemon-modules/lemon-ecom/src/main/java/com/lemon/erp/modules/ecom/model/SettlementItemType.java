package com.lemon.erp.modules.ecom.model;

/**
 * 结算明细类型（字典 ecom_settlement_item_type）：订单收入为正，退款和各项费用为负
 */
public enum SettlementItemType {
    /** 订单收入 */
    ORDER,
    /** 退款 */
    REFUND,
    /** 平台佣金 */
    COMMISSION,
    /** 配送费（FBA 等平台配送） */
    FULFILLMENT,
    /** 仓储费 */
    STORAGE,
    /** 广告费 */
    AD,
    /** 其他费用 */
    OTHER,
    /** 调整 */
    ADJUSTMENT;

    /** 计入订单费用的类型（按平台订单号回写到订单，重算利润） */
    public boolean orderFee() {
        return this == COMMISSION || this == FULFILLMENT;
    }
}
