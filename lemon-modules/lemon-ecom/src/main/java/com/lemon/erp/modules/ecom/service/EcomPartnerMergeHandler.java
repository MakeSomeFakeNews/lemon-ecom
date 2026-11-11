package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.framework.ref.MergeImpact;
import com.lemon.erp.framework.ref.PartnerMergeHandler;
import com.lemon.erp.framework.ref.PartnerType;
import com.lemon.erp.modules.ecom.mapper.EcomRefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 往来单位合并（客户）：店铺对应的客户改为保留的客户
 */
@Component
@RequiredArgsConstructor
public class EcomPartnerMergeHandler implements PartnerMergeHandler {

    private final EcomRefMapper refMapper;

    @Override
    public List<MergeImpact> impacts(PartnerType type, List<Long> sourceIds, Long targetId) {
        if (type != PartnerType.CUSTOMER) {
            return List.of();
        }
        long n = refMapper.countStoresByCustomers(sourceIds);
        return n == 0 ? List.of() : List.of(new MergeImpact("ecom.label.store", n, 0));
    }

    @Override
    public void merge(PartnerType type, List<Long> sourceIds, Long targetId, String targetName) {
        if (type == PartnerType.CUSTOMER) {
            refMapper.mergeStoreCustomer(sourceIds, targetId);
        }
    }
}
