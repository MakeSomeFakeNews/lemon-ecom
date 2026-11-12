package com.lemon.erp.modules.ecom.ads;

/**
 * 一项修改的结果
 *
 * @param platformId 新建的对象（否定关键词）在平台上的 ID，修改时为空
 * @param message    失败原因，成功为空
 */
public record AdsChangeResult(Long localId, boolean success, String platformId, String message) {

    public static AdsChangeResult ok(Long localId) {
        return new AdsChangeResult(localId, true, null, null);
    }

    public static AdsChangeResult failed(Long localId, String message) {
        return new AdsChangeResult(localId, false, null, message);
    }
}
