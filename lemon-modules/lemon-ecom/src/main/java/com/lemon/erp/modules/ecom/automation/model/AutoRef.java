package com.lemon.erp.modules.ecom.automation.model;

/**
 * 事件关联的业务对象：动作按需要的对象决定能否用于某个触发器（如挂起订单需要 ORDER，调价需要 LISTING）
 */
public enum AutoRef {
    /** 平台订单 */
    ORDER,
    /** 在线商品 */
    LISTING,
    /** 本地物料 */
    MATERIAL,
    /** 广告活动 */
    CAMPAIGN,
    /** 商品评价 */
    REVIEW,
    /** 买家消息会话 */
    THREAD,
    /** 店铺 */
    STORE,
    /** 头程货件 */
    INBOUND,
    /** FBA 索赔 */
    CLAIM,
    /** 商品监控 */
    MONITOR
}
