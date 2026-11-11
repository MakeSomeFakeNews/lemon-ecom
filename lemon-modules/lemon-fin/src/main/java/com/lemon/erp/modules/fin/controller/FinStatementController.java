package com.lemon.erp.modules.fin.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.fin.dto.StatementRequests;
import com.lemon.erp.modules.fin.entity.FinSupplierStatement;
import com.lemon.erp.modules.fin.model.StatementStatus;
import com.lemon.erp.modules.fin.service.FinStatementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 供应商对账（/fin/statement）
 */
@RestController
@RequiredArgsConstructor
public class FinStatementController {

    private final FinStatementService statementService;

    @GetMapping("/fin/statement")
    @PreAuthorize("@ss.hasPerm('fin:statement:list')")
    public R<PageResult<FinSupplierStatement>> page(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Long supplierId,
                                                    @RequestParam(required = false) StatementStatus status,
                                                    @RequestParam(required = false) String keyword) {
        return R.ok(statementService.page(current, size, supplierId, status, keyword, false));
    }

    @GetMapping("/fin/statement/{id}")
    @PreAuthorize("@ss.hasPerm('fin:statement:list')")
    public R<FinSupplierStatement> detail(@PathVariable Long id) {
        return R.ok(statementService.detail(id, null));
    }

    @PostMapping("/fin/statement")
    @PreAuthorize("@ss.hasPerm('fin:statement:add')")
    @OperLog(title = "供应商对账单", type = OperType.INSERT) // i18n-ignore
    public R<Long> generate(@Valid @RequestBody StatementRequests.Generate req) {
        return R.ok(statementService.generate(req));
    }

    @PostMapping("/fin/statement/{id}/send")
    @PreAuthorize("@ss.hasPerm('fin:statement:edit')")
    @OperLog(title = "供应商对账单-发送", type = OperType.UPDATE) // i18n-ignore
    public R<Void> send(@PathVariable Long id) {
        statementService.send(id);
        return R.ok();
    }

    @DeleteMapping("/fin/statement/{id}")
    @PreAuthorize("@ss.hasPerm('fin:statement:remove')")
    @OperLog(title = "供应商对账单", type = OperType.DELETE) // i18n-ignore
    public R<Void> delete(@PathVariable Long id) {
        statementService.delete(id);
        return R.ok();
    }
}
