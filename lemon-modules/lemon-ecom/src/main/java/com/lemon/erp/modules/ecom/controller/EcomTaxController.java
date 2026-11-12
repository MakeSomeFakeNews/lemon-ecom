package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.EcomProfitViews;
import com.lemon.erp.modules.ecom.entity.EcomTaxRate;
import com.lemon.erp.modules.ecom.model.TaxType;
import com.lemon.erp.modules.ecom.service.EcomTaxService;
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

/**
 * 电商税务 /ecom/tax：税率设置、重算订单税额、VAT 汇总
 */
@RestController
@RequestMapping("/ecom/tax")
@RequiredArgsConstructor
public class EcomTaxController {

    private final EcomTaxService taxService;

    @GetMapping("/rate")
    @PreAuthorize("@ss.hasPerm('ecom:tax:list')")
    public R<PageResult<EcomTaxRate>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String country, @RequestParam(required = false) TaxType taxType,
                                           @RequestParam(required = false) EnableStatus status) {
        return R.ok(taxService.page(current, size, country, taxType, status));
    }

    @PostMapping("/rate")
    @PreAuthorize("@ss.hasPerm('ecom:tax:add')")
    @OperLog(title = "电商税率", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Valid @RequestBody EcomTaxRate r) {
        return R.ok(taxService.create(r));
    }

    @PutMapping("/rate/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:tax:edit')")
    @OperLog(title = "电商税率", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody EcomTaxRate r) {
        taxService.update(id, r);
        return R.ok();
    }

    @PutMapping("/rate/{id}/status")
    @PreAuthorize("@ss.hasPerm('ecom:tax:edit')")
    @OperLog(title = "电商税率", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam EnableStatus status) {
        taxService.changeStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/rate/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:tax:remove')")
    @OperLog(title = "电商税率", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        taxService.delete(ids);
        return R.ok();
    }

    /** 按当前税率重算一段下单时间内订单的税额与利润，返回处理的订单数 */
    @PostMapping("/apply")
    @PreAuthorize("@ss.hasPerm('ecom:tax:apply')")
    @OperLog(title = "重算订单税额", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Integer> apply(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                            @RequestParam(required = false) Long storeId) {
        return R.ok(taxService.reapply(beginDate, endDate, storeId));
    }

    /** VAT 汇总：按收件国家 × 月份 */
    @GetMapping("/vat-summary")
    @PreAuthorize("@ss.hasPerm('ecom:tax:list')")
    public R<List<EcomProfitViews.VatRow>> vatSummary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                      @RequestParam(required = false) Long storeId) {
        return R.ok(taxService.vatSummary(beginDate, endDate, storeId));
    }
}
