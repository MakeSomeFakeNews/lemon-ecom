package com.lemon.erp.modules.ecom.automation.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.LoginUserLoader;
import com.lemon.erp.modules.ecom.automation.action.AutoActionHandler;
import com.lemon.erp.modules.ecom.automation.action.AutoOutcome;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.dto.AutoViews;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoLog;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoLogMapper;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoRuleMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoActionStatus;
import com.lemon.erp.modules.ecom.automation.model.AutoResult;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 规则执行引擎：一条事件逐条匹配该触发器的启用规则 ——
 * 不在对象范围内的忽略；条件不满足记「条件不满足」；同一对象在防抖时间内执行过记「防抖跳过」；达到每日上限记「达到上限」；
 * 否则以规则创建人的身份按顺序执行动作（单个动作失败不影响其他动作），记录每个动作的结果与耗时。
 * <p>
 * 引擎不开启事务：动作内部的写操作经过所属 Service 各自的事务，Webhook / 群机器人 / 平台接口在事务外调用。
 * 扫描类触发器的「条件不满足」「防抖跳过」不记日志（每次扫描都会重复），每日至少间隔 24 小时才会对同一对象再次执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoEngine {

    /** 扫描类触发器对同一对象的最短执行间隔（小时） */
    static final int SCAN_MIN_DEBOUNCE_HOURS = 24;
    private static final int DETAIL_MAX = 1000;
    private static final int MESSAGE_MAX = 500;
    private static final Object[] LOCKS = new Object[64];

    static {
        for (int i = 0; i < LOCKS.length; i++) {
            LOCKS[i] = new Object();
        }
    }

    private final AutoRuleCache ruleCache;
    private final AutoCatalog catalog;
    private final EcomAutoLogMapper logMapper;
    private final EcomAutoRuleMapper ruleMapper;
    private final LoginUserLoader loginUserLoader;
    private final DictLookup dictLookup;

    /** 处理一条事件（全部启用规则），返回执行了动作的规则数；单条规则出错只记日志 */
    public int handle(AutoEvent event) {
        int executed = 0;
        for (EcomAutoRule rule : ruleCache.rules(event.trigger())) {
            try {
                EcomAutoLog l = process(rule, event);
                if (l != null && l.getResult().executed()) {
                    executed++;
                }
            } catch (RuntimeException e) {
                log.warn("Automation rule {} failed on {}", rule.getId(), event.objectKey(), e);
            }
        }
        return executed;
    }

    /**
     * 一条规则处理一条事件
     *
     * @return 写入的日志，不在范围内 / 扫描类不记日志的情况为 null
     */
    public EcomAutoLog process(EcomAutoRule rule, AutoEvent event) {
        if (!AutoConditions.inScope(rule.getScope(), event)) {
            return null;
        }
        boolean scan = event.trigger().scan();
        long start = System.nanoTime();
        AutoConditions.Match m = AutoConditions.evaluate(rule.getCondition(), event, catalog.fields(event.trigger()), Messages.DEFAULT_LOCALE, this::dictLabel);
        if (!m.ok()) {
            return scan ? null : save(rule, event, AutoResult.NOT_MATCHED, m.detail(), List.of(), start, null);
        }
        synchronized (LOCKS[Math.floorMod((rule.getId() + ":" + event.objectKey()).hashCode(), LOCKS.length)]) {
            if (debounced(rule, event)) {
                return scan ? null : save(rule, event, AutoResult.DEBOUNCED, m.detail(), List.of(), start, null);
            }
            if (limited(rule)) {
                if (scan && logMapper.countRuleResult(rule.getId(), AutoResult.LIMITED.name(), LocalDate.now().atStartOfDay()) > 0) {
                    return null;
                }
                return save(rule, event, AutoResult.LIMITED, m.detail(), List.of(), start, null);
            }
            List<AutoViews.ActionResult> results = runActions(rule, event, null);
            EcomAutoLog l = save(rule, event, summarize(results), m.detail(), results, start, firstError(results));
            ruleMapper.markTriggered(rule.getId(), LocalDateTime.now(), l.getResult());
            return l;
        }
    }

    /** 字典名称（存库为中文；字典不存在或没有该取值时为取值本身） */
    public String dictLabel(String dict, String value) {
        try {
            return dictLookup.options(dict).getOrDefault(value, value);
        } catch (RuntimeException e) {
            return value;
        }
    }

    /** 同一对象在防抖时间内是否已执行过（扫描类至少 24 小时） */
    boolean debounced(EcomAutoRule rule, AutoEvent event) {
        int hours = rule.getDebounceHours() == null ? 0 : rule.getDebounceHours();
        if (event.trigger().scan()) {
            hours = Math.max(hours, SCAN_MIN_DEBOUNCE_HOURS);
        }
        return hours > 0 && logMapper.countExecuted(rule.getId(), event.objectKey(), LocalDateTime.now().minusHours(hours)) > 0;
    }

    boolean limited(EcomAutoRule rule) {
        int limit = rule.getDailyLimit() == null ? 0 : rule.getDailyLimit();
        return limit > 0 && logMapper.countRuleExecuted(rule.getId(), LocalDate.now().atStartOfDay()) >= limit;
    }

    /**
     * 以规则创建人的身份按顺序执行动作
     *
     * @param only 只执行这些位置的动作（重试失败的动作），为空执行全部
     */
    List<AutoViews.ActionResult> runActions(EcomAutoRule rule, AutoEvent event, List<Integer> only) {
        LoginUser user = rule.getCreateBy() == null ? null : loginUserLoader.loadLoginUser(rule.getCreateBy());
        return AutoRunAs.call(user, () -> {
            List<AutoViews.ActionResult> out = new ArrayList<>();
            List<AutoRuleParts.Action> actions = rule.getActions();
            for (int i = 0; i < actions.size(); i++) {
                if (only != null && !only.contains(i)) {
                    continue;
                }
                AutoRuleParts.Action a = actions.get(i);
                long t0 = System.nanoTime();
                AutoActionStatus status;
                String message;
                try {
                    AutoActionHandler h = catalog.handler(a.type());
                    if (!a.type().supports(event.trigger())) {
                        throw new BizException("ecom.error.auto.actionUnsupported", Messages.get(a.type().label()), Messages.get(event.trigger().label()));
                    }
                    AutoOutcome r = h.execute(a, event, rule);
                    status = r.status();
                    message = r.message();
                } catch (BizException e) {
                    status = AutoActionStatus.FAILED;
                    message = e.getMessage();
                } catch (RuntimeException e) {
                    log.warn("Automation action {} of rule {} failed on {}", a.type(), rule.getId(), event.objectKey(), e);
                    status = AutoActionStatus.FAILED;
                    message = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
                }
                out.add(new AutoViews.ActionResult(i, a.type(), status, AutoText.cut(message, MESSAGE_MAX), (System.nanoTime() - t0) / 1_000_000));
            }
            return out;
        });
    }

    /** 动作结果汇总：全部失败为失败，部分失败为部分成功，其余为成功（跳过、仅建议视为成功） */
    static AutoResult summarize(List<AutoViews.ActionResult> results) {
        long failed = results.stream().filter(r -> r.status() == AutoActionStatus.FAILED).count();
        if (failed == 0) {
            return AutoResult.SUCCESS;
        }
        return failed == results.size() ? AutoResult.FAILED : AutoResult.PARTIAL;
    }

    static String firstError(List<AutoViews.ActionResult> results) {
        return results.stream().filter(r -> r.status() == AutoActionStatus.FAILED).map(AutoViews.ActionResult::message).findFirst().orElse(null);
    }

    private EcomAutoLog save(EcomAutoRule rule, AutoEvent event, AutoResult result, String detail, List<AutoViews.ActionResult> actions, long start,
                             String error) {
        EcomAutoLog l = new EcomAutoLog();
        l.setRuleId(rule.getId());
        l.setRuleName(rule.getName());
        l.setTriggerCode(event.trigger());
        l.setObjectKey(AutoText.cut(event.objectKey(), 64));
        l.setObjectLabel(AutoText.cut(event.objectLabel(), 128));
        l.setStoreId(event.storeId());
        l.setResult(result);
        l.setMatchDetail(AutoText.cut(detail, DETAIL_MAX));
        l.setCostMs((int) ((System.nanoTime() - start) / 1_000_000));
        l.setErrorMsg(AutoText.cut(error, MESSAGE_MAX));
        l.setRetryCount(0);
        l.setEventJson(AutoStoredEvent.json(event));
        l.setActionsJson(AutoRuleCodec.json(actions));
        l.setActions(actions);
        logMapper.insert(l);
        return l;
    }

    // ---------------------------------------------------------------- 试运行

    /** 试运行：只判断范围、条件、防抖，说明每个动作将执行的内容，不执行、不写日志 */
    public AutoViews.TestSample test(EcomAutoRule rule, AutoEvent event, String source, Locale locale) {
        boolean inScope = AutoConditions.inScope(rule.getScope(), event);
        AutoConditions.Match m = AutoConditions.evaluate(rule.getCondition(), event, catalog.fields(event.trigger()), locale,
                Messages.DEFAULT_LOCALE.getLanguage().equals(locale.getLanguage()) ? this::dictLabel : (d, v) -> v);
        boolean debounced = rule.getId() != null && debounced(rule, event);
        List<AutoViews.TestAction> actions = new ArrayList<>();
        for (int i = 0; i < rule.getActions().size(); i++) {
            AutoRuleParts.Action a = rule.getActions().get(i);
            boolean available = a.type().supports(event.trigger());
            String desc;
            try {
                desc = available ? catalog.handler(a.type()).describe(a, event, rule)
                        : Messages.get("ecom.error.auto.actionUnsupported", Messages.get(a.type().label()), Messages.get(event.trigger().label()));
            } catch (BizException e) {
                available = false;
                desc = e.getMessage();
            }
            actions.add(new AutoViews.TestAction(i, a.type(), desc, available));
        }
        return new AutoViews.TestSample(source, event.objectKey(), event.objectLabel(), (String) event.field("storeName"), event.fields(), inScope,
                m.ok(), m.detail(), debounced, actions);
    }
}
