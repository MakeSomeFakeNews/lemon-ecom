package com.lemon.erp.modules.ecom.owh.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.FeeSyncRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.IdsRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.FeeSummary;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.SyncResult;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhFee;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhFeeAlloc;
import com.lemon.erp.modules.ecom.owh.model.OwhFeeStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhFeeType;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhFeeService;
import com.lemon.erp.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 海外仓费用 /ecom/owh/fee：费用明细、按月汇总、同步、重新入账、入账明细
 */
@RestController
@RequestMapping("/ecom/owh/fee")
@RequiredArgsConstructor
public class EcomOwhFeeController {

    private final EcomOwhFeeService feeService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:owhFee:list')")
    public R<PageResult<EcomOwhFee>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                          Long accountId, OwhFeeType feeType, OwhFeeStatus status,
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, String keyword) {
        return R.ok(feeService.page(current, size, accountId, feeType, status, beginDate, endDate, keyword));
    }

    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPerm('ecom:owhFee:list')")
    public R<FeeSummary> summary(Long accountId, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(feeService.summary(accountId, beginDate, endDate));
    }

    @GetMapping("/{id}/allocations")
    @PreAuthorize("@ss.hasPerm('ecom:owhFee:list')")
    public R<List<EcomOwhFeeAlloc>> allocations(@PathVariable Long id) {
        return R.ok(feeService.allocations(id));
    }

    /** 同步费用：账号为空时全部启用账号，日期为空取最近 7 天 */
    @PostMapping("/sync")
    @PreAuthorize("@ss.hasPerm('ecom:owhFee:sync')")
    @OperLog(title = "同步海外仓费用", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<List<SyncResult>> sync(@RequestBody FeeSyncRequest req) {
        LocalDate end = req.endDate() == null ? LocalDate.now() : req.endDate();
        LocalDate begin = req.beginDate() == null ? end.minusDays(7) : req.beginDate();
        if (begin.isAfter(end)) {
            throw new BizException("ecom.error.owh.dateRange");
        }
        if (req.accountId() == null) {
            return R.ok(feeService.syncAll(begin, end));
        }
        return R.ok(List.of(new SyncResult(String.valueOf(req.accountId()), feeService.sync(req.accountId(), begin, end), null)));
    }

    /** 重新入账（待入账 / 无法归属的费用），返回入账成功的条数 */
    @PostMapping("/repost")
    @PreAuthorize("@ss.hasPerm('ecom:owhFee:sync')")
    @OperLog(title = "海外仓费用重新入账", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<Integer> repost(@Validated @RequestBody IdsRequest req) {
        return R.ok(feeService.repost(req.ids()));
    }
}
