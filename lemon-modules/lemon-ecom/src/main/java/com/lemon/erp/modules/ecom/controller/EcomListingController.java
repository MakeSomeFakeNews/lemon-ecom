package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.common.validation.Create;
import com.lemon.erp.common.validation.Update;
import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import com.lemon.erp.modules.ecom.service.EcomListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 在线商品 /ecom/listing
 */
@RestController
@RequestMapping("/ecom/listing")
@RequiredArgsConstructor
public class EcomListingController {

    private final EcomListingService listingService;
    private final ImportExecutor importExecutor;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:listing:list')")
    public R<PageResult<EcomListing>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) ListingStatus status, @RequestParam(required = false) FulfillmentType fulfillment,
                                           @RequestParam(required = false) Boolean mapped, @RequestParam(required = false) Long materialId) {
        return R.ok(listingService.page(current, size, storeId, keyword, status, fulfillment, mapped, materialId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:listing:list')")
    public R<EcomListing> get(@PathVariable Long id) {
        return R.ok(listingService.detail(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:listing:add')")
    @OperLog(title = "在线商品", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated(Create.class) @RequestBody EcomListing listing) {
        return R.ok(listingService.create(listing));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:listing:edit')")
    @OperLog(title = "在线商品", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated(Update.class) @RequestBody EcomListing listing) {
        listingService.update(id, listing);
        return R.ok();
    }

    /** 只修改物料映射 */
    @PutMapping("/{id}/map")
    @PreAuthorize("@ss.hasPerm('ecom:listing:edit')")
    @OperLog(title = "在线商品", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> map(@PathVariable Long id, @RequestParam Long materialId, @RequestParam(required = false) BigDecimal packQty) {
        listingService.map(id, materialId, packQty);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:listing:remove')")
    @OperLog(title = "在线商品", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        listingService.delete(ids);
        return R.ok();
    }

    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('ecom:listing:import')")
    @OperLog(title = "在线商品", type = OperType.IMPORT) // i18n-ignore 操作日志标题存库
    public R<ImportResult> importRows(@Valid @RequestBody ImportRequest<EcomListing> req) {
        return R.ok(importExecutor.run(req, listingService::importRow, Create.class));
    }
}
