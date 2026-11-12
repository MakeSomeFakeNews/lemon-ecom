package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 渠道同步日志清理：每天删除 days 天（默认 90）之前的同步日志
 */
@Component
@RequiredArgsConstructor
public class EcomSyncLogCleanJob implements JobHandler {

    static final int DEFAULT_DAYS = 90;

    private final EcomChannelHubService hubService;

    @Override
    public String key() {
        return "ecom.syncLogClean";
    }

    @Override
    public String name() {
        return "ecom.label.hub.logCleanJob";
    }

    @Override
    public String defaultCron() {
        return "0 40 2 * * *";
    }

    @Override
    public String defaultParams() {
        return "days=" + DEFAULT_DAYS;
    }

    @Override
    public String run(JobParams params) {
        int days = params.getInt("days", DEFAULT_DAYS);
        return "删除 " + days + " 天前的同步日志 " + hubService.cleanLogs(days) + " 条"; // i18n-ignore 任务日志存库
    }
}
