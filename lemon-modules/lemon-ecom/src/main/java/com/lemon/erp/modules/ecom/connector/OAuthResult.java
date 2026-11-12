package com.lemon.erp.modules.ecom.connector;

import java.util.Map;

/**
 * OAuth 授权回调换取令牌的结果：要写入店铺授权信息的字段（access_token、refresh_token、过期时间、平台店铺 ID 等）
 *
 * @param sellerId 平台卖家 / 店铺 ID，写入店铺的卖家 ID（为空不修改）
 */
public record OAuthResult(Map<String, String> credentials, String sellerId) {
}
