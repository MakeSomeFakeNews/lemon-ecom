package com.lemon.erp.modules.ecom.carrier;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.model.EcomCarrier;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 收集全部物流商接口，按 {@link EcomCarrier} 查找
 */
@Component
public class CarrierRegistry {

    private final Map<EcomCarrier, CarrierConnector> connectors = new EnumMap<>(EcomCarrier.class);

    public CarrierRegistry(List<CarrierConnector> list) {
        for (CarrierConnector c : list) {
            connectors.put(c.carrier(), c);
        }
    }

    public CarrierConnector require(EcomCarrier carrier) {
        CarrierConnector c = carrier == null ? null : connectors.get(carrier);
        if (c == null) {
            throw new BizException("ecom.error.ops.carrierUnsupported", carrier == null ? "-" : carrier.name());
        }
        return c;
    }

    public List<CarrierConnector> all() {
        return connectors.values().stream().sorted(Comparator.comparing(CarrierConnector::carrier)).toList();
    }
}
