package com.lemon.erp.modules.ecom.dev.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.security.PermissionService;
import com.lemon.erp.framework.todo.TodoProvider;
import com.lemon.erp.modules.ecom.dev.entity.EcomDevProject;
import com.lemon.erp.modules.ecom.dev.mapper.EcomDevProjectMapper;
import com.lemon.erp.modules.ecom.dev.model.DevProjectStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页待办：待手工审批的开发项目（有审批权限）、1688 价格变动待确认、待付款的 1688 采购单
 */
@Component
@RequiredArgsConstructor
public class DevTodoProvider implements TodoProvider {

    private final EcomDevProjectMapper projectMapper;
    private final Ali1688OfferService offerService;
    private final Ali1688OrderService orderService;
    private final PermissionService permissionService;

    @Override
    public List<TodoItem> todos(Long userId) {
        List<TodoItem> items = new ArrayList<>();
        if (permissionService.hasPerm("ecom:devProject:approve")) {
            long n = projectMapper.selectCount(Wrappers.<EcomDevProject>lambdaQuery().eq(EcomDevProject::getStatus, DevProjectStatus.PENDING_APPROVAL)
                    .isNull(EcomDevProject::getFlowInstanceId));
            if (n > 0) {
                items.add(new TodoItem("ecom", "ecom.text.dev.todoPendingApproval", n, "/ecom/dev-project?status=PENDING_APPROVAL", "warning"));
            }
        }
        if (permissionService.hasPerm("ecom:ali1688Offer:list")) {
            long n = offerService.alertCount();
            if (n > 0) {
                items.add(new TodoItem("ecom", "ecom.text.dev.todoPriceAlert", n, "/ecom/ali1688-offer?priceAlert=1", "warning"));
            }
        }
        if (permissionService.hasPerm("ecom:ali1688Order:list")) {
            long n = orderService.waitPayCount();
            if (n > 0) {
                items.add(new TodoItem("ecom", "ecom.text.dev.todoWaitPay", n, "/ecom/ali1688-order?status=WAIT_BUYER_PAY", "info"));
            }
        }
        return items;
    }
}
