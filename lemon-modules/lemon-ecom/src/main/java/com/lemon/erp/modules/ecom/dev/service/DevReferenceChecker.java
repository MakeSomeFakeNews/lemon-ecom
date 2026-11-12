package com.lemon.erp.modules.ecom.dev.service;

import com.lemon.erp.framework.ref.MergeImpact;
import com.lemon.erp.framework.ref.PartnerMergeHandler;
import com.lemon.erp.framework.ref.PartnerType;
import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceChecker;
import com.lemon.erp.modules.ecom.dev.mapper.DevRefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 删除物料（1688 货源、1688 采购单）、供应商（样品、货源）、用户（开发负责人）前的引用检查；合并供应商时改指向保留的供应商
 */
@Component
@RequiredArgsConstructor
public class DevReferenceChecker implements ReferenceChecker, PartnerMergeHandler {

    private final DevRefMapper refMapper;

    @Override
    public String findReference(RefType type, Long id) {
        return switch (type) {
            case MATERIAL -> refMapper.findMaterial(id);
            case SUPPLIER -> refMapper.findSupplier(id);
            case USER -> refMapper.findUser(id);
            default -> null;
        };
    }

    @Override
    public List<MergeImpact> impacts(PartnerType type, List<Long> sourceIds, Long targetId) {
        if (type != PartnerType.SUPPLIER) {
            return List.of();
        }
        return refMapper.supplierImpacts(sourceIds).stream().filter(i -> i.getTotal() > 0).toList();
    }

    @Override
    public void merge(PartnerType type, List<Long> sourceIds, Long targetId, String targetName) {
        if (type == PartnerType.SUPPLIER) {
            refMapper.mergeSampleSupplier(sourceIds, targetId);
            refMapper.mergeOfferSupplier(sourceIds, targetId);
        }
    }
}
