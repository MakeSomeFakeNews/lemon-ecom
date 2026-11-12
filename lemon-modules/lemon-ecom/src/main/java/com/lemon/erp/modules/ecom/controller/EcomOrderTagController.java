package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.entity.EcomOrderTag;
import com.lemon.erp.modules.ecom.service.EcomOrderTagService;
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

/**
 * 订单标签 /ecom/order-tag
 */
@RestController
@RequestMapping("/ecom/order-tag")
@RequiredArgsConstructor
public class EcomOrderTagController {

    private final EcomOrderTagService tagService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:orderTag:list')")
    public R<List<EcomOrderTag>> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) EnableStatus status) {
        return R.ok(tagService.list(keyword, status));
    }

    /** 启用的标签（订单页筛选、打标、审单规则选择） */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasAnyPerm('ecom:order:list', 'ecom:orderTag:list', 'ecom:orderRule:list')")
    public R<List<EcomOrderTag>> options() {
        return R.ok(tagService.options());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:orderTag:list')")
    public R<EcomOrderTag> get(@PathVariable Long id) {
        return R.ok(tagService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:orderTag:add')")
    @OperLog(title = "订单标签", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated @RequestBody EcomOrderTag t) {
        return R.ok(tagService.create(t));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:orderTag:edit')")
    @OperLog(title = "订单标签", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody EcomOrderTag t) {
        tagService.update(id, t);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:orderTag:remove')")
    @OperLog(title = "订单标签", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        tagService.delete(ids);
        return R.ok();
    }
}
