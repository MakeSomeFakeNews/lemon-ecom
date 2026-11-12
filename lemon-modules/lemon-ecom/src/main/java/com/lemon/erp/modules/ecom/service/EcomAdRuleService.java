package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.ads.AdsChange;
import com.lemon.erp.modules.ecom.ads.AdsChangeResult;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.dto.AdsViews;
import com.lemon.erp.modules.ecom.entity.EcomAdCampaign;
import com.lemon.erp.modules.ecom.entity.EcomAdGroup;
import com.lemon.erp.modules.ecom.entity.EcomAdRule;
import com.lemon.erp.modules.ecom.entity.EcomAdRuleLog;
import com.lemon.erp.modules.ecom.entity.EcomAdSchedule;
import com.lemon.erp.modules.ecom.entity.EcomAdTarget;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomAdCampaignMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdGroupMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdRuleLogMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdRuleMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdTargetMapper;
import com.lemon.erp.modules.ecom.model.EcomAdAction;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.model.EcomAdLogSource;
import com.lemon.erp.modules.ecom.model.EcomAdLogStatus;
import com.lemon.erp.modules.ecom.model.EcomAdMetric;
import com.lemon.erp.modules.ecom.model.EcomAdOperator;
import com.lemon.erp.modules.ecom.model.EcomAdRunMode;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 广告规则：维护、预览（只计算建议不保存）、执行（写执行日志；自动执行时经广告接口修改）、处理建议（执行 / 标记手工执行 / 忽略）。
 * <p>
 * 计算：对象 = 规则层级下未归档的活动 / 广告组 / 投放（店铺、活动名称包含、活动标签任一、广告类型筛选；调整竞价 / 预算、仅提醒只针对启用的对象，
 * 暂停只针对启用的、启用只针对暂停的）；指标 = 统计窗口（近 N 天，截至昨天）内的合计（{@link EcomAdAnalysisService#metrics}），
 * 窗口内没有数据的对象指标为 0；全部条件满足时生成建议。比率在分母为 0 时视为没有值、条件不满足，
 * 例外：有花费没有销售额时 ACOS 视为无穷大（「ACOS 大于 x」成立）。
 * 竞价按基准竞价调整（百分比或固定值，限制在规则的上下限和平台最低竞价之间），有分时系数时平台上的竞价 = 新基准竞价 × 当前系数。
 * 同一规则同一对象当天已有日志、或有待处理的建议时不再生成。
 * <p>
 * 执行：平台接口调用不在事务中；按店铺分批提交，之后在事务中写日志、更新本地数据；店铺没有广告接口或对象没有平台 ID 时保留为待处理的建议。
 * 执行后给负责人发站内消息（规则负责人，没有时发给涉及店铺的负责人，都没有时发给有规则查看权限的用户）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomAdRuleService {

    static final String LIST_PERM = "ecom:adRule:list";
    static final String LINK = "/ecom/ad-rule";

    private final EcomAdRuleMapper ruleMapper;
    private final EcomAdRuleLogMapper logMapper;
    private final EcomAdCampaignMapper campaignMapper;
    private final EcomAdGroupMapper groupMapper;
    private final EcomAdTargetMapper targetMapper;
    private final EcomAdAnalysisService analysisService;
    private final EcomAdChangeService changeService;
    private final EcomAdStructureService structureService;
    private final EcomStoreService storeService;
    private final MessageSender messageSender;
    private final TransactionTemplate transactionTemplate;

    // ---------------------------------------------------------------- 维护

    public PageResult<EcomAdRule> page(long current, long size, String keyword, EnableStatus status) {
        Page<EcomAdRule> page = Page.of(current, size);
        ruleMapper.selectPageList(page, keyword == null ? null : keyword.trim(), status);
        page.getRecords().forEach(EcomAdRuleService::readConditions);
        return PageResult.of(page);
    }

    public EcomAdRule get(Long id) {
        EcomAdRule r = id == null ? null : ruleMapper.selectById(id);
        if (r == null) {
            throw new BizException("ecom.error.ads.ruleNotFound");
        }
        return readConditions(r);
    }

    public Long create(EcomAdRule r) {
        r.setId(null);
        normalize(r);
        if (r.getStatus() == null) {
            r.setStatus(EnableStatus.ENABLED);
        }
        r.setLastRunTime(null);
        r.setLastRunMessage(null);
        ruleMapper.insert(r);
        return r.getId();
    }

    public void update(Long id, EcomAdRule r) {
        EcomAdRule old = get(id);
        normalize(r);
        r.setId(id);
        r.setStatus(null);
        r.setLastRunTime(null);
        r.setLastRunMessage(null);
        ruleMapper.updateById(r);
        // 可清空的字段
        ruleMapper.update(Wrappers.<EcomAdRule>lambdaUpdate().set(EcomAdRule::getStoreIds, r.getStoreIds())
                .set(EcomAdRule::getCampaignKeyword, r.getCampaignKeyword()).set(EcomAdRule::getTags, r.getTags())
                .set(EcomAdRule::getAdType, r.getAdType()).set(EcomAdRule::getActionValue, r.getActionValue())
                .set(EcomAdRule::getMinValue, r.getMinValue()).set(EcomAdRule::getMaxValue, r.getMaxValue())
                .set(EcomAdRule::getOwnerId, r.getOwnerId()).set(EcomAdRule::getRemark, r.getRemark()).eq(EcomAdRule::getId, old.getId()));
    }

    public void changeStatus(Long id, EnableStatus status) {
        get(id);
        ruleMapper.update(Wrappers.<EcomAdRule>lambdaUpdate().set(EcomAdRule::getStatus, status).eq(EcomAdRule::getId, id));
    }

    /** 删除规则（执行日志保留，待处理的建议改为已忽略） */
    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        logMapper.update(Wrappers.<EcomAdRuleLog>lambdaUpdate().set(EcomAdRuleLog::getStatus, EcomAdLogStatus.IGNORED)
                .in(EcomAdRuleLog::getRuleId, ids).eq(EcomAdRuleLog::getStatus, EcomAdLogStatus.PENDING));
        ruleMapper.deleteByIds(ids);
    }

    /** 校验并规范化：动作与层级匹配、动作的值、上下限、执行时间；条件序列化为 JSON */
    static void normalize(EcomAdRule r) {
        r.setName(r.getName().trim());
        EcomAdAction a = r.getAction();
        if (a.isBudget() && r.getLevel() != EcomAdLevel.CAMPAIGN) {
            throw new BizException("ecom.error.ads.budgetOnlyCampaign");
        }
        if (a.isBid() && r.getLevel() == EcomAdLevel.CAMPAIGN) {
            throw new BizException("ecom.error.ads.bidNotCampaign");
        }
        if (a == EcomAdAction.BID_PERCENT || a == EcomAdAction.BUDGET_PERCENT) {
            BigDecimal v = r.getActionValue();
            if (v == null || v.signum() == 0 || v.compareTo(BigDecimal.valueOf(-90)) < 0 || v.compareTo(BigDecimal.valueOf(500)) > 0) {
                throw new BizException("ecom.error.ads.percentRange");
            }
        } else if (a == EcomAdAction.BID_SET || a == EcomAdAction.BUDGET_SET) {
            if (r.getActionValue() == null || r.getActionValue().signum() <= 0) {
                throw new BizException("ecom.error.ads.actionValueRequired");
            }
        } else {
            r.setActionValue(null);
        }
        if (!a.isBid() && !a.isBudget()) {
            r.setMinValue(null);
            r.setMaxValue(null);
        }
        if (r.getMinValue() != null && r.getMaxValue() != null && r.getMinValue().compareTo(r.getMaxValue()) > 0) {
            throw new BizException("ecom.error.ads.minMaxInvalid");
        }
        if (r.getRunMode() == EcomAdRunMode.DAILY && r.getRunHour() == null) {
            throw new BizException("ecom.error.ads.runHourRequired");
        }
        if (r.getRunHour() == null) {
            r.setRunHour(6);
        }
        r.setAutoApply(Integer.valueOf(1).equals(r.getAutoApply()) ? 1 : 0);
        r.setStoreIds(joinIds(r.getStoreIds()));
        r.setCampaignKeyword(StringUtils.hasText(r.getCampaignKeyword()) ? r.getCampaignKeyword().trim() : null);
        r.setTags(EcomAdStructureService.normalizeTags(r.getTags()));
        r.setConditionsJson(HttpJson.MAPPER.writeValueAsString(r.getConditions()));
    }

    static EcomAdRule readConditions(EcomAdRule r) {
        if (StringUtils.hasText(r.getConditionsJson())) {
            r.setConditions(HttpJson.MAPPER.readValue(r.getConditionsJson(), new TypeReference<List<EcomAdRule.Condition>>() {
            }));
        }
        return r;
    }

    private static String joinIds(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        List<String> ids = Arrays.stream(s.split(",")).map(String::trim).filter(x -> x.matches("\\d+")).distinct().toList();
        return ids.isEmpty() ? null : String.join(",", ids);
    }

    private static List<Long> storeIds(EcomAdRule r) {
        return r.getStoreIds() == null ? null : Arrays.stream(r.getStoreIds().split(",")).map(Long::valueOf).toList();
    }

    // ---------------------------------------------------------------- 计算

    /** 预览：按当前数据计算建议，不保存 */
    public List<AdsViews.Suggestion> preview(Long id) {
        return evaluate(get(id), LocalDate.now());
    }

    /** 计算建议（today 用于确定统计窗口：today − N ～ today − 1） */
    public List<AdsViews.Suggestion> evaluate(EcomAdRule rule, LocalDate today) {
        if (rule.getConditions() == null || rule.getConditions().isEmpty()) {
            readConditions(rule);
        }
        List<Long> stores = storeIds(rule);
        EcomAdAnalysisService.Range window = new EcomAdAnalysisService.Range(today.minusDays(rule.getWindowDays()), today.minusDays(1));
        List<AdsViews.RuleEntity> entities = switch (rule.getLevel()) {
            case CAMPAIGN -> campaignMapper.selectEntities(stores, rule.getCampaignKeyword(), rule.getAdType(), null, false);
            case AD_GROUP -> groupMapper.selectEntities(stores, rule.getCampaignKeyword(), rule.getAdType(), null, false);
            case TARGET -> targetMapper.selectEntities(stores, rule.getCampaignKeyword(), rule.getAdType(), null, null, false);
        };
        Set<String> tags = rule.getTags() == null ? Set.of() : new HashSet<>(Arrays.asList(rule.getTags().split(",")));
        Map<Long, AdsViews.Metrics> metrics = analysisService.metrics(rule.getLevel(), stores, null, null, window);
        Map<Long, EcomStore> storeCache = new HashMap<>();
        Map<Long, EcomAdSchedule> scheduleCache = new HashMap<>();
        List<AdsViews.Suggestion> out = new ArrayList<>();
        for (AdsViews.RuleEntity e : entities) {
            if (!tags.isEmpty() && (e.getCampaignTags() == null || Arrays.stream(e.getCampaignTags().split(",")).noneMatch(tags::contains))) {
                continue;
            }
            if (!applicable(rule.getAction(), e.getStatus())) {
                continue;
            }
            AdsViews.Metrics m = new AdsViews.Metrics().add(metrics.get(e.getId())).fill();
            if (!matches(rule.getConditions(), m)) {
                continue;
            }
            EcomStore store = storeCache.computeIfAbsent(e.getStoreId(), storeService::get);
            AdsViews.Suggestion s = suggestion(rule, e, m, changeService.minBid(store),
                    e.getScheduleFactor() == null ? null : scheduleCache.computeIfAbsent(e.getCampaignId(), changeService::activeSchedule));
            if (s != null) {
                out.add(s);
            }
        }
        return out;
    }

    private static boolean applicable(EcomAdAction action, EcomAdStatus status) {
        return switch (action) {
            case ENABLE -> status == EcomAdStatus.PAUSED;
            default -> status == EcomAdStatus.ENABLED;
        };
    }

    /** 全部条件满足 */
    public static boolean matches(List<EcomAdRule.Condition> conditions, AdsViews.Metrics m) {
        for (EcomAdRule.Condition c : conditions) {
            if (!compare(value(m, c.getMetric()), c.getOperator(), c.getValue())) {
                return false;
            }
        }
        return !conditions.isEmpty();
    }

    /** 指标值：比率分母为 0 时为 null（条件不满足）；有花费没有销售额的 ACOS 为无穷大 */
    public static Double value(AdsViews.Metrics m, EcomAdMetric metric) {
        return switch (metric) {
            case SPEND -> m.getSpend().doubleValue();
            case SALES -> m.getSales().doubleValue();
            case CLICKS -> (double) m.getClicks();
            case IMPRESSIONS -> (double) m.getImpressions();
            case ORDERS -> (double) m.getOrders();
            case ACOS -> m.getSpend().signum() == 0 ? null : m.getSales().signum() == 0 ? Double.POSITIVE_INFINITY : m.getAcos().doubleValue();
            case ROAS -> m.getRoas() == null ? null : m.getRoas().doubleValue();
            case CTR -> m.getCtr() == null ? null : m.getCtr().doubleValue();
            case CPC -> m.getCpc() == null ? null : m.getCpc().doubleValue();
            case CVR -> m.getCvr() == null ? null : m.getCvr().doubleValue();
        };
    }

    public static boolean compare(Double v, EcomAdOperator op, BigDecimal target) {
        if (v == null || target == null) {
            return false;
        }
        double t = target.doubleValue();
        return switch (op) {
            case GT -> v > t;
            case GTE -> v >= t;
            case LT -> v < t;
            case LTE -> v <= t;
            case EQ -> Math.abs(v - t) < 1e-9;
        };
    }

    /** 按动作计算新值，没有变化时返回 null */
    static AdsViews.Suggestion suggestion(EcomAdRule rule, AdsViews.RuleEntity e, AdsViews.Metrics m, BigDecimal minBid, EcomAdSchedule schedule) {
        AdsViews.Suggestion s = new AdsViews.Suggestion();
        s.setLevel(e.getLevel());
        s.setEntityId(e.getId());
        s.setStoreId(e.getStoreId());
        s.setStoreName(e.getStoreName());
        s.setCurrency(e.getCurrency());
        s.setCampaignId(e.getCampaignId());
        s.setCampaignName(e.getCampaignName());
        s.setEntityName(e.getName());
        s.setAction(rule.getAction());
        s.setMetrics(m);
        s.setPlatformId(e.getPlatformId());
        s.setTargetType(e.getTargetType());
        EcomAdAction a = rule.getAction();
        if (a.isBid()) {
            BigDecimal base = e.getBaseBid() != null ? e.getBaseBid() : e.getBid();
            if (base == null && a == EcomAdAction.BID_PERCENT) {
                return null;
            }
            BigDecimal nv = adjust(a == EcomAdAction.BID_PERCENT, base, rule.getActionValue(), rule.getMinValue(), rule.getMaxValue());
            if (minBid != null && nv.compareTo(minBid) < 0) {
                nv = minBid;
            }
            if (base != null && nv.compareTo(base) == 0) {
                return null;
            }
            s.setOldValue(base);
            s.setNewValue(nv);
            s.setEffectiveBid(e.getLevel() == EcomAdLevel.TARGET ? EcomAdChangeService.effectiveBid(nv, e.getScheduleFactor(), schedule, minBid) : nv);
        } else if (a.isBudget()) {
            BigDecimal old = e.getBudget();
            if (old == null && a == EcomAdAction.BUDGET_PERCENT) {
                return null;
            }
            BigDecimal nv = adjust(a == EcomAdAction.BUDGET_PERCENT, old, rule.getActionValue(), rule.getMinValue(), rule.getMaxValue());
            if (nv.signum() <= 0 || (old != null && nv.compareTo(old) == 0)) {
                return null;
            }
            s.setOldValue(old);
            s.setNewValue(nv);
        } else if (a == EcomAdAction.PAUSE || a == EcomAdAction.ENABLE) {
            s.setOldStatus(e.getStatus());
            s.setNewStatus(a == EcomAdAction.PAUSE ? EcomAdStatus.PAUSED : EcomAdStatus.ENABLED);
        }
        return s;
    }

    /** 百分比：原值 × (1 + v / 100)；固定值：v；再限制在 [min, max]，保留两位小数 */
    public static BigDecimal adjust(boolean percent, BigDecimal old, BigDecimal v, BigDecimal min, BigDecimal max) {
        BigDecimal nv = percent ? old.multiply(BigDecimal.ONE.add(v.divide(Decimals.HUNDRED, 6, RoundingMode.HALF_UP))) : v;
        if (min != null && nv.compareTo(min) < 0) {
            nv = min;
        }
        if (max != null && nv.compareTo(max) > 0) {
            nv = max;
        }
        return nv.setScale(2, RoundingMode.HALF_UP);
    }

    // ---------------------------------------------------------------- 执行

    /** 手工执行（页面「立即执行」） */
    public AdsViews.RunResult run(Long id) {
        return execute(get(id), LocalDateTime.now());
    }

    /** 定时任务：执行到点的每日规则（今天还没执行过的），单条失败不影响其他 */
    public List<String> runDue(LocalDateTime now) {
        List<String> results = new ArrayList<>();
        List<EcomAdRule> rules = ruleMapper.selectList(Wrappers.<EcomAdRule>lambdaQuery().eq(EcomAdRule::getStatus, EnableStatus.ENABLED)
                .eq(EcomAdRule::getRunMode, EcomAdRunMode.DAILY).eq(EcomAdRule::getRunHour, now.getHour()).orderByAsc(EcomAdRule::getId));
        for (EcomAdRule r : rules) {
            if (r.getLastRunTime() != null && !r.getLastRunTime().toLocalDate().isBefore(now.toLocalDate())) {
                continue;
            }
            try {
                AdsViews.RunResult res = execute(readConditions(r), now);
                results.add(r.getName() + ": " + res.message());
            } catch (RuntimeException e) {
                if (!(e instanceof BizException)) {
                    log.warn("Ad rule {} failed", r.getId(), e);
                }
                markRun(r.getId(), now, e.getMessage());
                results.add(r.getName() + ": " + e.getMessage());
            }
        }
        return results;
    }

    /** 计算 → （自动执行时）调用广告接口 → 写日志与本地数据 → 发消息 */
    public AdsViews.RunResult execute(EcomAdRule rule, LocalDateTime now) {
        List<AdsViews.Suggestion> all = evaluate(rule, now.toLocalDate());
        Set<String> done = new HashSet<>();
        for (EcomAdRuleLog l : logMapper.selectList(Wrappers.<EcomAdRuleLog>lambdaQuery().select(EcomAdRuleLog::getLevel, EcomAdRuleLog::getEntityId)
                .eq(EcomAdRuleLog::getRuleId, rule.getId())
                .and(w -> w.eq(EcomAdRuleLog::getStatus, EcomAdLogStatus.PENDING).or().ge(EcomAdRuleLog::getRunTime, now.toLocalDate().atStartOfDay())))) {
            done.add(l.getLevel() + "|" + l.getEntityId());
        }
        List<AdsViews.Suggestion> suggestions = all.stream().filter(s -> !done.contains(s.getLevel() + "|" + s.getEntityId())).toList();
        int skipped = all.size() - suggestions.size();
        boolean auto = Integer.valueOf(1).equals(rule.getAutoApply()) && rule.getAction() != EcomAdAction.NOTIFY;
        Map<Long, List<AdsViews.Suggestion>> byStore = new LinkedHashMap<>();
        suggestions.forEach(s -> byStore.computeIfAbsent(s.getStoreId(), k -> new ArrayList<>()).add(s));
        List<EcomAdRuleLog> logs = new ArrayList<>();
        Set<Long> storeIds = new LinkedHashSet<>(byStore.keySet());
        for (Map.Entry<Long, List<AdsViews.Suggestion>> e : byStore.entrySet()) {
            EcomStore store = storeService.get(e.getKey());
            Map<Long, AdsChangeResult> results = Map.of();
            if (auto) {
                List<AdsChange> changes = e.getValue().stream().map(EcomAdRuleService::change).toList();
                results = changeService.push(store, changes).results();
            }
            for (AdsViews.Suggestion s : e.getValue()) {
                EcomAdRuleLog l = toLog(rule.getId(), s, now);
                if (rule.getAction() == EcomAdAction.NOTIFY) {
                    l.setStatus(EcomAdLogStatus.NOTIFIED);
                } else if (!auto) {
                    l.setStatus(EcomAdLogStatus.PENDING);
                } else {
                    AdsChangeResult r = results.get(s.getEntityId());
                    if (r == null) {
                        l.setStatus(EcomAdLogStatus.PENDING);
                        l.setMessage(Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.ads.noApi"));
                    } else if (r.success()) {
                        l.setStatus(EcomAdLogStatus.APPLIED);
                        l.setAppliedTime(now);
                    } else {
                        l.setStatus(EcomAdLogStatus.FAILED);
                        l.setMessage(truncate(r.message(), 500));
                    }
                }
                logs.add(l);
            }
        }
        int applied = (int) logs.stream().filter(l -> l.getStatus() == EcomAdLogStatus.APPLIED).count();
        int pending = (int) logs.stream().filter(l -> l.getStatus() == EcomAdLogStatus.PENDING).count();
        int failed = (int) logs.stream().filter(l -> l.getStatus() == EcomAdLogStatus.FAILED).count();
        int notified = (int) logs.stream().filter(l -> l.getStatus() == EcomAdLogStatus.NOTIFIED).count();
        String message = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.ads.runSummary", logs.size(), applied, pending, failed, notified, skipped);
        Map<Long, AdsViews.Suggestion> byEntity = new HashMap<>();
        suggestions.forEach(s -> byEntity.put(s.getEntityId(), s));
        transactionTemplate.executeWithoutResult(st -> {
            for (int i = 0; i < logs.size(); i += 500) {
                logMapper.insertBatch(logs.subList(i, Math.min(logs.size(), i + 500)));
            }
            for (EcomAdRuleLog l : logs) {
                if (l.getStatus() == EcomAdLogStatus.APPLIED) {
                    applyLocal(l.getLevel(), l.getEntityId(), l.getAction(), l.getNewValue(), byEntity.get(l.getEntityId()).getEffectiveBid(), l.getNewStatus());
                }
            }
            markRun(rule.getId(), now, message);
            if (!logs.isEmpty()) {
                notifyOwners(rule, storeIds, message, now);
            }
        });
        return new AdsViews.RunResult(logs.size(), applied, pending, failed, notified, skipped, message);
    }

    private static AdsChange change(AdsViews.Suggestion s) {
        EcomAdAction a = s.getAction();
        BigDecimal bid = a.isBid() ? (s.getEffectiveBid() != null ? s.getEffectiveBid() : s.getNewValue()) : null;
        BigDecimal budget = a.isBudget() ? s.getNewValue() : null;
        return new AdsChange(s.getEntityId(), s.getLevel(), s.getPlatformId(), s.getTargetType(), bid, budget, s.getNewStatus());
    }

    private static EcomAdRuleLog toLog(Long ruleId, AdsViews.Suggestion s, LocalDateTime now) {
        EcomAdRuleLog l = new EcomAdRuleLog();
        l.setRuleId(ruleId);
        l.setSource(EcomAdLogSource.RULE);
        l.setRunTime(now);
        l.setStoreId(s.getStoreId());
        l.setLevel(s.getLevel());
        l.setEntityId(s.getEntityId());
        l.setCampaignId(s.getCampaignId());
        l.setEntityName(truncate(s.getEntityName(), 255));
        l.setAction(s.getAction());
        l.setOldValue(s.getOldValue());
        l.setNewValue(s.getNewValue());
        l.setOldStatus(s.getOldStatus());
        l.setNewStatus(s.getNewStatus());
        AdsViews.Metrics m = s.getMetrics();
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("impressions", m.getImpressions());
        snap.put("clicks", m.getClicks());
        snap.put("spend", m.getSpend());
        snap.put("sales", m.getSales());
        snap.put("orders", m.getOrders());
        snap.put("acos", m.getAcos());
        snap.put("roas", m.getRoas());
        snap.put("ctr", m.getCtr());
        snap.put("cpc", m.getCpc());
        snap.put("cvr", m.getCvr());
        if (s.getEffectiveBid() != null && s.getNewValue() != null && s.getEffectiveBid().compareTo(s.getNewValue()) != 0) {
            snap.put("effectiveBid", s.getEffectiveBid());
        }
        l.setMetrics(truncate(HttpJson.MAPPER.writeValueAsString(snap), 1000));
        return l;
    }

    /** 更新本地数据：竞价（投放按基准 + 实际竞价）、预算、状态 */
    private void applyLocal(EcomAdLevel level, Long id, EcomAdAction action, BigDecimal newValue, BigDecimal effectiveBid, EcomAdStatus newStatus) {
        if (action.isBid()) {
            if (level == EcomAdLevel.TARGET) {
                structureService.setTargetBid(id, newValue, effectiveBid == null ? newValue : effectiveBid);
            } else {
                structureService.setGroupBid(id, newValue);
            }
        } else if (action.isBudget()) {
            structureService.setCampaignBudget(id, newValue);
        } else if (newStatus != null) {
            structureService.setStatus(level, id, newStatus);
        }
    }

    private void markRun(Long ruleId, LocalDateTime now, String message) {
        ruleMapper.update(Wrappers.<EcomAdRule>lambdaUpdate().set(EcomAdRule::getLastRunTime, now)
                .set(EcomAdRule::getLastRunMessage, truncate(message, 500)).eq(EcomAdRule::getId, ruleId));
    }

    private void notifyOwners(EcomAdRule rule, Set<Long> storeIds, String summary, LocalDateTime now) {
        String title = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.ads.ruleNotifyTitle", rule.getName());
        MessageSender.Message msg = MessageSender.Message.of(MessageCategory.REMIND, title, summary, LINK)
                .dedup("ecom-ad-rule:" + rule.getId() + ":" + now.toLocalDate());
        Set<Long> users = new LinkedHashSet<>();
        if (rule.getOwnerId() != null) {
            users.add(rule.getOwnerId());
        } else {
            for (Long sid : storeIds) {
                Long owner = storeService.get(sid).getOwnerId();
                if (owner != null) {
                    users.add(owner);
                }
            }
        }
        if (users.isEmpty()) {
            messageSender.sendToPermission(LIST_PERM, msg);
        } else {
            messageSender.send(users, msg);
        }
    }

    // ---------------------------------------------------------------- 执行日志与建议

    public PageResult<EcomAdRuleLog> logs(long current, long size, Long ruleId, Long storeId, EcomAdLogStatus status, EcomAdLogSource source,
                                          LocalDate begin, LocalDate end, String keyword) {
        Page<EcomAdRuleLog> page = Page.of(current, size);
        logMapper.selectPageList(page, ruleId, storeId, status, source, begin == null ? null : begin.atStartOfDay(),
                end == null ? null : end.plusDays(1).atStartOfDay(), keyword == null || keyword.isBlank() ? null : keyword.trim());
        return PageResult.of(page);
    }

    /**
     * 执行待处理的建议：按日志中的新值（竞价为基准竞价，按对象当前的分时系数换算实际竞价）调用广告接口；
     * 店铺没有广告接口或对象没有平台 ID 的保持待处理（请在平台后台修改后「标记已手工执行」）
     */
    public AdsViews.RunResult applyLogs(List<Long> ids) {
        List<EcomAdRuleLog> logs = pendingLogs(ids);
        Map<Long, List<EcomAdRuleLog>> byStore = new LinkedHashMap<>();
        logs.forEach(l -> byStore.computeIfAbsent(l.getStoreId(), k -> new ArrayList<>()).add(l));
        int applied = 0;
        int failed = 0;
        int pending = 0;
        LocalDateTime now = LocalDateTime.now();
        Long userId = SecurityUtils.getUserIdOrNull();
        for (Map.Entry<Long, List<EcomAdRuleLog>> e : byStore.entrySet()) {
            EcomStore store = storeService.get(e.getKey());
            BigDecimal minBid = changeService.minBid(store);
            Map<Long, BigDecimal> effective = new HashMap<>();
            List<AdsChange> changes = new ArrayList<>();
            for (EcomAdRuleLog l : e.getValue()) {
                AdsChange c = currentChange(l, minBid, effective);
                if (c != null) {
                    changes.add(c);
                }
            }
            Map<Long, AdsChangeResult> results = changeService.push(store, changes).results();
            List<Runnable> updates = new ArrayList<>();
            for (EcomAdRuleLog l : e.getValue()) {
                AdsChangeResult r = results.get(l.getEntityId());
                if (r == null) {
                    pending++;
                    continue;
                }
                if (r.success()) {
                    applied++;
                    updates.add(() -> {
                        applyLocal(l.getLevel(), l.getEntityId(), l.getAction(), l.getNewValue(), effective.get(l.getEntityId()), l.getNewStatus());
                        setLogStatus(l.getId(), EcomAdLogStatus.APPLIED, null, now, userId);
                    });
                } else {
                    failed++;
                    updates.add(() -> setLogStatus(l.getId(), EcomAdLogStatus.FAILED, truncate(r.message(), 500), now, userId));
                }
            }
            transactionTemplate.executeWithoutResult(st -> updates.forEach(Runnable::run));
        }
        String message = Messages.get("ecom.text.ads.applySummary", applied, failed, pending);
        return new AdsViews.RunResult(logs.size(), applied, pending, failed, 0, 0, message);
    }

    /** 按日志构造修改；对象已不存在时返回 null（保持待处理） */
    private AdsChange currentChange(EcomAdRuleLog l, BigDecimal minBid, Map<Long, BigDecimal> effective) {
        String platformId;
        EcomAdTargetType targetType = null;
        BigDecimal bid = null;
        switch (l.getLevel()) {
            case CAMPAIGN -> {
                EcomAdCampaign c = campaignMapper.selectById(l.getEntityId());
                if (c == null) {
                    return null;
                }
                platformId = c.getPlatformCampaignId();
            }
            case AD_GROUP -> {
                EcomAdGroup g = groupMapper.selectById(l.getEntityId());
                if (g == null) {
                    return null;
                }
                platformId = g.getPlatformAdGroupId();
                bid = l.getAction().isBid() ? l.getNewValue() : null;
            }
            default -> {
                EcomAdTarget t = targetMapper.selectById(l.getEntityId());
                if (t == null) {
                    return null;
                }
                platformId = t.getPlatformTargetId();
                targetType = t.getTargetType();
                if (l.getAction().isBid()) {
                    bid = EcomAdChangeService.effectiveBid(l.getNewValue(), t.getScheduleFactor(),
                            t.getScheduleFactor() == null ? null : changeService.activeSchedule(t.getCampaignId()), minBid);
                }
            }
        }
        if (bid != null) {
            effective.put(l.getEntityId(), bid);
        }
        return new AdsChange(l.getEntityId(), l.getLevel(), platformId, targetType, bid, l.getAction().isBudget() ? l.getNewValue() : null,
                l.getNewStatus());
    }

    /** 标记已手工执行（已在平台后台修改）：更新本地数据 */
    @Transactional
    public int markManual(List<Long> ids) {
        List<EcomAdRuleLog> logs = pendingLogs(ids);
        LocalDateTime now = LocalDateTime.now();
        Long userId = SecurityUtils.getUserIdOrNull();
        for (EcomAdRuleLog l : logs) {
            BigDecimal eff = null;
            if (l.getAction().isBid() && l.getLevel() == EcomAdLevel.TARGET) {
                EcomAdTarget t = targetMapper.selectById(l.getEntityId());
                if (t != null) {
                    eff = EcomAdChangeService.effectiveBid(l.getNewValue(), t.getScheduleFactor(),
                            t.getScheduleFactor() == null ? null : changeService.activeSchedule(t.getCampaignId()), null);
                }
            }
            applyLocal(l.getLevel(), l.getEntityId(), l.getAction(), l.getNewValue(), eff, l.getNewStatus());
            setLogStatus(l.getId(), EcomAdLogStatus.MANUAL, null, now, userId);
        }
        return logs.size();
    }

    /** 忽略建议 */
    public int ignore(List<Long> ids) {
        List<EcomAdRuleLog> logs = pendingLogs(ids);
        LocalDateTime now = LocalDateTime.now();
        Long userId = SecurityUtils.getUserIdOrNull();
        logs.forEach(l -> setLogStatus(l.getId(), EcomAdLogStatus.IGNORED, null, now, userId));
        return logs.size();
    }

    private List<EcomAdRuleLog> pendingLogs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("ecom.error.ads.selectLogs");
        }
        List<EcomAdRuleLog> logs = logMapper.selectList(Wrappers.<EcomAdRuleLog>lambdaQuery().in(EcomAdRuleLog::getId, ids)
                .eq(EcomAdRuleLog::getStatus, EcomAdLogStatus.PENDING));
        if (logs.isEmpty()) {
            throw new BizException("ecom.error.ads.noPendingLogs");
        }
        return logs;
    }

    private void setLogStatus(Long id, EcomAdLogStatus status, String message, LocalDateTime time, Long userId) {
        logMapper.update(Wrappers.<EcomAdRuleLog>lambdaUpdate().set(EcomAdRuleLog::getStatus, status).set(EcomAdRuleLog::getMessage, message)
                .set(EcomAdRuleLog::getAppliedTime, time).set(EcomAdRuleLog::getAppliedBy, userId).eq(EcomAdRuleLog::getId, id));
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
