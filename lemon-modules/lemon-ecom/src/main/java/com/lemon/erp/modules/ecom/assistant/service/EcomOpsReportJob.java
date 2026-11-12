package com.lemon.erp.modules.ecom.assistant.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 运营日报：默认每天 8:30 生成昨天的日报并推送给订阅人（按租户逐个执行；时间在「定时任务」中修改）。
 * 参数 daysAgo=1 表示统计几天前（默认 1，即昨天）
 */
@Component
@RequiredArgsConstructor
public class EcomOpsReportJob implements JobHandler {

    public static final String KEY = "ecom.opsReport";
    public static final String CRON = "0 30 8 * * *";

    private final EcomOpsReportPushService pushService;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String name() {
        return "ecom.label.asst.opsReportJob";
    }

    @Override
    public String defaultCron() {
        return CRON;
    }

    @Override
    public String defaultParams() {
        return "daysAgo=1";
    }

    @Override
    public String run(JobParams params) {
        int daysAgo = Math.max(0, params.getInt("daysAgo", 1));
        EcomOpsReportPushService.RunResult r = pushService.runScheduled(LocalDate.now().minusDays(daysAgo));
        return "生成日报 " + r.reports() + " 份，推送 " + r.pushed() + " 人，跳过 " + r.skipped() + " 人"; // i18n-ignore 任务日志存库
    }
}
