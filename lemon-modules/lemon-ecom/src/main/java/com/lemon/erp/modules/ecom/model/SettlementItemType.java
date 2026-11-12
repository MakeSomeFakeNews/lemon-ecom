package com.lemon.erp.modules.ecom.model;

/**
 * 结算明细类型（字典 ecom_settlement_item_type）：订单收入、赔偿为正，退款和各项费用为负。
 * 导入时可直接填写类型，也可填写平台报告中的原始类型名，由「费用映射」（ecom_fee_mapping）识别
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
    /** 月度仓储费 */
    STORAGE,
    /** 长期仓储费 */
    LONG_STORAGE,
    /** 移除 / 弃置费 */
    REMOVAL,
    /** 退货处理费 */
    RETURN_FEE,
    /** 入库配置费 */
    INBOUND_FEE,
    /** 订阅费（店铺月租） */
    SUBSCRIPTION,
    /** 促销费 */
    PROMOTION,
    /** 优惠券 */
    COUPON,
    /** 秒杀（Deal）费 */
    DEAL,
    /** 广告费 */
    AD,
    /** 赔偿（Reimbursement，正数） */
    REIMBURSEMENT,
    /** 预留 / 释放（资金暂扣，不影响利润） */
    RESERVE,
    /** 平台代收代缴税（不影响利润：代收为正、代缴为负） */
    MARKETPLACE_TAX,
    /** 其他费用 */
    OTHER,
    /** 调整 */
    ADJUSTMENT;

    /** 回写到订单的费用栏 */
    public enum OrderFee {
        COMMISSION, FULFILLMENT, REFUND, RETURN_FEE, OTHER
    }

    /** 利润报表中的费用栏 */
    public enum ProfitColumn {
        SALES, REFUND, COMMISSION, FULFILLMENT, STORAGE, RETURN_FEE, OTHER, TAX
    }

    /** 计入订单费用的类型（按平台订单号回写到订单，重算利润） */
    public boolean orderFee() {
        return orderFeeColumn() != null;
    }

    /**
     * 挂订单的明细回写到订单的哪一栏；订单收入、广告费（广告花费取广告日报）、预留、代收代缴税不回写
     */
    public OrderFee orderFeeColumn() {
        return switch (this) {
            case COMMISSION -> OrderFee.COMMISSION;
            case FULFILLMENT -> OrderFee.FULFILLMENT;
            case REFUND -> OrderFee.REFUND;
            case RETURN_FEE -> OrderFee.RETURN_FEE;
            case ORDER, AD, RESERVE, MARKETPLACE_TAX -> null;
            default -> OrderFee.OTHER;
        };
    }

    /**
     * 不挂订单时是否分摊到 SKU：订单收入、退款按店铺计入，广告费取广告日报，预留与代收代缴税不影响利润，都不分摊
     */
    public boolean allocatable() {
        return this != ORDER && this != REFUND && this != AD && this != RESERVE && this != MARKETPLACE_TAX;
    }

    /** 默认分摊方式：仓储、移除、入库、配送、退货类费用按销量，其余按销售额 */
    public FeeAllocMethod defaultAllocMethod() {
        return switch (this) {
            case STORAGE, LONG_STORAGE, REMOVAL, INBOUND_FEE, FULFILLMENT, RETURN_FEE -> FeeAllocMethod.QTY;
            default -> FeeAllocMethod.SALES;
        };
    }

    /** 利润报表的费用栏；广告费、预留不计入（广告花费取广告日报） */
    public ProfitColumn profitColumn() {
        return switch (this) {
            case ORDER -> ProfitColumn.SALES;
            case REFUND -> ProfitColumn.REFUND;
            case COMMISSION -> ProfitColumn.COMMISSION;
            case FULFILLMENT -> ProfitColumn.FULFILLMENT;
            case STORAGE, LONG_STORAGE -> ProfitColumn.STORAGE;
            case RETURN_FEE -> ProfitColumn.RETURN_FEE;
            case MARKETPLACE_TAX -> ProfitColumn.TAX;
            case AD, RESERVE -> null;
            default -> ProfitColumn.OTHER;
        };
    }
}
