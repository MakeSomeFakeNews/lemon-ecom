package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 广告的定时任务：每小时执行到点的广告规则（ecom.adRules）、每小时分时调价（ecom.adSchedule）、每天同步广告结构与报告（ecom.adsSync）。
 * 任务按租户逐个执行，任务内逐条规则 / 策略 / 店铺处理，单条失败不影响其他
 */
@Configuration
@RequiredArgsConstructor
public class EcomAdJobs {

    @Bean
    JobHandler ecomAdRuleJob(EcomAdRuleService ruleService) {
        return new Handler("ecom.adRules", "ecom.label.ads.ruleJob", "0 10 * * * *", p -> summary(ruleService.runDue(LocalDateTime.now())));
    }

    @Bean
    JobHandler ecomAdScheduleJob(EcomAdScheduleService scheduleService) {
        return new Handler("ecom.adSchedule", "ecom.label.ads.scheduleJob", "0 0 * * * *", p -> summary(scheduleService.applyAll(LocalDateTime.now())));
    }

    @Bean
    JobHandler ecomAdsSyncJob(EcomAdsSyncService syncService) {
        return new Handler("ecom.adsSync", "ecom.label.ads.syncJob", "0 30 4 * * *",
                p -> summary(syncService.syncAll(p.getInt("days", EcomAdsSyncService.DEFAULT_DAYS))));
    }

    private static String summary(List<String> lines) {
        if (lines.isEmpty()) {
            return "-";
        }
        String s = String.join("; ", lines);
        return s.length() <= 1000 ? s : s.substring(0, 1000) + "...";
    }

    private record Handler(String key, String name, String defaultCron, java.util.function.Function<JobParams, String> body) implements JobHandler {
        @Override
        public String run(JobParams params) {
            return body.apply(params);
        }
    }
}
