package com.lemon.erp.modules.ecom.dashboard;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.modules.ecom.mapper.EcomReportMapper;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.ReplenishLevel;
import com.lemon.erp.modules.ecom.service.EcomReplenishService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 看板待办：待审核 / 异常 / 逾期未发货订单、在途货件、紧急补货 SKU（按各自页面的查看权限）
 */
@Component
@RequiredArgsConstructor
public class EcomCoreDashboardTodos implements EcomDashboardTodoProvider {

    private final EcomReportMapper reportMapper;
    private final EcomReplenishService replenishService;
    private final PermissionService permissionService;

    @Override
    public List<EcomDashboardTodo> todos() {
        List<EcomDashboardTodo> out = new ArrayList<>();
        if (permissionService.hasPerm("ecom:order:list")) {
            Map<String, Object> c = reportMapper.selectOrderCounts(LocalDateTime.now());
            add(out, "order.pending", "ecom.text.fix.todo.order.pending", num(c, "pending"), "ecom.text.fix.unit.order",
                    "/ecom/order", Map.of("localStatus", EcomLocalStatus.PENDING.name()),
                    EcomDashboardTodo.WARNING, "ClockCircleOutlined", 10);
            add(out, "order.exception", "ecom.text.fix.todo.order.exception", num(c, "exception"), "ecom.text.fix.unit.order",
                    "/ecom/order", Map.of("localStatus", EcomLocalStatus.EXCEPTION.name()),
                    EcomDashboardTodo.DANGER, "WarningOutlined", 20);
            add(out, "order.overdue", "ecom.text.fix.todo.order.overdue", num(c, "overdue"), "ecom.text.fix.unit.order",
                    "/ecom/order", Map.of("overdue", "1"), EcomDashboardTodo.DANGER,
                    "FieldTimeOutlined", 30);
        }
        if (permissionService.hasPerm("ecom:inbound:list")) {
            add(out, "inbound.transit", "ecom.text.fix.todo.inbound.transit", reportMapper.countInTransitInbounds(), "ecom.text.fix.unit.shipment",
                    "/ecom/inbound", Map.of(), EcomDashboardTodo.INFO,
                    "SendOutlined", 40);
        }
        if (permissionService.hasPerm("ecom:replenish:list")) {
            add(out, "replenish.urgent", "ecom.text.fix.todo.replenish.urgent", replenishService.countUrgent(), "ecom.text.fix.unit.sku",
                    "/ecom/replenish", Map.of("level", ReplenishLevel.URGENT.name()),
                    EcomDashboardTodo.WARNING, "AlertOutlined", 50);
        }
        return out;
    }

    /** 数量大于 0 时加入一项（标题、单位为消息键） */
    static void add(List<EcomDashboardTodo> out, String key, String titleKey, long count, String unitKey, String path, Map<String, String> query,
                    String tone, String icon, int sort) {
        if (count > 0) {
            out.add(new EcomDashboardTodo(key, Messages.get(titleKey), count, Messages.get(unitKey), path, query, tone, icon, sort));
        }
    }

    private static long num(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        return v == null ? 0 : ((Number) v).longValue();
    }
}
