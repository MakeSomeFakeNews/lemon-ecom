package com.lemon.erp.modules.scm.controller;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.scm.dto.CustomerPriceImportRow;
import com.lemon.erp.modules.scm.dto.PricingViews;
import com.lemon.erp.modules.scm.entity.ScmCustomerPrice;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.service.ScmCustomerPriceService;
import com.lemon.erp.modules.scm.service.ScmPricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 价格体系：开单默认价、价格参考、客户执行价（含导入导出）
 */
@RestController
@RequestMapping("/scm")
@RequiredArgsConstructor
public class ScmPricingController {

    private final ScmPricingService pricingService;
    private final ScmCustomerPriceService customerPriceService;

    /**
     * 开单默认价（含税、基本单位、本位币）：销售订单、出入库直接开单选择物料后带出
     *
     * @param kind SALES / PURCHASE
     * @param qtys 与 materialIds 一一对应的数量（可省略）
     */
    @GetMapping("/pricing/default")
    @PreAuthorize("@ss.hasAnyPerm('scm:sales:add', 'scm:sales:edit', 'scm:salesOut:add', 'scm:purchase:add', 'scm:purchase:edit', 'scm:purchaseIn:add')")
    public R<List<PricingViews.DefaultPrice>> defaults(@RequestParam OrderKind kind, @RequestParam(required = false) Long partnerId,
                                                       @RequestParam List<Long> materialIds,
                                                       @RequestParam(required = false) List<BigDecimal> qtys,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(pricingService.defaults(kind, partnerId, materialIds, qtys, date));
    }

    /**
     * 价格参考：销售历史、进货历史、进货数量、供商对比、客户对比，按权限裁剪分区
     *
     * @param keyword 往来单位编码或名称
     * @param kind    partnerId 的种类（SALES 客户 / PURCHASE 供应商），指定时 partnerId 只筛选该方向的分区
     */
    @GetMapping("/pricing/reference")
    @PreAuthorize("@ss.hasAnyPerm('scm:sales:list', 'scm:salesOut:list', 'scm:purchase:list', 'scm:purchaseIn:list')")
    public R<PricingViews.Reference> reference(@RequestParam Long materialId, @RequestParam(required = false) Long partnerId,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) OrderKind kind,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return R.ok(pricingService.reference(new PricingViews.ReferenceQuery(materialId, partnerId, kw, start, end,
                ScmPricingService.REFERENCE_LIMIT), kind));
    }

    // ---------------------------------------------------------------- 客户执行价

    @GetMapping("/customer/{customerId}/prices")
    @PreAuthorize("@ss.hasPerm('scm:customer:list')")
    public R<List<ScmCustomerPrice>> customerPrices(@PathVariable Long customerId) {
        return R.ok(customerPriceService.list(customerId));
    }

    /**
     * 导出执行价（含客户、物料编码名称），列与导入模板一致
     *
     * @param customerId 只导出该客户，为空导出全部
     * @param keyword    客户编码 / 名称
     */
    @GetMapping("/customer-price/export")
    @PreAuthorize("@ss.hasPerm('scm:customer:list')")
    public R<List<ScmCustomerPrice>> exportCustomerPrices(@RequestParam(required = false) Long customerId,
                                                          @RequestParam(required = false) String keyword) {
        return R.ok(customerPriceService.export(customerId, keyword));
    }

    /** Excel 导入：按 客户编码 × 物料编码 新增或更新执行价（文件中没有的不变），任何一行出错整批不导入 */
    @PostMapping("/customer-price/import")
    @PreAuthorize("@ss.hasPerm('scm:customer:edit')")
    @OperLog(title = "客户执行价-导入", type = OperType.IMPORT) // i18n-ignore
    public R<ImportResult> importCustomerPrices(@Valid @RequestBody ImportRequest<CustomerPriceImportRow> req) {
        return R.ok(customerPriceService.importRows(req));
    }

    @PutMapping("/customer/{customerId}/prices")
    @PreAuthorize("@ss.hasPerm('scm:customer:edit')")
    @OperLog(title = "客户执行价", type = OperType.UPDATE) // i18n-ignore
    public R<Void> saveCustomerPrices(@PathVariable Long customerId, @RequestBody List<ScmCustomerPrice> rows) {
        customerPriceService.save(customerId, rows);
        return R.ok();
    }
}
