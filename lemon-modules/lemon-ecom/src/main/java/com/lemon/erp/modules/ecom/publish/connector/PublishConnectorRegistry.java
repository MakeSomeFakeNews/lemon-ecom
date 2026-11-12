package com.lemon.erp.modules.ecom.publish.connector;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 按平台查找刊登连接器
 */
@Component
public class PublishConnectorRegistry {

    private final Map<EcomPlatform, PublishConnector> connectors = new EnumMap<>(EcomPlatform.class);

    public PublishConnectorRegistry(ObjectProvider<PublishConnector> provider) {
        provider.orderedStream().forEach(c -> connectors.put(c.platform(), c));
    }

    public Optional<PublishConnector> find(EcomPlatform platform) {
        return Optional.ofNullable(platform == null ? null : connectors.get(platform));
    }

    public boolean supports(EcomPlatform platform, PublishCapability capability) {
        return find(platform).map(c -> c.supports(capability)).orElse(false);
    }

    public List<PublishConnector> all() {
        return List.copyOf(connectors.values());
    }
}
