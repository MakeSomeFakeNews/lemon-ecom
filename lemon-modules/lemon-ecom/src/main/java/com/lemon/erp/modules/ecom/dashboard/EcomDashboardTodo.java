package com.lemon.erp.modules.ecom.dashboard;

import java.util.Map;

/**
 * 电商看板的一项待办（由 {@link EcomDashboardTodoProvider} 提供）
 *
 * @param key   唯一标识（如 order.pending），前端据此去重
 * @param title 标题（按当前语言）
 * @param count 数量（0 的不返回）
 * @param unit  单位（按当前语言，如「单」「个」）
 * @param path  点击跳转的页面路径（如 /ecom/order），为空不可点击
 * @param query 跳转时的查询参数（页面据此设置初始筛选），可为空
 * @param tone  色调：info / warning / danger
 * @param icon  图标名（Ant Design 图标组件名，如 ClockCircleOutlined，前端没有的用默认图标）
 * @param sort  排序（越小越靠前）
 */
public record EcomDashboardTodo(String key, String title, long count, String unit, String path, Map<String, String> query, String tone,
                                String icon, int sort) {

    public static final String INFO = "info";
    public static final String WARNING = "warning";
    public static final String DANGER = "danger";
}
