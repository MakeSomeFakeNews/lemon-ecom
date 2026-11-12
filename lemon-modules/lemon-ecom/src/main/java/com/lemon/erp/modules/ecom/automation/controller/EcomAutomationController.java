package com.lemon.erp.modules.ecom.automation.controller;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.log.OperLog;
import com.lemon.erp.framework.log.OperType;
import com.lemon.erp.modules.ecom.automation.dto.AutoLogQuery;
import com.lemon.erp.modules.ecom.automation.dto.AutoRequests;
import com.lemon.erp.modules.ecom.automation.dto.AutoViews;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoLog;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoTodo;
import com.lemon.erp.modules.ecom.automation.model.AutoTodoStatus;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.automation.service.AutoCatalog;
import com.lemon.erp.modules.ecom.automation.service.AutoLogService;
import com.lemon.erp.modules.ecom.automation.service.AutoRuleService;
import com.lemon.erp.modules.ecom.automation.service.AutoTemplates;
import com.lemon.erp.modules.ecom.automation.service.AutoTodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 自动化规则中心 /ecom/automation：目录与模板、规则维护 / 试运行 / 立即执行、执行日志与重试、待办提醒
 */
@RestController
@RequestMapping("/ecom/automation")
@RequiredArgsConstructor
public class EcomAutomationController {

    private static final String TITLE = "自动化规则"; // i18n-ignore 操作日志标题存库
    private static final String LOG_TITLE = "自动化执行日志"; // i18n-ignore 操作日志标题存库

    private final AutoCatalog catalog;
    private final AutoRuleService ruleService;
    private final AutoLogService logService;
    private final AutoTodoService todoService;

    // ---------------------------------------------------------------- 目录

    /** 触发器（含字段）、动作、运算符、公共变量 */
    @GetMapping("/catalog")
    @PreAuthorize("@ss.hasPerm('ecom:automation:list')")
    public R<AutoViews.Catalog> catalog() {
        return R.ok(catalog.view());
    }

    @GetMapping("/templates")
    @PreAuthorize("@ss.hasPerm('ecom:automation:list')")
    public R<List<AutoViews.TemplateView>> templates() {
        return R.ok(AutoTemplates.views());
    }

    /** 通知接收人可选的角色 */
    @GetMapping("/roles")
    @PreAuthorize("@ss.hasPerm('ecom:automation:list')")
    public R<List<AutoViews.RoleOption>> roles() {
        return R.ok(ruleService.roles());
    }

    // ---------------------------------------------------------------- 规则

    @GetMapping("/rule")
    @PreAuthorize("@ss.hasPerm('ecom:automation:list')")
    public R<PageResult<EcomAutoRule>> page(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword, @RequestParam(required = false) AutoTrigger trigger,
                                            @RequestParam(required = false) EnableStatus status) {
        return R.ok(ruleService.page(current, size, keyword, trigger, status));
    }

    @GetMapping("/rule/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:automation:list')")
    public R<EcomAutoRule> get(@PathVariable Long id) {
        return R.ok(ruleService.get(id));
    }

    @PostMapping("/rule")
    @PreAuthorize("@ss.hasPerm('ecom:automation:add')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> create(@Valid @RequestBody EcomAutoRule r) {
        return R.ok(ruleService.create(r));
    }

    @PutMapping("/rule/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:automation:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody EcomAutoRule r) {
        ruleService.update(id, r);
        return R.ok();
    }

    @PutMapping("/rule/{id}/status")
    @PreAuthorize("@ss.hasPerm('ecom:automation:edit')")
    @OperLog(title = TITLE, type = OperType.UPDATE)
    public R<Void> changeStatus(@PathVariable Long id, @RequestParam EnableStatus status) {
        ruleService.changeStatus(id, status);
        return R.ok();
    }

    @DeleteMapping("/rule/{ids}")
    @PreAuthorize("@ss.hasPerm('ecom:automation:remove')")
    @OperLog(title = TITLE, type = OperType.DELETE)
    public R<Void> delete(@PathVariable List<Long> ids) {
        ruleService.delete(ids);
        return R.ok();
    }

    @PostMapping("/rule/{id}/copy")
    @PreAuthorize("@ss.hasPerm('ecom:automation:add')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> copy(@PathVariable Long id) {
        return R.ok(ruleService.copy(id));
    }

    @PostMapping("/rule/template/{code}")
    @PreAuthorize("@ss.hasPerm('ecom:automation:add')")
    @OperLog(title = TITLE, type = OperType.INSERT)
    public R<Long> fromTemplate(@PathVariable String code, @Valid @RequestBody(required = false) AutoRequests.FromTemplate req) {
        return R.ok(ruleService.createFromTemplate(code, req == null ? null : req.storeIds()));
    }

    /** 试运行：规则可以是未保存的编辑内容 */
    @PostMapping("/rule/test")
    @PreAuthorize("@ss.hasPerm('ecom:automation:test')")
    public R<AutoViews.TestResult> test(@Valid @RequestBody EcomAutoRule r) {
        return R.ok(ruleService.test(r));
    }

    /** 立即执行（扫描类触发器） */
    @PostMapping("/rule/{id}/run")
    @PreAuthorize("@ss.hasPerm('ecom:automation:edit')")
    @OperLog(title = TITLE, type = OperType.OTHER)
    public R<AutoViews.RunResult> run(@PathVariable Long id) {
        return R.ok(ruleService.runNow(id));
    }

    // ---------------------------------------------------------------- 执行日志

    @GetMapping("/log")
    @PreAuthorize("@ss.hasPerm('ecom:automationLog:list')")
    public R<PageResult<EcomAutoLog>> logs(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                           @ModelAttribute AutoLogQuery q) {
        return R.ok(logService.page(current, size, q));
    }

    @GetMapping("/log/stats")
    @PreAuthorize("@ss.hasPerm('ecom:automationLog:list')")
    public R<AutoViews.LogStats> stats(@RequestParam(defaultValue = "14") int days, @RequestParam(required = false) Long ruleId) {
        return R.ok(logService.stats(days, ruleId));
    }

    @GetMapping("/log/{id}")
    @PreAuthorize("@ss.hasPerm('ecom:automationLog:list')")
    public R<EcomAutoLog> log(@PathVariable Long id) {
        return R.ok(logService.detail(id));
    }

    @PostMapping("/log/{id}/retry")
    @PreAuthorize("@ss.hasPerm('ecom:automationLog:retry')")
    @OperLog(title = LOG_TITLE, type = OperType.OTHER)
    public R<EcomAutoLog> retry(@PathVariable Long id) {
        return R.ok(logService.retry(id));
    }

    // ---------------------------------------------------------------- 待办提醒

    @GetMapping("/todo")
    @PreAuthorize("@ss.hasPerm('ecom:automationTodo:list')")
    public R<PageResult<EcomAutoTodo>> todos(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(defaultValue = "true") boolean mine, @RequestParam(required = false) AutoTodoStatus status,
                                             @RequestParam(required = false) String keyword) {
        return R.ok(todoService.page(current, size, mine, status, keyword));
    }

    @PutMapping("/todo/status")
    @PreAuthorize("@ss.hasPerm('ecom:automationTodo:list')")
    @OperLog(title = LOG_TITLE, type = OperType.UPDATE)
    public R<Integer> todoStatus(@Valid @RequestBody AutoRequests.TodoStatus req) {
        return R.ok(todoService.changeStatus(req.ids(), req.status()));
    }
}
