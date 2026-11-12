package com.lemon.erp.modules.ecom.publish.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import com.lemon.erp.modules.ecom.dto.IdsRequest;
import com.lemon.erp.modules.ecom.publish.dto.PublishRequests;
import com.lemon.erp.modules.ecom.publish.dto.PublishViews;
import com.lemon.erp.modules.ecom.publish.entity.EcomPriceChange;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeSource;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeStatus;
import com.lemon.erp.modules.ecom.publish.model.PriceChangeType;
import com.lemon.erp.modules.ecom.publish.service.EcomPriceChangeService;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 改价 / 改库存 /ecom/price-change：批量修改（预览、执行，在在线商品页面多选后使用）与改价历史
 */
@RestController
@RequestMapping("/ecom/price-change")
@RequiredArgsConstructor
public class EcomPriceChangeController {

    private static final String TITLE = "批量改价"; // i18n-ignore 操作日志标题存库

    private final EcomPriceChangeService changeService;

    @GetMapping
    @PreAuthorize("@ss.hasAnyPerm('ecom:priceChange:list', 'ecom:listing:list')")
    public R<PageResult<EcomPriceChange>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) Long storeId, @RequestParam(required = false) Long listingId,
                                               @RequestParam(required = false) String keyword, @RequestParam(required = false) PriceChangeType changeType,
                                               @RequestParam(required = false) PriceChangeSource source, @RequestParam(required = false) PriceChangeStatus status,
                                               @RequestParam(required = false) String batchNo,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return R.ok(changeService.page(current, size, storeId, listingId, keyword, changeType, source, status, batchNo, beginDate, endDate));
    }

    @PostMapping("/preview")
    @PreAuthorize("@ss.hasPerm('ecom:priceChange:add')")
    public R<PublishViews.ChangeResult> preview(@Valid @RequestBody PublishRequests.BatchChange req) {
        return R.ok(changeService.preview(req));
    }

    @PostMapping("/apply")
    @PreAuthorize("@ss.hasPerm('ecom:priceChange:add')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<PublishViews.ChangeResult> apply(@Valid @RequestBody PublishRequests.BatchChange req) {
        return R.ok(changeService.apply(req));
    }

    /** 锁定（locked = true）/ 解除手工库存 */
    @PutMapping("/stock-lock")
    @PreAuthorize("@ss.hasPerm('ecom:priceChange:add')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Integer> stockLock(@RequestParam boolean locked, @Valid @RequestBody IdsRequest req) {
        return R.ok(changeService.lockStock(req.ids(), locked));
    }
}
