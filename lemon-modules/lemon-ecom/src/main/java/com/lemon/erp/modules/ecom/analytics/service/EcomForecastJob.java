package com.lemon.erp.modules.ecom.analytics.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 销量预测：每天 04:00 为全部在售 SKU 生成从今天起 90 天的预测（按租户逐个执行，由定时任务框架处理；参数在「销量预测 → 预测参数」中设置）
 */
@Component
@RequiredArgsConstructor
public class EcomForecastJob implements JobHandler {

    private final EcomForecastService forecastService;

    @Override
    public String key() {
        return "ecom.forecast";
    }

    @Override
    public String name() {
        return "ecom.label.analytics.forecastJob";
    }

    @Override
    public String defaultCron() {
        return "0 0 4 * * *";
    }

    @Override
    public String run(JobParams params) {
        return EcomStockSnapshotJob.summary("生成预测", forecastService.generate(null)); // i18n-ignore 任务日志存库
    }
}
