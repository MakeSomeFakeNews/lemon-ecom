package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.connector.AuthType;
import com.lemon.erp.modules.ecom.connector.ConnectorCapability;
import com.lemon.erp.modules.ecom.connector.ConnectorInfo;
import com.lemon.erp.modules.ecom.connector.CredentialField;
import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.util.List;
import java.util.Set;

/**
 * 返回前端的渠道接口说明：授权字段的名称已按当前语言解析（labelKey 是后端消息键，前端不翻译）
 *
 * @param hasSandbox 平台是否有公开的沙箱环境，没有时只能用测试店铺验证
 */
public record ConnectorView(EcomPlatform platform, AuthType authType, List<Field> fields, String productionBaseUrl, String sandboxBaseUrl,
                            boolean hasSandbox, Set<ConnectorCapability> capabilities, String docUrl) {

    /** 授权字段：label 为当前语言的名称 */
    public record Field(String key, String label, boolean secret, boolean required, boolean oauth) {
    }

    public static ConnectorView of(ConnectorInfo info) {
        List<Field> fields = info.fields() == null ? List.of() : info.fields().stream().map(ConnectorView::field).toList();
        return new ConnectorView(info.platform(), info.authType(), fields, info.productionBaseUrl(), info.sandboxBaseUrl(),
                info.sandboxBaseUrl() != null && !info.sandboxBaseUrl().isBlank(),
                info.capabilities() == null ? Set.of() : info.capabilities(), info.docUrl());
    }

    private static Field field(CredentialField f) {
        String label = f.labelKey() == null ? f.key() : Messages.get(f.labelKey());
        return new Field(f.key(), label, f.secret(), f.required(), f.oauth());
    }
}
