package com.lemon.erp.modules.ecom.model;

/**
 * 包裹物流轨迹归一后的状态（字典 ecom_track_status）：各物流商、17TRACK 的状态码和轨迹描述都映射到这几种
 */
public enum EcomTrackStatus {
    /** 暂无轨迹（还没上网） */
    NOT_FOUND,
    /** 已揽收 */
    PICKED_UP,
    /** 运输中 */
    IN_TRANSIT,
    /** 到达待取（派送中、到达自提点） */
    PICKUP,
    /** 已签收 */
    DELIVERED,
    /** 异常（投递失败、清关异常、地址问题、丢件等） */
    EXCEPTION,
    /** 退回 */
    RETURNED;

    /** 是否为终态（签收、退回后不再拉取轨迹） */
    public boolean isFinal() {
        return this == DELIVERED || this == RETURNED;
    }
}
