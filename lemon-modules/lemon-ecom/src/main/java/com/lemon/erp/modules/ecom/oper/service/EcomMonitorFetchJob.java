package com.lemon.erp.modules.ecom.oper.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.modules.ecom.oper.dto.OperViews;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 商品监控取数：每天 07:10 对监控中、开启自动取数、平台支持接口的商品取数并生成快照与预警（按租户逐个执行）
 */
@Component
@RequiredArgsConstructor
public class EcomMonitorFetchJob implements JobHandler {

    private final EcomMonitorService monitorService;

    @Override
    public String key() {
        return "ecom.monitorFetch";
    }

    @Override
    public String name() {
        return "ecom.label.oper.monitorFetchJob";
    }

    @Override
    public String defaultCron() {
        return "0 10 7 * * *";
    }

    @Override
    public String run(JobParams params) {
        OperViews.FetchOutcome r = monitorService.fetchAll();
        return "监控 " + r.total() + " 个，成功 " + r.success() + " 个，失败 " + r.failed() + " 个，新预警 " + r.alerts() + " 条" // i18n-ignore 任务日志存库
                + (r.message() == null ? "" : "：" + r.message());
    }
}
