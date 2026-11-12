package com.lemon.erp.modules.ecom.cs.connector;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 按平台查找客服连接器
 */
@Component
public class MessageConnectorRegistry {

    private final Map<EcomPlatform, MessageConnector> connectors = new EnumMap<>(EcomPlatform.class);

    public MessageConnectorRegistry(ObjectProvider<MessageConnector> provider) {
        provider.orderedStream().forEach(c -> connectors.put(c.platform(), c));
    }

    public Optional<MessageConnector> find(EcomPlatform platform) {
        return Optional.ofNullable(platform == null ? null : connectors.get(platform));
    }

    /** 平台是否有该能力 */
    public boolean supports(EcomPlatform platform, CsCapability capability) {
        return find(platform).map(c -> c.supports(capability)).orElse(false);
    }

    public List<MessageConnector> all() {
        return List.copyOf(connectors.values());
    }
}
