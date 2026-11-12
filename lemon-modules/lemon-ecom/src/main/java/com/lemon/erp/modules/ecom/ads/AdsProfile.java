package com.lemon.erp.modules.ecom.ads;

/**
 * 广告配置文件（亚马逊一个卖家账号在一个站点上的广告账户）
 *
 * @param profileId     配置文件 ID（调用业务接口时放在 Amazon-Advertising-API-Scope 请求头）
 * @param marketplaceId 站点 ID（如 ATVPDKIKX0DER）
 */
public record AdsProfile(String profileId, String countryCode, String currencyCode, String timezone, String accountName, String accountType,
                         String marketplaceId) {
}
