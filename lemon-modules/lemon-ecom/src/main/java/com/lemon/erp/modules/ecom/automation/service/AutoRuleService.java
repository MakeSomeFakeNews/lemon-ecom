package com.lemon.erp.modules.ecom.automation.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.automation.dto.AutoRuleParts;
import com.lemon.erp.modules.ecom.automation.dto.AutoViews;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoLog;
import com.lemon.erp.modules.ecom.automation.entity.EcomAutoRule;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoLogMapper;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoRuleMapper;
import com.lemon.erp.modules.ecom.automation.mapper.EcomAutoScanMapper;
import com.lemon.erp.modules.ecom.automation.model.AutoFieldType;
import com.lemon.erp.modules.ecom.automation.model.AutoLogic;
import com.lemon.erp.modules.ecom.automation.model.AutoTrigger;
import com.lemon.erp.modules.ecom.automation.trigger.AutoEvent;
import com.lemon.erp.modules.ecom.automation.trigger.AutoField;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 自动化规则：维护（校验字段、运算符、动作与触发器是否匹配，密钥加密）、启停、复制、从模板创建、试运行、立即执行（扫描类）
 */
@Service
@RequiredArgsConstructor
public class AutoRuleService {

    static final int DEFAULT_DEBOUNCE_HOURS = 24;
    private static final int TEST_SAMPLES = 5;

    private final EcomAutoRuleMapper ruleMapper;
    private final EcomAutoLogMapper logMapper;
    private final EcomAutoScanMapper scanMapper;
    private final AutoRuleCodec codec;
    private final AutoCatalog catalog;
    private final AutoRuleCache ruleCache;
    private final AutoEngine engine;

    // ---------------------------------------------------------------- 查询

    public PageResult<EcomAutoRule> page(long current, long size, String keyword, AutoTrigger trigger, EnableStatus status) {
        Page<EcomAutoRule> page = Page.of(current, size);
        ruleMapper.selectPageList(page, keyword, trigger, status, LocalDateTime.now().minusDays(7));
        page.getRecords().forEach(r -> codec.masked(codec.read(r)));
        return PageResult.of(page);
    }

    /** 规则（密钥为掩码，附带指定用户的昵称） */
    public EcomAutoRule get(Long id) {
        EcomAutoRule r = codec.masked(require(id));
        Set<Long> users = new LinkedHashSet<>();
        r.getActions().stream().filter(a -> a.userIds() != null).forEach(a -> users.addAll(a.userIds()));
        Map<Long, String> names = new java.util.LinkedHashMap<>();
        if (!users.isEmpty()) {
            scanMapper.selectUserNames(users).forEach(m -> names.put(((Number) m.get("id")).longValue(), String.valueOf(m.get("nickname"))));
        }
        r.setUserNames(names);
        return r;
    }

    /** 规则（已解析，密钥为密文） */
    public EcomAutoRule require(Long id) {
        EcomAutoRule r = id == null ? null : ruleMapper.selectById(id);
        if (r == null) {
            throw new BizException("ecom.error.auto.ruleNotFound");
        }
        return codec.read(r);
    }

    public List<AutoViews.RoleOption> roles() {
        return scanMapper.selectRoles();
    }

    // ---------------------------------------------------------------- 维护

    public Long create(EcomAutoRule r) {
        r.setId(null);
        normalize(r);
        codec.write(r, null);
        r.setTriggerCount(0);
        r.setLastTriggerTime(null);
        r.setLastResult(null);
        ruleMapper.insert(r);
        ruleCache.invalidate();
        return r.getId();
    }

    public void update(Long id, EcomAutoRule r) {
        EcomAutoRule old = require(id);
        normalize(r);
        codec.write(r, old.getActions());
        r.setId(id);
        r.setCreateBy(null);
        r.setTriggerCount(null);
        r.setLastTriggerTime(null);
        r.setLastResult(null);
        ruleMapper.updateById(r);
        // 清空的来源模板、备注（updateById 不更新空值）
        ruleMapper.update(Wrappers.<EcomAutoRule>lambdaUpdate().set(EcomAutoRule::getRemark, r.getRemark())
                .set(EcomAutoRule::getTemplateCode, r.getTemplateCode()).eq(EcomAutoRule::getId, id));
        ruleCache.invalidate();
    }

    public void changeStatus(Long id, EnableStatus status) {
        require(id);
        ruleMapper.update(Wrappers.<EcomAutoRule>lambdaUpdate().set(EcomAutoRule::getStatus, status).eq(EcomAutoRule::getId, id));
        ruleCache.invalidate();
    }

    /** 删除规则（执行日志保留，规则名称已记录在日志中） */
    public void delete(List<Long> ids) {
        ruleMapper.deleteByIds(ids);
        ruleCache.invalidate();
    }

    /** 复制为新规则（停用状态，名称加「副本」） */
    public Long copy(Long id) {
        EcomAutoRule src = require(id);
        EcomAutoRule r = new EcomAutoRule();
        r.setName(AutoText.cut(Messages.get("ecom.text.auto.copyName", src.getName()), 64));
        r.setTriggerCode(src.getTriggerCode());
        r.setStatus(EnableStatus.DISABLED);
        r.setScope(src.getScope());
        r.setCondition(src.getCondition());
        r.setActions(src.getActions().stream().map(a -> StringUtils.hasText(a.secret()) ? a.withSecret(AutoRuleCodec.MASK) : a).toList());
        r.setDebounceHours(src.getDebounceHours());
        r.setDailyLimit(src.getDailyLimit());
        r.setTemplateCode(src.getTemplateCode());
        r.setRemark(src.getRemark());
        normalize(r);
        codec.write(r, src.getActions());
        r.setTriggerCount(0);
        ruleMapper.insert(r);
        ruleCache.invalidate();
        return r.getId();
    }

    /** 一键从模板创建（启用；通知与待办发给店铺负责人和当前用户） */
    public Long createFromTemplate(String code, List<Long> storeIds) {
        EcomAutoRule r = AutoTemplates.build(code, SecurityUtils.getUserIdOrNull());
        if (storeIds != null && !storeIds.isEmpty()) {
            r.setScope(new AutoRuleParts.Scope(storeIds, List.of(), List.of(), List.of(), List.of()));
        }
        return create(r);
    }

    /** 补全默认值并校验：条件字段属于触发器、运算符适用于字段类型、需要值的填写了值（数字字段为数字），动作适用于触发器且配置合法 */
    void normalize(EcomAutoRule r) {
        if (r.getTriggerCode() == null) {
            throw new BizException("ecom.error.auto.triggerRequired");
        }
        r.setName(r.getName() == null ? null : r.getName().trim());
        if (r.getStatus() == null) {
            r.setStatus(EnableStatus.ENABLED);
        }
        if (r.getDebounceHours() == null) {
            r.setDebounceHours(DEFAULT_DEBOUNCE_HOURS);
        }
        if (r.getDailyLimit() == null) {
            r.setDailyLimit(0);
        }
        r.setRemark(StringUtils.hasText(r.getRemark()) ? r.getRemark().trim() : null);
        r.setScope(normalizeScope(r.getScope()));
        Map<String, AutoField> fields = catalog.fields(r.getTriggerCode());
        r.setCondition(normalizeGroup(r.getCondition(), fields, false));
        if (r.getActions() == null || r.getActions().isEmpty()) {
            throw new BizException("ecom.error.auto.actionRequired");
        }
        for (AutoRuleParts.Action a : r.getActions()) {
            if (a.type() == null) {
                throw new BizException("ecom.error.auto.actionTypeRequired");
            }
            if (!a.type().supports(r.getTriggerCode())) {
                throw new BizException("ecom.error.auto.actionUnsupported", Messages.get(a.type().label()), Messages.get(r.getTriggerCode().label()));
            }
            catalog.handler(a.type()).validate(a);
        }
    }

    private static AutoRuleParts.Scope normalizeScope(AutoRuleParts.Scope s) {
        if (s == null) {
            return AutoRuleParts.Scope.empty();
        }
        return new AutoRuleParts.Scope(distinct(s.storeIds()), distinct(s.platforms()), trimmed(s.sites(), true), trimmed(s.skus(), false),
                trimmed(s.tags(), false));
    }

    private AutoRuleParts.Group normalizeGroup(AutoRuleParts.Group g, Map<String, AutoField> fields, boolean nested) {
        if (g == null) {
            return AutoRuleParts.Group.empty();
        }
        List<AutoRuleParts.Condition> items = new ArrayList<>();
        for (AutoRuleParts.Condition c : g.items() == null ? List.<AutoRuleParts.Condition>of() : g.items()) {
            AutoField f = fields.get(c.field());
            if (f == null) {
                throw new BizException("ecom.error.auto.fieldInvalid", c.field());
            }
            if (c.operator() == null || !c.operator().supports(f.type())) {
                throw new BizException("ecom.error.auto.operatorInvalid", Messages.get(f.label()));
            }
            String value = c.value() == null ? null : c.value().trim();
            if (c.operator().needsValue()) {
                if (!StringUtils.hasText(value)) {
                    throw new BizException("ecom.error.auto.valueRequired", Messages.get(f.label()));
                }
                if (f.type() == AutoFieldType.NUMBER && !isNumber(value)) {
                    throw new BizException("ecom.error.auto.numberInvalid", Messages.get(f.label()));
                }
            } else {
                value = null;
            }
            items.add(new AutoRuleParts.Condition(c.field(), c.operator(), value));
        }
        List<AutoRuleParts.Group> groups = new ArrayList<>();
        if (g.groups() != null) {
            for (AutoRuleParts.Group sub : g.groups()) {
                if (nested) {
                    throw new BizException("ecom.error.auto.groupTooDeep");
                }
                AutoRuleParts.Group n = normalizeGroup(sub, fields, true);
                if (!AutoConditions.isEmpty(n)) {
                    groups.add(n);
                }
            }
        }
        return new AutoRuleParts.Group(g.logic() == null ? AutoLogic.AND : g.logic(), items, groups);
    }

    private static boolean isNumber(String v) {
        try {
            new BigDecimal(v);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static <T> List<T> distinct(List<T> l) {
        return l == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(l.stream().filter(java.util.Objects::nonNull).toList()));
    }

    private static List<String> trimmed(List<String> l, boolean upper) {
        if (l == null) {
            return List.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String s : l) {
            if (StringUtils.hasText(s)) {
                out.add(upper ? s.trim().toUpperCase(Locale.ROOT) : s.trim());
            }
        }
        return new ArrayList<>(out);
    }

    // ---------------------------------------------------------------- 试运行与立即执行

    /**
     * 试运行（不保存规则、不执行动作、不写日志）：样本依次取该触发器最近执行日志中的事件、当前的真实数据，都没有时用样例数据，
     * 每个样本给出是否在范围内、条件判断说明、正式执行时是否会被防抖跳过，以及每个动作将执行的内容
     */
    public AutoViews.TestResult test(EcomAutoRule r) {
        normalize(r);
        Locale locale = LocaleContextHolder.getLocale();
        List<AutoViews.TestSample> samples = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (EcomAutoLog l : logMapper.selectRecentEvents(r.getTriggerCode(), TEST_SAMPLES * 4)) {
            if (samples.size() >= TEST_SAMPLES || !seen.add(l.getObjectKey())) {
                continue;
            }
            samples.add(engine.test(r, AutoStoredEvent.parse(r.getTriggerCode(), l.getObjectKey(), l.getEventJson()), "LOG", locale));
        }
        if (samples.size() < TEST_SAMPLES) {
            for (AutoEvent e : catalog.source(r.getTriggerCode()).recent(r.getTriggerCode(), TEST_SAMPLES * 2)) {
                if (samples.size() >= TEST_SAMPLES) {
                    break;
                }
                if (seen.add(e.objectKey())) {
                    samples.add(engine.test(r, e, "REAL", locale));
                }
            }
        }
        if (samples.isEmpty()) {
            samples.add(engine.test(r, sample(r.getTriggerCode()), "SAMPLE", locale));
        }
        long matched = samples.stream().filter(s -> s.inScope() && s.matched()).count();
        long run = samples.stream().filter(s -> s.inScope() && s.matched() && !s.debounced()).count();
        return new AutoViews.TestResult(samples, Messages.get("ecom.text.auto.testSummary", samples.size(), matched, run));
    }

    /** 样例事件（字段的样例值） */
    public AutoEvent sample(AutoTrigger trigger) {
        AutoEvent.Builder b = AutoEvent.builder(trigger, "SAMPLE").label(Messages.get("ecom.text.auto.sampleObject"));
        for (AutoField f : catalog.fields(trigger).values()) {
            Object v = f.sample();
            if (f.type() == AutoFieldType.NUMBER && v != null && !(v instanceof BigDecimal)) {
                v = new BigDecimal(v.toString());
            }
            if (f.type() == AutoFieldType.TEXT && v instanceof String s && s.contains(",")) {
                v = Arrays.asList(s.split(","));
            }
            b.field(f.key(), v);
        }
        return b.build();
    }

    /** 立即执行（扫描类触发器）：扫描当前数据，按这条规则处理（防抖、上限照常生效），返回事件数与执行次数 */
    public AutoViews.RunResult runNow(Long id) {
        EcomAutoRule r = require(id);
        if (!r.getTriggerCode().scan()) {
            throw new BizException("ecom.error.auto.realtimeNoRun");
        }
        List<AutoEvent> events = catalog.source(r.getTriggerCode()).scan(r.getTriggerCode());
        int executed = 0;
        for (AutoEvent e : events) {
            EcomAutoLog l = engine.process(r, e);
            if (l != null && l.getResult().executed()) {
                executed++;
            }
        }
        return new AutoViews.RunResult(events.size(), executed, Messages.get("ecom.text.auto.runResult", events.size(), executed));
    }
}
