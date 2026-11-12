package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 在线商品同步：每天从平台拉取一次在线商品（平台支持拉取商品、有授权的启用店铺）
 */
@Component
@RequiredArgsConstructor
public class EcomListingSyncJob implements JobHandler {

    private final EcomListingSyncService listingSyncService;

    @Override
    public String key() {
        return "ecom.listingSync";
    }

    @Override
    public String name() {
        return "ecom.label.hub.listingSyncJob";
    }

    @Override
    public String defaultCron() {
        return "0 20 3 * * *";
    }

    @Override
    public String run(JobParams params) {
        return EcomJobSummaries.summary(listingSyncService.syncListingsAll(), "更新"); // i18n-ignore 任务日志存库
    }
}
