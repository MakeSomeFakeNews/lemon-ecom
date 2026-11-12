package com.lemon.erp.modules.ecom.dashboard;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.dto.ChannelHubViews;
import com.lemon.erp.modules.ecom.model.StoreAuthStatus;
import com.lemon.erp.modules.ecom.service.EcomAdsAuthService;
import com.lemon.erp.modules.ecom.service.EcomChannelHubService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 看板待办：店铺授权失效（最近一次刷新令牌 / 测试授权失败，需要重新授权）、店铺同步失败（订单、发货回传、库存推送、商品同步最近一次失败）、
 * 广告同步失败（广告授权页记录的最近一次同步失败原因）。只统计启用的店铺
 */
@Component
@RequiredArgsConstructor
public class EcomChannelDashboardTodos implements EcomDashboardTodoProvider {

    private final EcomChannelHubService hubService;
    private final EcomAdsAuthService adsAuthService;
    private final PermissionService permissionService;

    @Override
    public List<EcomDashboardTodo> todos() {
        List<EcomDashboardTodo> out = new ArrayList<>();
        if (permissionService.hasPerm("ecom:channel:list")) {
            long reauth = 0;
            long failed = 0;
            for (ChannelHubViews.PlatformGroup g : hubService.overview()) {
                for (ChannelHubViews.StoreHealth s : g.stores()) {
                    if (s.status() != EnableStatus.ENABLED) {
                        continue;
                    }
                    if (s.authStatus() == StoreAuthStatus.REAUTH) {
                        reauth++;
                    } else if (s.problem()) {
                        failed++;
                    }
                }
            }
            EcomCoreDashboardTodos.add(out, "store.reauth", "ecom.text.fix.todo.store.reauth", reauth, "ecom.text.fix.unit.store",
                    "/ecom/channel", Map.of(), EcomDashboardTodo.DANGER, "DisconnectOutlined", 80);
            EcomCoreDashboardTodos.add(out, "store.syncFailed", "ecom.text.fix.todo.store.syncFailed", failed, "ecom.text.fix.unit.store",
                    "/ecom/channel", Map.of(), EcomDashboardTodo.WARNING, "SyncOutlined", 90);
        }
        if (permissionService.hasPerm("ecom:ad:list")) {
            long adsFailed = adsAuthService.stores().stream()
                    .filter(s -> s.isHasCredentials() && StringUtils.hasText(s.getAdsLastSyncMessage()))
                    .map(AdsViews.StoreAuth::getStoreId).distinct().count();
            EcomCoreDashboardTodos.add(out, "ads.syncFailed", "ecom.text.fix.todo.ads.syncFailed", adsFailed, "ecom.text.fix.unit.store",
                    "/ecom/ad", Map.of("tab", "auth"), EcomDashboardTodo.WARNING,
                    "SoundOutlined", 100);
        }
        return out;
    }
}
