package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.PlatformStockImportRow;
import com.lemon.erp.modules.ecom.entity.EcomPlatformStock;
import com.lemon.erp.modules.ecom.service.EcomPlatformStockService;
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
 * 平台仓库存 /ecom/platform-stock
 */
@RestController
@RequestMapping("/ecom/platform-stock")
@RequiredArgsConstructor
public class EcomPlatformStockController {

    private final EcomPlatformStockService stockService;
    private final ImportExecutor importExecutor;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:platformStock:list')")
    public R<PageResult<EcomPlatformStock>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Boolean diffOnly) {
        return R.ok(stockService.page(current, size, storeId, keyword, diffOnly));
    }

    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('ecom:platformStock:import')")
    @OperLog(title = "平台仓库存", type = OperType.IMPORT) // i18n-ignore 操作日志标题存库
    public R<ImportResult> importRows(@Valid @RequestBody ImportRequest<PlatformStockImportRow> req) {
        return R.ok(importExecutor.run(req, stockService::importRow));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:platformStock:import')")
    @OperLog(title = "平台仓库存", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        stockService.delete(ids);
        return R.ok();
    }
}
