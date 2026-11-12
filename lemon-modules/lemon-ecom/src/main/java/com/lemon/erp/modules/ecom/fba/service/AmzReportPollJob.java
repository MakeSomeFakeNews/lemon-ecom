package com.lemon.erp.modules.ecom.fba.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 亚马逊报告任务轮询：每 2 分钟查询到期任务的生成状态，完成的下载并处理（按租户逐个执行）
 */
@Component
@RequiredArgsConstructor
public class AmzReportPollJob implements JobHandler {

    private final AmzReportService reportService;

    @Override
    public String key() {
        return "ecom.amzReportPoll";
    }

    @Override
    public String name() {
        return "ecom.label.fba.reportPollJob";
    }

    @Override
    public String defaultCron() {
        return "0 */2 * * * *";
    }

    @Override
    public String run(JobParams params) {
        int n = reportService.pollDue();
        return "查询报告任务 " + n + " 个"; // i18n-ignore 任务日志存库
    }
}
