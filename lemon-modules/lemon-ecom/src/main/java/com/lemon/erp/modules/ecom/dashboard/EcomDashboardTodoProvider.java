package com.lemon.erp.modules.ecom.dashboard;

import java.util.List;

/**
 * 电商看板「待处理事项」的扩展点：各功能实现为 Spring Bean，看板按 sort 汇总显示（{@link EcomDashboardService#todos()}）。
 * <p>
 * 约定：只返回当前用户有权限查看的事项（用 {@code PermissionService.hasPerm} 判断），数量为 0 的不返回；查询要轻（COUNT / GROUP BY），
 * 不调用平台接口；单个提供者出错只记录日志、不影响其他。已实现：订单与补货（{@link EcomCoreDashboardTodos}）、客服（差评待处理、买家消息超时，
 * {@link EcomCsDashboardTodos}）、渠道（店铺授权失效、同步失败、广告同步失败，{@link EcomChannelDashboardTodos}）、
 * 海外仓与 FBA（待推送海外仓、推送失败、待提交索赔、索赔即将到期，{@link EcomOwhFbaDashboardTodos}）；其他功能实现本接口即可出现在看板上
 */
public interface EcomDashboardTodoProvider {

    List<EcomDashboardTodo> todos();
}
