package com.lemon.erp.modules.ecom.assistant.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantRequests;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantViews;
import com.lemon.erp.modules.ecom.assistant.entity.EcomBannedWord;
import com.lemon.erp.modules.ecom.assistant.model.BannedWordLevel;
import com.lemon.erp.modules.ecom.assistant.service.EcomBannedWordService;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
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

import java.util.List;

/**
 * 违禁词库 /ecom/banned-word：维护、批量添加、启用停用；刊登编辑页取适用的词（实时检查）、合规检查
 */
@RestController
@RequestMapping("/ecom/banned-word")
@RequiredArgsConstructor
public class EcomBannedWordController {

    private final EcomBannedWordService wordService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:bannedWord:list')")
    public R<PageResult<EcomBannedWord>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
                                              @RequestParam(required = false) String keyword, @RequestParam(required = false) String wordType,
                                              @RequestParam(required = false) BannedWordLevel level, @RequestParam(required = false) EcomPlatform platform,
                                              @RequestParam(required = false) String marketplace, @RequestParam(required = false) EnableStatus status) {
        return R.ok(wordService.page(current, size, keyword, wordType, level, platform, marketplace, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:bannedWord:list')")
    public R<EcomBannedWord> get(@PathVariable Long id) {
        return R.ok(wordService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:bannedWord:add')")
    @OperLog(title = "违禁词库", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Long> create(@Valid @RequestBody AssistantRequests.WordSave req) {
        return R.ok(wordService.create(req));
    }

    /** 批量添加，返回新增条数 */
    @PostMapping("/batch")
    @PreAuthorize("@ss.hasPerm('ecom:bannedWord:add')")
    @OperLog(title = "违禁词库", type = OperType.INSERT) // i18n-ignore 操作日志标题存库
    public R<Integer> batch(@Valid @RequestBody AssistantRequests.WordBatch req) {
        return R.ok(wordService.batchAdd(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:bannedWord:edit')")
    @OperLog(title = "违禁词库", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AssistantRequests.WordSave req) {
        wordService.update(id, req);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('ecom:bannedWord:edit')")
    @OperLog(title = "违禁词库", type = OperType.UPDATE) // i18n-ignore 操作日志标题存库
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam EnableStatus status) {
        wordService.changeStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:bannedWord:remove')")
    @OperLog(title = "违禁词库", type = OperType.DELETE) // i18n-ignore 操作日志标题存库
    public R<Void> delete(@PathVariable List<Long> ids) {
        wordService.delete(ids);
        return R.ok();
    }

    /** 适用于平台 / 站点的启用词（刊登编辑页实时检查） */
    @GetMapping("/applicable")
    @PreAuthorize("@ss.hasAnyPerm('ecom:publish:list', 'ecom:bannedWord:list')")
    public R<List<AssistantViews.ApplicableWord>> applicable(@RequestParam(required = false) EcomPlatform platform,
                                                             @RequestParam(required = false) String marketplace) {
        return R.ok(wordService.applicableWords(platform, marketplace));
    }

    /** 合规检查：标题、五点、描述 */
    @PostMapping("/check")
    @PreAuthorize("@ss.hasAnyPerm('ecom:publish:list', 'ecom:bannedWord:list')")
    public R<AssistantViews.CheckResult> check(@Valid @RequestBody AssistantRequests.CheckRequest req) {
        return R.ok(wordService.check(req.platform(), req.marketplace(), req.title(), req.bullets(), req.description()));
    }
}
