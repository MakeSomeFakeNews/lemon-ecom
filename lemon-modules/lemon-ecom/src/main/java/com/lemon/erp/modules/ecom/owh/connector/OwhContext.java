package com.lemon.erp.modules.ecom.owh.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 调用海外仓接口的上下文：解密后的授权、环境、接口地址
 *
 * @param accountName 账号名称（错误提示用）
 * @param apiBaseUrl  自定义接口地址，为空时按环境取 {@link OwhInfo} 中的地址
 * @param currency    账号的默认费用币种（服务商返回的费用没有币种时使用）
 */
public record OwhContext(String accountName, Map<String, String> credentials, boolean sandbox, String apiBaseUrl, String currency) {

    public OwhContext {
        credentials = credentials == null ? Map.of() : Map.copyOf(credentials);
    }

    /** 接口地址（去掉末尾的 /） */
    public String endpoint(OwhInfo info) {
        String url = StringUtils.hasText(apiBaseUrl) ? apiBaseUrl.trim() : sandbox ? info.sandboxUrl() : info.productionUrl();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** 必填的授权字段，缺少时提示 */
    public String require(OwhInfo info, String key) {
        String v = credentials.get(key);
        if (!StringUtils.hasText(v)) {
            throw new BizException("ecom.error.owh.credentialMissing", accountName, Messages.get(info.nameKey()), key);
        }
        return v.trim();
    }

    public String optional(String key) {
        String v = credentials.get(key);
        return StringUtils.hasText(v) ? v.trim() : null;
    }
}
