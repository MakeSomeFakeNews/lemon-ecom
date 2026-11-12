package com.lemon.erp.modules.ecom.assistant.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantRequests;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantViews;
import com.lemon.erp.modules.ecom.assistant.entity.EcomOpsReport;
import com.lemon.erp.modules.ecom.assistant.entity.EcomOpsReportSub;
import com.lemon.erp.modules.ecom.assistant.service.EcomOpsReportPushService;
import com.lemon.erp.modules.ecom.assistant.service.EcomOpsReportService;
import com.lemon.erp.modules.ecom.assistant.service.EcomOpsReportSubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 运营日报 /ecom/ops-report：列表、详情、立即生成、重新生成 AI 建议、推送给订阅人、删除；订阅设置
 */
@RestController
@RequestMapping("/ecom/ops-report")
@RequiredArgsConstructor
public class EcomOpsReportController {

    private final EcomOpsReportService reportService;
    private final EcomOpsReportPushService pushService;
    private final EcomOpsReportSubService subService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:opsReport:list')")
    public R<PageResult<EcomOpsReport>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                             @RequestParam(required = false) Long storeId) {
        return R.ok(reportService.page(current, size, beginDate, endDate, storeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:opsReport:list')")
    public R<AssistantViews.ReportDetail> detail(@PathVariable Long id) {
        return R.ok(reportService.detail(id));
    }

    /** 立即生成（同一天同一范围覆盖），返回日报 ID */
    @PostMapping("/generate")
    @PreAuthorize("@ss.hasPerm('ecom:opsReport:generate')")
    @OperLog(title = "运营日报", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> generate(@Valid @RequestBody AssistantRequests.ReportGenerate req) {
        return R.ok(reportService.generateManual(req.date(), req.storeIds(), !Boolean.FALSE.equals(req.ai())).getId());
    }

    /** 重新生成 AI 要点与建议 */
    @PostMapping("/{id}/advice")
    @PreAuthorize("@ss.hasPerm('ecom:opsReport:generate')")
    @OperLog(title = "运营日报", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> advice(@PathVariable Long id) {
        reportService.regenerateAdvice(id);
        return R.ok();
    }

    /** 推送给店铺范围一致的订阅人，返回人数 */
    @PostMapping("/{id}/push")
    @PreAuthorize("@ss.hasPerm('ecom:opsReport:generate')")
    @OperLog(title = "运营日报", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Integer> push(@PathVariable Long id) {
        return R.ok(pushService.pushToSubscribers(id));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:opsReport:remove')")
    @OperLog(title = "运营日报", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        reportService.delete(ids);
        return R.ok();
    }

    // ---------------------------------------------------------------- 订阅

    @GetMapping("/sub")
    @PreAuthorize("@ss.hasPerm('ecom:opsReport:subscribe')")
    public R<List<EcomOpsReportSub>> subs() {
        return R.ok(subService.list());
    }

    @GetMapping("/sub/options")
    @PreAuthorize("@ss.hasPerm('ecom:opsReport:list')")
    public R<AssistantViews.SubOptions> subOptions() {
        return R.ok(subService.options());
    }

    @PutMapping("/sub")
    @PreAuthorize("@ss.hasPerm('ecom:opsReport:subscribe')")
    @OperLog(title = "运营日报订阅", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Long> saveSub(@Valid @RequestBody AssistantRequests.SubSave req) {
        return R.ok(subService.save(req));
    }

    @DeleteMapping("/sub/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:opsReport:subscribe')")
    @OperLog(title = "运营日报订阅", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> deleteSub(@PathVariable List<Long> ids) {
        subService.delete(ids);
        return R.ok();
    }
}
