package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.todo.TodoProvider;
import com.lemon.erp.modules.ecom.entity.EcomInbound;
import com.lemon.erp.modules.ecom.entity.EcomReturn;
import com.lemon.erp.modules.ecom.mapper.EcomInboundMapper;
import com.lemon.erp.modules.ecom.mapper.EcomReportMapper;
import com.lemon.erp.modules.ecom.mapper.EcomReturnMapper;
import com.lemon.erp.modules.ecom.model.EcomReturnStatus;
import com.lemon.erp.modules.ecom.model.InboundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 首页待办：待处理 / 异常 / 逾期未发货的平台订单、在途货件、待处理退货退款（按权限显示）
 */
@Component
@RequiredArgsConstructor
public class EcomTodoProvider implements TodoProvider {

    private static final String ORDER_PERM = "ecom:order:list";
    private static final String INBOUND_PERM = "ecom:inbound:list";
    private static final String RETURN_PERM = "ecom:return:list";

    private final EcomReportMapper reportMapper;
    private final EcomInboundMapper inboundMapper;
    private final EcomReturnMapper returnMapper;
    private final PermissionService permissionService;

    @Override
    public List<TodoItem> todos(Long userId) {
        List<TodoItem> items = new ArrayList<>();
        if (permissionService.hasPerm(ORDER_PERM)) {
            Map<String, Object> counts = reportMapper.selectOrderCounts(LocalDateTime.now());
            add(items, counts, "pending", "ecom.text.todoOrderPending", "/ecom/order?localStatus=PENDING", "info");
            add(items, counts, "exception", "ecom.text.todoOrderException", "/ecom/order?localStatus=EXCEPTION", "danger");
            add(items, counts, "overdue", "ecom.text.todoOrderOverdue", "/ecom/order?overdue=true", "danger");
        }
        if (permissionService.hasPerm(INBOUND_PERM)) {
            long transit = inboundMapper.selectCount(Wrappers.<EcomInbound>lambdaQuery().eq(EcomInbound::getStatus, InboundStatus.SHIPPED));
            if (transit > 0) {
                items.add(new TodoItem("ecom", "ecom.text.todoInboundTransit", transit, "/ecom/inbound?status=SHIPPED", "info"));
            }
        }
        if (permissionService.hasPerm(RETURN_PERM)) {
            long pending = returnMapper.selectCount(Wrappers.<EcomReturn>lambdaQuery().eq(EcomReturn::getStatus, EcomReturnStatus.PENDING));
            if (pending > 0) {
                items.add(new TodoItem("ecom", "ecom.text.todoReturnPending", pending, "/ecom/return?status=PENDING", "warning"));
            }
        }
        return items;
    }

    private static void add(List<TodoItem> items, Map<String, Object> counts, String key, String title, String link, String level) {
        Object v = counts == null ? null : counts.get(key);
        long count = v == null ? 0 : ((Number) v).longValue();
        if (count > 0) {
            items.add(new TodoItem("ecom", title, count, link, level));
        }
    }
}
