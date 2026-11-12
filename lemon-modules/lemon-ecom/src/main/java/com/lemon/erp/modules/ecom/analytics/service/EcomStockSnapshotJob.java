package com.lemon.erp.modules.ecom.analytics.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.modules.ecom.analytics.dto.GenerateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 每日库存快照：每天 23:50 记录各在售 SKU 的平台仓 / 本地仓可售数（按租户逐个执行，由定时任务框架处理）
 */
@Component
@RequiredArgsConstructor
public class EcomStockSnapshotJob implements JobHandler {

    private final EcomStockSnapshotService snapshotService;

    @Override
    public String key() {
        return "ecom.stockSnapshot";
    }

    @Override
    public String name() {
        return "ecom.label.analytics.snapshotJob";
    }

    @Override
    public String defaultCron() {
        return "0 50 23 * * *";
    }

    @Override
    public String run(JobParams params) {
        GenerateResult r = snapshotService.record(LocalDate.now());
        return summary("记录快照", r); // i18n-ignore 任务日志存库
    }

    static String summary(String verb, GenerateResult r) {
        StringBuilder sb = new StringBuilder(verb).append(" ").append(r.total()).append(" 个 SKU"); // i18n-ignore 任务日志存库
        if (r.failed() > 0) {
            sb.append("，失败 ").append(r.failed()).append(" 个：").append(r.firstError()); // i18n-ignore 任务日志存库
        }
        return sb.toString();
    }
}
