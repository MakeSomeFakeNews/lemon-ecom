package com.lemon.erp.modules.ecom.owh.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.SyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

/**
 * 海外仓定时任务（只处理启用且开启自动同步的账号）：出库（自动推送 + 同步出库状态，每 15 分钟）、库存同步（每 2 小时）、
 * 入库单 / 退件单 / 调拨单同步（每小时）、费用同步（每天，最近 7 天）
 */
@Configuration
@RequiredArgsConstructor
public class EcomOwhJobs {

    private final EcomOwhStockService stockService;
    private final EcomOwhInboundService inboundService;
    private final EcomOwhOutboundService outboundService;
    private final EcomOwhFeeService feeService;
    private final EcomOwhReturnService returnService;
    private final EcomOwhTransferService transferService;

    @Bean
    public JobHandler ecomOwhOutboundJob() {
        return new Job("ecom.owhOutbound", "ecom.label.owh.job.outbound", "0 */15 * * * *", () -> {
            List<SyncResult> pushed = outboundService.autoPush();
            List<SyncResult> synced = outboundService.syncAll();
            return "自动推送 " + pushed.size() + " 个包裹" + failures(pushed) + "；同步出库单 " + synced.size() + " 张" + failures(synced); // i18n-ignore 任务日志存库
        });
    }

    @Bean
    public JobHandler ecomOwhStockJob() {
        return new Job("ecom.owhStock", "ecom.label.owh.job.stock", "0 0 */2 * * *", () -> {
            List<SyncResult> r = stockService.syncAll();
            return "同步 " + r.size() + " 个账号，库存 " + r.stream().mapToInt(SyncResult::count).sum() + " 行" + failures(r); // i18n-ignore 任务日志存库
        });
    }

    @Bean
    public JobHandler ecomOwhInboundJob() {
        return new Job("ecom.owhInbound", "ecom.label.owh.job.inbound", "0 10 * * * *", () -> {
            List<SyncResult> r = inboundService.syncAll();
            List<SyncResult> ret = returnService.syncAll();
            List<SyncResult> tr = transferService.syncAll();
            return "同步入库单 " + r.size() + " 张" + failures(r) + "；退件单 " + ret.size() + " 张" + failures(ret) // i18n-ignore 任务日志存库
                    + "；调拨单 " + tr.size() + " 张" + failures(tr); // i18n-ignore 任务日志存库
        });
    }

    @Bean
    public JobHandler ecomOwhFeeJob() {
        return new Job("ecom.owhFee", "ecom.label.owh.job.fee", "0 30 3 * * *", () -> {
            LocalDate end = LocalDate.now();
            List<SyncResult> r = feeService.syncAll(end.minusDays(7), end);
            return "同步 " + r.size() + " 个账号，新增费用 " + r.stream().mapToInt(SyncResult::count).sum() + " 条" + failures(r); // i18n-ignore 任务日志存库
        });
    }

    private static String failures(List<SyncResult> results) {
        StringBuilder sb = new StringBuilder();
        results.stream().filter(r -> r.message() != null).limit(20)
                .forEach(r -> sb.append(sb.isEmpty() ? "，失败：" : "; ").append(r.name()).append(": ").append(r.message())); // i18n-ignore 任务日志存库
        return sb.toString();
    }

    /** 任务的执行体 */
    private interface Body {
        String run();
    }

    private record Job(String key, String name, String defaultCron, Body body) implements JobHandler {
        @Override
        public String run(JobParams params) {
            return body.run();
        }
    }
}
