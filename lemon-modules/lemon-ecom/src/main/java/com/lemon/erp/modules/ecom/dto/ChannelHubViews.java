package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.connector.AuthType;
import com.lemon.erp.modules.ecom.connector.ConnectorCapability;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.StoreAuthStatus;
import com.lemon.erp.modules.ecom.model.SyncType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 渠道接入总览：按平台分组的店铺授权状态、各能力最近一次同步结果、近 24 小时失败次数
 */
public final class ChannelHubViews {

    private ChannelHubViews() {
    }

    /**
     * 一个平台
     *
     * @param supported 是否已有渠道接口实现（没有时只能手工导入）
     */
    public record PlatformGroup(EcomPlatform platform, boolean supported, AuthType authType, Set<ConnectorCapability> capabilities,
                                boolean hasSandbox, String docUrl, List<StoreHealth> stores) {
    }

    /**
     * 一个店铺
     *
     * @param problem 有问题：需重新授权、最近一次同步失败或近 24 小时有失败
     */
    public record StoreHealth(Long id, String code, String name, String marketplace, EnableStatus status, boolean sandbox,
                              StoreAuthStatus authStatus, LocalDateTime authorizedTime, LocalDateTime lastSyncTime, String lastSyncMessage,
                              boolean orderSync, boolean inventorySync, List<SyncHealth> syncs, int failures24h, boolean problem) {
    }

    /**
     * 店铺的一类同步（拉取订单、回传发货、回传库存、拉取商品、授权）
     *
     * @param supported 平台接口是否支持（授权始终为 true）
     * @param success   最近一次是否成功，没有记录为 null
     */
    public record SyncHealth(SyncType type, boolean supported, Boolean success, LocalDateTime time, String message, int failures24h) {
    }

    /** 同步日志统计（Mapper 结果） */
    @Data
    public static class LogStat {
        private Long storeId;
        private SyncType syncType;
        private Integer success;
        private LocalDateTime startTime;
        private String message;
        private Integer failures;
    }
}
