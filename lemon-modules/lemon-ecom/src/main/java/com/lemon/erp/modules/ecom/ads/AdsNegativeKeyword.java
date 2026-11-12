package com.lemon.erp.modules.ecom.ads;

import com.lemon.erp.modules.ecom.model.EcomAdMatchType;
import com.lemon.erp.modules.ecom.model.EcomAdType;

/**
 * 要添加的否定关键词（广告组级）
 *
 * @param adType 所属活动的广告类型（亚马逊 SP / SB 的接口不同），为空按商品推广
 */
public record AdsNegativeKeyword(Long localId, String campaignPlatformId, String adGroupPlatformId, String text, EcomAdMatchType matchType,
                                 EcomAdType adType) {

    public AdsNegativeKeyword(Long localId, String campaignPlatformId, String adGroupPlatformId, String text, EcomAdMatchType matchType) {
        this(localId, campaignPlatformId, adGroupPlatformId, text, matchType, null);
    }

    public EcomAdType adTypeOrSp() {
        return adType == null ? EcomAdType.SP : adType;
    }
}
