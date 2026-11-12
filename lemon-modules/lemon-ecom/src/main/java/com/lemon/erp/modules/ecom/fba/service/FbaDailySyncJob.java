package com.lemon.erp.modules.ecom.fba.service;

import com.lemon.erp.framework.job.JobHandler;
import com.lemon.erp.framework.job.JobParams;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * FBA 报告每日同步：给有渠道授权、启用的亚马逊店铺创建库存规划、FBA 库存、库存分类账、赔偿、移除订单报告
 * （按日期的报告取最近 days 天，默认 30），之后由 {@link AmzReportPollJob} 轮询下载；默认停用，在定时任务页面启用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FbaDailySyncJob implements JobHandler {

    static final List<AmzReportType> TYPES = List.of(AmzReportType.FBA_INVENTORY_PLANNING, AmzReportType.FBA_MYI_INVENTORY,
            AmzReportType.LEDGER_DETAIL, AmzReportType.REIMBURSEMENTS, AmzReportType.REMOVAL_ORDERS);

    private final AmzReportService reportService;
    private final FbaSupport support;

    @Override
    public String key() {
        return "ecom.fbaDailySync";
    }

    @Override
    public String name() {
        return "ecom.label.fba.dailySyncJob";
    }

    @Override
    public String defaultCron() {
        return "0 30 4 * * *";
    }

    @Override
    public boolean defaultEnabled() {
        return false;
    }

    @Override
    public String defaultParams() {
        return "days=30";
    }

    @Override
    public String run(JobParams params) {
        int days = params.getInt("days", 30);
        int stores = 0;
        int created = 0;
        StringBuilder failed = new StringBuilder();
        for (EcomStore s : support.amazonStores()) {
            if (s.demoStore() || s.getStatus() != EnableStatus.ENABLED || s.getCredentials() == null || s.getCredentials().isBlank()) {
                continue;
            }
            stores++;
            try {
                created += reportService.createAll(s.getId(), TYPES, days).size();
            } catch (RuntimeException e) {
                failed.append(s.getName()).append(": ").append(e.getMessage()).append("; ");
            }
        }
        return "店铺 " + stores + " 个，创建报告任务 " + created + " 个" + (failed.isEmpty() ? "" : "，失败：" + failed); // i18n-ignore 任务日志存库
    }
}
