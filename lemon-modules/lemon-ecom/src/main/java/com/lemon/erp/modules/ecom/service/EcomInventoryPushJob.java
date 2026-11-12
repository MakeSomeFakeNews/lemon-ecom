package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 渠道库存回传：每 15 分钟把开启了自动回传库存的店铺中有变化的可售数量回传平台
 */
@Component
@RequiredArgsConstructor
public class EcomInventoryPushJob implements JobHandler {

    private final EcomListingSyncService listingSyncService;

    @Override
    public String key() {
        return "ecom.inventoryPush";
    }

    @Override
    public String name() {
        return "ecom.label.hub.inventoryPushJob";
    }

    @Override
    public String defaultCron() {
        return "0 */15 * * * *";
    }

    @Override
    public String run(JobParams params) {
        return EcomJobSummaries.summary(listingSyncService.pushInventoryAll(), "回传"); // i18n-ignore 任务日志存库
    }
}
