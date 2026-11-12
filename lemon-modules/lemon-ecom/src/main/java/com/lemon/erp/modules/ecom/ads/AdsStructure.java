package com.lemon.erp.modules.ecom.ads;

import com.lemon.erp.modules.ecom.model.EcomAdMatchType;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import com.lemon.erp.modules.ecom.model.EcomAdType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * 从广告接口拉取的结构：活动、广告组、投放（含否定关键词），用平台 ID 互相关联
 *
 * @param adTypes 这次完整拉取了的广告类型（本地有平台 ID 但没有返回的对象只在这些类型中标记为已归档；某种类型拉取失败或账号未开通时不在其中，
 *                避免误归档），为空表示全部类型
 */
public record AdsStructure(List<Campaign> campaigns, List<AdGroup> adGroups, List<Target> targets, Set<EcomAdType> adTypes) {

    public AdsStructure(List<Campaign> campaigns, List<AdGroup> adGroups, List<Target> targets) {
        this(campaigns, adGroups, targets, null);
    }

    public record Campaign(String platformId, String name, EcomAdType adType, EcomAdStatus status, BigDecimal dailyBudget,
                           String biddingStrategy, String targetingType, LocalDate startDate, LocalDate endDate) {
    }

    public record AdGroup(String platformId, String campaignPlatformId, String name, EcomAdStatus status, BigDecimal defaultBid) {
    }

    /**
     * @param text 关键词文字，或商品 / 自动投放的表达式（如 asin="B0XXXX"、close-match）
     */
    public record Target(String platformId, String campaignPlatformId, String adGroupPlatformId, EcomAdTargetType targetType, String text,
                         EcomAdMatchType matchType, EcomAdStatus status, BigDecimal bid) {
    }
}
