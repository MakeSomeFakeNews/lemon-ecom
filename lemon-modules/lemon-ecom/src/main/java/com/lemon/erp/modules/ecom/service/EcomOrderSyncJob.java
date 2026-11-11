package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.modules.ecom.dto.SyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 平台订单同步：每 30 分钟同步一次有渠道授权的店铺（亚马逊订单接口限速，间隔不要短于 10 分钟）
 */
@Component
@RequiredArgsConstructor
public class EcomOrderSyncJob implements JobHandler {

    private final EcomSyncService syncService;

    @Override
    public String key() {
        return "ecom.orderSync";
    }

    @Override
    public String name() {
        return "ecom.label.orderSyncJob";
    }

    @Override
    public String defaultCron() {
        return "0 */30 * * * *";
    }

    @Override
    public String run(JobParams params) {
        List<SyncResult> results = syncService.syncAll();
        int created = results.stream().mapToInt(SyncResult::created).sum();
        long failed = results.stream().filter(r -> r.message() != null).count();
        StringBuilder sb = new StringBuilder("同步 " + results.size() + " 个店铺，新增订单 " + created + " 张"); // i18n-ignore 任务日志存库
        if (failed > 0) {
            sb.append("，失败 ").append(failed).append(" 个："); // i18n-ignore 任务日志存库
            results.stream().filter(r -> r.message() != null).forEach(r -> sb.append(r.storeName()).append(": ").append(r.message()).append("; "));
        }
        return sb.toString();
    }
}
