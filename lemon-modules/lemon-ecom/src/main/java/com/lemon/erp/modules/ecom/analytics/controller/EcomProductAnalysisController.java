package com.lemon.erp.modules.ecom.analytics.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.modules.ecom.analytics.dto.ProductViews;
import com.lemon.erp.modules.ecom.analytics.model.AbcMetric;
import com.lemon.erp.modules.ecom.analytics.service.EcomProductAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 商品分析 /ecom/analytics/product：ABC、滞销与库龄（含动销率）、售罄与周转、断货天数、新品表现、退货率排行。
 * 日期参数 beginDate / endDate（含首尾），导出由前端生成（按钮权限 ecom:productAnalysis:export）
 */
@RestController
@RequestMapping("/ecom/analytics/product")
@RequiredArgsConstructor
public class EcomProductAnalysisController {

    private final EcomProductAnalysisService analysisService;

    @GetMapping("/abc")
    @PreAuthorize("@ss.hasPerm('ecom:productAnalysis:list')")
    public R<ProductViews.AbcResult> abc(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                         @RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) AbcMetric metric, @RequestParam(required = false) BigDecimal aPercent,
                                         @RequestParam(required = false) BigDecimal bPercent) {
        return R.ok(analysisService.abc(beginDate, endDate, storeId, keyword, metric, aPercent, bPercent));
    }

    /** 滞销（最近 days 天没有销量但有库存，默认 30 天）与动销率 */
    @GetMapping("/slow")
    @PreAuthorize("@ss.hasPerm('ecom:productAnalysis:list')")
    public R<ProductViews.SlowResult> slow(@RequestParam(defaultValue = "30") int days, @RequestParam(required = false) Long storeId,
                                           @RequestParam(required = false) String keyword) {
        return R.ok(analysisService.slowMoving(days, storeId, keyword));
    }

    @GetMapping("/sell-through")
    @PreAuthorize("@ss.hasPerm('ecom:productAnalysis:list')")
    public R<List<ProductViews.SellThroughRow>> sellThrough(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword) {
        return R.ok(analysisService.sellThrough(beginDate, endDate, storeId, keyword));
    }

    @GetMapping("/stockout")
    @PreAuthorize("@ss.hasPerm('ecom:productAnalysis:list')")
    public R<List<ProductViews.StockoutRow>> stockout(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword) {
        return R.ok(analysisService.stockout(beginDate, endDate, storeId, keyword));
    }

    /** 新品表现（首单日在范围内，默认最近 90 天） */
    @GetMapping("/new-products")
    @PreAuthorize("@ss.hasPerm('ecom:productAnalysis:list')")
    public R<List<ProductViews.NewProductRow>> newProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword) {
        return R.ok(analysisService.newProducts(beginDate, endDate, storeId, keyword));
    }

    /** 退货率排行（期间内下单的订单，默认最近 90 天） */
    @GetMapping("/return-rate")
    @PreAuthorize("@ss.hasPerm('ecom:productAnalysis:list')")
    public R<ProductViews.ReturnResult> returnRate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword) {
        return R.ok(analysisService.returnRate(beginDate, endDate, storeId, keyword));
    }
}
