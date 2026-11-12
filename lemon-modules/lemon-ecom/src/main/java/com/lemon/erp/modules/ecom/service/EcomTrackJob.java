package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 物流轨迹拉取：每 2 小时拉取已发货、未签收的包裹轨迹，更新状态与预警（按租户逐个执行）
 */
@Component
@RequiredArgsConstructor
public class EcomTrackJob implements JobHandler {

    private final EcomTrackService trackService;

    @Override
    public String key() {
        return "ecom.trackPull";
    }

    @Override
    public String name() {
        return "ecom.label.ord.trackJob";
    }

    @Override
    public String defaultCron() {
        return "0 15 */2 * * *";
    }

    @Override
    public String run(JobParams params) {
        int[] r = trackService.pullDue();
        return "拉取 " + r[0] + " 个包裹的轨迹" + (r[1] > 0 ? "，失败 " + r[1] + " 个" : ""); // i18n-ignore 任务日志存库
    }
}
