package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.util.List;
import java.util.Set;

/**
 * 渠道接口的说明：平台、授权方式、授权字段、是否有沙箱、支持的能力、开发者文档地址
 *
 * @param sandboxBaseUrl 沙箱 / 测试环境的接口地址，为空表示平台没有公开的沙箱（只能用测试店铺）
 * @param docUrl         官方开放平台文档地址
 */
public record ConnectorInfo(EcomPlatform platform, AuthType authType, List<CredentialField> fields, String productionBaseUrl,
                            String sandboxBaseUrl, Set<ConnectorCapability> capabilities, String docUrl) {

    public boolean supports(ConnectorCapability capability) {
        return capabilities.contains(capability);
    }
}
