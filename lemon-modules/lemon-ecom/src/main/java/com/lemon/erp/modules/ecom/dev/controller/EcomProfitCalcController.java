package com.lemon.erp.modules.ecom.dev.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dev.dto.ProfitCalcRequest;
import com.lemon.erp.modules.ecom.dev.dto.ProfitCompareRequest;
import com.lemon.erp.modules.ecom.dev.dto.ProfitDefaults;
import com.lemon.erp.modules.ecom.dev.dto.ProfitResult;
import com.lemon.erp.modules.ecom.dev.dto.ProfitSchemeRequest;
import com.lemon.erp.modules.ecom.dev.entity.EcomCommissionRate;
import com.lemon.erp.modules.ecom.dev.entity.EcomProfitScheme;
import com.lemon.erp.modules.ecom.dev.service.EcomProfitCalcService;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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

/**
 * 利润试算 /ecom/dev/profit：试算、多平台对比、默认参数、试算方案、平台佣金费率。开发项目详情中的试算按开发项目权限也可使用
 */
@RestController
@RequestMapping("/ecom/dev/profit")
@RequiredArgsConstructor
public class EcomProfitCalcController {

    private static final String CALC_PERM = "@ss.hasAnyPerm('ecom:profitCalc:list', 'ecom:devProject:list')";

    private final EcomProfitCalcService calcService;

    @PostMapping("/calc")
    @PreAuthorize(CALC_PERM)
    public R<ProfitResult> calc(@Validated @RequestBody ProfitCalcRequest req) {
        return R.ok(calcService.calc(req));
    }

    @PostMapping("/compare")
    @PreAuthorize(CALC_PERM)
    public R<List<ProfitResult>> compare(@Validated @RequestBody ProfitCompareRequest req) {
        return R.ok(calcService.compare(req));
    }

    /** 平台 / 站点 / 类目的默认佣金、币种、汇率、税率 */
    @GetMapping("/defaults")
    @PreAuthorize(CALC_PERM)
    public R<ProfitDefaults> defaults(@RequestParam EcomPlatform platform, @RequestParam(required = false) String marketplace,
                                      @RequestParam(required = false) String category) {
        return R.ok(calcService.defaults(platform, marketplace, category));
    }

    @GetMapping("/scheme")
    @PreAuthorize(CALC_PERM)
    public R<PageResult<EcomProfitScheme>> schemes(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) Long projectId, @RequestParam(required = false) String keyword) {
        return R.ok(calcService.pageSchemes(current, size, projectId, keyword));
    }

    /** 方案详情（含试算参数 params） */
    @GetMapping("/scheme/{id}")
    @PreAuthorize(CALC_PERM)
    public R<EcomProfitScheme> scheme(@PathVariable Long id) {
        return R.ok(calcService.scheme(id));
    }

    @PostMapping("/scheme")
    @PreAuthorize("@ss.hasAnyPerm('ecom:profitCalc:save', 'ecom:devProject:edit')")
    @OperLog(title = "利润试算方案", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> saveScheme(@Validated @RequestBody ProfitSchemeRequest req) {
        return R.ok(calcService.saveScheme(req));
    }

    @DeleteMapping("/scheme/{ids}")
    @PreAuthorize("@ss.hasAnyPerm('ecom:profitCalc:save', 'ecom:devProject:edit')")
    @OperLog(title = "利润试算方案", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> deleteSchemes(@PathVariable List<Long> ids) {
        calcService.deleteSchemes(ids);
        return R.ok();
    }

    @GetMapping("/rate")
    @PreAuthorize(CALC_PERM)
    public R<List<EcomCommissionRate>> rates(@RequestParam(required = false) EcomPlatform platform, @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) EnableStatus status) {
        return R.ok(calcService.rates(platform, keyword, status));
    }

    @PostMapping("/rate")
    @PreAuthorize("@ss.hasPerm('ecom:profitCalc:rate')")
    @OperLog(title = "平台佣金费率", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> createRate(@Validated @RequestBody EcomCommissionRate r) {
        return R.ok(calcService.createRate(r));
    }

    @PutMapping("/rate/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:profitCalc:rate')")
    @OperLog(title = "平台佣金费率", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> updateRate(@PathVariable Long id, @Validated @RequestBody EcomCommissionRate r) {
        calcService.updateRate(id, r);
        return R.ok();
    }

    @DeleteMapping("/rate/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:profitCalc:rate')")
    @OperLog(title = "平台佣金费率", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> deleteRates(@PathVariable List<Long> ids) {
        calcService.deleteRates(ids);
        return R.ok();
    }
}
