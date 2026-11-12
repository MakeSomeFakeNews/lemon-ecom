package com.lemon.erp.modules.ecom.owh.connector;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.owh.model.OwhProvider;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 收集全部海外仓接口，按 {@link OwhProvider} 查找
 */
@Component
public class OwhRegistry {

    private final Map<OwhProvider, OwhConnector> connectors = new EnumMap<>(OwhProvider.class);

    public OwhRegistry(List<OwhConnector> list) {
        for (OwhConnector c : list) {
            connectors.put(c.provider(), c);
        }
    }

    public OwhConnector require(OwhProvider provider) {
        OwhConnector c = provider == null ? null : connectors.get(provider);
        if (c == null) {
            throw new BizException("ecom.error.owh.providerUnsupported", provider == null ? "-" : provider.name());
        }
        return c;
    }

    public List<OwhConnector> all() {
        return connectors.values().stream().sorted(Comparator.comparing(OwhConnector::provider)).toList();
    }
}
