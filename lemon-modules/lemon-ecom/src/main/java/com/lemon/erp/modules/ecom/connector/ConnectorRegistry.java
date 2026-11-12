package com.lemon.erp.modules.ecom.connector;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * 按平台查找渠道接口实现
 */
@Component
public class ConnectorRegistry {

    private final Map<EcomPlatform, ChannelConnector> connectors = new EnumMap<>(EcomPlatform.class);

    public ConnectorRegistry(ObjectProvider<ChannelConnector> provider) {
        provider.orderedStream().forEach(c -> connectors.put(c.platform(), c));
    }

    public Optional<ChannelConnector> find(EcomPlatform platform) {
        return Optional.ofNullable(connectors.get(platform));
    }

    /** 全部连接器，按平台枚举顺序 */
    public java.util.List<ChannelConnector> all() {
        return java.util.List.copyOf(connectors.values());
    }

    public boolean supports(EcomPlatform platform) {
        return connectors.containsKey(platform);
    }
}
