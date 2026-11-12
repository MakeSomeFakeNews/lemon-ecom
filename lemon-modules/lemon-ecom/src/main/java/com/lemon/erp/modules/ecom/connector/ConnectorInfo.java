package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.util.List;
import java.util.Set;

/**
 * 渠道接口的说明：平台、授权方式、授权字段、是否有沙箱、支持的能力、开发者文档地址，以及店铺页面显示的接入说明
 *
 * @param sandboxBaseUrl  沙箱 / 测试环境的接口地址，为空表示平台没有公开的沙箱（只能用测试店铺）
 * @param docUrl          官方开放平台文档地址
 * @param perStoreBaseUrl 每个店铺的接口域名不同（如 Shopify 的 xxx.myshopify.com），productionBaseUrl 只是示意
 * @param redirectNote    回调地址的特殊说明（消息键，如 eBay 的 RuName 要在后台配置 accept URL、TikTok 回调在 Partner Center 配置），没有为 null
 * @param orderWindowNote 拉取订单的时间窗口说明（消息键，如 Ozon 只能按创建时间过滤），没有为 null
 * @param lookbackHours   定时同步订单时在「上次同步时间 − 重叠时间」之外再往前回看的小时数（平台不能按更新时间过滤时用它补抓状态变化），默认 0
 */
public record ConnectorInfo(EcomPlatform platform, AuthType authType, List<CredentialField> fields, String productionBaseUrl,
                            String sandboxBaseUrl, Set<ConnectorCapability> capabilities, String docUrl, boolean perStoreBaseUrl,
                            String redirectNote, String orderWindowNote, int lookbackHours) {

    public ConnectorInfo(EcomPlatform platform, AuthType authType, List<CredentialField> fields, String productionBaseUrl,
                         String sandboxBaseUrl, Set<ConnectorCapability> capabilities, String docUrl) {
        this(platform, authType, fields, productionBaseUrl, sandboxBaseUrl, capabilities, docUrl, false, null, null, 0);
    }

    public boolean supports(ConnectorCapability capability) {
        return capabilities.contains(capability);
    }

    public ConnectorInfo withPerStoreBaseUrl() {
        return new ConnectorInfo(platform, authType, fields, productionBaseUrl, sandboxBaseUrl, capabilities, docUrl, true,
                redirectNote, orderWindowNote, lookbackHours);
    }

    /** @param messageKey 回调地址说明的消息键 */
    public ConnectorInfo withRedirectNote(String messageKey) {
        return new ConnectorInfo(platform, authType, fields, productionBaseUrl, sandboxBaseUrl, capabilities, docUrl, perStoreBaseUrl,
                messageKey, orderWindowNote, lookbackHours);
    }

    /** @param messageKey 订单时间窗口说明的消息键 */
    public ConnectorInfo withOrderWindowNote(String messageKey) {
        return new ConnectorInfo(platform, authType, fields, productionBaseUrl, sandboxBaseUrl, capabilities, docUrl, perStoreBaseUrl,
                redirectNote, messageKey, lookbackHours);
    }

    public ConnectorInfo withLookbackHours(int hours) {
        return new ConnectorInfo(platform, authType, fields, productionBaseUrl, sandboxBaseUrl, capabilities, docUrl, perStoreBaseUrl,
                redirectNote, orderWindowNote, Math.max(0, hours));
    }

    /** 连接器自己缓存的字段键（令牌、过期时间等），修改店铺时始终保留 */
    public Set<String> cachedKeys() {
        if (fields == null) {
            return Set.of();
        }
        return fields.stream().filter(CredentialField::cached).map(CredentialField::key).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
