package com.lemon.erp.modules.ecom.fba.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.fba.dto.FbaRequests;
import com.lemon.erp.modules.ecom.fba.entity.EcomAmzReport;
import com.lemon.erp.modules.ecom.fba.model.AmzReportStatus;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import com.lemon.erp.modules.ecom.fba.service.AmzReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 亚马逊报告 /ecom/amz-report：创建报告任务（异步生成，定时任务轮询下载）、上传报告文件、立即刷新状态
 */
@RestController
@RequestMapping("/ecom/amz-report")
@RequiredArgsConstructor
public class EcomAmzReportController {

    private final AmzReportService reportService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:amzReport:list')")
    public R<PageResult<EcomAmzReport>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) Long storeId, @RequestParam(required = false) AmzReportType reportType,
                                             @RequestParam(required = false) AmzReportStatus status) {
        return R.ok(reportService.page(current, size, storeId, reportType, status));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:amzReport:add')")
    @OperLog(title = "亚马逊报告", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Valid @RequestBody FbaRequests.ReportCreate req) {
        return R.ok(reportService.create(req));
    }

    @PostMapping("/upload")
    @PreAuthorize("@ss.hasPerm('ecom:amzReport:add')")
    @OperLog(title = "上传亚马逊报告", type = OperType.IMPORT) // i18n-ignore 操作日志标题存库
    public R<Long> upload(@Valid @RequestBody FbaRequests.ReportUpload req) {
        return R.ok(reportService.upload(req));
    }

    /** 立即查询一次状态（完成时下载并处理） */
    @PostMapping("/{id}/poll")
    @PreAuthorize("@ss.hasPerm('ecom:amzReport:list')")
    public R<EcomAmzReport> poll(@PathVariable Long id) {
        return R.ok(reportService.poll(id));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:amzReport:remove')")
    @OperLog(title = "亚马逊报告", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        reportService.delete(ids);
        return R.ok();
    }
}
