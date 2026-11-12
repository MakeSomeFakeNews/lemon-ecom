package com.lemon.erp.modules.ecom.analytics.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.ecom.analytics.dto.SalesMatrix;
import com.lemon.erp.modules.ecom.analytics.dto.SalesQuery;
import com.lemon.erp.modules.ecom.analytics.dto.SkuTrend;
import com.lemon.erp.modules.ecom.analytics.service.EcomSalesStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 销量统计 /ecom/analytics/sales：销量矩阵（维度 × 时间段，环比 / 同比）、单个 SKU 的销量走势（叠加库存快照与断货）。
 * 导出由前端按矩阵生成 Excel（按钮权限 ecom:salesStats:export）
 */
@RestController
@RequestMapping("/ecom/analytics/sales")
@RequiredArgsConstructor
public class EcomSalesStatsController {

    private final EcomSalesStatsService salesStatsService;

    @GetMapping("/matrix")
    @PreAuthorize("@ss.hasPerm('ecom:salesStats:list')")
    public R<SalesMatrix> matrix(SalesQuery query) {
        return R.ok(salesStatsService.matrix(query));
    }

    @GetMapping("/sku-trend")
    @PreAuthorize("@ss.hasPerm('ecom:salesStats:list')")
    public R<SkuTrend> skuTrend(@RequestParam Long listingId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(salesStatsService.skuTrend(listingId, beginDate, endDate));
    }
}
