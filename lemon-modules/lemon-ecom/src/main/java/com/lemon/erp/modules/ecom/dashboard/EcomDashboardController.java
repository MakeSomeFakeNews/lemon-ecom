package com.lemon.erp.modules.ecom.dashboard;

import com.lemon.erp.common.result.R;
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
 * 电商看板 /ecom/dashboard：关键指标与环比 / 同比、实时销量、Top SKU、店铺排行、平台占比、待处理事项（金额为本位币）
 */
@RestController
@RequestMapping("/ecom/dashboard")
@RequiredArgsConstructor
public class EcomDashboardController {

    private final EcomDashboardService dashboardService;

    @GetMapping("/kpi")
    @PreAuthorize("@ss.hasPerm('ecom:dashboard:list')")
    public R<List<EcomDashboardViews.KpiPeriod>> kpi(@RequestParam(required = false) Long storeId) {
        return R.ok(dashboardService.kpi(storeId));
    }

    @GetMapping("/hourly")
    @PreAuthorize("@ss.hasPerm('ecom:dashboard:list')")
    public R<EcomDashboardViews.Hourly> hourly(@RequestParam(required = false) Long storeId) {
        return R.ok(dashboardService.hourly(storeId));
    }

    @GetMapping("/top-sku")
    @PreAuthorize("@ss.hasPerm('ecom:dashboard:list')")
    public R<List<EcomDashboardViews.SkuRank>> topSku(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                      @RequestParam(required = false) Long storeId,
                                                      @RequestParam(required = false) EcomDashboardService.SkuRankBy by,
                                                      @RequestParam(defaultValue = "10") int limit) {
        return R.ok(dashboardService.topSku(beginDate, endDate, storeId, by, limit));
    }

    @GetMapping("/store-rank")
    @PreAuthorize("@ss.hasPerm('ecom:dashboard:list')")
    public R<List<EcomDashboardViews.StoreRank>> storeRank(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(dashboardService.storeRank(beginDate, endDate));
    }

    @GetMapping("/platform-share")
    @PreAuthorize("@ss.hasPerm('ecom:dashboard:list')")
    public R<List<EcomDashboardViews.PlatformShare>> platformShare(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(dashboardService.platformShare(beginDate, endDate));
    }

    /** 待处理事项（各功能注册的待办，只含当前用户有权限的） */
    @GetMapping("/todos")
    @PreAuthorize("@ss.hasPerm('ecom:dashboard:list')")
    public R<List<EcomDashboardTodo>> todos() {
        return R.ok(dashboardService.todos());
    }
}
