package com.lemon.erp.modules.ecom.carrier;

import com.lemon.erp.modules.ecom.model.EcomTrackStatus;

import java.util.List;

/**
 * 查询轨迹的结果：归一后的整体状态与轨迹（物流商返回的顺序）
 *
 * @param status 整体状态（物流商返回了状态码时取它，否则按最新一条轨迹的描述判断）
 */
public record CarrierTrackInfo(EcomTrackStatus status, List<CarrierTrackEvent> events) {

    public CarrierTrackInfo {
        events = events == null ? List.of() : List.copyOf(events);
        status = status == null ? TrackStatusNormalizer.overall(events) : status;
    }

    /** 只有轨迹、按描述判断状态 */
    public static CarrierTrackInfo of(List<CarrierTrackEvent> events) {
        return new CarrierTrackInfo(null, events);
    }
}
