package com.lemon.erp.modules.ecom.owh.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.AdjustRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.AdjustResult;
import com.lemon.erp.modules.ecom.owh.dto.OwhViews.SyncResult;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhStock;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 海外仓库存 /ecom/owh/stock：库存与 ERP 账面差异、同步、按海外仓数量调整
 */
@RestController
@RequestMapping("/ecom/owh/stock")
@RequiredArgsConstructor
public class EcomOwhStockController {

    private final EcomOwhStockService stockService;

    /** 分页：accountId、warehouseId、keyword（SKU / 物料名称）、diffOnly（只看有差异的） */
    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:owhStock:list')")
    public R<PageResult<EcomOwhStock>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                            Long accountId, Long warehouseId, String keyword, @RequestParam(defaultValue = "false") boolean diffOnly) {
        return R.ok(stockService.page(current, size, accountId, warehouseId, keyword, diffOnly));
    }

    /** 汇总：skuCount、diffCount、available、transit、locked、erpQty、syncTime */
    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPerm('ecom:owhStock:list')")
    public R<Map<String, Object>> summary(Long accountId, Long warehouseId) {
        return R.ok(stockService.summary(accountId, warehouseId));
    }

    /** 同步库存：accountId 为空时同步全部启用账号 */
    @PostMapping("/sync")
    @PreAuthorize("@ss.hasPerm('ecom:owhStock:sync')")
    @OperLog(title = "海外仓库存同步", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<List<SyncResult>> sync(@RequestParam(required = false) Long accountId) {
        return R.ok(accountId == null ? stockService.syncAll() : List.of(stockService.syncQuietly(accountId)));
    }

    /** 按海外仓数量调整 ERP 库存（生成其他入库 / 其他出库） */
    @PostMapping("/adjust")
    @PreAuthorize("@ss.hasPerm('ecom:owhStock:adjust')")
    @OperLog(title = "海外仓库存调整", type = OperType.OTHER) // i18n-ignore 操作日志标题存库
    public R<AdjustResult> adjust(@Validated @RequestBody AdjustRequest req) {
        return R.ok(stockService.adjust(req));
    }
}
