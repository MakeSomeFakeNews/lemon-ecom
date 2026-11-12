package com.lemon.erp.modules.ecom.automation.service;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.todo.TodoProvider;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoLogMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoResult;
import com.lemon.erp.modules.ecom.automation.model.AutoTodoStatus;
import com.lemon.erp.modules.ecom.dashboard.EcomDashboardTodo;
import com.lemon.erp.modules.ecom.dashboard.EcomDashboardTodoProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 待办汇总：首页「我的待办」与电商看板「待处理事项」显示自己未完成的自动化待办；电商看板另显示今天执行失败的规则执行（可重试）
 */
@Component
@RequiredArgsConstructor
public class AutoTodoProviders implements TodoProvider, EcomDashboardTodoProvider {

    static final String TODO_PERM = "ecom:automationTodo:list";
    static final String LOG_PERM = "ecom:automationLog:list";

    private final AutoTodoService todoService;
    private final EcomAutoLogMapper logMapper;
    private final PermissionService permissionService;

    @Override
    public List<TodoItem> todos(Long userId) {
        if (!permissionService.hasPerm(TODO_PERM)) {
            return List.of();
        }
        long open = todoService.countOpen(userId);
        return open > 0 ? List.of(new TodoItem("ecom", "ecom.text.auto.todoOpen", open, "/ecom/automation-todo", "warning")) : List.of();
    }

    @Override
    public List<EcomDashboardTodo> todos() {
        List<EcomDashboardTodo> out = new ArrayList<>();
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId != null && permissionService.hasPerm(TODO_PERM)) {
            long open = todoService.countOpen(userId);
            if (open > 0) {
                out.add(new EcomDashboardTodo("auto.todo", Messages.get("ecom.text.auto.todoOpen"), open, Messages.get("ecom.text.auto.unitItem"),
                        "/ecom/automation-todo", Map.of("status", AutoTodoStatus.OPEN.name()), EcomDashboardTodo.WARNING, "BellOutlined", 90));
            }
        }
        if (permissionService.hasPerm(LOG_PERM)) {
            long failed = 0;
            for (Map<String, Object> row : logMapper.countByResult(LocalDate.now().atStartOfDay(), null)) {
                AutoResult r = AutoResult.valueOf(String.valueOf(row.get("result")));
                if (r == AutoResult.FAILED || r == AutoResult.PARTIAL) {
                    failed += ((Number) row.get("cnt")).longValue();
                }
            }
            if (failed > 0) {
                out.add(new EcomDashboardTodo("auto.failed", Messages.get("ecom.text.auto.todoFailed"), failed, Messages.get("ecom.text.auto.unitTimes"),
                        "/ecom/automation-log", Map.of("result", AutoResult.FAILED.name()), EcomDashboardTodo.DANGER, "ThunderboltOutlined", 91));
            }
        }
        return out;
    }
}
