package com.lemon.erp.modules.ecom.automation.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.dto.AutoViews;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoLog;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoTodo;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoLogMapper;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoRuleMapper;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoTodoMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoActionStatus;
import com.lemon.erp.modules.ecom.automation.model.AutoActionType;
import com.lemon.erp.modules.ecom.automation.model.AutoFieldType;
import com.lemon.erp.modules.ecom.automation.model.AutoLogic;
import com.lemon.erp.modules.ecom.automation.model.AutoOperator;
import com.lemon.erp.modules.ecom.automation.model.AutoRef;
import com.lemon.erp.modules.ecom.automation.model.AutoResult;
import com.lemon.erp.modules.ecom.automation.model.AutoTodoStatus;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.automation.service.AutoCatalog;
import com.lemon.erp.modules.ecom.automation.service.AutoConditions;
import com.lemon.erp.modules.ecom.automation.service.AutoEngine;
import com.lemon.erp.modules.ecom.automation.service.AutoRuleCodec;
import com.lemon.erp.modules.ecom.automation.service.AutoRuleService;
import com.lemon.erp.modules.ecom.automation.service.AutoStoredEvent;
import com.lemon.erp.modules.ecom.automation.service.AutoText;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import com.lemon.erp.modules.ecom.automation.trigger.AutoField;
import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.demo.DemoContributor;
import com.lemon.erp.modules.ecom.demo.DemoKeys;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示数据 · 自动化规则（order 190）：从模板创建 10 条启用的规则（限定演示店铺）和 2 条自定义规则（且 / 或条件、Webhook），
 * 用演示数据中的真实对象（订单、在线商品、广告活动、评价、监控预警、货件等）生成近 14 天的执行日志：
 * 成功、部分成功 / 失败（Webhook 超时、没有接收人）、防抖跳过、条件不满足（真实判断）、达到上限，并生成若干待办提醒（部分已完成）。
 * 日志只是历史记录，不实际执行动作（不会挂起演示订单、不改价）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoDemoContributor implements DemoContributor {

    static final int DAYS = 14;
    private static final String[] TEMPLATES = {"HIJACK_NOTIFY", "STOCKOUT_PLAN", "REVIEW_TICKET", "LOSS_HOLD", "ACOS_PAUSE", "BUDGET_RAISE", "AUTH_ALERT",
            "MSG_TIMEOUT", "CLAIM_DEADLINE", "COMPETITOR_SUGGEST", "INBOUND_OVERDUE", "RETURN_RATE"};

    private final AutoRuleService ruleService;
    private final AutoCatalog catalog;
    private final AutoEngine engine;
    private final EcomStoreService storeService;
    private final EcomAutoRuleMapper ruleMapper;
    private final EcomAutoLogMapper logMapper;
    private final EcomAutoTodoMapper todoMapper;

    @Override
    public int order() {
        return 190;
    }

    @Override
    public String key() {
        return "automation";
    }

    @Override
    public void generate(DemoContext ctx) {
        List<Long> stores = storeService.all().stream().filter(s -> s.getCode() != null && s.getCode().startsWith(DemoKeys.STORE_CODE_PREFIX))
                .map(EcomStore::getId).toList();
        if (stores.isEmpty()) {
            return;
        }
        List<Long> operators = ctx.get(DemoKeys.OPERATOR_USER_IDS);
        List<EcomAutoRule> rules = new ArrayList<>();
        for (String code : TEMPLATES) {
            rules.add(ruleService.require(ruleService.createFromTemplate(code, stores)));
        }
        rules.add(ruleService.require(ruleService.create(bigOrderRule(stores))));
        Long webhook = ruleService.create(webhookRule(stores));
        ruleService.changeStatus(webhook, EnableStatus.DISABLED);
        rules.add(ruleService.require(webhook));

        List<EcomStore> demoStores = storeService.byIds(stores);
        for (EcomAutoRule rule : rules) {
            try {
                logs(ctx, rule, operators, demoStores);
            } catch (RuntimeException e) {
                log.warn("Demo automation logs for rule {} failed: {}", rule.getName(), e.getMessage());
            }
        }
    }

    /** 自定义规则：新订单 金额（本位币）≥ 1500 或 （收件国家属于 AU、CA 且 件数 ≥ 3）→ 通知运营 */
    private EcomAutoRule bigOrderRule(List<Long> stores) {
        EcomAutoRule r = new EcomAutoRule();
        r.setName(Messages.get("ecom.text.auto.demo.bigOrder"));
        r.setTriggerCode(AutoTrigger.ORDER_NEW);
        r.setScope(new AutoRuleParts.Scope(stores, List.of(), List.of(), List.of(), List.of()));
        r.setCondition(new AutoRuleParts.Group(AutoLogic.OR, List.of(new AutoRuleParts.Condition("totalBase", AutoOperator.GE, "1500")),
                List.of(new AutoRuleParts.Group(AutoLogic.AND, List.of(new AutoRuleParts.Condition("country", AutoOperator.IN, "AU,CA"),
                        new AutoRuleParts.Condition("itemQty", AutoOperator.GE, "3")), List.of()))));
        r.setActions(List.of(new AutoRuleParts.Action(AutoActionType.NOTIFY, null, null, true, true, null, Messages.get("ecom.text.auto.demo.bigOrderTitle"),
                Messages.get("ecom.text.auto.demo.bigOrderContent"), null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null)));
        r.setDebounceHours(0);
        r.setDailyLimit(0);
        r.setRemark(Messages.get("ecom.text.auto.demo.bigOrderRemark"));
        return r;
    }

    /** 自定义规则：渠道同步失败 → 推送到运维系统 Webhook（停用，历史日志中有超时失败） */
    private EcomAutoRule webhookRule(List<Long> stores) {
        EcomAutoRule r = new EcomAutoRule();
        r.setName(Messages.get("ecom.text.auto.demo.webhook"));
        r.setTriggerCode(AutoTrigger.SYNC_FAILED);
        r.setScope(new AutoRuleParts.Scope(stores, List.of(), List.of(), List.of(), List.of()));
        r.setCondition(AutoRuleParts.Group.empty());
        r.setActions(List.of(new AutoRuleParts.Action(AutoActionType.WEBHOOK, null, null, null, null, null, null, null, null,
                "https://ops.example.com/hooks/lemon-erp", "demo-secret", null, null, null, null, null, null, null, null, null, null, null, null, null)));
        r.setDebounceHours(6);
        r.setDailyLimit(0);
        return r;
    }

    /**
     * 一条规则近 14 天的日志：实时触发器用真实对象构造事件并真实判断条件（不满足的记「条件不满足」）；
     * 扫描类只记录满足条件的，当前满足的对象不足时，取真实对象把条件中的数值调整为满足（表示历史上某天满足过），
     * 没有任何对象（如没有同步失败、预算耗尽的记录）时按演示店铺生成样例事件
     */
    private void logs(DemoContext ctx, EcomAutoRule rule, List<Long> operators, List<EcomStore> stores) {
        AutoTrigger t = rule.getTriggerCode();
        Map<String, AutoField> fields = catalog.fields(t);
        List<AutoEvent> real = new ArrayList<>(catalog.source(t).recent(t, t == AutoTrigger.ORDER_NEW ? 120 : 40));
        List<AutoEvent> pool = new ArrayList<>();
        if (!t.scan()) {
            pool.addAll(real);
        } else {
            real.stream().filter(e -> AutoConditions.evaluate(rule.getCondition(), e, fields, Messages.DEFAULT_LOCALE).ok()).forEach(pool::add);
            for (AutoEvent e : real) {
                if (pool.size() >= 4) {
                    break;
                }
                if (!pool.contains(e)) {
                    pool.add(satisfy(e, rule.getCondition(), fields));
                }
            }
        }
        if (pool.isEmpty()) {
            for (EcomStore s : stores.subList(0, Math.min(4, stores.size()))) {
                pool.add(satisfy(sample(t, s), rule.getCondition(), fields));
            }
        }
        if (pool.isEmpty()) {
            return;
        }
        LocalDate today = ctx.today();
        int count = t.scan() ? ctx.between(8, 16) : ctx.between(15, 35);
        int executed = 0;
        LocalDateTime last = null;
        AutoResult lastResult = null;
        for (int i = 0; i < count; i++) {
            AutoEvent e = pool.get(i % pool.size());
            LocalDateTime time = today.minusDays(ctx.between(0, DAYS - 1)).atTime(ctx.between(7, 22), ctx.between(0, 59), ctx.between(0, 59));
            if (time.isAfter(LocalDateTime.now())) {
                time = LocalDateTime.now().minusMinutes(ctx.between(5, 120));
            }
            AutoConditions.Match m = AutoConditions.evaluate(rule.getCondition(), e, fields, Messages.DEFAULT_LOCALE, engine::dictLabel);
            AutoResult result;
            if (!m.ok()) {
                if (t.scan()) {
                    continue;
                }
                result = AutoResult.NOT_MATCHED;
            } else {
                double p = ctx.random().nextDouble();
                result = p < 0.68 ? AutoResult.SUCCESS : p < 0.78 ? AutoResult.DEBOUNCED : p < 0.88 ? AutoResult.PARTIAL : p < 0.95 ? AutoResult.FAILED
                        : AutoResult.LIMITED;
                if (rule.getDailyLimit() == 0 && result == AutoResult.LIMITED) {
                    result = AutoResult.SUCCESS;
                }
                if (rule.getActions().size() == 1 && result == AutoResult.PARTIAL) {
                    result = AutoResult.FAILED;
                }
            }
            List<AutoViews.ActionResult> actions = result.executed() ? actions(ctx, rule, e, result) : List.of();
            EcomAutoLog l = new EcomAutoLog();
            l.setRuleId(rule.getId());
            l.setRuleName(rule.getName());
            l.setTriggerCode(t);
            l.setObjectKey(AutoText.cut(e.objectKey(), 64));
            l.setObjectLabel(AutoText.cut(e.objectLabel(), 128));
            l.setStoreId(e.storeId());
            l.setResult(result);
            l.setMatchDetail(AutoText.cut(m.detail(), 1000));
            l.setCostMs(result.executed() ? ctx.between(40, 2600) : ctx.between(2, 30));
            l.setErrorMsg(actions.stream().filter(a -> a.status() == AutoActionStatus.FAILED).map(AutoViews.ActionResult::message).findFirst()
                    .orElse(null));
            l.setRetryCount(0);
            l.setEventJson(AutoStoredEvent.json(e));
            l.setActionsJson(AutoRuleCodec.json(actions));
            l.setCreateTime(time);
            l.setUpdateTime(time);
            logMapper.insert(l);
            if (result.executed()) {
                executed++;
                if (last == null || time.isAfter(last)) {
                    last = time;
                    lastResult = result;
                }
                todo(ctx, rule, e, time, operators);
            }
        }
        if (executed > 0) {
            ruleMapper.update(Wrappers.<EcomAutoRule>lambdaUpdate().set(EcomAutoRule::getTriggerCount, executed).set(EcomAutoRule::getLastTriggerTime, last)
                    .set(EcomAutoRule::getLastResult, lastResult).eq(EcomAutoRule::getId, rule.getId()));
        }
    }

    /** 把事件中条件涉及的字段调整为满足条件（数字按阈值上下浮动，是否 / 文本取条件值），只调整根组的「全部满足」条件和「任一满足」的第一个 */
    private static AutoEvent satisfy(AutoEvent e, AutoRuleParts.Group g, Map<String, AutoField> fields) {
        if (AutoConditions.isEmpty(g) || g.items() == null) {
            return e;
        }
        Map<String, Object> values = new LinkedHashMap<>(e.fields());
        List<AutoRuleParts.Condition> items = g.logic() == AutoLogic.OR ? g.items().stream().limit(1).toList() : g.items();
        for (AutoRuleParts.Condition c : items) {
            AutoField f = fields.get(c.field());
            if (f == null || c.value() == null) {
                continue;
            }
            if (f.type() == AutoFieldType.NUMBER) {
                BigDecimal v = new BigDecimal(c.value().split("[,，]")[0].trim());
                BigDecimal base = v.signum() == 0 ? BigDecimal.ONE : v.abs();
                BigDecimal out = switch (c.operator()) {
                    case GT -> v.add(base.multiply(new BigDecimal("0.35")));
                    case GE -> v.add(base.multiply(new BigDecimal("0.1")));
                    case LT -> v.subtract(base.multiply(new BigDecimal("0.4")));
                    case LE -> v.subtract(base.multiply(new BigDecimal("0.1")));
                    default -> v;
                };
                values.put(c.field(), out.setScale(2, RoundingMode.HALF_UP));
            } else if (c.operator() == AutoOperator.EQ || c.operator() == AutoOperator.IN) {
                String v = c.value().split("[,，]")[0].trim();
                values.put(c.field(), f.type() == AutoFieldType.BOOLEAN ? Boolean.valueOf(v) : v);
            }
        }
        return new AutoEvent(e.trigger(), e.objectKey(), e.objectLabel(), e.storeId(), e.platform(), e.site(), e.skus(), e.tags(), values, e.refs(), e.link(),
                e.time());
    }

    /** 没有真实对象时按演示店铺生成的样例事件 */
    private AutoEvent sample(AutoTrigger t, EcomStore s) {
        AutoEvent base = ruleService.sample(t);
        Map<String, Object> values = new LinkedHashMap<>(base.fields());
        values.put("storeName", s.getName());
        values.put("platform", s.getPlatform() == null ? null : s.getPlatform().name());
        values.put("site", s.getMarketplace());
        values.put("storeCode", s.getCode());
        Map<AutoRef, Long> refs = new EnumMap<>(AutoRef.class);
        refs.put(AutoRef.STORE, s.getId());
        String label = t == AutoTrigger.STORE_AUTH_INVALID || t == AutoTrigger.SYNC_FAILED ? s.getName() : String.valueOf(base.fields().values().iterator().next());
        return new AutoEvent(t, "SAMPLE:" + s.getId() + ":" + t.name(), label, s.getId(), s.getPlatform(), s.getMarketplace(), List.of(), List.of(), values, refs,
                null, LocalDateTime.now());
    }

    /** 动作结果：成功时都成功（调价仅建议），失败时第一个动作失败，部分成功时最后一个动作失败 */
    private List<AutoViews.ActionResult> actions(DemoContext ctx, EcomAutoRule rule, AutoEvent e, AutoResult result) {
        List<AutoViews.ActionResult> out = new ArrayList<>();
        List<AutoRuleParts.Action> list = rule.getActions();
        for (int i = 0; i < list.size(); i++) {
            AutoRuleParts.Action a = list.get(i);
            boolean fail = result == AutoResult.FAILED && i == 0 || result == AutoResult.PARTIAL && i == list.size() - 1;
            AutoActionStatus status = fail ? AutoActionStatus.FAILED : a.type() == AutoActionType.LISTING_REPRICE && Boolean.TRUE.equals(a.suggestOnly())
                    ? AutoActionStatus.SUGGESTED : AutoActionStatus.SUCCESS;
            out.add(new AutoViews.ActionResult(i, a.type(), status, fail ? failure(ctx, a) : success(ctx, a, e, rule), ctx.between(15, 1800)));
        }
        return out;
    }

    private static String success(DemoContext ctx, AutoRuleParts.Action a, AutoEvent e, EcomAutoRule rule) {
        return switch (a.type()) {
            case NOTIFY -> AutoText.t("ecom.text.auto.out.notified", ctx.between(1, 3), AutoText.t("ecom.text.auto.channel.SITE"));
            case TODO -> AutoText.t("ecom.text.auto.out.todoCreated", 1);
            case ORDER_HOLD -> AutoText.t("ecom.text.auto.out.held", AutoText.render(a.reason(), e, rule));
            case PURCHASE_PLAN -> AutoText.t("ecom.text.auto.out.planCreated", "PP" + LocalDate.now().toString().replace("-", "") + ctx.between(100, 999));
            case CS_TICKET -> AutoText.t("ecom.text.auto.out.ticketCreated", ctx.between(100, 999), e.field("orderNo"));
            case AD_PAUSE -> AutoText.t("ecom.text.auto.out.adPausedLocal", e.objectLabel());
            case AD_ADJUST -> AutoText.t("ecom.text.auto.out.budgetLocal", e.objectLabel(), "30.00", "36.00");
            case LISTING_REPRICE -> AutoText.t("ecom.text.auto.out.priceSuggested", e.field("sku"), e.field("price"), e.field("competitorPrice"),
                    e.field("currency"));
            case WEBHOOK -> AutoText.t("ecom.text.auto.out.webhook", 200, 1);
            default -> AutoText.t("ecom.text.auto.out.notified", 1, AutoText.t("ecom.text.auto.channel.SITE"));
        };
    }

    private static String failure(DemoContext ctx, AutoRuleParts.Action a) {
        return switch (a.type()) {
            case WEBHOOK -> AutoText.t("ecom.error.auto.webhookFailed", ctx.pick("HttpTimeoutException: request timed out", "HTTP 502 Bad Gateway"));
            case NOTIFY, TODO -> AutoText.t("ecom.error.auto.noRecipient");
            case PURCHASE_PLAN -> AutoText.t("ecom.error.auto.noSuggestQty", "DM-");
            case CS_TICKET -> AutoText.t("ecom.error.auto.ticketNoOrder");
            default -> AutoText.t("ecom.error.auto.webhookFailed", "HTTP 503");
        };
    }

    /** 有待办动作的执行：给店铺负责人和规则创建人（模板规则的待办同时发给两人）各建一条待办，早于 3 天的大多已完成 */
    private void todo(DemoContext ctx, EcomAutoRule rule, AutoEvent e, LocalDateTime time, List<Long> operators) {
        AutoRuleParts.Action a = rule.getActions().stream().filter(x -> x.type() == AutoActionType.TODO).findFirst().orElse(null);
        if (a == null || todoMapper.exists(Wrappers.<EcomAutoTodo>lambdaQuery().eq(EcomAutoTodo::getRuleId, rule.getId())
                .eq(EcomAutoTodo::getObjectKey, e.objectKey()))) {
            return;
        }
        Long owner = e.storeId() == null ? null : storeService.byIds(List.of(e.storeId())).stream().findFirst().map(EcomStore::getOwnerId).orElse(null);
        java.util.Set<Long> assignees = new java.util.LinkedHashSet<>();
        assignees.add(owner != null ? owner : operators == null || operators.isEmpty() ? rule.getCreateBy() : operators.get(0));
        assignees.add(rule.getCreateBy());
        assignees.remove(null);
        boolean done = time.isBefore(LocalDateTime.now().minusDays(3)) && ctx.chance(0.75);
        for (Long assignee : assignees) {
            EcomAutoTodo t = new EcomAutoTodo();
            t.setRuleId(rule.getId());
            t.setAssigneeId(assignee);
            t.setStoreId(e.storeId());
            t.setTitle(AutoText.cut(AutoText.render(a.title(), e, rule), 128));
            t.setContent(AutoText.cut(AutoText.render(a.content(), e, rule), 500));
            t.setLink(e.link());
            t.setObjectKey(AutoText.cut(e.objectKey(), 64));
            t.setDueDate(a.dueDays() == null ? null : time.toLocalDate().plusDays(a.dueDays()));
            t.setStatus(done ? AutoTodoStatus.DONE : AutoTodoStatus.OPEN);
            t.setDoneBy(done ? assignee : null);
            t.setDoneTime(done ? time.plusHours(ctx.between(2, 30)) : null);
            t.setCreateTime(time);
            t.setUpdateTime(time);
            todoMapper.insert(t);
        }
    }
}
