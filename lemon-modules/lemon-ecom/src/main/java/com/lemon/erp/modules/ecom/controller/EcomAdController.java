package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.AdImportRow;
import com.lemon.erp.modules.ecom.dto.EcomReports;
import com.lemon.erp.modules.ecom.entity.EcomAdDaily;
import com.lemon.erp.modules.ecom.service.EcomAdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 广告数据 /ecom/ad
 */
@RestController
@RequestMapping("/ecom/ad")
@RequiredArgsConstructor
public class EcomAdController {

    private final EcomAdService adService;
    private final ImportExecutor importExecutor;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<PageResult<EcomAdDaily>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Long storeId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                           @RequestParam(required = false) String keyword) {
        return R.ok(adService.page(current, size, storeId, beginDate, endDate, keyword));
    }

    /** 汇总：groupBy = store / campaign / sku */
    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPerm('ecom:ad:list')")
    public R<List<EcomReports.AdRow>> summary(@RequestParam(required = false) Long storeId,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                              @RequestParam(defaultValue = "store") String groupBy) {
        return R.ok(adService.summary(storeId, beginDate, endDate, groupBy));
    }

    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('ecom:ad:import')")
    @OperLog(title = "广告数据", type = OperType.IMPORT) // i18n-ignore 操作日志标题存库
    public R<ImportResult> importRows(@Valid @RequestBody ImportRequest<AdImportRow> req) {
        return R.ok(importExecutor.run(req, adService::importRow));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:ad:remove')")
    @OperLog(title = "广告数据", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        adService.delete(ids);
        return R.ok();
    }
}
