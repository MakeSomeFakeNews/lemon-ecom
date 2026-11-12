package com.lemon.erp.modules.ecom.fba.controller;

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

import java.util.List;
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaStorageRate;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import com.lemon.erp.modules.ecom.fba.model.FbaHealthAdvice;
import com.lemon.erp.modules.ecom.fba.service.AmzReportService;
import com.lemon.erp.modules.ecom.fba.service.FbaHealthService;
import com.lemon.erp.modules.ecom.fba.service.FbaRateService;

/**
 * FBA 库存健康 /ecom/fba/health：可售 / 在途 / 预留 / 不可售 / 调查中、库龄分段、冗余、预估仓储费、处理建议；仓储费费率维护
 */
@RestController
@RequestMapping("/ecom/fba/health")
@RequiredArgsConstructor
public class EcomFbaHealthController {

    private final FbaHealthService healthService;
    private final FbaRateService rateService;
    private final AmzReportService reportService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:fbaHealth:list')")
    public R<PageResult<FbaViews.HealthRow>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) FbaHealthAdvice advice, @RequestParam(required = false) Boolean excessOnly,
                                                  @RequestParam(required = false) Boolean agedOnly, @RequestParam(required = false) Integer excessDays) {
        return R.ok(healthService.page(current, size, new FbaHealthService.Query(storeId, keyword, advice, excessOnly, agedOnly, excessDays)));
    }

    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPerm('ecom:fbaHealth:list')")
    public R<FbaViews.HealthSummary> summary(@RequestParam(required = false) Long storeId, @RequestParam(required = false) Integer excessDays) {
        return R.ok(healthService.summary(storeId, excessDays));
    }

    /** 从亚马逊同步：创建库存规划、FBA 库存两个报告任务（异步），返回任务 ID */
    @PostMapping("/sync/{storeId}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaHealth:sync')")
    @OperLog(title = "同步 FBA 库存健康", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<List<Long>> sync(@PathVariable Long storeId) {
        return R.ok(reportService.createAll(storeId, List.of(AmzReportType.FBA_INVENTORY_PLANNING, AmzReportType.FBA_MYI_INVENTORY), 30));
    }

    /** 按头程签收先进先出推算库龄，返回写入条数 */
    @PostMapping("/rebuild")
    @PreAuthorize("@ss.hasPerm('ecom:fbaHealth:sync')")
    @OperLog(title = "推算 FBA 库龄", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Integer> rebuild(@RequestParam Long storeId) {
        return R.ok(healthService.rebuildFromInbound(storeId));
    }

    @GetMapping("/rates")
    @PreAuthorize("@ss.hasPerm('ecom:fbaHealth:list')")
    public R<List<EcomFbaStorageRate>> rates(@RequestParam(required = false) String marketplace) {
        return R.ok(rateService.storageRates(marketplace));
    }

    @PostMapping("/rates")
    @PreAuthorize("@ss.hasPerm('ecom:fbaHealth:rate')")
    @OperLog(title = "FBA 仓储费费率", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> addRate(@Valid @RequestBody EcomFbaStorageRate rate) {
        return R.ok(rateService.saveStorageRate(null, rate));
    }

    @PutMapping("/rates/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaHealth:rate')")
    @OperLog(title = "FBA 仓储费费率", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Long> updateRate(@PathVariable Long id, @Valid @RequestBody EcomFbaStorageRate rate) {
        return R.ok(rateService.saveStorageRate(id, rate));
    }

    @DeleteMapping("/rates/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaHealth:rate')")
    @OperLog(title = "FBA 仓储费费率", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> deleteRates(@PathVariable List<Long> ids) {
        rateService.deleteStorageRates(ids);
        return R.ok();
    }
}
