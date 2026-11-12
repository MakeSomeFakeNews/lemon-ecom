package com.lemon.erp.modules.ecom.publish.controller;

import com.lemon.erp.common.enums.EnableStatus;
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
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.publish.dto.PublishRequests;
import com.lemon.erp.modules.ecom.publish.dto.PublishViews;
import com.lemon.erp.modules.ecom.publish.entity.EcomPriceTemplate;
import com.lemon.erp.modules.ecom.publish.service.EcomPriceTemplateService;

import java.util.List;

/**
 * 价格模板 /ecom/price-template：维护与定价试算
 */
@RestController
@RequestMapping("/ecom/price-template")
@RequiredArgsConstructor
public class EcomPriceTemplateController {

    private static final String TITLE = "价格模板"; // i18n-ignore 操作日志标题存库
    private static final String READ = "@ss.hasAnyPerm('ecom:priceTemplate:list', 'ecom:publish:list', 'ecom:repricer:list', 'ecom:priceChange:list')";

    private final EcomPriceTemplateService templateService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:priceTemplate:list')")
    public R<PageResult<EcomPriceTemplate>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String keyword, @RequestParam(required = false) EcomPlatform platform,
                                                 @RequestParam(required = false) EnableStatus status) {
        return R.ok(templateService.page(current, size, keyword, platform, status));
    }

    @GetMapping("/options")
    @PreAuthorize(READ)
    public R<List<EcomPriceTemplate>> options(@RequestParam(required = false) EcomPlatform platform) {
        return R.ok(templateService.options(platform));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ)
    public R<EcomPriceTemplate> get(@PathVariable Long id) {
        return R.ok(templateService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:priceTemplate:add')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody EcomPriceTemplate t) {
        return R.ok(templateService.create(t));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:priceTemplate:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody EcomPriceTemplate t) {
        templateService.update(id, t);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:priceTemplate:remove')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        templateService.delete(ids);
        return R.ok();
    }

    /** 定价试算：建议价、预估利润与利润率 */
    @PostMapping("/quote")
    @PreAuthorize(READ)
    public R<PublishViews.PriceQuote> quote(@Valid @RequestBody PublishRequests.Quote req) {
        return R.ok(templateService.quote(req));
    }
}
