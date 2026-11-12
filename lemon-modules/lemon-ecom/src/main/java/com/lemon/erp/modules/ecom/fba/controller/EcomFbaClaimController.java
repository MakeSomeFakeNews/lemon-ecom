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
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaClaim;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaLedger;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaReimbursement;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimStatus;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimType;
import com.lemon.erp.modules.ecom.fba.service.AmzReportService;
import com.lemon.erp.modules.ecom.fba.service.FbaClaimService;

/**
 * FBA 索赔 /ecom/fba/claim：索赔清单（自动识别、手工登记、Case 跟踪）、库存流水、赔偿记录（自动匹配）、货件对账
 */
@RestController
@RequestMapping("/ecom/fba/claim")
@RequiredArgsConstructor
public class EcomFbaClaimController {

    /** 同步库存流水的天数（覆盖丢失 / 损坏的 60 天索赔期） */
    private static final int LEDGER_DAYS = 90;
    /** 同步赔偿的天数 */
    private static final int REIMBURSEMENT_DAYS = 180;

    private final FbaClaimService claimService;
    private final AmzReportService reportService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:fbaClaim:list')")
    public R<PageResult<EcomFbaClaim>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) Long storeId, @RequestParam(required = false) FbaClaimType claimType,
                                            @RequestParam(required = false) FbaClaimStatus status, @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer expiringDays) {
        return R.ok(claimService.page(current, size, storeId, claimType, status, keyword, expiringDays));
    }

    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPerm('ecom:fbaClaim:list')")
    public R<FbaViews.ClaimSummary> summary(@RequestParam(required = false) Long storeId) {
        return R.ok(claimService.summary(storeId));
    }

    /** 自动识别可索赔项（店铺为空时识别全部亚马逊店铺） */
    @PostMapping("/detect")
    @PreAuthorize("@ss.hasPerm('ecom:fbaClaim:add')")
    @OperLog(title = "识别 FBA 索赔", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<FbaViews.DetectResult> detect(@RequestParam(required = false) Long storeId) {
        return R.ok(claimService.detect(storeId));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:fbaClaim:add')")
    @OperLog(title = "FBA 索赔", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Valid @RequestBody FbaRequests.ClaimCreate req) {
        return R.ok(claimService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaClaim:edit')")
    @OperLog(title = "FBA 索赔", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody FbaRequests.ClaimUpdate req) {
        claimService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaClaim:remove')")
    @OperLog(title = "FBA 索赔", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        claimService.delete(ids);
        return R.ok();
    }

    /** 同步：创建库存分类账明细（最近 90 天）与赔偿（最近 180 天）报告任务 */
    @PostMapping("/sync/{storeId}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaClaim:sync')")
    @OperLog(title = "同步 FBA 流水与赔偿", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<List<Long>> sync(@PathVariable Long storeId) {
        List<Long> ids = new java.util.ArrayList<>(reportService.createAll(storeId, List.of(AmzReportType.LEDGER_DETAIL), LEDGER_DAYS));
        ids.addAll(reportService.createAll(storeId, List.of(AmzReportType.REIMBURSEMENTS), REIMBURSEMENT_DAYS));
        return R.ok(ids);
    }

    @GetMapping("/ledger")
    @PreAuthorize("@ss.hasPerm('ecom:fbaClaim:list')")
    public R<PageResult<EcomFbaLedger>> ledger(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) Long storeId, @RequestParam(required = false) String eventType,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(claimService.ledger(current, size, storeId, eventType, keyword, beginDate, endDate));
    }

    @GetMapping("/reimbursement")
    @PreAuthorize("@ss.hasPerm('ecom:fbaClaim:list')")
    public R<PageResult<EcomFbaReimbursement>> reimbursements(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                              @RequestParam(required = false) Long storeId, @RequestParam(required = false) Boolean matched,
                                                              @RequestParam(required = false) String keyword) {
        return R.ok(claimService.reimbursements(current, size, storeId, matched, keyword));
    }

    /** 重新匹配未匹配的赔偿，返回匹配条数 */
    @PostMapping("/reimbursement/match")
    @PreAuthorize("@ss.hasPerm('ecom:fbaClaim:edit')")
    @OperLog(title = "匹配 FBA 赔偿", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Integer> match(@RequestParam(required = false) Long storeId) {
        return R.ok(claimService.matchReimbursements(storeId));
    }

    @GetMapping("/receipt")
    @PreAuthorize("@ss.hasPerm('ecom:fbaClaim:list')")
    public R<PageResult<FbaViews.ReceiptRow>> receipts(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                       @RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Boolean shortOnly) {
        return R.ok(claimService.receipts(current, size, storeId, keyword, shortOnly));
    }

    @PutMapping("/receipt")
    @PreAuthorize("@ss.hasPerm('ecom:fbaClaim:edit')")
    @OperLog(title = "FBA 货件实收", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> saveReceipt(@Valid @RequestBody FbaRequests.ReceiptSave req) {
        claimService.saveReceipt(req);
        return R.ok();
    }

    /** 调用亚马逊入库货件接口同步实收件数，返回更新的明细数 */
    @PostMapping("/receipt/sync/{inboundId}")
    @PreAuthorize("@ss.hasPerm('ecom:fbaClaim:sync')")
    @OperLog(title = "同步 FBA 货件实收", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Integer> syncReceipts(@PathVariable Long inboundId) {
        return R.ok(claimService.syncReceipts(inboundId));
    }
}
