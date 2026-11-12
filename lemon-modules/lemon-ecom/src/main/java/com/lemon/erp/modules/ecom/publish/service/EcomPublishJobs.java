package com.lemon.erp.modules.ecom.publish.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 刊登与定价的定时任务：每 5 分钟处理刊登队列（遗漏的待发布、平台异步处理中的结果、超时）（ecom.publishQueue），
 * 每 15 分钟执行到了执行间隔的自动调价规则（ecom.repricer）。按租户逐个执行，单条失败不影响其他
 */
@Configuration
@RequiredArgsConstructor
public class EcomPublishJobs {

    @Bean
    JobHandler ecomPublishQueueJob(EcomPublishWorker worker) {
        return new Handler("ecom.publishQueue", "ecom.label.pub.queueJob", "0 */5 * * * *", p -> summary(worker.runQueue(LocalDateTime.now())));
    }

    @Bean
    JobHandler ecomRepricerJob(EcomRepricerService repricerService) {
        return new Handler("ecom.repricer", "ecom.label.pub.repricerJob", "0 */15 * * * *", p -> summary(repricerService.runDue(LocalDateTime.now())));
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
