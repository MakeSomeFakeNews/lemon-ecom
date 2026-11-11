package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.FundPlan;
import com.lemon.erp.modules.fin.dto.FundPlanEntry;
import com.lemon.erp.modules.fin.dto.FundTransferRequest;
import com.lemon.erp.modules.fin.entity.FinFundPlanItem;
import com.lemon.erp.modules.fin.entity.FinFundTransfer;
import com.lemon.erp.modules.fin.model.PlanUnit;
import com.lemon.erp.modules.fin.service.FinFundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/**
 * 资金管理：账户调拨、资金计划
 */
@RestController
@RequestMapping("/fin")
@RequiredArgsConstructor
public class FinFundController {

    private final FinFundService fundService;

    // ---------------------------------------------------------------- 账户调拨

    @GetMapping("/fund-transfer")
    @PreAuthorize("@ss.hasPerm('fin:transfer:list')")
    public R<PageResult<FinFundTransfer>> pageTransfers(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                        @RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size) {
        return R.ok(fundService.pageTransfers(current, size, keyword, begin, end));
    }

    @GetMapping("/fund-transfer/{id}")
    @PreAuthorize("@ss.hasPerm('fin:transfer:list')")
    public R<FinFundTransfer> getTransfer(@PathVariable Long id) {
        return R.ok(fundService.getTransfer(id));
    }

    @PostMapping("/fund-transfer")
    @PreAuthorize("@ss.hasPerm('fin:transfer:edit')")
    @OperLog(title = "账户调拨", type = OperType.INSERT) // i18n-ignore
    public R<Long> createTransfer(@Valid @RequestBody FundTransferRequest req) {
        return R.ok(fundService.createTransfer(req));
    }

    @DeleteMapping("/fund-transfer/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:transfer:edit')")
    @OperLog(title = "账户调拨", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteTransfers(@PathVariable List<Long> ids) {
        fundService.deleteTransfers(ids);
        return R.ok();
    }

    // ---------------------------------------------------------------- 资金计划

    @GetMapping("/fund-plan")
    @PreAuthorize("@ss.hasPerm('fin:fundPlan:list')")
    public R<FundPlan> plan(@RequestParam(defaultValue = "WEEK") PlanUnit unit, @RequestParam(defaultValue = "12") int periods) {
        return R.ok(fundService.plan(unit, periods));
    }

    /** 一期的收支明细；from 为空表示已逾期 */
    @GetMapping("/fund-plan/entries")
    @PreAuthorize("@ss.hasPerm('fin:fundPlan:list')")
    public R<List<FundPlanEntry>> planEntries(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return R.ok(fundService.planEntries(from, to));
    }

    @GetMapping("/fund-plan/items")
    @PreAuthorize("@ss.hasPerm('fin:fundPlan:list')")
    public R<PageResult<FinFundPlanItem>> pageItems(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate begin,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                                    @RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size) {
        return R.ok(fundService.pageItems(current, size, begin, end));
    }

    @PostMapping("/fund-plan/items")
    @PreAuthorize("@ss.hasPerm('fin:fundPlan:edit')")
    @OperLog(title = "资金计划项", type = OperType.INSERT) // i18n-ignore
    public R<Void> createItem(@Validated(Create.class) @RequestBody FinFundPlanItem item) {
        item.setId(null);
        fundService.saveItem(item);
        return R.ok();
    }

    @PutMapping("/fund-plan/items/{id}")
    @PreAuthorize("@ss.hasPerm('fin:fundPlan:edit')")
    @OperLog(title = "资金计划项", type = OperType.UPDATE) // i18n-ignore
    public R<Void> updateItem(@PathVariable Long id, @Valid @RequestBody FinFundPlanItem item) {
        item.setId(id);
        fundService.saveItem(item);
        return R.ok();
    }

    @DeleteMapping("/fund-plan/items/{ids}")
    @PreAuthorize("@ss.hasPerm('fin:fundPlan:edit')")
    @OperLog(title = "资金计划项", type = OperType.DELETE) // i18n-ignore
    public R<Void> deleteItems(@PathVariable List<Long> ids) {
        fundService.deleteItems(ids);
        return R.ok();
    }
}
