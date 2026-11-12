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
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaSizeTier;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import com.lemon.erp.modules.ecom.fba.service.AmzReportService;
import com.lemon.erp.modules.ecom.fba.service.FbaFeeAuditService;
import com.lemon.erp.modules.ecom.fba.service.FbaFeeCalculator;
import com.lemon.erp.modules.ecom.fba.service.FbaRateService;

import java.math.BigDecimal;

/**
 * FBA 费用核查 /ecom/fba/fee：配送费核查与生成索赔、商品尺寸、尺寸分段与配送费费率、试算
 */
@RestController
@RequestMapping("/ecom/fba/fee")
@RequiredArgsConstructor
public class EcomFbaFeeController {

    private final FbaFeeAuditService auditService;
    private final FbaRateService rateService;
    private final AmzReportService reportService;

    @GetMapping("/audit")
    @PreAuthorize("@ss.hasPerm('ecom:fbaFee:list')")
    public R<FbaViews.AuditResult> audit(@RequestParam(required = false) Long storeId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                         @RequestParam(required = false) Boolean overOnly) {
        return R.ok(auditService.audit(storeId, beginDate, endDate, overOnly));
    }

    /** 选中的多收 SKU 生成「多收配送费」索赔，返回新增条数 */
    @PostMapping("/audit/claims")
    @PreAuthorize("@ss.hasPerm('ecom:fbaClaim:add')")
    @OperLog(title = "FBA 费用核查生成索赔", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Integer> claims(@Valid @RequestBody FbaRequests.AuditClaims req) {
        return R.ok(auditService.generateClaims(req));
    }

    @GetMapping("/dims")
    @PreAuthorize("@ss.hasPerm('ecom:fbaFee:list')")
    public R<PageResult<FbaViews.DimRow>> dims(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Boolean missingOnly) {
        return R.ok(auditService.dims(current, size, storeId, keyword, missingOnly));
    }

    @PutMapping("/dims")
    @PreAuthorize("@ss.hasPerm('ecom:fbaFee:edit')")
    @OperLog(title = "FBA 商品尺寸", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> saveDim(@Valid @RequestBody FbaRequests.DimSave req) {
        auditService.saveDim(req);
        return R.ok();
    }

    @DeleteMapping("/dims/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaFee:edit')")
    @OperLog(title = "FBA 商品尺寸", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> deleteDims(@PathVariable List<Long> ids) {
        auditService.deleteDims(ids);
        return R.ok();
    }

    /** 同步：创建预估 FBA 费用报告任务（带入商品尺寸重量） */
    @PostMapping("/dims/sync/{storeId}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaFee:edit')")
    @OperLog(title = "同步 FBA 商品尺寸", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<List<Long>> syncDims(@PathVariable Long storeId) {
        return R.ok(reportService.createAll(storeId, List.of(AmzReportType.ESTIMATED_FEES), 30));
    }

    @GetMapping("/tiers")
    @PreAuthorize("@ss.hasPerm('ecom:fbaFee:list')")
    public R<List<EcomFbaSizeTier>> tiers(@RequestParam(required = false) String marketplace) {
        return R.ok(rateService.tiers(marketplace));
    }

    @PostMapping("/tiers")
    @PreAuthorize("@ss.hasPerm('ecom:fbaFee:edit')")
    @OperLog(title = "FBA 配送费分段", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> addTier(@Valid @RequestBody EcomFbaSizeTier tier) {
        return R.ok(rateService.saveTier(null, tier));
    }

    @PutMapping("/tiers/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaFee:edit')")
    @OperLog(title = "FBA 配送费分段", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Long> updateTier(@PathVariable Long id, @Valid @RequestBody EcomFbaSizeTier tier) {
        return R.ok(rateService.saveTier(id, tier));
    }

    @DeleteMapping("/tiers/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaFee:edit')")
    @OperLog(title = "FBA 配送费分段", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> deleteTiers(@PathVariable List<Long> ids) {
        rateService.deleteTiers(ids);
        return R.ok();
    }

    /** 配送费试算（cm / kg），没有匹配的分段时返回空 */
    @GetMapping("/quote")
    @PreAuthorize("@ss.hasPerm('ecom:fbaFee:list')")
    public R<FbaViews.FeeQuote> quote(@RequestParam String marketplace, @RequestParam BigDecimal length, @RequestParam BigDecimal width,
                                      @RequestParam BigDecimal height, @RequestParam BigDecimal weight) {
        return R.ok(rateService.quote(marketplace, new FbaFeeCalculator.Dims(length, width, height, weight)));
    }
}
