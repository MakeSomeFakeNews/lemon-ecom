package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.entity.EcomOrderRule;
import com.lemon.erp.modules.ecom.service.EcomOrderRuleService;
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
 * 审单规则 /ecom/order-rule
 */
@RestController
@RequestMapping("/ecom/order-rule")
@RequiredArgsConstructor
public class EcomOrderRuleController {

    private final EcomOrderRuleService ruleService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:orderRule:list')")
    public R<PageResult<EcomOrderRule>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword, @RequestParam(required = false) EnableStatus status) {
        return R.ok(ruleService.page(current, size, keyword, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:orderRule:list')")
    public R<EcomOrderRule> get(@PathVariable Long id) {
        return R.ok(ruleService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:orderRule:add')")
    @OperLog(title = "审单规则", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Validated @RequestBody EcomOrderRule rule) {
        return R.ok(ruleService.create(rule));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:orderRule:edit')")
    @OperLog(title = "审单规则", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Validated @RequestBody EcomOrderRule rule) {
        ruleService.update(id, rule);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('ecom:orderRule:edit')")
    @OperLog(title = "审单规则", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam EnableStatus status) {
        ruleService.changeStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:orderRule:remove')")
    @OperLog(title = "审单规则", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        ruleService.delete(ids);
        return R.ok();
    }
}
