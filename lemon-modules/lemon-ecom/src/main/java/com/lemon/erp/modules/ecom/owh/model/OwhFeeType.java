package com.lemon.erp.modules.ecom.owh.model;

/**
 * 海外仓费用类型（字典 ecom_owh_fee_type）：出库单的操作费、尾程运费计入订单物流成本；仓租、入库费按 SKU 分摊到店铺 SKU（利润报表仓储费 / 其他费用）
 */
public enum OwhFeeType {
    /** 仓租 */
    STORAGE,
    /** 操作费（出库拣货、打包） */
    OPERATION,
    /** 尾程运费 */
    FREIGHT,
    /** 入库费（卸货、上架） */
    INBOUND,
    /** 其他 */
    OTHER;

    /** 跟随出库单、计入订单物流成本的费用 */
    public boolean orderCost() {
        return this == OPERATION || this == FREIGHT;
    }
}
