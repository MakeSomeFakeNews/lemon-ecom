package com.lemon.erp.modules.ecom.dto;

/**
 * 一个店铺一次订单同步的结果
 *
 * @param fetched 从平台取到的订单数
 * @param created 新增
 * @param updated 状态有变化而更新的
 * @param message 失败原因，成功为空
 */
public record SyncResult(Long storeId, String storeName, int fetched, int created, int updated, String message) {

    public static SyncResult failed(Long storeId, String storeName, String message) {
        return new SyncResult(storeId, storeName, 0, 0, 0, message);
    }
}
