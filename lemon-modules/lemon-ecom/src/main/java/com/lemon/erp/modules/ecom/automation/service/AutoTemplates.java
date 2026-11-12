package com.lemon.erp.modules.ecom.automation.service;

import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.dto.AutoViews;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.model.AutoLogic;
import com.lemon.erp.modules.ecom.automation.model.AutoOperator;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.connector.HttpJson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内置规则模板：常见场景的触发器 + 条件 + 动作，一键创建（名称、消息标题与内容按当前语言生成，通知 / 待办同时发给店铺负责人和创建人）。
 * 新增模板：在 {@link #TEMPLATES} 中加一项，语言包 ecom.text.auto.tpl.&lt;代码&gt;.name / .desc / .title / .content 同步增加
 */
public final class AutoTemplates {

    /** 模板定义；actions 中 title / content 为 true 表示使用模板的消息标题 / 内容 */
    record Def(String code, AutoTrigger trigger, AutoLogic logic, List<AutoRuleParts.Condition> conditions, List<Map<String, Object>> actions,
               int debounceHours, int dailyLimit) {
    }

    static final List<Def> TEMPLATES = List.of(
            new Def("HIJACK_NOTIFY", AutoTrigger.LISTING_HIJACKED, AutoLogic.AND, List.of(cond("monitorType", AutoOperator.EQ, "OWN")),
                    List.of(notice(), todo(1)), 24, 0),
            new Def("STOCKOUT_PLAN", AutoTrigger.STOCKOUT_RISK, AutoLogic.AND, List.of(cond("coverDays", AutoOperator.LT, "15")),
                    List.of(Map.of("type", AutoActionType.PURCHASE_PLAN.name()), notice()), 72, 50),
            new Def("REVIEW_TICKET", AutoTrigger.REVIEW_NEGATIVE, AutoLogic.AND,
                    List.of(cond("rating", AutoOperator.LE, "1"), cond("hasOrder", AutoOperator.EQ, "true")),
                    List.of(Map.of("type", AutoActionType.CS_TICKET.name(), "ticketType", "REFUND_ONLY"), notice()), 0, 0),
            new Def("LOSS_HOLD", AutoTrigger.ORDER_LOSS, AutoLogic.AND,
                    List.of(cond("estProfit", AutoOperator.LT, "0"), cond("fulfillment", AutoOperator.EQ, "SELLER")),
                    List.of(Map.of("type", AutoActionType.ORDER_HOLD.name(), "reason", true), notice()), 0, 0),
            new Def("ACOS_PAUSE", AutoTrigger.AD_ACOS_HIGH, AutoLogic.AND,
                    List.of(cond("acos", AutoOperator.GT, "50"), cond("spend", AutoOperator.GT, "20")),
                    List.of(Map.of("type", AutoActionType.AD_PAUSE.name()), notice()), 24, 20),
            new Def("BUDGET_RAISE", AutoTrigger.AD_BUDGET_EXHAUSTED, AutoLogic.AND,
                    List.of(cond("usagePercent", AutoOperator.GE, "100"), cond("acos", AutoOperator.LT, "30")),
                    List.of(Map.of("type", AutoActionType.AD_ADJUST.name(), "adTarget", "BUDGET", "adjustMode", "PERCENT", "value", 20, "maxValue", 200),
                            notice()), 24, 10),
            new Def("AUTH_ALERT", AutoTrigger.STORE_AUTH_INVALID, AutoLogic.AND, List.of(),
                    List.of(notice(), todo(1)), 24, 0),
            new Def("SYNC_FAIL_NOTIFY", AutoTrigger.SYNC_FAILED, AutoLogic.AND, List.of(),
                    List.of(notice()), 6, 0),
            new Def("MSG_TIMEOUT", AutoTrigger.MESSAGE_TIMEOUT, AutoLogic.AND, List.of(cond("overdueHours", AutoOperator.GE, "0")),
                    List.of(notice()), 24, 0),
            new Def("CLAIM_DEADLINE", AutoTrigger.FBA_CLAIM_DEADLINE, AutoLogic.AND, List.of(cond("daysLeft", AutoOperator.LE, "7")),
                    List.of(todo(3), notice()), 72, 0),
            new Def("COMPETITOR_SUGGEST", AutoTrigger.COMPETITOR_PRICE_LOWER, AutoLogic.AND,
                    List.of(cond("gapPercent", AutoOperator.GT, "5"), cond("buyboxWinner", AutoOperator.EQ, "false")),
                    List.of(Map.of("type", AutoActionType.LISTING_REPRICE.name(), "adjustMode", "PERCENT", "value", -3, "suggestOnly", true), notice()),
                    24, 0),
            new Def("INBOUND_OVERDUE", AutoTrigger.INBOUND_OVERDUE, AutoLogic.AND, List.of(cond("overdueDays", AutoOperator.GE, "3")),
                    List.of(notice(), todo(2)), 72, 0),
            new Def("RETURN_RATE", AutoTrigger.RETURN_RATE_HIGH, AutoLogic.AND,
                    List.of(cond("returnRate", AutoOperator.GT, "10"), cond("orderedQty", AutoOperator.GE, "20")),
                    List.of(notice(), todo(7)), 168, 0),
            new Def("EXCEPTION_NOTIFY", AutoTrigger.ORDER_EXCEPTION, AutoLogic.AND, List.of(),
                    List.of(notice()), 0, 100));

    private AutoTemplates() {
    }

    /** 模板列表（按当前语言） */
    public static List<AutoViews.TemplateView> views() {
        List<AutoViews.TemplateView> out = new ArrayList<>();
        for (Def d : TEMPLATES) {
            out.add(new AutoViews.TemplateView(d.code(), Messages.get(key(d, "name")), Messages.get(key(d, "desc")), d.trigger(), d.trigger().category(),
                    Messages.get(d.trigger().label()), d.actions().stream().map(a -> AutoActionType.valueOf((String) a.get("type"))).toList(),
                    build(d, null)));
        }
        return out;
    }

    /**
     * 按模板构造规则（未保存）
     *
     * @param userId 通知 / 待办的接收人（创建人），为空只发给店铺负责人
     */
    public static EcomAutoRule build(String code, Long userId) {
        return build(TEMPLATES.stream().filter(d -> d.code().equals(code)).findFirst()
                .orElseThrow(() -> new BizException("ecom.error.auto.templateNotFound", code)), userId);
    }

    static EcomAutoRule build(Def d, Long userId) {
        EcomAutoRule r = new EcomAutoRule();
        r.setName(Messages.get(key(d, "name")));
        r.setTriggerCode(d.trigger());
        r.setStatus(EnableStatus.ENABLED);
        r.setScope(AutoRuleParts.Scope.empty());
        r.setCondition(new AutoRuleParts.Group(d.logic(), d.conditions(), List.of()));
        List<AutoRuleParts.Action> actions = new ArrayList<>();
        for (Map<String, Object> a : d.actions()) {
            Map<String, Object> m = new LinkedHashMap<>(a);
            if (Boolean.TRUE.equals(m.get("title"))) {
                m.put("title", Messages.get(key(d, "title")));
            }
            if (Boolean.TRUE.equals(m.get("content"))) {
                m.put("content", Messages.get(key(d, "content")));
            }
            if (Boolean.TRUE.equals(m.get("reason"))) {
                m.put("reason", Messages.get(key(d, "reason")));
            }
            if (Boolean.TRUE.equals(m.get("storeOwner")) && userId != null) {
                m.put("userIds", List.of(userId));
            }
            actions.add(HttpJson.MAPPER.convertValue(m, AutoRuleParts.Action.class));
        }
        r.setActions(actions);
        r.setDebounceHours(d.debounceHours());
        r.setDailyLimit(d.dailyLimit());
        r.setTemplateCode(d.code());
        r.setRemark(Messages.get(key(d, "desc")));
        return r;
    }

    private static String key(Def d, String part) {
        return "ecom.text.auto.tpl." + d.code() + "." + part;
    }

    private static AutoRuleParts.Condition cond(String field, AutoOperator op, String value) {
        return new AutoRuleParts.Condition(field, op, value);
    }

    /** 站内消息通知店铺负责人（和创建人），使用模板的标题与内容 */
    private static Map<String, Object> notice() {
        return Map.of("type", AutoActionType.NOTIFY.name(), "storeOwner", true, "site", true, "title", true, "content", true);
    }

    private static Map<String, Object> todo(int dueDays) {
        return Map.of("type", AutoActionType.TODO.name(), "storeOwner", true, "title", true, "content", true, "dueDays", dueDays);
    }
}
