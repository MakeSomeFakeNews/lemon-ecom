package com.lemon.erp.modules.ecom.publish.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
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
import com.lemon.erp.modules.ecom.publish.dto.PublishRequests;
import com.lemon.erp.modules.ecom.publish.dto.PublishViews;
import com.lemon.erp.modules.ecom.publish.entity.EcomCompetitorPrice;
import com.lemon.erp.modules.ecom.publish.model.CompetitorSource;
import com.lemon.erp.modules.ecom.publish.service.EcomCompetitorPriceService;

import java.util.List;

/**
 * 竞品价格 /ecom/competitor-price：快照列表（可只看每个商品最近一条）、手工录入、Excel 导入、经平台接口拉取（亚马逊）、删除
 */
@RestController
@RequestMapping("/ecom/competitor-price")
@RequiredArgsConstructor
public class EcomCompetitorPriceController {

    private static final String TITLE = "竞品价格"; // i18n-ignore 操作日志标题存库

    private final EcomCompetitorPriceService priceService;
    private final ImportExecutor importExecutor;

    @GetMapping
    @PreAuthorize("@ss.hasAnyPerm('ecom:competitor:list', 'ecom:repricer:list')")
    public R<PageResult<EcomCompetitorPrice>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) Long storeId, @RequestParam(required = false) Long listingId,
                                                   @RequestParam(required = false) String keyword, @RequestParam(required = false) CompetitorSource source,
                                                   @RequestParam(defaultValue = "false") boolean latestOnly) {
        return R.ok(priceService.page(current, size, storeId, listingId, keyword, source, latestOnly));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:competitor:add')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody EcomCompetitorPrice p) {
        return R.ok(priceService.create(p));
    }

    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('ecom:competitor:add')")
    @OperLog(title = TITLE, type = OperType.IMPORT)
    public R<ImportResult> importRows(@Valid @RequestBody ImportRequest<PublishRequests.CompetitorImportRow> req) {
        return R.ok(importExecutor.run(req, priceService::importRow));
    }

    /** 经平台接口拉取（亚马逊 Product Pricing API） */
    @PostMapping("/fetch")
    @PreAuthorize("@ss.hasPerm('ecom:competitor:add')")
    @OperLog(title = TITLE, type = OperType.OTHER)
    public R<PublishViews.FetchResult> fetch(@Valid @RequestBody PublishRequests.FetchCompetitors req) {
        return R.ok(priceService.fetch(req));
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:competitor:remove')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        priceService.delete(ids);
        return R.ok();
    }
}
