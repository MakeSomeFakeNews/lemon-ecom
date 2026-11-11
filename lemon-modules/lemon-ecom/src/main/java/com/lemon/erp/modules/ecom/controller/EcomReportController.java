package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.ecom.dto.EcomReports;
import com.lemon.erp.modules.ecom.service.EcomReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 电商看板与利润分析 /ecom/report（金额为本位币）
 */
@RestController
@RequestMapping("/ecom/report")
@RequiredArgsConstructor
public class EcomReportController {

    private final EcomReportService reportService;

    @GetMapping("/overview")
    @PreAuthorize("@ss.hasPerm('ecom:dashboard:list')")
    public R<EcomReports.Overview> overview() {
        return R.ok(reportService.overview());
    }

    @GetMapping("/trend")
    @PreAuthorize("@ss.hasPerm('ecom:dashboard:list')")
    public R<List<EcomReports.TrendRow>> trend(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                               @RequestParam(required = false) Long storeId) {
        return R.ok(reportService.trend(beginDate, endDate, storeId));
    }

    @GetMapping("/store")
    @PreAuthorize("@ss.hasAnyPerm('ecom:report:list', 'ecom:dashboard:list')")
    public R<List<EcomReports.StoreRow>> byStore(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                 @RequestParam(required = false) Long storeId) {
        return R.ok(reportService.byStore(beginDate, endDate, storeId));
    }

    @GetMapping("/sku")
    @PreAuthorize("@ss.hasPerm('ecom:report:list')")
    public R<List<EcomReports.SkuRow>> bySku(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                             @RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword) {
        return R.ok(reportService.bySku(beginDate, endDate, storeId, keyword));
    }

    @GetMapping("/country")
    @PreAuthorize("@ss.hasAnyPerm('ecom:report:list', 'ecom:dashboard:list')")
    public R<List<EcomReports.CountryRow>> byCountry(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                     @RequestParam(required = false) Long storeId) {
        return R.ok(reportService.byCountry(beginDate, endDate, storeId));
    }

    @GetMapping("/fees")
    @PreAuthorize("@ss.hasAnyPerm('ecom:report:list', 'ecom:dashboard:list')")
    public R<List<EcomReports.FeeRow>> fees(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                            @RequestParam(required = false) Long storeId) {
        return R.ok(reportService.fees(beginDate, endDate, storeId));
    }
}
