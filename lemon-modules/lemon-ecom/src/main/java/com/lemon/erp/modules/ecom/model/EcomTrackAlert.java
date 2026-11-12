package com.lemon.erp.modules.ecom.model;

/**
 * 物流轨迹预警（字典 ecom_track_alert）：拉取轨迹后按订单作业设置的时限判断
 */
public enum EcomTrackAlert {
    /** 超时未上网：发货后超过设定小时数仍没有轨迹 */
    NO_SCAN,
    /** 运输超时：上网后超过设定天数未签收，或轨迹超过设定天数没有更新 */
    TRANSIT_TIMEOUT,
    /** 轨迹异常：最新状态为异常 / 退回 */
    EXCEPTION
}
