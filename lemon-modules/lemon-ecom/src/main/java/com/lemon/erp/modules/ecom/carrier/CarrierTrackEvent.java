package com.lemon.erp.modules.ecom.carrier;

/**
 * 一条物流轨迹
 *
 * @param time        发生时间（物流商返回的原文）
 * @param location    地点
 * @param description 描述
 */
public record CarrierTrackEvent(String time, String location, String description) {
}
