package com.lemon.erp.modules.ecom.dev.controller;

import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dev.ali1688.Ali1688Models.Product;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688OfferRequest;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.ParsedOffer;
import com.lemon.erp.modules.ecom.dev.dto.Ali1688Views.SyncSummary;
import com.lemon.erp.modules.ecom.dev.dto.IdList;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688Offer;
import com.lemon.erp.modules.ecom.dev.entity.Ecom1688PriceLog;
import com.lemon.erp.modules.ecom.dev.service.Ali1688OfferService;
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

import java.util.List;
import java.util.Map;

/**
 * 1688 货源 /ecom/1688/offer：物料关联 1688 商品、链接解析、商品预览、同步价格库存、价格变动
 */
@RestController
@RequestMapping("/ecom/1688/offer")
@RequiredArgsConstructor
public class Ali1688OfferController {

    private static final String TITLE = "1688 货源"; // i18n-ignore 操作日志标题存库

    private final Ali1688OfferService offerService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Offer:list')")
    public R<PageResult<Ecom1688Offer>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword, @RequestParam(required = false) Integer priceAlert,
                                             @RequestParam(required = false) Long supplierId) {
        return R.ok(offerService.page(current, size, keyword, priceAlert, supplierId));
    }

    /** 物料的货源（物料 ID 逗号分隔），key 为物料 ID */
    @GetMapping("/by-materials")
    @PreAuthorize("@ss.hasAnyPerm('ecom:ali1688Offer:list', 'ecom:ali1688Order:list')")
    public R<Map<Long, Ecom1688Offer>> byMaterials(@RequestParam List<Long> materialIds) {
        return R.ok(offerService.byMaterials(materialIds));
    }

    /** 解析 1688 链接 / offerId */
    @GetMapping("/parse")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Offer:list')")
    public R<ParsedOffer> parse(@RequestParam String text) {
        return R.ok(offerService.parse(text));
    }

    /** 预览 1688 商品（需要已授权的账号） */
    @GetMapping("/preview")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Offer:edit')")
    public R<Product> preview(@RequestParam String offer, @RequestParam(required = false) Long accountId) {
        return R.ok(offerService.preview(offer, accountId));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Offer:edit')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> link(@Validated @RequestBody Ali1688OfferRequest req) {
        return R.ok(offerService.link(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Offer:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody Ali1688OfferRequest req) {
        offerService.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Offer:edit')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        offerService.delete(ids);
        return R.ok();
    }

    /** 同步价格与库存：ids 为空时同步全部 */
    @PostMapping("/sync")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Offer:sync')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<SyncSummary> sync(@RequestBody(required = false) IdList req) {
        return R.ok(offerService.sync(req == null ? null : req.ids()));
    }

    /** 确认价格变动 */
    @PostMapping("/ack")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Offer:edit')")
    public R<Void> acknowledge(@Validated @RequestBody IdList req) {
        offerService.acknowledge(req.ids());
        return R.ok();
    }

    @GetMapping("/{id}/price-logs")
    @PreAuthorize("@ss.hasPerm('ecom:ali1688Offer:list')")
    public R<List<Ecom1688PriceLog>> priceLogs(@PathVariable Long id) {
        return R.ok(offerService.priceLogs(id));
    }
}
