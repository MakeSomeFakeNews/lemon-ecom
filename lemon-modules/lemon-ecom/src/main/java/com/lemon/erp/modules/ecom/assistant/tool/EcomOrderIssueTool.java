package com.lemon.erp.modules.ecom.assistant.tool;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.ecom.assistant.dto.AssistantRows;
import com.lemon.erp.modules.ecom.assistant.mapper.EcomAssistantQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单异常与超时：不传 kind 时按店铺汇总异常（异常 / 挂起 / 缺货）、已超过承诺发货时间未发出、即将到期（默认 24 小时内）、待处理的订单数；
 * 传 kind 时列出该类订单明细（距承诺发货截止的小时数，负数为已超时）。权限与「平台订单」页面一致
 */
@Component
@RequiredArgsConstructor
public class EcomOrderIssueTool implements AiTool {

    enum Kind {
        EXCEPTION, OVERDUE, DUE_SOON
    }

    private final EcomAssistantQueryMapper queryMapper;
    private final AssistantToolSupport support;

    @Override
    public String name() {
        return "ecom_order_issues";
    }

    @Override
    public String labelKey() {
        return "ecom.ai.asst.tool.orderIssues";
    }

    @Override
    public String descriptionKey() {
        return "ecom.ai.asst.tool.orderIssues.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        AiSchema s = AiSchema.object()
                .prop("kind", AiSchema.enumOf("ecom.ai.asst.param.orderKind", Kind.EXCEPTION.name(), Kind.OVERDUE.name(), Kind.DUE_SOON.name()), false)
                .prop("hours", AiSchema.integer("ecom.ai.asst.param.dueHours").min(1).max(168), false);
        AssistantToolSupport.scopeProps(s);
        return AssistantToolSupport.topProp(s).build();
    }

    @Override
    public String permission() {
        return "ecom:order:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        AssistantToolSupport.Scope scope = support.scope(args);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = now.plusHours(args.getInt("hours", 24));
        int top = AssistantToolSupport.top(args, 50);
        if (!args.has("kind")) {
            return AiToolResult.rows(queryMapper.selectOrderIssueSummary(scope.ids(), now, due));
        }
        Kind kind = Kind.valueOf(args.getString("kind"));
        List<AssistantRows.OrderIssue> rows = queryMapper.selectOrderIssues(scope.ids(), kind.name(), now, due, top);
        rows.forEach(r -> r.setHoursLeft(r.getShipBy() == null ? null : Duration.between(now, r.getShipBy()).toHours()));
        return AiToolResult.rows(rows);
    }
}
