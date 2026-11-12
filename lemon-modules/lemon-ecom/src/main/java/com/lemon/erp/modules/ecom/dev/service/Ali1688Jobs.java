package com.lemon.erp.modules.ecom.dev.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.SyncSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 1688 定时任务：同步未结束的采购单（状态、实付、物流，每小时）、同步货源价格与库存（每天 7 点，价格变动提醒）。
 * 没有启用且已授权的 1688 账号时跳过
 */
@Configuration
@RequiredArgsConstructor
public class Ali1688Jobs {

    private final Ali1688AccountService accountService;
    private final Ali1688OrderService orderService;
    private final Ali1688OfferService offerService;

    @Bean
    public JobHandler ali1688OrderSyncJob() {
        return new JobHandler() {
            @Override
            public String key() {
                return "ecom.ali1688OrderSync";
            }

            @Override
            public String name() {
                return "ecom.label.dev.orderSyncJob";
            }

            @Override
            public String defaultCron() {
                return "0 10 * * * *";
            }

            @Override
            public String run(JobParams params) {
                if (!accountService.hasUsableAccount()) {
                    return "没有已授权的 1688 账号，跳过"; // i18n-ignore 任务日志存库
                }
                return summary(orderService.syncMany(null));
            }
        };
    }

    @Bean
    public JobHandler ali1688PriceSyncJob() {
        return new JobHandler() {
            @Override
            public String key() {
                return "ecom.ali1688PriceSync";
            }

            @Override
            public String name() {
                return "ecom.label.dev.priceSyncJob";
            }

            @Override
            public String defaultCron() {
                return "0 0 7 * * *";
            }

            @Override
            public String run(JobParams params) {
                if (!accountService.hasUsableAccount()) {
                    return "没有已授权的 1688 账号，跳过"; // i18n-ignore 任务日志存库
                }
                return summary(offerService.sync(null));
            }
        };
    }

    private static String summary(SyncSummary s) {
        StringBuilder sb = new StringBuilder("共 " + s.total() + " 个，成功 " + s.success() + "，变化 " + s.changed()); // i18n-ignore 任务日志存库
        if (s.failed() > 0) {
            sb.append("，失败 ").append(s.failed()).append("：").append(String.join("; ", s.messages())); // i18n-ignore 任务日志存库
        }
        return sb.length() > 2000 ? sb.substring(0, 2000) : sb.toString();
    }
}
