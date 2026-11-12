package com.lemon.erp.modules.ecom.connector;

/**
 * 店铺授权信息中的一个字段（店铺表单按它生成输入框）
 *
 * @param key      JSON 键名
 * @param labelKey 显示名称的消息键
 * @param secret   是否敏感（输入框按密码显示，保存后不再回显）
 * @param required 是否必填
 * @param oauth    是否由 OAuth 授权回调自动写入（表单中只读显示「已授权」）
 */
public record CredentialField(String key, String labelKey, boolean secret, boolean required, boolean oauth) {

    public static CredentialField text(String key, String labelKey, boolean required) {
        return new CredentialField(key, labelKey, false, required, false);
    }

    public static CredentialField secret(String key, String labelKey) {
        return new CredentialField(key, labelKey, true, true, false);
    }

    public static CredentialField token(String key, String labelKey) {
        return new CredentialField(key, labelKey, true, false, true);
    }
}
