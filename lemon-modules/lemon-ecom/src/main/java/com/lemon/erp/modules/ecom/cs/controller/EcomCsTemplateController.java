package com.lemon.erp.modules.ecom.cs.controller;

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
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.modules.ecom.cs.dto.CsViews;
import com.lemon.erp.modules.ecom.cs.entity.EcomReplyTemplate;
import com.lemon.erp.modules.ecom.cs.service.EcomReplyTemplateService;
import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.util.List;

/**
 * 快捷回复模板 /ecom/cs/template
 */
@RestController
@RequestMapping("/ecom/cs/template")
@RequiredArgsConstructor
public class EcomCsTemplateController {

    private final EcomReplyTemplateService templateService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:csTemplate:list')")
    public R<PageResult<EcomReplyTemplate>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String language, @RequestParam(required = false) EcomPlatform platform,
                                                 @RequestParam(required = false) String keyword, @RequestParam(required = false) EnableStatus status) {
        return R.ok(templateService.page(current, size, language, platform, keyword, status));
    }

    /** 回复时选择的模板（启用、适用于平台），回复消息或评价的人可用 */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:reply') or @ss.hasPerm('ecom:csReview:reply')")
    public R<List<EcomReplyTemplate>> options(@RequestParam(required = false) EcomPlatform platform) {
        return R.ok(templateService.options(platform));
    }

    /** 支持的变量名 */
    @GetMapping("/variables")
    @PreAuthorize("@ss.hasPerm('ecom:csTemplate:list')")
    public R<List<String>> variables() {
        return R.ok(EcomReplyTemplateService.VARIABLES);
    }

    /** 按会话 / 评价 / 订单替换变量 */
    @GetMapping("/{id}/render")
    @PreAuthorize("@ss.hasPerm('ecom:csMessage:reply') or @ss.hasPerm('ecom:csReview:reply')")
    public R<CsViews.Rendered> render(@PathVariable Long id, @RequestParam(required = false) Long threadId,
                                      @RequestParam(required = false) Long reviewId, @RequestParam(required = false) Long orderId) {
        return R.ok(templateService.render(id, threadId, reviewId, orderId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:csTemplate:list')")
    public R<EcomReplyTemplate> get(@PathVariable Long id) {
        return R.ok(templateService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:csTemplate:add')")
    @OperLog(title = "回复模板", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Valid @RequestBody EcomReplyTemplate t) {
        return R.ok(templateService.create(t));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:csTemplate:edit')")
    @OperLog(title = "回复模板", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody EcomReplyTemplate t) {
        templateService.update(id, t);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:csTemplate:remove')")
    @OperLog(title = "回复模板", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        templateService.delete(ids);
        return R.ok();
    }
}
