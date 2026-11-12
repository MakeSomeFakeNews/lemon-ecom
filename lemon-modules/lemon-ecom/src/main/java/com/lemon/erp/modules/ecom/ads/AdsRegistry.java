package com.lemon.erp.modules.ecom.ads;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 广告接口注册表：收集全部 {@link AdsConnector} Bean，按平台查找；同一平台有两个实现时启动失败
 */
@Component
public class AdsRegistry {

    private final Map<EcomPlatform, AdsConnector> connectors = new EnumMap<>(EcomPlatform.class);

    public AdsRegistry(List<AdsConnector> list) {
        for (AdsConnector c : list) {
            if (connectors.put(c.platform(), c) != null) {
                throw new IllegalStateException("Duplicate ads connector for platform " + c.platform());
            }
        }
    }

    public Optional<AdsConnector> find(EcomPlatform platform) {
        return Optional.ofNullable(platform == null ? null : connectors.get(platform));
    }

    public List<AdsConnector> all() {
        return List.copyOf(connectors.values());
    }
}
