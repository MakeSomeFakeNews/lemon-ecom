package com.lemon.erp.modules.ecom.carrier;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 调用物流商接口的上下文：解密后的授权、环境、接口地址、渠道的产品代码
 *
 * @param channelName 渠道名称（错误提示用）
 * @param apiBaseUrl  自定义接口地址，为空时按环境取 {@link CarrierInfo} 中的地址
 */
public record CarrierContext(String channelName, Map<String, String> credentials, boolean sandbox, String apiBaseUrl, String serviceCode) {

    public CarrierContext {
        credentials = credentials == null ? Map.of() : Map.copyOf(credentials);
    }

    /** 接口地址（去掉末尾的 /） */
    public String endpoint(CarrierInfo info) {
        String url = StringUtils.hasText(apiBaseUrl) ? apiBaseUrl.trim() : sandbox ? info.sandboxUrl() : info.productionUrl();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** 必填的授权字段，缺少时提示 */
    public String require(CarrierInfo info, String key) {
        String v = credentials.get(key);
        if (!StringUtils.hasText(v)) {
            throw new BizException("ecom.error.ops.carrierCredentialMissing", channelName, Messages.get(info.nameKey()), key);
        }
        return v.trim();
    }

    public String optional(String key) {
        String v = credentials.get(key);
        return StringUtils.hasText(v) ? v.trim() : null;
    }

    public String requireService() {
        if (!StringUtils.hasText(serviceCode)) {
            throw new BizException("ecom.error.ops.carrierServiceMissing", channelName);
        }
        return serviceCode.trim();
    }
}
