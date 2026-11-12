package com.lemon.erp.modules.ecom.owh.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.DiffHandleRequest;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhDiff;
import com.lemon.erp.modules.ecom.owh.model.OwhDiffSource;
import com.lemon.erp.modules.ecom.owh.model.OwhDiffStatus;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhDiffService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 海外仓收货差异 /ecom/owh/diff：入库单、退件单、调拨单的实收差异与处理
 */
@RestController
@RequestMapping("/ecom/owh/diff")
@RequiredArgsConstructor
public class EcomOwhDiffController {

    private final EcomOwhDiffService diffService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:owhDiff:list')")
    public R<PageResult<EcomOwhDiff>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                           OwhDiffSource sourceType, OwhDiffStatus status, Long warehouseId, String keyword) {
        return R.ok(diffService.page(current, size, sourceType, status, warehouseId, keyword));
    }

    /** 某张单据的差异（入库单、退件单、调拨单详情用） */
    @GetMapping("/by-source")
    @PreAuthorize("@ss.hasPerm('ecom:owhDiff:list') or @ss.hasPerm('ecom:owhInbound:list') or @ss.hasPerm('ecom:owhReturn:list') or @ss.hasPerm('ecom:owhTransfer:list')")
    public R<List<EcomOwhDiff>> bySource(@RequestParam OwhDiffSource sourceType, @RequestParam Long sourceId) {
        return R.ok(diffService.bySource(sourceType, sourceId));
    }

    /** 处理差异：LOSS 确认损耗 / FOUND 找回补收 / ACCEPTED 确认溢收 */
    @PostMapping("/{id}/handle")
    @PreAuthorize("@ss.hasPerm('ecom:owhDiff:handle')")
    @OperLog(title = "海外仓收货差异处理", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> handle(@PathVariable Long id, @Validated @RequestBody DiffHandleRequest req) {
        diffService.handle(id, req);
        return R.ok();
    }
}
