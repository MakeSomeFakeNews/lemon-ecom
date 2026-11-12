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
import com.lemon.erp.modules.ecom.publish.entity.EcomPublishTemplate;
import com.lemon.erp.modules.ecom.publish.service.EcomPublishTemplateService;

import java.util.List;

/**
 * 刊登模板 /ecom/publish-template
 */
@RestController
@RequestMapping("/ecom/publish-template")
@RequiredArgsConstructor
public class EcomPublishTemplateController {

    private static final String TITLE = "刊登模板"; // i18n-ignore 操作日志标题存库

    private final EcomPublishTemplateService templateService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:publishTemplate:list')")
    public R<PageResult<EcomPublishTemplate>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String keyword, @RequestParam(required = false) EcomPlatform platform,
                                                   @RequestParam(required = false) EnableStatus status) {
        return R.ok(templateService.page(current, size, keyword, platform, status));
    }

    @GetMapping("/options")
    @PreAuthorize("@ss.hasAnyPerm('ecom:publish:list', 'ecom:publishTemplate:list')")
    public R<List<EcomPublishTemplate>> options(@RequestParam(required = false) EcomPlatform platform) {
        return R.ok(templateService.options(platform));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasAnyPerm('ecom:publish:list', 'ecom:publishTemplate:list')")
    public R<EcomPublishTemplate> get(@PathVariable Long id) {
        return R.ok(templateService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:publishTemplate:add')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody EcomPublishTemplate t) {
        return R.ok(templateService.create(t));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:publishTemplate:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody EcomPublishTemplate t) {
        templateService.update(id, t);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:publishTemplate:remove')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        templateService.delete(ids);
        return R.ok();
    }
}
