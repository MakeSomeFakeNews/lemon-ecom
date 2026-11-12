package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 渠道令牌刷新：每小时对已授权的 OAuth 店铺调用一次 ensureToken，刷新失败的店铺标记需要重新授权
 */
@Component
@RequiredArgsConstructor
public class EcomTokenRefreshJob implements JobHandler {

    private final EcomSyncService syncService;

    @Override
    public String key() {
        return "ecom.tokenRefresh";
    }

    @Override
    public String name() {
        return "ecom.label.hub.tokenRefreshJob";
    }

    @Override
    public String defaultCron() {
        return "0 5 * * * *";
    }

    @Override
    public String run(JobParams params) {
        return EcomJobSummaries.summary(syncService.refreshTokens(), "刷新"); // i18n-ignore 任务日志存库
    }
}
