package com.lemon.erp.modules.ecom.ads;

import com.lemon.erp.modules.ecom.model.EcomAdMatchType;

/**
 * 要添加的否定关键词（广告组级）
 */
public record AdsNegativeKeyword(Long localId, String campaignPlatformId, String adGroupPlatformId, String text, EcomAdMatchType matchType) {
}
