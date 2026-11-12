package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.ads.AdsChange;
import com.lemon.erp.modules.ecom.ads.AdsChangeResult;
import com.lemon.erp.modules.ecom.connector.HttpJson;
import com.lemon.erp.modules.ecom.entity.EcomAdCampaign;
import com.lemon.erp.modules.ecom.entity.EcomAdSchedule;
import com.lemon.erp.modules.ecom.entity.EcomAdTarget;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomAdScheduleMapper;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 分时调价：活动级的 星期 × 小时 竞价系数表。定时任务每小时（以及保存策略后的「立即应用」）按店铺时区的当前时段，
 * 把活动下启用的投放（不含否定关键词）的竞价设为 基准竞价 × 系数（限制在策略的最低 / 最高竞价和平台最低竞价之间）——
 * 每次都从基准竞价计算，不会在上一次的结果上累乘；基准竞价只由用户或广告规则修改。
 * 竞价修改经广告接口提交（不在事务中），成功的再在事务中写本地；店铺没有广告接口时不执行（只记录原因）。
 * 停用或删除策略时恢复基准竞价
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomAdScheduleService {

    static final BigDecimal MIN_FACTOR = new BigDecimal("0.1");
    static final BigDecimal MAX_FACTOR = new BigDecimal("3");

    private final EcomAdScheduleMapper scheduleMapper;
    private final EcomAdStructureService structureService;
    private final EcomAdChangeService changeService;
    private final EcomAdsAuthService authService;
    private final EcomStoreService storeService;
    private final TransactionTemplate transactionTemplate;

    /** 一次应用的结果 */
    public record ApplyResult(BigDecimal factor, int changed, int failed, int skipped, String message) {
    }

    public List<EcomAdSchedule> list(Long storeId) {
        List<EcomAdSchedule> list = scheduleMapper.selectByStore(storeId);
        list.forEach(EcomAdScheduleService::readFactors);
        return list;
    }

    /** 活动的分时策略，没有时返回全部为 1 的默认表（未保存） */
    public EcomAdSchedule byCampaign(Long campaignId) {
        EcomAdCampaign c = structureService.campaign(campaignId);
        EcomAdSchedule s = scheduleMapper.selectOne(Wrappers.<EcomAdSchedule>lambdaQuery().eq(EcomAdSchedule::getCampaignId, campaignId));
        if (s == null) {
            s = new EcomAdSchedule();
            s.setCampaignId(campaignId);
            s.setStoreId(c.getStoreId());
            s.setStatus(EnableStatus.DISABLED);
            s.setFactors(defaultFactors());
        } else {
            readFactors(s);
        }
        s.setCampaignName(c.getName());
        return s;
    }

    /**
     * 保存（按活动新增或修改）；启用时立即按当前时段应用一次（平台调用在事务外），停用时恢复基准竞价
     */
    public ApplyResult save(Long campaignId, EcomAdSchedule req) {
        EcomAdCampaign c = structureService.campaign(campaignId);
        validate(req.getFactors());
        if (req.getMinBid() != null && req.getMaxBid() != null && req.getMinBid().compareTo(req.getMaxBid()) > 0) {
            throw new BizException("ecom.error.ads.minMaxInvalid");
        }
        EcomAdSchedule old = scheduleMapper.selectOne(Wrappers.<EcomAdSchedule>lambdaQuery().eq(EcomAdSchedule::getCampaignId, campaignId));
        EcomAdSchedule s = old == null ? new EcomAdSchedule() : old;
        s.setCampaignId(campaignId);
        s.setStoreId(c.getStoreId());
        s.setStatus(req.getStatus() == null ? EnableStatus.ENABLED : req.getStatus());
        s.setFactors(req.getFactors());
        s.setFactorsJson(HttpJson.MAPPER.writeValueAsString(scaled(req.getFactors())));
        s.setMinBid(req.getMinBid());
        s.setMaxBid(req.getMaxBid());
        s.setRemark(req.getRemark());
        if (old == null) {
            scheduleMapper.insert(s);
        } else {
            scheduleMapper.updateById(s);
            scheduleMapper.update(Wrappers.<EcomAdSchedule>lambdaUpdate().set(EcomAdSchedule::getMinBid, s.getMinBid())
                    .set(EcomAdSchedule::getMaxBid, s.getMaxBid()).eq(EcomAdSchedule::getId, s.getId()));
        }
        return s.getStatus() == EnableStatus.ENABLED ? apply(s, LocalDateTime.now()) : restore(s);
    }

    /** 删除策略并恢复基准竞价 */
    public ApplyResult delete(Long campaignId) {
        EcomAdSchedule s = scheduleMapper.selectOne(Wrappers.<EcomAdSchedule>lambdaQuery().eq(EcomAdSchedule::getCampaignId, campaignId));
        if (s == null) {
            throw new BizException("ecom.error.ads.scheduleNotFound");
        }
        ApplyResult r = restore(readFactors(s));
        scheduleMapper.deleteById(s.getId());
        return r;
    }

    /** 立即按当前时段应用一次 */
    public ApplyResult applyNow(Long campaignId) {
        EcomAdSchedule s = scheduleMapper.selectOne(Wrappers.<EcomAdSchedule>lambdaQuery().eq(EcomAdSchedule::getCampaignId, campaignId));
        if (s == null || s.getStatus() != EnableStatus.ENABLED) {
            throw new BizException("ecom.error.ads.scheduleNotFound");
        }
        return apply(readFactors(s), LocalDateTime.now());
    }

    /** 定时任务：应用全部启用的策略，单个失败不影响其他 */
    public List<String> applyAll(LocalDateTime now) {
        List<String> out = new ArrayList<>();
        for (EcomAdSchedule s : scheduleMapper.selectList(Wrappers.<EcomAdSchedule>lambdaQuery().eq(EcomAdSchedule::getStatus, EnableStatus.ENABLED)
                .orderByAsc(EcomAdSchedule::getId))) {
            try {
                ApplyResult r = apply(readFactors(s), now);
                out.add(s.getCampaignId() + ": " + r.message());
            } catch (RuntimeException e) {
                if (!(e instanceof BizException)) {
                    log.warn("Ad schedule {} failed", s.getId(), e);
                }
                mark(s.getId(), now, null, e.getMessage());
                out.add(s.getCampaignId() + ": " + e.getMessage());
            }
        }
        return out;
    }

    /**
     * 按 now 所在时段（店铺时区）应用系数：竞价 = 基准竞价 × 系数，与当前竞价相同的不提交
     */
    public ApplyResult apply(EcomAdSchedule s, LocalDateTime now) {
        EcomStore store = storeService.get(s.getStoreId());
        BigDecimal factor = factorAt(s.getFactors(), now, zone(store));
        return push(s, store, factor, now);
    }

    /** 恢复基准竞价（停用 / 删除策略时） */
    public ApplyResult restore(EcomAdSchedule s) {
        EcomStore store = storeService.get(s.getStoreId());
        return push(s, store, null, LocalDateTime.now());
    }

    private ApplyResult push(EcomAdSchedule s, EcomStore store, BigDecimal factor, LocalDateTime now) {
        if (authService.connector(store).isEmpty()) {
            String msg = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.ads.scheduleNoApi");
            mark(s.getId(), now, factor, msg);
            return new ApplyResult(factor, 0, 0, 0, msg);
        }
        BigDecimal minBid = changeService.minBid(store);
        List<AdsChange> changes = new ArrayList<>();
        Map<Long, BigDecimal[]> planned = new HashMap<>();
        List<Long> factorOnly = new ArrayList<>();
        int skipped = 0;
        for (EcomAdTarget t : structureService.targetsOfCampaign(s.getCampaignId())) {
            if (t.getTargetType() == EcomAdTargetType.NEGATIVE || t.getStatus() != EcomAdStatus.ENABLED) {
                continue;
            }
            BigDecimal base = t.getBaseBid() != null ? t.getBaseBid() : t.getBid();
            if (base == null || !StringUtils.hasText(t.getPlatformTargetId())) {
                skipped++;
                continue;
            }
            BigDecimal bid = EcomAdChangeService.effectiveBid(base, factor, factor == null ? null : s, minBid);
            if (t.getBid() != null && bid.compareTo(t.getBid()) == 0) {
                if (!same(t.getScheduleFactor(), factor) || t.getBaseBid() == null) {
                    factorOnly.add(t.getId());
                    planned.put(t.getId(), new BigDecimal[]{base, bid});
                }
                continue;
            }
            planned.put(t.getId(), new BigDecimal[]{base, bid});
            changes.add(AdsChange.bid(t.getId(), EcomAdLevel.TARGET, t.getPlatformTargetId(), t.getTargetType(), bid));
        }
        Map<Long, AdsChangeResult> results = changes.isEmpty() ? Map.of() : changeService.push(store, changes).results();
        int changed = 0;
        int failed = 0;
        List<Long> ok = new ArrayList<>(factorOnly);
        String firstError = null;
        for (AdsChange c : changes) {
            AdsChangeResult r = results.get(c.localId());
            if (r != null && r.success()) {
                changed++;
                ok.add(c.localId());
            } else {
                failed++;
                if (firstError == null && r != null) {
                    firstError = r.message();
                }
            }
        }
        String msg = Messages.get(Messages.DEFAULT_LOCALE, "ecom.text.ads.scheduleApplied", factor == null ? "-" : factor.toPlainString(), changed, failed)
                + (firstError == null ? "" : " " + firstError);
        int skippedFinal = skipped;
        transactionTemplate.executeWithoutResult(st -> {
            for (Long id : ok) {
                BigDecimal[] p = planned.get(id);
                structureService.setScheduledBid(id, p[0], p[1], factor);
            }
            mark(s.getId(), now, factor, msg);
        });
        return new ApplyResult(factor, changed, failed, skippedFinal, msg);
    }

    private void mark(Long id, LocalDateTime now, BigDecimal factor, String message) {
        scheduleMapper.update(Wrappers.<EcomAdSchedule>lambdaUpdate().set(EcomAdSchedule::getLastAppliedTime, now)
                .set(EcomAdSchedule::getLastFactor, factor)
                .set(EcomAdSchedule::getLastMessage, message == null || message.length() <= 500 ? message : message.substring(0, 500))
                .eq(EcomAdSchedule::getId, id));
    }

    // ---------------------------------------------------------------- 系数表

    /** 当前时段的系数：店铺时区下的星期（周一 = 0）与小时 */
    public static BigDecimal factorAt(List<List<BigDecimal>> factors, LocalDateTime now, ZoneId zone) {
        ZonedDateTime local = now.atZone(ZoneId.systemDefault()).withZoneSameInstant(zone);
        BigDecimal f = factors.get(local.getDayOfWeek().getValue() - 1).get(local.getHour());
        return f == null ? BigDecimal.ONE : f.setScale(2, RoundingMode.HALF_UP);
    }

    static ZoneId zone(EcomStore store) {
        if (StringUtils.hasText(store.getTimezone())) {
            try {
                return ZoneId.of(store.getTimezone().trim());
            } catch (RuntimeException ignored) {
                // 时区写错时按服务器时区
            }
        }
        return ZoneId.systemDefault();
    }

    static void validate(List<List<BigDecimal>> factors) {
        if (factors == null || factors.size() != 7 || factors.stream().anyMatch(r -> r == null || r.size() != 24)) {
            throw new BizException("ecom.error.ads.factorsInvalid");
        }
        for (List<BigDecimal> row : factors) {
            for (BigDecimal f : row) {
                if (f == null || f.compareTo(MIN_FACTOR) < 0 || f.compareTo(MAX_FACTOR) > 0) {
                    throw new BizException("ecom.error.ads.factorRange", MIN_FACTOR, MAX_FACTOR);
                }
            }
        }
    }

    private static List<List<BigDecimal>> scaled(List<List<BigDecimal>> factors) {
        return factors.stream().map(r -> r.stream().map(f -> f.setScale(2, RoundingMode.HALF_UP)).toList()).toList();
    }

    public static List<List<BigDecimal>> defaultFactors() {
        List<List<BigDecimal>> f = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            List<BigDecimal> row = new ArrayList<>();
            for (int h = 0; h < 24; h++) {
                row.add(BigDecimal.ONE);
            }
            f.add(row);
        }
        return f;
    }

    static EcomAdSchedule readFactors(EcomAdSchedule s) {
        s.setFactors(StringUtils.hasText(s.getFactorsJson())
                ? HttpJson.MAPPER.readValue(s.getFactorsJson(), new TypeReference<List<List<BigDecimal>>>() {
                })
                : defaultFactors());
        return s;
    }

    private static boolean same(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }

    Optional<EcomAdSchedule> find(Long campaignId) {
        return Optional.ofNullable(scheduleMapper.selectOne(Wrappers.<EcomAdSchedule>lambdaQuery().eq(EcomAdSchedule::getCampaignId, campaignId)));
    }
}
