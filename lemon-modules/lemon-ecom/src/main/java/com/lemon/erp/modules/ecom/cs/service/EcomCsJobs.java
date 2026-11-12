package com.lemon.erp.modules.ecom.cs.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.modules.ecom.cs.dto.CsViews;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 客服的定时任务（按租户逐个执行）：买家消息每 10 分钟同步，商品评价每小时同步，请求评价每天一次（默认停用，开启后自动对发货 5–30 天的亚马逊订单请求评价）
 */
public final class EcomCsJobs {

    private EcomCsJobs() {
    }

    /** 任务日志（存库内容用中文） */
    static String summary(List<CsViews.SyncOutcome> results, String what) {
        int created = results.stream().mapToInt(CsViews.SyncOutcome::created).sum();
        List<CsViews.SyncOutcome> failed = results.stream().filter(r -> r.message() != null).toList();
        StringBuilder sb = new StringBuilder("处理 " + results.size() + " 个店铺，" + what + " " + created + " 条"); // i18n-ignore 任务日志存库
        if (!failed.isEmpty()) {
            sb.append("，失败 ").append(failed.size()).append(" 个："); // i18n-ignore 任务日志存库
            failed.forEach(r -> sb.append(r.storeName()).append(": ").append(r.message()).append("; "));
        }
        return sb.toString();
    }

    /** 买家消息同步：每 10 分钟 */
    @Component
    @RequiredArgsConstructor
    public static class MessageSync implements JobHandler {

        private final EcomCsSyncService syncService;

        @Override
        public String key() {
            return "ecom.csMessageSync";
        }

        @Override
        public String name() {
            return "ecom.label.cs.messageSyncJob";
        }

        @Override
        public String defaultCron() {
            return "0 */10 * * * *";
        }

        @Override
        public String run(JobParams params) {
            return summary(syncService.syncMessagesAll(), "新消息"); // i18n-ignore 任务日志存库
        }
    }

    /** 商品评价同步：每小时 */
    @Component
    @RequiredArgsConstructor
    public static class ReviewSync implements JobHandler {

        private final EcomCsSyncService syncService;

        @Override
        public String key() {
            return "ecom.csReviewSync";
        }

        @Override
        public String name() {
            return "ecom.label.cs.reviewSyncJob";
        }

        @Override
        public String defaultCron() {
            return "0 25 * * * *";
        }

        @Override
        public String run(JobParams params) {
            return summary(syncService.syncReviewsAll(), "新评价"); // i18n-ignore 任务日志存库
        }
    }

    /** 请求评价：每天 10:30，默认停用 */
    @Component
    @RequiredArgsConstructor
    public static class ReviewRequest implements JobHandler {

        private final EcomReviewRequestService requestService;

        @Override
        public String key() {
            return "ecom.csReviewRequest";
        }

        @Override
        public String name() {
            return "ecom.label.cs.reviewRequestJob";
        }

        @Override
        public String defaultCron() {
            return "0 30 10 * * *";
        }

        @Override
        public boolean defaultEnabled() {
            return false;
        }

        @Override
        public String run(JobParams params) {
            return summary(requestService.requestDueAll(), "已请求"); // i18n-ignore 任务日志存库
        }
    }
}
