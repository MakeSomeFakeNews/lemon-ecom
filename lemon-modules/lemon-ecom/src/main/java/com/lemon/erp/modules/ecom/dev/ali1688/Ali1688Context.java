package com.lemon.erp.modules.ecom.dev.ali1688;

/**
 * 调用 1688 接口所需的账号信息（已解密）
 *
 * @param gateway     网关地址（如 https://gw.open.1688.com/openapi，不带结尾斜杠）
 * @param accessToken 访问令牌，授权前为空
 */
public record Ali1688Context(String gateway, String appKey, String appSecret, String accessToken) {
}
