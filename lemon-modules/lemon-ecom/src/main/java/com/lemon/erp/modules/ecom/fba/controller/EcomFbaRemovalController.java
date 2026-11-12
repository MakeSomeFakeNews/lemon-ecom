package com.lemon.erp.modules.ecom.fba.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
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
import com.lemon.erp.modules.ecom.fba.dto.FbaRequests;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaRemoval;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import com.lemon.erp.modules.ecom.fba.model.FbaRemovalStatus;
import com.lemon.erp.modules.ecom.fba.model.FbaRemovalType;
import com.lemon.erp.modules.ecom.fba.service.AmzReportService;
import com.lemon.erp.modules.ecom.fba.service.FbaRemovalService;

/**
 * FBA 移除订单 /ecom/fba/removal：移除 / 弃置计划、提交、按移除订单报告同步、本地仓收货、弃置出库
 */
@RestController
@RequestMapping("/ecom/fba/removal")
@RequiredArgsConstructor
public class EcomFbaRemovalController {

    /** 同步移除订单报告的天数 */
    private static final int SYNC_DAYS = 90;

    private final FbaRemovalService removalService;
    private final AmzReportService reportService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:fbaRemoval:list')")
    public R<PageResult<EcomFbaRemoval>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) Long storeId, @RequestParam(required = false) FbaRemovalType removalType,
                                              @RequestParam(required = false) FbaRemovalStatus status, @RequestParam(required = false) String keyword) {
        return R.ok(removalService.page(current, size, storeId, removalType, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaRemoval:list')")
    public R<EcomFbaRemoval> get(@PathVariable Long id) {
        return R.ok(removalService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:fbaRemoval:add')")
    @OperLog(title = "移除订单", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Valid @RequestBody FbaRequests.RemovalRequest req) {
        return R.ok(removalService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaRemoval:edit')")
    @OperLog(title = "移除订单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody FbaRequests.RemovalRequest req) {
        removalService.update(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("@ss.hasPerm('ecom:fbaRemoval:edit')")
    @OperLog(title = "提交移除订单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> submit(@PathVariable Long id, @Valid @RequestBody FbaRequests.RemovalSubmit req) {
        removalService.submit(id, req);
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPerm('ecom:fbaRemoval:edit')")
    @OperLog(title = "取消移除订单", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> cancel(@PathVariable Long id) {
        removalService.cancel(id);
        return R.ok();
    }

    /** 本地仓收货（良品 / 不良品），返回生成的调拨单号 */
    @PostMapping("/{id}/receive")
    @PreAuthorize("@ss.hasPerm('ecom:fbaRemoval:receive')")
    @OperLog(title = "移除订单收货", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<List<String>> receive(@PathVariable Long id, @Valid @RequestBody FbaRequests.RemovalReceive req) {
        return R.ok(removalService.receive(id, req));
    }

    /** 弃置出库（平台仓报废出库），返回出库单号 */
    @PostMapping("/{id}/dispose")
    @PreAuthorize("@ss.hasPerm('ecom:fbaRemoval:receive')")
    @OperLog(title = "移除订单弃置出库", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<String> dispose(@PathVariable Long id,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(removalService.dispose(id, date));
    }

    /** 同步：创建最近 90 天的移除订单详情报告任务 */
    @PostMapping("/sync/{storeId}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaRemoval:sync')")
    @OperLog(title = "同步移除订单", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<List<Long>> sync(@PathVariable Long storeId) {
        return R.ok(reportService.createAll(storeId, List.of(AmzReportType.REMOVAL_ORDERS), SYNC_DAYS));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaRemoval:remove')")
    @OperLog(title = "移除订单", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        removalService.delete(ids);
        return R.ok();
    }
}
