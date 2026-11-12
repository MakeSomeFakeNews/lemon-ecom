package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.modules.ecom.entity.EcomAdCampaign;
import com.lemon.erp.modules.ecom.entity.EcomAdDaily;
import com.lemon.erp.modules.ecom.entity.EcomAdGroup;
import com.lemon.erp.modules.ecom.entity.EcomAdSchedule;
import com.lemon.erp.modules.ecom.entity.EcomAdSearchTerm;
import com.lemon.erp.modules.ecom.entity.EcomAdStat;
import com.lemon.erp.modules.ecom.entity.EcomAdTarget;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomAdCampaignMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdDailyMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdGroupMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdScheduleMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdSearchTermMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdStatMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdTargetMapper;
import com.lemon.erp.modules.ecom.model.EcomAdMatchType;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import com.lemon.erp.modules.ecom.model.EcomAdType;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 广告结构（活动 / 广告组 / 投放）的维护与查找。竞价、预算、状态的修改要同步到平台，走 {@link EcomAdChangeService}；
 * 这里只维护名称、标签等本地字段，以及导入 / 同步时按平台 ID 或名称查找、自动建立
 */
@Service
@RequiredArgsConstructor
public class EcomAdStructureService {

    private final EcomAdCampaignMapper campaignMapper;
    private final EcomAdGroupMapper groupMapper;
    private final EcomAdTargetMapper targetMapper;
    private final EcomAdStatMapper statMapper;
    private final EcomAdSearchTermMapper searchTermMapper;
    private final EcomAdDailyMapper dailyMapper;
    private final EcomAdScheduleMapper scheduleMapper;
    private final EcomStoreService storeService;

    // ---------------------------------------------------------------- 活动

    public PageResult<EcomAdCampaign> page(long current, long size, Long storeId, String keyword, EcomAdStatus status, EcomAdType adType) {
        Page<EcomAdCampaign> page = Page.of(current, size);
        campaignMapper.selectPageList(page, storeId, trim(keyword), status, adType);
        return PageResult.of(page);
    }

    public EcomAdCampaign detail(Long id) {
        EcomAdCampaign c = campaignMapper.selectDetail(id);
        if (c == null) {
            throw new BizException("ecom.error.ads.campaignNotFound");
        }
        return c;
    }

    public EcomAdCampaign campaign(Long id) {
        EcomAdCampaign c = id == null ? null : campaignMapper.selectById(id);
        if (c == null) {
            throw new BizException("ecom.error.ads.campaignNotFound");
        }
        return c;
    }

    public List<EcomAdCampaign> campaigns(java.util.Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : campaignMapper.selectByIds(ids);
    }

    /** 手工新建活动（没有广告接口的平台，或先在本地规划）：同一店铺内名称不能重复 */
    public Long createCampaign(EcomAdCampaign c) {
        if (c.getStoreId() == null) {
            throw new BizException("ecom.valid.ads.storeRequired");
        }
        EcomStore store = storeService.get(c.getStoreId());
        c.setId(null);
        c.setName(c.getName().trim());
        checkCampaignName(store.getId(), c.getName(), null);
        if (c.getAdType() == null) {
            c.setAdType(defaultType(store.getPlatform()));
        }
        if (c.getStatus() == null) {
            c.setStatus(EcomAdStatus.ENABLED);
        }
        c.setDailyBudget(c.getDailyBudget() == null ? null : Decimals.money(c.getDailyBudget()));
        c.setTags(normalizeTags(c.getTags()));
        c.setSyncedTime(null);
        campaignMapper.insert(c);
        return c.getId();
    }

    /** 修改活动的本地字段（名称、类型、平台 ID、竞价策略、日期、标签、备注）；预算与状态通过「修改预算 / 暂停 / 启用」同步到平台 */
    public void updateCampaign(Long id, EcomAdCampaign c) {
        EcomAdCampaign old = campaign(id);
        String name = c.getName().trim();
        checkCampaignName(old.getStoreId(), name, id);
        campaignMapper.update(Wrappers.<EcomAdCampaign>lambdaUpdate()
                .set(EcomAdCampaign::getName, name)
                .set(c.getAdType() != null, EcomAdCampaign::getAdType, c.getAdType())
                .set(EcomAdCampaign::getPlatformCampaignId, StringUtils.hasText(c.getPlatformCampaignId()) ? c.getPlatformCampaignId().trim() : null)
                .set(EcomAdCampaign::getBiddingStrategy, c.getBiddingStrategy())
                .set(EcomAdCampaign::getTargetingType, c.getTargetingType())
                .set(EcomAdCampaign::getStartDate, c.getStartDate())
                .set(EcomAdCampaign::getEndDate, c.getEndDate())
                .set(EcomAdCampaign::getTags, normalizeTags(c.getTags()))
                .set(EcomAdCampaign::getRemark, c.getRemark())
                .eq(EcomAdCampaign::getId, id));
    }

    /** 删除活动及其广告组、投放、统计、搜索词、分时策略；广告日报保留（利润分析仍按它扣除花费），只解除关联 */
    @Transactional
    public void deleteCampaigns(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        targetMapper.delete(Wrappers.<EcomAdTarget>lambdaQuery().in(EcomAdTarget::getCampaignId, ids));
        groupMapper.delete(Wrappers.<EcomAdGroup>lambdaQuery().in(EcomAdGroup::getCampaignId, ids));
        statMapper.delete(Wrappers.<EcomAdStat>lambdaQuery().in(EcomAdStat::getCampaignId, ids));
        searchTermMapper.delete(Wrappers.<EcomAdSearchTerm>lambdaQuery().in(EcomAdSearchTerm::getCampaignId, ids));
        scheduleMapper.delete(Wrappers.<EcomAdSchedule>lambdaQuery().in(EcomAdSchedule::getCampaignId, ids));
        dailyMapper.update(Wrappers.<EcomAdDaily>lambdaUpdate().set(EcomAdDaily::getCampaignId, null).set(EcomAdDaily::getAdGroupId, null)
                .in(EcomAdDaily::getCampaignId, ids));
        campaignMapper.deleteByIds(ids);
    }

    private void checkCampaignName(Long storeId, String name, Long excludeId) {
        if (campaignMapper.exists(Wrappers.<EcomAdCampaign>lambdaQuery().eq(EcomAdCampaign::getStoreId, storeId).eq(EcomAdCampaign::getName, name)
                .ne(excludeId != null, EcomAdCampaign::getId, excludeId))) {
            throw new BizException("ecom.error.ads.campaignNameExists", name);
        }
    }

    public static EcomAdType defaultType(EcomPlatform platform) {
        return platform == EcomPlatform.AMAZON ? EcomAdType.SP : EcomAdType.GENERIC;
    }

    // ---------------------------------------------------------------- 广告组

    public List<EcomAdGroup> groups(Long campaignId) {
        return groupMapper.selectByCampaign(campaignId);
    }

    public EcomAdGroup group(Long id) {
        EcomAdGroup g = id == null ? null : groupMapper.selectById(id);
        if (g == null) {
            throw new BizException("ecom.error.ads.adGroupNotFound");
        }
        return g;
    }

    public Long createGroup(EcomAdGroup g) {
        EcomAdCampaign c = campaign(g.getCampaignId());
        g.setId(null);
        g.setStoreId(c.getStoreId());
        g.setName(g.getName().trim());
        if (groupMapper.exists(Wrappers.<EcomAdGroup>lambdaQuery().eq(EcomAdGroup::getCampaignId, c.getId()).eq(EcomAdGroup::getName, g.getName()))) {
            throw new BizException("ecom.error.ads.adGroupNameExists", g.getName());
        }
        if (g.getStatus() == null) {
            g.setStatus(EcomAdStatus.ENABLED);
        }
        g.setDefaultBid(g.getDefaultBid() == null ? null : Decimals.money(g.getDefaultBid()));
        g.setSyncedTime(null);
        groupMapper.insert(g);
        return g.getId();
    }

    public void updateGroup(Long id, EcomAdGroup g) {
        EcomAdGroup old = group(id);
        String name = g.getName().trim();
        if (groupMapper.exists(Wrappers.<EcomAdGroup>lambdaQuery().eq(EcomAdGroup::getCampaignId, old.getCampaignId()).eq(EcomAdGroup::getName, name)
                .ne(EcomAdGroup::getId, id))) {
            throw new BizException("ecom.error.ads.adGroupNameExists", name);
        }
        groupMapper.update(Wrappers.<EcomAdGroup>lambdaUpdate().set(EcomAdGroup::getName, name)
                .set(EcomAdGroup::getPlatformAdGroupId, StringUtils.hasText(g.getPlatformAdGroupId()) ? g.getPlatformAdGroupId().trim() : null)
                .eq(EcomAdGroup::getId, id));
    }

    @Transactional
    public void deleteGroup(Long id) {
        EcomAdGroup g = group(id);
        targetMapper.delete(Wrappers.<EcomAdTarget>lambdaQuery().eq(EcomAdTarget::getAdGroupId, id));
        statMapper.delete(Wrappers.<EcomAdStat>lambdaQuery().eq(EcomAdStat::getAdGroupId, id));
        dailyMapper.update(Wrappers.<EcomAdDaily>lambdaUpdate().set(EcomAdDaily::getAdGroupId, null).eq(EcomAdDaily::getAdGroupId, id));
        groupMapper.deleteById(g.getId());
    }

    // ---------------------------------------------------------------- 投放

    public PageResult<EcomAdTarget> targets(long current, long size, Long campaignId, Long adGroupId, String keyword, EcomAdStatus status,
                                            EcomAdTargetType targetType) {
        Page<EcomAdTarget> page = Page.of(current, size);
        targetMapper.selectPageList(page, campaignId, adGroupId, trim(keyword), status, targetType);
        return PageResult.of(page);
    }

    public EcomAdTarget target(Long id) {
        EcomAdTarget t = id == null ? null : targetMapper.selectById(id);
        if (t == null) {
            throw new BizException("ecom.error.ads.targetNotFound");
        }
        return t;
    }

    public List<EcomAdTarget> targetsOfCampaign(Long campaignId) {
        return targetMapper.selectList(Wrappers.<EcomAdTarget>lambdaQuery().eq(EcomAdTarget::getCampaignId, campaignId));
    }

    /** 手工新建投放：关键词投放必须有匹配方式，否定关键词只能是否定精准 / 否定词组；基准竞价 = 竞价 */
    public Long createTarget(EcomAdTarget t) {
        EcomAdGroup g = group(t.getAdGroupId());
        t.setId(null);
        t.setStoreId(g.getStoreId());
        t.setCampaignId(g.getCampaignId());
        t.setTargetText(t.getTargetText().trim());
        checkMatch(t.getTargetType(), t.getMatchType());
        if (findTarget(g.getId(), null, t.getTargetText(), t.getMatchType(), t.getTargetType()) != null) {
            throw new BizException("ecom.error.ads.targetExists", t.getTargetText());
        }
        if (t.getStatus() == null) {
            t.setStatus(EcomAdStatus.ENABLED);
        }
        t.setBid(t.getTargetType() == EcomAdTargetType.NEGATIVE || t.getBid() == null ? null : Decimals.money(t.getBid()));
        t.setBaseBid(t.getBid());
        t.setScheduleFactor(null);
        t.setSyncedTime(null);
        targetMapper.insert(t);
        return t.getId();
    }

    /** 修改投放的本地字段（文字、匹配方式、平台 ID）；竞价与状态通过「修改竞价 / 暂停 / 启用」同步到平台 */
    public void updateTarget(Long id, EcomAdTarget t) {
        EcomAdTarget old = target(id);
        checkMatch(old.getTargetType(), t.getMatchType());
        targetMapper.update(Wrappers.<EcomAdTarget>lambdaUpdate().set(EcomAdTarget::getTargetText, t.getTargetText().trim())
                .set(EcomAdTarget::getMatchType, t.getMatchType())
                .set(EcomAdTarget::getPlatformTargetId, StringUtils.hasText(t.getPlatformTargetId()) ? t.getPlatformTargetId().trim() : null)
                .eq(EcomAdTarget::getId, id));
    }

    @Transactional
    public void deleteTargets(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        statMapper.delete(Wrappers.<EcomAdStat>lambdaQuery().eq(EcomAdStat::getLevel, com.lemon.erp.modules.ecom.model.EcomAdLevel.TARGET)
                .in(EcomAdStat::getEntityId, ids));
        targetMapper.deleteByIds(ids);
    }

    private static void checkMatch(EcomAdTargetType type, EcomAdMatchType match) {
        boolean negativeMatch = match == EcomAdMatchType.NEGATIVE_EXACT || match == EcomAdMatchType.NEGATIVE_PHRASE;
        boolean ok = switch (type) {
            case KEYWORD -> match != null && !negativeMatch;
            case NEGATIVE -> negativeMatch;
            default -> match == null;
        };
        if (!ok) {
            throw new BizException("ecom.error.ads.matchTypeInvalid");
        }
    }

    // ---------------------------------------------------------------- 导入 / 同步时查找与建立

    /** 按平台 ID 找活动，没有时按名称找（导入的活动没有平台 ID，同步时补上） */
    public EcomAdCampaign findCampaign(Long storeId, String platformId, String name) {
        if (StringUtils.hasText(platformId)) {
            EcomAdCampaign c = Queries.first(campaignMapper, Wrappers.<EcomAdCampaign>lambdaQuery().eq(EcomAdCampaign::getStoreId, storeId)
                    .eq(EcomAdCampaign::getPlatformCampaignId, platformId.trim()).orderByAsc(EcomAdCampaign::getId));
            if (c != null) {
                return c;
            }
        }
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return Queries.first(campaignMapper, Wrappers.<EcomAdCampaign>lambdaQuery().eq(EcomAdCampaign::getStoreId, storeId)
                .eq(EcomAdCampaign::getName, name.trim()).orderByAsc(EcomAdCampaign::getId));
    }

    /** 找不到时建立（报告导入、旧版广告日报导入），返回 [活动, 是否新建] */
    public EcomAdCampaign ensureCampaign(EcomStore store, String platformId, String name, EcomAdType adType, boolean[] created) {
        EcomAdCampaign c = findCampaign(store.getId(), platformId, name);
        if (c != null) {
            if (StringUtils.hasText(platformId) && c.getPlatformCampaignId() == null) {
                c.setPlatformCampaignId(platformId.trim());
                campaignMapper.update(Wrappers.<EcomAdCampaign>lambdaUpdate().set(EcomAdCampaign::getPlatformCampaignId, c.getPlatformCampaignId())
                        .eq(EcomAdCampaign::getId, c.getId()));
            }
            return c;
        }
        c = new EcomAdCampaign();
        c.setStoreId(store.getId());
        c.setName(StringUtils.hasText(name) ? name.trim() : platformId.trim());
        c.setPlatformCampaignId(StringUtils.hasText(platformId) ? platformId.trim() : null);
        c.setAdType(adType == null ? defaultType(store.getPlatform()) : adType);
        c.setStatus(EcomAdStatus.ENABLED);
        campaignMapper.insert(c);
        if (created != null) {
            created[0] = true;
        }
        return c;
    }

    public EcomAdGroup findGroup(Long campaignId, String platformId, String name) {
        if (StringUtils.hasText(platformId)) {
            EcomAdGroup g = Queries.first(groupMapper, Wrappers.<EcomAdGroup>lambdaQuery().eq(EcomAdGroup::getCampaignId, campaignId)
                    .eq(EcomAdGroup::getPlatformAdGroupId, platformId.trim()).orderByAsc(EcomAdGroup::getId));
            if (g != null) {
                return g;
            }
        }
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return Queries.first(groupMapper, Wrappers.<EcomAdGroup>lambdaQuery().eq(EcomAdGroup::getCampaignId, campaignId)
                .eq(EcomAdGroup::getName, name.trim()).orderByAsc(EcomAdGroup::getId));
    }

    public EcomAdGroup ensureGroup(EcomAdCampaign c, String platformId, String name, boolean[] created) {
        if (!StringUtils.hasText(platformId) && !StringUtils.hasText(name)) {
            return null;
        }
        EcomAdGroup g = findGroup(c.getId(), platformId, name);
        if (g != null) {
            if (StringUtils.hasText(platformId) && g.getPlatformAdGroupId() == null) {
                g.setPlatformAdGroupId(platformId.trim());
                groupMapper.update(Wrappers.<EcomAdGroup>lambdaUpdate().set(EcomAdGroup::getPlatformAdGroupId, g.getPlatformAdGroupId())
                        .eq(EcomAdGroup::getId, g.getId()));
            }
            return g;
        }
        g = new EcomAdGroup();
        g.setStoreId(c.getStoreId());
        g.setCampaignId(c.getId());
        g.setName(StringUtils.hasText(name) ? name.trim() : platformId.trim());
        g.setPlatformAdGroupId(StringUtils.hasText(platformId) ? platformId.trim() : null);
        g.setStatus(EcomAdStatus.ENABLED);
        groupMapper.insert(g);
        if (created != null) {
            created[0] = true;
        }
        return g;
    }

    /** 按平台 ID，或 广告组 + 文字 + 匹配方式 + 类型 找投放 */
    public EcomAdTarget findTarget(Long adGroupId, String platformId, String text, EcomAdMatchType match, EcomAdTargetType type) {
        if (StringUtils.hasText(platformId)) {
            EcomAdTarget t = Queries.first(targetMapper, Wrappers.<EcomAdTarget>lambdaQuery().eq(EcomAdTarget::getAdGroupId, adGroupId)
                    .eq(EcomAdTarget::getPlatformTargetId, platformId.trim()).orderByAsc(EcomAdTarget::getId));
            if (t != null) {
                return t;
            }
        }
        if (!StringUtils.hasText(text)) {
            return null;
        }
        List<EcomAdTarget> list = targetMapper.selectList(Wrappers.<EcomAdTarget>lambdaQuery().eq(EcomAdTarget::getAdGroupId, adGroupId)
                .eq(EcomAdTarget::getTargetText, text.trim()));
        return list.stream().filter(t -> t.getMatchType() == match && (type == null || t.getTargetType() == type)).findFirst().orElse(null);
    }

    public EcomAdTarget ensureTarget(EcomAdGroup g, String platformId, String text, EcomAdMatchType match, EcomAdTargetType type, boolean[] created) {
        String t0 = StringUtils.hasText(text) ? text.trim() : platformId;
        if (t0 == null) {
            return null;
        }
        EcomAdTargetType tt = type == null ? (match == null ? EcomAdTargetType.PRODUCT : EcomAdTargetType.KEYWORD) : type;
        EcomAdTarget t = findTarget(g.getId(), platformId, t0, match, tt);
        if (t != null) {
            if (StringUtils.hasText(platformId) && t.getPlatformTargetId() == null) {
                t.setPlatformTargetId(platformId.trim());
                targetMapper.update(Wrappers.<EcomAdTarget>lambdaUpdate().set(EcomAdTarget::getPlatformTargetId, t.getPlatformTargetId())
                        .eq(EcomAdTarget::getId, t.getId()));
            }
            return t;
        }
        t = new EcomAdTarget();
        t.setStoreId(g.getStoreId());
        t.setCampaignId(g.getCampaignId());
        t.setAdGroupId(g.getId());
        t.setTargetType(tt);
        t.setTargetText(t0.length() > 255 ? t0.substring(0, 255) : t0);
        t.setMatchType(match);
        t.setPlatformTargetId(StringUtils.hasText(platformId) ? platformId.trim() : null);
        t.setStatus(EcomAdStatus.ENABLED);
        targetMapper.insert(t);
        if (created != null) {
            created[0] = true;
        }
        return t;
    }

    public void updateCampaignRow(EcomAdCampaign c) {
        campaignMapper.updateById(c);
    }

    public void updateGroupRow(EcomAdGroup g) {
        groupMapper.updateById(g);
    }

    public void updateTargetRow(EcomAdTarget t) {
        targetMapper.updateById(t);
    }

    /** 本地修改竞价：基准竞价与实际竞价（有分时系数时不同） */
    public void setTargetBid(Long id, java.math.BigDecimal baseBid, java.math.BigDecimal bid) {
        targetMapper.update(Wrappers.<EcomAdTarget>lambdaUpdate().set(EcomAdTarget::getBaseBid, baseBid).set(EcomAdTarget::getBid, bid)
                .eq(EcomAdTarget::getId, id));
    }

    /** 分时调价后：基准竞价（原来为空时补上）、实际竞价与当前系数（factor 为空表示已恢复基准竞价） */
    public void setScheduledBid(Long id, java.math.BigDecimal baseBid, java.math.BigDecimal bid, java.math.BigDecimal factor) {
        targetMapper.update(Wrappers.<EcomAdTarget>lambdaUpdate().set(EcomAdTarget::getBaseBid, baseBid).set(EcomAdTarget::getBid, bid)
                .set(EcomAdTarget::getScheduleFactor, factor).eq(EcomAdTarget::getId, id));
    }

    public void setGroupBid(Long id, java.math.BigDecimal bid) {
        groupMapper.update(Wrappers.<EcomAdGroup>lambdaUpdate().set(EcomAdGroup::getDefaultBid, bid).eq(EcomAdGroup::getId, id));
    }

    public void setCampaignBudget(Long id, java.math.BigDecimal budget) {
        campaignMapper.update(Wrappers.<EcomAdCampaign>lambdaUpdate().set(EcomAdCampaign::getDailyBudget, budget).eq(EcomAdCampaign::getId, id));
    }

    public void setStatus(com.lemon.erp.modules.ecom.model.EcomAdLevel level, Long id, EcomAdStatus status) {
        switch (level) {
            case CAMPAIGN -> campaignMapper.update(Wrappers.<EcomAdCampaign>lambdaUpdate().set(EcomAdCampaign::getStatus, status).eq(EcomAdCampaign::getId, id));
            case AD_GROUP -> groupMapper.update(Wrappers.<EcomAdGroup>lambdaUpdate().set(EcomAdGroup::getStatus, status).eq(EcomAdGroup::getId, id));
            default -> targetMapper.update(Wrappers.<EcomAdTarget>lambdaUpdate().set(EcomAdTarget::getStatus, status).eq(EcomAdTarget::getId, id));
        }
    }

    /** 标签规范化：去空格、去重，逗号分隔 */
    static String normalizeTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return null;
        }
        String s = String.join(",", java.util.Arrays.stream(tags.split("[,，;；]")).map(String::trim).filter(StringUtils::hasText).distinct().toList());
        return s.isEmpty() ? null : s;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    static boolean same(Object a, Object b) {
        return Objects.equals(a, b);
    }
}
