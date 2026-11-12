package com.lemon.erp.modules.ecom.oper.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.modules.ecom.dto.IdsRequest;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.oper.dto.AlertQuery;
import com.lemon.erp.modules.ecom.oper.dto.MonitorQuery;
import com.lemon.erp.modules.ecom.oper.dto.OperRequests;
import com.lemon.erp.modules.ecom.oper.dto.OperViews;
import com.lemon.erp.modules.ecom.oper.dto.SnapshotImportRow;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitor;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitorAlert;
import com.lemon.erp.modules.ecom.oper.service.EcomMonitorService;

import java.util.List;

/**
 * ASIN / 商品监控 /ecom/oper/monitor：监控对象、快照（接口取数、手工录入、Excel 导入）、趋势与变化预警
 */
@RestController
@RequestMapping("/ecom/oper/monitor")
@RequiredArgsConstructor
public class EcomMonitorController {

    private final EcomMonitorService monitorService;
    private final ImportExecutor importExecutor;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:monitor:list')")
    public R<PageResult<EcomMonitor>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size, MonitorQuery query) {
        return R.ok(monitorService.page(current, size, query));
    }

    /** 支持接口取数的平台 */
    @GetMapping("/platforms")
    @PreAuthorize("@ss.hasPerm('ecom:monitor:list')")
    public R<List<EcomPlatform>> platforms() {
        return R.ok(monitorService.fetchPlatforms());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:monitor:list')")
    public R<EcomMonitor> detail(@PathVariable Long id) {
        return R.ok(monitorService.detail(id));
    }

    /** 趋势：最近 days 天（默认 60）的快照与最近的预警 */
    @GetMapping("/{id}/trend")
    @PreAuthorize("@ss.hasPerm('ecom:monitor:list')")
    public R<OperViews.MonitorTrend> trend(@PathVariable Long id, @RequestParam(defaultValue = "60") int days) {
        return R.ok(monitorService.trend(id, days));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:monitor:add')")
    @OperLog(title = "商品监控", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Valid @RequestBody EcomMonitor m) {
        return R.ok(monitorService.create(m));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:monitor:edit')")
    @OperLog(title = "商品监控", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody EcomMonitor m) {
        monitorService.update(id, m);
        return R.ok();
    }

    /** 暂停 / 恢复监控 */
    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('ecom:monitor:edit')")
    @OperLog(title = "商品监控", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam EnableStatus status) {
        monitorService.changeStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:monitor:remove')")
    @OperLog(title = "商品监控", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        monitorService.delete(ids);
        return R.ok();
    }

    /** 立即通过平台接口取数 */
    @PostMapping("/fetch")
    @PreAuthorize("@ss.hasPerm('ecom:monitor:fetch')")
    @OperLog(title = "商品监控", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<OperViews.FetchOutcome> fetch(@Valid @RequestBody IdsRequest req) {
        return R.ok(monitorService.fetch(req.ids()));
    }

    /** 手工录入一天的快照，返回新产生的预警 */
    @PostMapping("/{id}/snapshot")
    @PreAuthorize("@ss.hasPerm('ecom:monitor:snapshot')")
    @OperLog(title = "商品监控", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<List<EcomMonitorAlert>> record(@PathVariable Long id, @Valid @RequestBody OperRequests.SnapshotRequest req) {
        return R.ok(monitorService.record(id, req));
    }

    @PostMapping("/snapshot/import")
    @PreAuthorize("@ss.hasPerm('ecom:monitor:snapshot')")
    @OperLog(title = "商品监控", type = OperType.IMPORT) // i18n-ignore 操作日志标题存库
    public R<ImportResult> importSnapshots(@Valid @RequestBody ImportRequest<SnapshotImportRow> req) {
        return R.ok(importExecutor.run(req, monitorService::importRow));
    }

    @GetMapping("/alert")
    @PreAuthorize("@ss.hasPerm('ecom:monitor:list')")
    public R<PageResult<EcomMonitorAlert>> alerts(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size, AlertQuery query) {
        return R.ok(monitorService.alerts(current, size, query));
    }

    /** 未处理预警数（按类型） */
    @GetMapping("/alert/counts")
    @PreAuthorize("@ss.hasPerm('ecom:monitor:list')")
    public R<List<OperViews.AlertCount>> alertCounts(AlertQuery query) {
        return R.ok(monitorService.alertCounts(query));
    }

    @PostMapping("/alert/handle")
    @PreAuthorize("@ss.hasPerm('ecom:monitor:alert')")
    @OperLog(title = "商品监控预警", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> handle(@Valid @RequestBody OperRequests.AlertHandleRequest req) {
        monitorService.handle(req);
        return R.ok();
    }
}
