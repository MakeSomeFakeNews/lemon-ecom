package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.excel.ImportExecutor;
import com.lemon.erp.framework.excel.ImportRequest;
import com.lemon.erp.framework.excel.ImportResult;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.IdsRequest;
import com.lemon.erp.modules.ecom.dto.ListingPairsRequest;
import com.lemon.erp.modules.ecom.dto.MaterialPairing;
import com.lemon.erp.modules.ecom.dto.PairingImportRow;
import com.lemon.erp.modules.ecom.dto.UnpairedListing;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.service.EcomListingService;
import com.lemon.erp.modules.ecom.service.EcomPairingService;
import jakarta.validation.Valid;
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
import java.util.Map;

/**
 * SKU 配对中心 /ecom/pairing：以物料为中心的平台 SKU、未配对的平台 SKU 与推荐、批量确认 / 导入 / 解除配对、重新匹配异常订单
 */
@RestController
@RequestMapping("/ecom/pairing")
@RequiredArgsConstructor
public class EcomPairingController {

    private final EcomPairingService pairingService;
    private final EcomListingService listingService;
    private final ImportExecutor importExecutor;

    /** 物料及其在线商品数、店铺数（pairedOnly 只看有配对的） */
    @GetMapping("/materials")
    @PreAuthorize("@ss.hasPerm('ecom:pairing:list')")
    public R<PageResult<MaterialPairing>> materials(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword, @RequestParam(required = false) Boolean pairedOnly) {
        return R.ok(pairingService.materials(current, size, keyword, pairedOnly));
    }

    /** 物料在各店铺、各平台的在线商品（单品映射与组合商品） */
    @GetMapping("/materials/{materialId}/listings")
    @PreAuthorize("@ss.hasPerm('ecom:pairing:list')")
    public R<List<EcomListing>> listingsOfMaterial(@PathVariable Long materialId) {
        return R.ok(pairingService.listingsOfMaterial(materialId));
    }

    /** 未配对的平台 SKU 与推荐物料 */
    @GetMapping("/unpaired")
    @PreAuthorize("@ss.hasPerm('ecom:pairing:list')")
    public R<PageResult<UnpairedListing>> unpaired(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) Long storeId, @RequestParam(required = false) String keyword) {
        return R.ok(pairingService.unpaired(current, size, storeId, keyword));
    }

    /** 确认配对（一键确认推荐、手工选择） */
    @PostMapping("/confirm")
    @PreAuthorize("@ss.hasPerm('ecom:pairing:edit')")
    @OperLog(title = "SKU 配对", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Integer> confirm(@Validated @RequestBody ListingPairsRequest req) {
        return R.ok(listingService.mapAll(req.pairs()));
    }

    /** 解除配对 */
    @PostMapping("/unpair")
    @PreAuthorize("@ss.hasPerm('ecom:pairing:edit')")
    @OperLog(title = "SKU 配对", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> unpair(@Validated @RequestBody IdsRequest req) {
        listingService.unpair(req.ids());
        return R.ok();
    }

    /** 批量导入配对：店铺编码、平台 SKU、物料编码、数量；任何一行出错整批不导入 */
    @PostMapping("/import")
    @PreAuthorize("@ss.hasPerm('ecom:pairing:import')")
    @OperLog(title = "SKU 配对", type = OperType.IMPORT) // i18n-ignore 操作日志标题存库
    public R<ImportResult> importRows(@Valid @RequestBody ImportRequest<PairingImportRow> req) {
        return R.ok(importExecutor.run(req, pairingService::importRow));
    }

    /** 重新匹配异常订单（配对变化后），可按店铺：{ matched, remaining } */
    @PostMapping("/rematch")
    @PreAuthorize("@ss.hasPerm('ecom:order:outbound')")
    @OperLog(title = "SKU 配对", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Map<String, Integer>> rematch(@RequestParam(required = false) Long storeId) {
        return R.ok(pairingService.rematchExceptions(storeId));
    }
}
