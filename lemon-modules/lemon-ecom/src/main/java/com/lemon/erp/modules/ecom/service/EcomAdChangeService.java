package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.ads.AdsCapability;
import com.lemon.erp.modules.ecom.ads.AdsChange;
import com.lemon.erp.modules.ecom.ads.AdsChangeResult;
import com.lemon.erp.modules.ecom.ads.AdsConnector;
import com.lemon.erp.modules.ecom.ads.AdsContext;
import com.lemon.erp.modules.ecom.entity.EcomAdCampaign;
import com.lemon.erp.modules.ecom.entity.EcomAdGroup;
import com.lemon.erp.modules.ecom.entity.EcomAdRuleLog;
import com.lemon.erp.modules.ecom.entity.EcomAdSchedule;
import com.lemon.erp.modules.ecom.entity.EcomAdTarget;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomAdRuleLogMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdScheduleMapper;
import com.lemon.erp.modules.ecom.model.EcomAdAction;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.model.EcomAdLogSource;
import com.lemon.erp.modules.ecom.model.EcomAdLogStatus;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 修改竞价 / 预算 / 状态：店铺有广告接口、对象有平台 ID 时先调用接口（不在事务中），成功后再在事务中更新本地数据并写执行日志；
 * 没有接口时只修改本地（视为已在平台后台手工修改）。规则自动执行、分时调价也经过 {@link #push}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomAdChangeService {

    private final EcomAdsAuthService authService;
    private final EcomAdStructureService structureService;
    private final EcomStoreService storeService;
    private final EcomAdRuleLogMapper logMapper;
    private final EcomAdScheduleMapper scheduleMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * 调用结果：available 表示店铺有可用的广告接口；results 只包含实际提交给接口的修改（没有平台 ID、接口不支持该能力的不提交），
     * 按 localId 对应
     */
    public record PushOutcome(boolean available, Map<Long, AdsChangeResult> results) {

        public AdsChangeResult result(Long localId) {
            return results.get(localId);
        }
    }

    /** 把修改提交给店铺的广告接口（调用方保证不在事务中） */
    public PushOutcome push(EcomStore store, List<AdsChange> changes) {
        Optional<AdsConnector> connector = authService.connector(store);
        if (connector.isEmpty() || changes.isEmpty()) {
            return new PushOutcome(connector.isPresent(), Map.of());
        }
        AdsConnector c = connector.get();
        List<AdsChange> pushable = changes.stream().filter(ch -> pushable(c, ch)).toList();
        Map<Long, AdsChangeResult> results = new HashMap<>();
        if (pushable.isEmpty()) {
            return new PushOutcome(true, results);
        }
        try {
            AdsContext ctx = authService.requireContext(store);
            for (AdsChangeResult r : c.applyChanges(ctx, pushable)) {
                results.put(r.localId(), r);
            }
        } catch (RuntimeException e) {
            if (!(e instanceof BizException)) {
                log.warn("Ads change failed for store {}", store.getCode(), e);
            }
            pushable.forEach(ch -> results.put(ch.localId(), AdsChangeResult.failed(ch.localId(), e.getMessage())));
        }
        pushable.forEach(ch -> results.putIfAbsent(ch.localId(), AdsChangeResult.failed(ch.localId(), "no result")));
        return new PushOutcome(true, results);
    }

    private static boolean pushable(AdsConnector c, AdsChange ch) {
        if (!StringUtils.hasText(ch.platformId())) {
            return false;
        }
        return (ch.bid() == null || c.supports(AdsCapability.UPDATE_BID)) && (ch.budget() == null || c.supports(AdsCapability.UPDATE_BUDGET))
                && (ch.status() == null || c.supports(AdsCapability.UPDATE_STATE));
    }

    /** 平台允许的最低竞价（没有接口时 0.01） */
    public BigDecimal minBid(EcomStore store) {
        return authService.connector(store).map(c -> c.info().minBid()).orElse(new BigDecimal("0.01"));
    }

    /** 活动启用的分时策略，没有为 null */
    public EcomAdSchedule activeSchedule(Long campaignId) {
        EcomAdSchedule s = scheduleMapper.selectOne(Wrappers.<EcomAdSchedule>lambdaQuery().eq(EcomAdSchedule::getCampaignId, campaignId));
        return s != null && s.getStatus() == EnableStatus.ENABLED ? s : null;
    }

    /**
     * 实际竞价：有分时系数时 = 基准竞价 × 系数（限制在策略的最低 / 最高竞价之间），否则 = 基准竞价；都不低于平台最低竞价
     */
    public static BigDecimal effectiveBid(BigDecimal base, BigDecimal factor, EcomAdSchedule schedule, BigDecimal minBid) {
        if (base == null) {
            return null;
        }
        BigDecimal v = base;
        if (factor != null && schedule != null) {
            v = base.multiply(factor);
            if (schedule.getMinBid() != null && v.compareTo(schedule.getMinBid()) < 0) {
                v = schedule.getMinBid();
            }
            if (schedule.getMaxBid() != null && v.compareTo(schedule.getMaxBid()) > 0) {
                v = schedule.getMaxBid();
            }
        }
        v = v.setScale(2, RoundingMode.HALF_UP);
        return minBid != null && v.compareTo(minBid) < 0 ? minBid : v;
    }

    // ---------------------------------------------------------------- 页面手工修改

    /**
     * 修改竞价（投放的基准竞价 / 广告组默认竞价）
     *
     * @return 是否已同步到平台（false 表示只修改了本地）
     */
    public boolean changeBid(EcomAdLevel level, Long id, BigDecimal bid) {
        if (bid == null || bid.signum() <= 0) {
            throw new BizException("ecom.error.ads.bidInvalid");
        }
        BigDecimal base = Decimals.money(bid);
        if (level == EcomAdLevel.TARGET) {
            EcomAdTarget t = structureService.target(id);
            if (t.getTargetType() == EcomAdTargetType.NEGATIVE) {
                throw new BizException("ecom.error.ads.negativeNoBid");
            }
            EcomStore store = storeService.get(t.getStoreId());
            BigDecimal min = minBid(store);
            if (base.compareTo(min) < 0) {
                throw new BizException("ecom.error.ads.bidBelowMin", min);
            }
            BigDecimal eff = effectiveBid(base, t.getScheduleFactor(), t.getScheduleFactor() == null ? null : activeSchedule(t.getCampaignId()), min);
            boolean pushed = pushOne(store, AdsChange.bid(t.getId(), EcomAdLevel.TARGET, t.getPlatformTargetId(), t.getTargetType(), eff));
            transactionTemplate.executeWithoutResult(s -> {
                structureService.setTargetBid(t.getId(), base, eff);
                writeManualLog(store, EcomAdLevel.TARGET, t.getId(), t.getCampaignId(), t.getTargetText(), EcomAdAction.BID_SET,
                        t.getBaseBid() == null ? t.getBid() : t.getBaseBid(), base, null, null, pushed);
            });
            return pushed;
        }
        if (level == EcomAdLevel.AD_GROUP) {
            EcomAdGroup g = structureService.group(id);
            EcomStore store = storeService.get(g.getStoreId());
            BigDecimal min = minBid(store);
            if (base.compareTo(min) < 0) {
                throw new BizException("ecom.error.ads.bidBelowMin", min);
            }
            boolean pushed = pushOne(store, AdsChange.bid(g.getId(), EcomAdLevel.AD_GROUP, g.getPlatformAdGroupId(), null, base));
            transactionTemplate.executeWithoutResult(s -> {
                structureService.setGroupBid(g.getId(), base);
                writeManualLog(store, EcomAdLevel.AD_GROUP, g.getId(), g.getCampaignId(), g.getName(), EcomAdAction.BID_SET, g.getDefaultBid(), base,
                        null, null, pushed);
            });
            return pushed;
        }
        throw new BizException("ecom.error.ads.levelInvalid");
    }

    /** 修改活动日预算 */
    public boolean changeBudget(Long campaignId, BigDecimal budget) {
        if (budget == null || budget.signum() <= 0) {
            throw new BizException("ecom.error.ads.budgetInvalid");
        }
        BigDecimal v = Decimals.money(budget);
        EcomAdCampaign c = structureService.campaign(campaignId);
        EcomStore store = storeService.get(c.getStoreId());
        boolean pushed = pushOne(store, new AdsChange(c.getId(), EcomAdLevel.CAMPAIGN, c.getPlatformCampaignId(), null, null, v, null));
        transactionTemplate.executeWithoutResult(s -> {
            structureService.setCampaignBudget(c.getId(), v);
            writeManualLog(store, EcomAdLevel.CAMPAIGN, c.getId(), c.getId(), c.getName(), EcomAdAction.BUDGET_SET, c.getDailyBudget(), v, null, null, pushed);
        });
        return pushed;
    }

    /** 暂停 / 启用（已归档的不能修改） */
    public boolean changeStatus(EcomAdLevel level, Long id, EcomAdStatus status) {
        if (status == null || status == EcomAdStatus.ARCHIVED) {
            throw new BizException("ecom.error.ads.statusInvalid");
        }
        Long storeId;
        Long campaignId;
        String platformId;
        String name;
        EcomAdStatus old;
        EcomAdTargetType targetType = null;
        switch (level) {
            case CAMPAIGN -> {
                EcomAdCampaign c = structureService.campaign(id);
                storeId = c.getStoreId();
                campaignId = c.getId();
                platformId = c.getPlatformCampaignId();
                name = c.getName();
                old = c.getStatus();
            }
            case AD_GROUP -> {
                EcomAdGroup g = structureService.group(id);
                storeId = g.getStoreId();
                campaignId = g.getCampaignId();
                platformId = g.getPlatformAdGroupId();
                name = g.getName();
                old = g.getStatus();
            }
            default -> {
                EcomAdTarget t = structureService.target(id);
                storeId = t.getStoreId();
                campaignId = t.getCampaignId();
                platformId = t.getPlatformTargetId();
                name = t.getTargetText();
                old = t.getStatus();
                targetType = t.getTargetType();
            }
        }
        if (old == EcomAdStatus.ARCHIVED) {
            throw new BizException("ecom.error.ads.archived");
        }
        EcomStore store = storeService.get(storeId);
        boolean pushed = pushOne(store, new AdsChange(id, level, platformId, targetType, null, null, status));
        transactionTemplate.executeWithoutResult(s -> {
            structureService.setStatus(level, id, status);
            writeManualLog(store, level, id, campaignId, name, status == EcomAdStatus.PAUSED ? EcomAdAction.PAUSE : EcomAdAction.ENABLE, null, null,
                    old, status, pushed);
        });
        return pushed;
    }

    /** 提交一项修改：没有接口 / 没有平台 ID 返回 false（只改本地），接口返回失败时报错 */
    private boolean pushOne(EcomStore store, AdsChange change) {
        PushOutcome o = push(store, List.of(change));
        AdsChangeResult r = o.result(change.localId());
        if (r == null) {
            return false;
        }
        if (!r.success()) {
            throw new BizException("ecom.error.ads.pushFailed", r.message());
        }
        return true;
    }

    private void writeManualLog(EcomStore store, EcomAdLevel level, Long entityId, Long campaignId, String name, EcomAdAction action,
                                BigDecimal oldValue, BigDecimal newValue, EcomAdStatus oldStatus, EcomAdStatus newStatus, boolean pushed) {
        EcomAdRuleLog l = new EcomAdRuleLog();
        LocalDateTime now = LocalDateTime.now();
        l.setSource(EcomAdLogSource.MANUAL);
        l.setRunTime(now);
        l.setStoreId(store.getId());
        l.setLevel(level);
        l.setEntityId(entityId);
        l.setCampaignId(campaignId);
        l.setEntityName(name);
        l.setAction(action);
        l.setOldValue(oldValue);
        l.setNewValue(newValue);
        l.setOldStatus(oldStatus);
        l.setNewStatus(newStatus);
        l.setStatus(pushed ? EcomAdLogStatus.APPLIED : EcomAdLogStatus.MANUAL);
        l.setAppliedTime(now);
        l.setAppliedBy(SecurityUtils.getUserIdOrNull());
        logMapper.insert(l);
    }

    // ---------------------------------------------------------------- 否定关键词

    /** 添加否定关键词的结果：created 新建的条数，pushed 其中已同步到平台的条数，failed 平台返回失败（未保存）的条数 */
    public record NegateResult(int created, int pushed, int failed, int skipped, String message) {
    }

    /**
     * 把搜索词添加为广告组的否定关键词：已存在的跳过；店铺有广告接口、广告组有平台 ID 时先在平台上创建（成功的保存平台 ID，失败的不保存），
     * 没有接口时只保存在本地（请在平台后台同样添加）
     */
    public NegateResult negate(com.lemon.erp.modules.ecom.dto.AdNegateRequest req) {
        com.lemon.erp.modules.ecom.model.EcomAdMatchType match = req.getMatchType() == com.lemon.erp.modules.ecom.model.EcomAdMatchType.NEGATIVE_PHRASE
                ? com.lemon.erp.modules.ecom.model.EcomAdMatchType.NEGATIVE_PHRASE : com.lemon.erp.modules.ecom.model.EcomAdMatchType.NEGATIVE_EXACT;
        Map<Long, List<Object[]>> byStore = new java.util.LinkedHashMap<>();
        int skipped = 0;
        java.util.Set<String> seen = new java.util.HashSet<>();
        long seq = 0;
        for (com.lemon.erp.modules.ecom.dto.AdNegateRequest.Item it : req.getItems()) {
            EcomAdGroup g = structureService.group(it.getAdGroupId());
            String text = it.getText().trim();
            if (!seen.add(g.getId() + "|" + text.toLowerCase())
                    || structureService.findTarget(g.getId(), null, text, match, EcomAdTargetType.NEGATIVE) != null) {
                skipped++;
                continue;
            }
            EcomAdCampaign c = structureService.campaign(g.getCampaignId());
            byStore.computeIfAbsent(g.getStoreId(), k -> new java.util.ArrayList<>()).add(new Object[]{++seq, g, c, text});
        }
        int created = 0;
        int pushed = 0;
        int failed = 0;
        for (Map.Entry<Long, List<Object[]>> e : byStore.entrySet()) {
            EcomStore store = storeService.get(e.getKey());
            Optional<AdsConnector> connector = authService.connector(store).filter(c -> c.supports(AdsCapability.NEGATIVE_KEYWORDS));
            Map<Long, AdsChangeResult> results = new HashMap<>();
            if (connector.isPresent()) {
                List<com.lemon.erp.modules.ecom.ads.AdsNegativeKeyword> list = new java.util.ArrayList<>();
                for (Object[] x : e.getValue()) {
                    EcomAdGroup g = (EcomAdGroup) x[1];
                    EcomAdCampaign c = (EcomAdCampaign) x[2];
                    if (StringUtils.hasText(g.getPlatformAdGroupId()) && StringUtils.hasText(c.getPlatformCampaignId())) {
                        list.add(new com.lemon.erp.modules.ecom.ads.AdsNegativeKeyword((Long) x[0], c.getPlatformCampaignId(), g.getPlatformAdGroupId(),
                                (String) x[3], match));
                    }
                }
                if (!list.isEmpty()) {
                    try {
                        for (AdsChangeResult r : connector.get().createNegativeKeywords(authService.requireContext(store), list)) {
                            results.put(r.localId(), r);
                        }
                    } catch (RuntimeException ex) {
                        list.forEach(k -> results.put(k.localId(), AdsChangeResult.failed(k.localId(), ex.getMessage())));
                    }
                }
            }
            List<EcomAdTarget> toSave = new java.util.ArrayList<>();
            for (Object[] x : e.getValue()) {
                AdsChangeResult r = results.get((Long) x[0]);
                if (r != null && !r.success()) {
                    failed++;
                    continue;
                }
                EcomAdGroup g = (EcomAdGroup) x[1];
                EcomAdTarget t = new EcomAdTarget();
                t.setAdGroupId(g.getId());
                t.setTargetType(EcomAdTargetType.NEGATIVE);
                t.setTargetText((String) x[3]);
                t.setMatchType(match);
                t.setStatus(EcomAdStatus.ENABLED);
                t.setPlatformTargetId(r == null ? null : r.platformId());
                toSave.add(t);
                if (r != null) {
                    pushed++;
                }
            }
            transactionTemplate.executeWithoutResult(st -> toSave.forEach(t -> {
                Long id = structureService.createTarget(t);
                if (t.getPlatformTargetId() != null) {
                    structureService.updateTargetRow(withId(id, t));
                }
            }));
            created += toSave.size();
        }
        return new NegateResult(created, pushed, failed, skipped,
                com.lemon.erp.common.i18n.Messages.get("ecom.text.ads.negateSummary", created, pushed, failed, skipped));
    }

    private static EcomAdTarget withId(Long id, EcomAdTarget t) {
        EcomAdTarget u = new EcomAdTarget();
        u.setId(id);
        u.setPlatformTargetId(t.getPlatformTargetId());
        return u;
    }
}
