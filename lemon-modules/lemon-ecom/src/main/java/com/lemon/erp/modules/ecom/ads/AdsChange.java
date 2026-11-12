package com.lemon.erp.modules.ecom.ads;

import com.lemon.erp.modules.ecom.model.EcomAdLevel;
import com.lemon.erp.modules.ecom.model.EcomAdStatus;
import com.lemon.erp.modules.ecom.model.EcomAdTargetType;
import com.lemon.erp.modules.ecom.model.EcomAdType;

import java.math.BigDecimal;

/**
 * 对广告接口的一项修改：竞价（广告组默认竞价 / 投放竞价）、日预算（活动）、状态，为空的项不修改
 *
 * @param localId            本地记录 ID（结果按它对应）
 * @param targetType         投放的类型（亚马逊关键词与商品投放是两个接口），其他层级为空
 * @param adType             所属活动的广告类型（亚马逊 SP / SB / SD 是不同的接口），为空按商品推广
 * @param campaignPlatformId 所属活动的平台 ID（亚马逊品牌推广修改关键词 / 投放时必须带上），可为空
 * @param adGroupPlatformId  所属广告组的平台 ID（同上），可为空
 */
public record AdsChange(Long localId, EcomAdLevel level, String platformId, EcomAdTargetType targetType, BigDecimal bid, BigDecimal budget,
                        EcomAdStatus status, EcomAdType adType, String campaignPlatformId, String adGroupPlatformId) {

    public AdsChange(Long localId, EcomAdLevel level, String platformId, EcomAdTargetType targetType, BigDecimal bid, BigDecimal budget,
                     EcomAdStatus status) {
        this(localId, level, platformId, targetType, bid, budget, status, null, null, null);
    }

    public static AdsChange bid(Long localId, EcomAdLevel level, String platformId, EcomAdTargetType targetType, BigDecimal bid) {
        return new AdsChange(localId, level, platformId, targetType, bid, null, null);
    }

    /** 补充广告类型与上级的平台 ID（{@code EcomAdChangeService} 提交前调用） */
    public AdsChange withContext(EcomAdType type, String campaignPid, String adGroupPid) {
        return new AdsChange(localId, level, platformId, targetType, bid, budget, status, type, campaignPid, adGroupPid);
    }

    /** 广告类型，为空时为商品推广 */
    public EcomAdType adTypeOrSp() {
        return adType == null ? EcomAdType.SP : adType;
    }
}
