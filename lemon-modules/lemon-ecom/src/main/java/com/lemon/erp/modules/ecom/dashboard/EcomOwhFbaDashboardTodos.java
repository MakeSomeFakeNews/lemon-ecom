package com.lemon.erp.modules.ecom.dashboard;

import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.model.FbaClaimStatus;
import com.lemon.erp.modules.ecom.fba.service.FbaClaimService;
import com.lemon.erp.modules.ecom.owh.model.OwhOutboundStatus;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhOutboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 看板待办：待推送海外仓的包裹、海外仓推送失败的出库单（海外仓出库页面）、待提交的 FBA 索赔与 7 天内到期的索赔（FBA 索赔页面）
 */
@Component
@RequiredArgsConstructor
public class EcomOwhFbaDashboardTodos implements EcomDashboardTodoProvider {

    private final EcomOwhOutboundService owhOutboundService;
    private final FbaClaimService claimService;
    private final PermissionService permissionService;

    @Override
    public List<EcomDashboardTodo> todos() {
        List<EcomDashboardTodo> out = new ArrayList<>();
        if (permissionService.hasPerm("ecom:owhOutbound:list")) {
            Map<String, Long> counts = owhOutboundService.statusCounts(null, null, null, null);
            EcomCoreDashboardTodos.add(out, "owh.pending", "ecom.text.fix.todo.owh.pending", nz(counts.get(EcomOwhOutboundService.PENDING)),
                    "ecom.text.fix.unit.package", "/ecom/owh-outbound", Map.of(), EcomDashboardTodo.WARNING, "CloudUploadOutlined", 110);
            EcomCoreDashboardTodos.add(out, "owh.failed", "ecom.text.fix.todo.owh.failed", nz(counts.get(OwhOutboundStatus.FAILED.name())),
                    "ecom.text.fix.unit.order", "/ecom/owh-outbound", Map.of(), EcomDashboardTodo.DANGER, "WarningOutlined", 111);
        }
        if (permissionService.hasPerm("ecom:fbaClaim:list")) {
            FbaViews.ClaimSummary s = claimService.summary(null);
            long pending = 0;
            for (Map<String, Object> row : s.byStatus()) {
                if (FbaClaimStatus.PENDING.name().equals(String.valueOf(row.get("status"))) && row.get("cnt") instanceof Number n) {
                    pending = n.longValue();
                }
            }
            EcomCoreDashboardTodos.add(out, "fba.claimPending", "ecom.text.fix.todo.fba.claimPending", pending, "ecom.text.fix.unit.claim",
                    "/ecom/fba-claim", Map.of(), EcomDashboardTodo.WARNING, "FileExclamationOutlined", 120);
            EcomCoreDashboardTodos.add(out, "fba.claimExpiring", "ecom.text.fix.todo.fba.claimExpiring", s.expiringSoon(), "ecom.text.fix.unit.claim",
                    "/ecom/fba-claim", Map.of(), EcomDashboardTodo.DANGER, "FieldTimeOutlined", 121);
        }
        return out;
    }

    private static long nz(Long v) {
        return v == null ? 0 : v;
    }
}
