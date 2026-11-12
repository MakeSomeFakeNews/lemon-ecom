package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.SettlementImportRow;
import com.lemon.erp.modules.ecom.entity.EcomFeeAllocation;
import com.lemon.erp.modules.ecom.entity.EcomSettlement;
import com.lemon.erp.modules.ecom.model.FeeAllocMethod;
import com.lemon.erp.modules.ecom.model.SettlementStatus;
import com.lemon.erp.modules.ecom.service.EcomSettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台结算 /ecom/settlement
 */
@RestController
@RequestMapping("/ecom/settlement")
@RequiredArgsConstructor
public class EcomSettlementController {

    private final EcomSettlementService settlementService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:settlement:list')")
    public R<PageResult<EcomSettlement>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) Long storeId, @RequestParam(required = false) SettlementStatus status,
                                              @RequestParam(required = false) String keyword) {
        return R.ok(settlementService.page(current, size, storeId, status, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:settlement:list')")
    public R<EcomSettlement> get(@PathVariable Long id) {
        return R.ok(settlementService.detail(id));
    }

    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('ecom:settlement:import')")
    @OperLog(title = "平台结算", type = OperType.IMPORT) // i18n-ignore 操作日志标题存库
    public R<ImportResult> importRows(@Valid @RequestBody ImportRequest<SettlementImportRow> req) {
        return R.ok(settlementService.importRows(req));
    }

    /** 确认：回写订单费用；传入收款账户时生成收款单 */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("@ss.hasPerm('ecom:settlement:confirm')")
    @OperLog(title = "平台结算确认", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Void> confirm(@PathVariable Long id, @RequestParam(required = false) Long accountId) {
        settlementService.confirm(id, accountId);
        return R.ok();
    }

    /** 不挂订单费用的分摊结果 */
    @GetMapping("/{id}/allocations")
    @PreAuthorize("@ss.hasPerm('ecom:settlement:list')")
    public R<List<EcomFeeAllocation>> allocations(@PathVariable Long id) {
        return R.ok(settlementService.allocations(id));
    }

    /** 重新分摊不挂订单的费用：method 为 QTY 按销量 / SALES 按销售额，为空按类型默认 */
    @PostMapping("/{id}/allocate")
    @PreAuthorize("@ss.hasPerm('ecom:settlement:allocate')")
    @OperLog(title = "平台结算分摊", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Integer> allocate(@PathVariable Long id, @RequestParam(required = false) FeeAllocMethod method) {
        return R.ok(settlementService.reallocate(id, method));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:settlement:remove')")
    @OperLog(title = "平台结算", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        settlementService.delete(ids);
        return R.ok();
    }
}
