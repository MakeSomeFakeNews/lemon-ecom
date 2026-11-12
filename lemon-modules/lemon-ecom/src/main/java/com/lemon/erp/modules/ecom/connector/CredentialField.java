package com.lemon.erp.modules.ecom.connector;

/**
 * 店铺授权信息中的一个字段（店铺表单按它生成输入框）
 *
 * @param key      JSON 键名
 * @param labelKey 显示名称的消息键
 * @param secret   是否敏感（输入框按密码显示，保存后不再回显）
 * @param required 是否必填
 * @param oauth    是否由 OAuth 授权回调自动写入（表单中只读显示「已授权」）
 * @param cached   连接器自己缓存的值（换取的访问令牌、过期时间、令牌归属摘要等）：店铺表单不显示，修改店铺时始终保留原值
 */
public record CredentialField(String key, String labelKey, boolean secret, boolean required, boolean oauth, boolean cached) {

    public CredentialField(String key, String labelKey, boolean secret, boolean required, boolean oauth) {
        this(key, labelKey, secret, required, oauth, false);
    }

    public static CredentialField text(String key, String labelKey, boolean required) {
        return new CredentialField(key, labelKey, false, required, false, false);
    }

    public static CredentialField secret(String key, String labelKey) {
        return new CredentialField(key, labelKey, true, true, false, false);
    }

    public static CredentialField token(String key, String labelKey) {
        return new CredentialField(key, labelKey, true, false, true, false);
    }

    /** 由 OAuth 回调写入、表单只读显示的非敏感字段（平台店铺 ID、卖家名称等） */
    public static CredentialField oauthInfo(String key, String labelKey) {
        return new CredentialField(key, labelKey, false, false, true, false);
    }

    /** 连接器缓存的字段（不显示、修改店铺时保留）；secret 表示值是令牌 */
    public static CredentialField cached(String key, boolean secret) {
        return new CredentialField(key, null, secret, false, false, true);
    }
}
