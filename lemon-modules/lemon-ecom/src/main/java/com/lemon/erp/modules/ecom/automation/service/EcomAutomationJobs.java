package com.lemon.erp.modules.ecom.automation.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.modules.ecom.automation.dto.AutoViews;
import com.lemon.erp.modules.ecom.automation.model.AutoScanMode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自动化规则的定时任务（按租户逐个执行）：
 * <ul>
 *   <li>ecom.autoScanFrequent：每 15 分钟扫描买家消息超时、广告预算耗尽；</li>
 *   <li>ecom.autoScanDaily：每天 08:30 扫描断货风险、低于安全库存、头程超期、广告 ACOS、索赔临近截止、退货率、竞品低价，
 *       并清理 keepDays（默认 90）天前的执行日志。</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class EcomAutomationJobs {

    private final AutoScanService scanService;
    private final AutoLogService logService;

    @Bean
    JobHandler autoScanFrequentJob() {
        return new JobHandler() {
            @Override
            public String key() {
                return "ecom.autoScanFrequent";
            }

            @Override
            public String name() {
                return "ecom.label.auto.job.frequent";
            }

            @Override
            public String defaultCron() {
                return "0 */15 * * * *";
            }

            @Override
            public String run(JobParams params) {
                return text(scanService.scan(AutoScanMode.FREQUENT));
            }
        };
    }

    @Bean
    JobHandler autoScanDailyJob() {
        return new JobHandler() {
            @Override
            public String key() {
                return "ecom.autoScanDaily";
            }

            @Override
            public String name() {
                return "ecom.label.auto.job.daily";
            }

            @Override
            public String defaultCron() {
                return "0 30 8 * * *";
            }

            @Override
            public String defaultParams() {
                return "keepDays=90";
            }

            @Override
            public String run(JobParams params) {
                AutoViews.RunResult r = scanService.scan(AutoScanMode.DAILY);
                int removed = logService.cleanup(params.getInt("keepDays", 90));
                return text(r) + "，清理日志 " + removed + " 条"; // i18n-ignore 任务日志存库
            }
        };
    }

    private static String text(AutoViews.RunResult r) {
        return "事件 " + r.events() + " 个，执行 " + r.executed() + " 次" + (r.message() == null ? "" : "；失败：" + r.message()); // i18n-ignore 任务日志存库
    }
}
