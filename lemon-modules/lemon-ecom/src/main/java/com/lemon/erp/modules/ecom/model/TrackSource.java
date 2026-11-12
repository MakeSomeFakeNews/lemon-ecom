package com.lemon.erp.modules.ecom.model;

/**
 * 物流轨迹的来源：包裹对接了物流商时先用物流商接口，没有对接或物流商查不到时用 17TRACK 通用查询
 */
public enum TrackSource {
    /** 物流商接口（4PX、云途） */
    CARRIER,
    /** 17TRACK 通用查询 */
    TRACK17
}
