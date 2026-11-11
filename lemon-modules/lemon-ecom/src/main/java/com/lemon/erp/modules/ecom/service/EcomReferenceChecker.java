package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.framework.ref.RefType;
import com.lemon.erp.framework.ref.ReferenceChecker;
import com.lemon.erp.modules.ecom.mapper.EcomRefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 删除物料、仓库、客户、供应商、用户前检查本模块是否引用
 */
@Component
@RequiredArgsConstructor
public class EcomReferenceChecker implements ReferenceChecker {

    private final EcomRefMapper refMapper;

    @Override
    public String findReference(RefType type, Long id) {
        return switch (type) {
            case MATERIAL -> refMapper.findMaterial(id);
            case WAREHOUSE -> refMapper.findWarehouse(id);
            case CUSTOMER -> refMapper.findCustomer(id);
            case SUPPLIER -> refMapper.findSupplier(id);
            case USER -> refMapper.findUser(id);
            default -> null;
        };
    }
}
