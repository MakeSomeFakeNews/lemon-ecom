package com.lemon.erp.modules.ecom.carrier;

/**
 * 物流商授权的一个字段（物流渠道表单按它生成输入框）
 *
 * @param key      JSON 键名
 * @param labelKey 显示名称的消息键
 * @param secret   是否敏感（按密码输入，保存后不回显）
 * @param required 是否必填
 */
public record CarrierField(String key, String labelKey, boolean secret, boolean required) {

    public static CarrierField text(String key, String labelKey, boolean required) {
        return new CarrierField(key, labelKey, false, required);
    }

    public static CarrierField secret(String key, String labelKey) {
        return new CarrierField(key, labelKey, true, true);
    }
}
