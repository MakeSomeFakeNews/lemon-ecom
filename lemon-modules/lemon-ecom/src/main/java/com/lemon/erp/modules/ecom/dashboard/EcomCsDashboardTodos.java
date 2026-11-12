package com.lemon.erp.modules.ecom.dashboard;

import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.ecom.cs.dto.ReviewQuery;
import com.lemon.erp.modules.ecom.cs.dto.ThreadQuery;
import com.lemon.erp.modules.ecom.cs.model.CsReviewStatus;
import com.lemon.erp.modules.ecom.cs.service.EcomCsThreadService;
import com.lemon.erp.modules.ecom.cs.service.EcomReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 看板待办：差评待处理（低分评价处理状态为待处理）、买家消息超时（过了平台回复时限还没回复）
 */
@Component
@RequiredArgsConstructor
public class EcomCsDashboardTodos implements EcomDashboardTodoProvider {

    private final EcomCsThreadService threadService;
    private final EcomReviewService reviewService;
    private final PermissionService permissionService;

    @Override
    public List<EcomDashboardTodo> todos() {
        List<EcomDashboardTodo> out = new ArrayList<>();
        if (permissionService.hasPerm("ecom:csMessage:list")) {
            Long overdue = threadService.counts(new ThreadQuery()).get("OVERDUE");
            EcomCoreDashboardTodos.add(out, "cs.overdue", "ecom.text.fix.todo.cs.overdue", overdue == null ? 0 : overdue, "ecom.text.fix.unit.thread",
                    "/ecom/cs-message", Map.of("tab", "OVERDUE"),
                    EcomDashboardTodo.DANGER, "MessageOutlined", 60);
        }
        if (permissionService.hasPerm("ecom:csReview:list")) {
            long pending = reviewService.summary(new ReviewQuery()).pendingCount();
            EcomCoreDashboardTodos.add(out, "cs.badReview", "ecom.text.fix.todo.cs.badReview", pending, "ecom.text.fix.unit.review",
                    "/ecom/cs-review",
                    Map.of("tab", "review", "handleStatus", CsReviewStatus.PENDING.name()), EcomDashboardTodo.DANGER, "DislikeOutlined", 70);
        }
        return out;
    }
}
