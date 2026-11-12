package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.PayoutImportRequest;
import com.lemon.erp.modules.ecom.dto.PayoutImportResult;
import com.lemon.erp.modules.ecom.dto.PayoutReceiveRequest;
import com.lemon.erp.modules.ecom.entity.EcomPayout;
import com.lemon.erp.modules.ecom.model.PayoutStatus;
import com.lemon.erp.modules.ecom.service.EcomPayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 回款对账 /ecom/payout：结算单应到账与银行 / 收款平台到账的匹配
 */
@RestController
@RequestMapping("/ecom/payout")
@RequiredArgsConstructor
public class EcomPayoutController {

    private final EcomPayoutService payoutService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:payout:list')")
    public R<PageResult<EcomPayout>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) PayoutStatus status, @RequestParam(required = false) Long storeId,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                          @RequestParam(required = false) String keyword) {
        return R.ok(payoutService.page(current, size, status, storeId, beginDate, endDate, keyword));
    }

    /** 各状态的记录数与本位币金额 */
    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPerm('ecom:payout:list')")
    public R<List<Map<String, Object>>> summary(@RequestParam(required = false) Long storeId) {
        return R.ok(payoutService.summary(storeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:payout:list')")
    public R<EcomPayout> get(@PathVariable Long id) {
        return R.ok(payoutService.detail(id));
    }

    /** 导入银行 / 收款平台流水并自动匹配 */
    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('ecom:payout:import')")
    @OperLog(title = "回款流水", type = OperType.IMPORT) // i18n-ignore 操作日志标题存库
    public R<PayoutImportResult> importRows(@Valid @RequestBody PayoutImportRequest req) {
        return R.ok(payoutService.importRows(req));
    }

    /** 手工登记到账 */
    @PostMapping("/{id}/receive")
    @PreAuthorize("@ss.hasPerm('ecom:payout:receive')")
    @OperLog(title = "登记到账", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> receive(@PathVariable Long id, @Valid @RequestBody PayoutReceiveRequest req) {
        payoutService.receive(id, req);
        return R.ok();
    }

    /** 手工匹配：未匹配的流水 id 并入未到账记录 pendingId */
    @PostMapping("/{id}/match")
    @PreAuthorize("@ss.hasPerm('ecom:payout:receive')")
    @OperLog(title = "匹配到账", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> match(@PathVariable Long id, @RequestParam Long pendingId) {
        payoutService.match(id, pendingId);
        return R.ok();
    }

    /** 撤销到账（删除未审核的差额凭证） */
    @PostMapping("/{id}/unmatch")
    @PreAuthorize("@ss.hasPerm('ecom:payout:remove')")
    @OperLog(title = "撤销到账", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> unmatch(@PathVariable Long id) {
        payoutService.unmatch(id);
        return R.ok();
    }

    /** 删除未匹配的流水 */
    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:payout:remove')")
    @OperLog(title = "回款流水", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        payoutService.delete(ids);
        return R.ok();
    }
}
