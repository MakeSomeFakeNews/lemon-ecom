package com.lemon.erp.modules.ecom.oper.model;

/**
 * 商品监控的变化预警（字典 ecom_monitor_alert_type）
 */
public enum EcomMonitorAlertType {
    /** 大类排名大跌（变差超过监控设置的百分比） */
    RANK_DROP,
    /** 评分下降（达到监控设置的分值） */
    RATING_DROP,
    /** 新增跟卖（在售卖家数增加） */
    NEW_SELLER,
    /** 标题被改 */
    TITLE_CHANGED,
    /** 主图被改 */
    IMAGE_CHANGED,
    /** 五点描述被改 */
    BULLETS_CHANGED,
    /** 被下架（在售变为下架） */
    DELISTED
}
