package com.lemon.erp.modules.ecom.publish.model;

/**
 * 自动调价策略（字典 ecom_reprice_strategy）
 */
public enum RepriceStrategy {
    /** 跟随竞品最低价 */
    MATCH_LOWEST,
    /** 低于竞品最低价 X（金额或百分比） */
    BEAT_LOWEST,
    /** 跟随购物车价（已持有购物车时不变） */
    MATCH_BUYBOX,
    /** 按价格模板与目标利润率计算 */
    TARGET_MARGIN,
    /** 按库存 / 销量：可售天数多时降价、少时涨价 */
    BY_STOCK
}
