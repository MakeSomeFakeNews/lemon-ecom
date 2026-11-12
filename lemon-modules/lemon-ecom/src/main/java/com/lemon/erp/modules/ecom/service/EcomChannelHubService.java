package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.connector.ChannelConnector;
import com.lemon.erp.modules.ecom.connector.ConnectorCapability;
import com.lemon.erp.modules.ecom.connector.ConnectorInfo;
import com.lemon.erp.modules.ecom.connector.ConnectorRegistry;
import com.lemon.erp.modules.ecom.dto.ChannelHubViews.LogStat;
import com.lemon.erp.modules.ecom.dto.ChannelHubViews.PlatformGroup;
import com.lemon.erp.modules.ecom.dto.ChannelHubViews.StoreHealth;
import com.lemon.erp.modules.ecom.dto.ChannelHubViews.SyncHealth;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.entity.EcomSyncLog;
import com.lemon.erp.modules.ecom.mapper.EcomChannelMapper;
import com.lemon.erp.modules.ecom.mapper.EcomSyncLogMapper;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.StoreAuthStatus;
import com.lemon.erp.modules.ecom.model.SyncType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 渠道接入总览：按平台分组列出全部店铺的授权状态、环境、各类同步最近一次的结果与近 24 小时失败次数；店铺列表的授权状态；同步日志清理
 */
@Service
@RequiredArgsConstructor
public class EcomChannelHubService {

    /** 总览中列出的同步类型（平台仓库存暂无同步任务，不列出） */
    static final List<SyncType> HUB_TYPES = List.of(SyncType.AUTH, SyncType.ORDERS, SyncType.SHIP_CONFIRM, SyncType.INVENTORY_PUSH, SyncType.LISTINGS);
    private static final Map<SyncType, ConnectorCapability> CAPABILITY = Map.of(SyncType.ORDERS, ConnectorCapability.ORDERS,
            SyncType.SHIP_CONFIRM, ConnectorCapability.SHIP_CONFIRM, SyncType.INVENTORY_PUSH, ConnectorCapability.INVENTORY_PUSH,
            SyncType.LISTINGS, ConnectorCapability.LISTINGS);

    private final EcomStoreService storeService;
    private final EcomSyncService syncService;
    private final ConnectorRegistry registry;
    private final EcomChannelMapper channelMapper;
    private final EcomSyncLogMapper logMapper;

    /** 店铺列表补充授权状态 */
    public void fillAuthStatus(List<EcomStore> stores) {
        if (stores.isEmpty()) {
            return;
        }
        List<Long> ids = stores.stream().map(EcomStore::getId).toList();
        Map<Long, Boolean> lastAuth = syncService.lastAuthSuccess(ids);
        Map<Long, EcomStore> full = new HashMap<>();
        // 列表不查询授权信息，按 ID 取完整记录计算
        storeService.byIds(ids).forEach(s -> full.put(s.getId(), s));
        for (EcomStore s : stores) {
            EcomStore f = full.getOrDefault(s.getId(), s);
            s.setAuthStatus(syncService.authStatus(f, storeService.credentials(f), lastAuth.get(s.getId())));
        }
    }

    /** 渠道接入总览：已有渠道接口的平台在前（按平台枚举顺序），没有店铺的平台不列出 */
    public List<PlatformGroup> overview() {
        List<EcomStore> stores = storeService.all();
        Map<Long, Map<SyncType, LogStat>> latest = new HashMap<>();
        if (!stores.isEmpty()) {
            for (LogStat s : channelMapper.selectLatestLogs(null)) {
                latest.computeIfAbsent(s.getStoreId(), k -> new EnumMap<>(SyncType.class)).put(s.getSyncType(), s);
            }
        }
        Map<Long, Map<SyncType, Integer>> failures = new HashMap<>();
        for (LogStat s : channelMapper.selectFailCounts(LocalDateTime.now().minusHours(24))) {
            failures.computeIfAbsent(s.getStoreId(), k -> new EnumMap<>(SyncType.class)).put(s.getSyncType(), s.getFailures());
        }
        Map<EcomPlatform, List<StoreHealth>> byPlatform = new EnumMap<>(EcomPlatform.class);
        for (EcomStore s : stores) {
            ChannelConnector connector = registry.find(s.getPlatform()).orElse(null);
            Map<SyncType, LogStat> last = latest.getOrDefault(s.getId(), Map.of());
            Map<SyncType, Integer> fails = failures.getOrDefault(s.getId(), Map.of());
            LogStat auth = last.get(SyncType.AUTH);
            StoreAuthStatus authStatus = syncService.authStatus(s, storeService.credentials(s),
                    auth == null ? null : Integer.valueOf(1).equals(auth.getSuccess()));
            List<SyncHealth> syncs = new ArrayList<>();
            int total = 0;
            boolean problem = authStatus == StoreAuthStatus.REAUTH;
            for (SyncType type : HUB_TYPES) {
                ConnectorCapability cap = CAPABILITY.get(type);
                boolean supported = connector != null && (cap == null || connector.supports(cap));
                LogStat l = last.get(type);
                int f = fails.getOrDefault(type, 0);
                total += f;
                Boolean success = l == null ? null : Integer.valueOf(1).equals(l.getSuccess());
                if (Boolean.FALSE.equals(success)) {
                    problem = true;
                }
                syncs.add(new SyncHealth(type, supported, success, l == null ? null : l.getStartTime(), l == null ? null : l.getMessage(), f));
            }
            byPlatform.computeIfAbsent(s.getPlatform(), k -> new ArrayList<>()).add(new StoreHealth(s.getId(), s.getCode(), s.getName(),
                    s.getMarketplace(), s.getStatus(), Integer.valueOf(1).equals(s.getSandbox()), authStatus, s.getAuthorizedTime(),
                    s.getLastSyncTime(), s.getLastSyncMessage(), !Integer.valueOf(0).equals(s.getOrderSync()),
                    Integer.valueOf(1).equals(s.getInventorySync()), syncs, total, problem && s.getStatus() == EnableStatus.ENABLED));
        }
        List<PlatformGroup> groups = new ArrayList<>();
        for (boolean withConnector : new boolean[]{true, false}) {
            byPlatform.forEach((platform, list) -> {
                ChannelConnector c = registry.find(platform).orElse(null);
                if ((c != null) != withConnector) {
                    return;
                }
                ConnectorInfo info = c == null ? null : c.info();
                groups.add(new PlatformGroup(platform, c != null, info == null ? null : info.authType(),
                        info == null || info.capabilities() == null ? Set.of() : info.capabilities(),
                        info != null && info.sandboxBaseUrl() != null && !info.sandboxBaseUrl().isBlank(),
                        info == null ? null : info.docUrl(), list));
            });
        }
        return groups;
    }

    /** 删除 days 天之前的同步日志（定时任务），返回删除条数 */
    public int cleanLogs(int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(Math.max(1, days));
        return logMapper.delete(Wrappers.<EcomSyncLog>lambdaQuery().lt(EcomSyncLog::getStartTime, before));
    }
}
