package com.lemon.erp.modules.ecom.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.dto.IdsRequest;
import com.lemon.erp.modules.ecom.entity.EcomAdRule;
import com.lemon.erp.modules.ecom.entity.EcomAdRuleLog;
import com.lemon.erp.modules.ecom.model.EcomAdLogSource;
import com.lemon.erp.modules.ecom.model.EcomAdLogStatus;
import com.lemon.erp.modules.ecom.service.EcomAdRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/**
 * 广告规则 /ecom/ad-rule：维护、预览、立即执行，执行日志与建议的处理
 */
@RestController
@RequestMapping("/ecom/ad-rule")
@RequiredArgsConstructor
public class EcomAdRuleController {

    private static final String TITLE = "广告规则"; // i18n-ignore 操作日志标题存库

    private final EcomAdRuleService ruleService;

    @GetMapping
    @PreAuthorize("@ss.hasPerm('ecom:adRule:list')")
    public R<PageResult<EcomAdRule>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword, @RequestParam(required = false) EnableStatus status) {
        return R.ok(ruleService.page(current, size, keyword, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:adRule:list')")
    public R<EcomAdRule> get(@PathVariable Long id) {
        return R.ok(ruleService.get(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPerm('ecom:adRule:add')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody EcomAdRule r) {
        return R.ok(ruleService.create(r));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:adRule:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody EcomAdRule r) {
        ruleService.update(id, r);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPerm('ecom:adRule:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam EnableStatus status) {
        ruleService.changeStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:adRule:remove')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        ruleService.delete(ids);
        return R.ok();
    }

    /** 预览：按当前数据计算建议，不保存、不执行 */
    @GetMapping("/{id}/preview")
    @PreAuthorize("@ss.hasPerm('ecom:adRule:list')")
    public R<List<AdsViews.Suggestion>> preview(@PathVariable Long id) {
        return R.ok(ruleService.preview(id));
    }

    /** 立即执行：写执行日志；规则设置了自动执行时经广告接口修改 */
    @PostMapping("/{id}/run")
    @PreAuthorize("@ss.hasPerm('ecom:adRule:execute')")
    @OperLog(title = TITLE, type = OperType.OTHER)
    public R<AdsViews.RunResult> run(@PathVariable Long id) {
        return R.ok(ruleService.run(id));
    }

    @GetMapping("/logs")
    @PreAuthorize("@ss.hasPerm('ecom:adRule:list')")
    public R<PageResult<EcomAdRuleLog>> logs(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) Long ruleId, @RequestParam(required = false) Long storeId,
                                             @RequestParam(required = false) EcomAdLogStatus status, @RequestParam(required = false) EcomAdLogSource source,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beginDate,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                             @RequestParam(required = false) String keyword) {
        return R.ok(ruleService.logs(current, size, ruleId, storeId, status, source, beginDate, endDate, keyword));
    }

    /** 执行待处理的建议（经广告接口） */
    @PostMapping("/logs/apply")
    @PreAuthorize("@ss.hasPerm('ecom:adRule:execute')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<AdsViews.RunResult> apply(@Valid @RequestBody IdsRequest req) {
        return R.ok(ruleService.applyLogs(req.ids()));
    }

    /** 标记已手工执行（已在平台后台修改） */
    @PostMapping("/logs/manual")
    @PreAuthorize("@ss.hasPerm('ecom:adRule:execute')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Integer> manual(@Valid @RequestBody IdsRequest req) {
        return R.ok(ruleService.markManual(req.ids()));
    }

    @PostMapping("/logs/ignore")
    @PreAuthorize("@ss.hasPerm('ecom:adRule:execute')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Integer> ignore(@Valid @RequestBody IdsRequest req) {
        return R.ok(ruleService.ignore(req.ids()));
    }
}
