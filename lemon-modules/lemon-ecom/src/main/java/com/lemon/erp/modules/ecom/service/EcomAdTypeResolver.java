package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.modules.ecom.ads.AdsChange;
import com.lemon.erp.modules.ecom.entity.EcomAdCampaign;
import com.lemon.erp.modules.ecom.entity.EcomAdGroup;
import com.lemon.erp.modules.ecom.entity.EcomAdTarget;
import com.lemon.erp.modules.ecom.mapper.EcomAdCampaignMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdGroupMapper;
import com.lemon.erp.modules.ecom.mapper.EcomAdTargetMapper;
import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 提交广告修改前补齐每项修改所属活动的广告类型（SP / SB / SD）与上级的平台 ID：亚马逊三种广告是不同的接口，
 * 品牌推广修改关键词 / 投放时还要带上活动和广告组的平台 ID。按层级批量查询，已带广告类型的不再查询
 */
@Service
@RequiredArgsConstructor
public class EcomAdTypeResolver {

    private final EcomAdCampaignMapper campaignMapper;
    private final EcomAdGroupMapper groupMapper;
    private final EcomAdTargetMapper targetMapper;

    public List<AdsChange> enrich(List<AdsChange> changes) {
        Set<Long> targetIds = new HashSet<>();
        Set<Long> groupIds = new HashSet<>();
        Set<Long> campaignIds = new HashSet<>();
        for (AdsChange c : changes) {
            if (c.adType() != null || c.localId() == null) {
                continue;
            }
            switch (c.level()) {
                case CAMPAIGN -> campaignIds.add(c.localId());
                case AD_GROUP -> groupIds.add(c.localId());
                default -> targetIds.add(c.localId());
            }
        }
        if (targetIds.isEmpty() && groupIds.isEmpty() && campaignIds.isEmpty()) {
            return changes;
        }
        Map<Long, EcomAdTarget> targets = new HashMap<>();
        if (!targetIds.isEmpty()) {
            targetMapper.selectByIds(targetIds).forEach(t -> {
                targets.put(t.getId(), t);
                if (t.getAdGroupId() != null) {
                    groupIds.add(t.getAdGroupId());
                }
            });
        }
        Map<Long, EcomAdGroup> groups = new HashMap<>();
        if (!groupIds.isEmpty()) {
            groupMapper.selectByIds(groupIds).forEach(g -> {
                groups.put(g.getId(), g);
                campaignIds.add(g.getCampaignId());
            });
        }
        targets.values().forEach(t -> campaignIds.add(t.getCampaignId()));
        Map<Long, EcomAdCampaign> campaigns = new HashMap<>();
        campaignIds.remove(null);
        if (!campaignIds.isEmpty()) {
            campaignMapper.selectByIds(campaignIds).forEach(c -> campaigns.put(c.getId(), c));
        }
        List<AdsChange> out = new ArrayList<>(changes.size());
        for (AdsChange c : changes) {
            if (c.adType() != null || c.localId() == null) {
                out.add(c);
                continue;
            }
            EcomAdCampaign campaign;
            EcomAdGroup group = null;
            if (c.level() == EcomAdLevel.CAMPAIGN) {
                campaign = campaigns.get(c.localId());
            } else if (c.level() == EcomAdLevel.AD_GROUP) {
                group = groups.get(c.localId());
                campaign = group == null ? null : campaigns.get(group.getCampaignId());
            } else {
                EcomAdTarget t = targets.get(c.localId());
                group = t == null ? null : groups.get(t.getAdGroupId());
                campaign = t == null ? null : campaigns.get(t.getCampaignId());
            }
            out.add(campaign == null ? c : c.withContext(campaign.getAdType(), campaign.getPlatformCampaignId(),
                    group == null ? null : group.getPlatformAdGroupId()));
        }
        return out;
    }
}
