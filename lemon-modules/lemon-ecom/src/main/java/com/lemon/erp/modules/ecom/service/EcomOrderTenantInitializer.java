package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantInitializer;
import com.lemon.erp.modules.ecom.entity.EcomRemoteArea;
import com.lemon.erp.modules.ecom.mapper.EcomRemoteAreaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 新租户：从平台复制偏远地区邮编段（地址校验识别偏远地区）
 */
@Component
@RequiredArgsConstructor
public class EcomOrderTenantInitializer implements TenantInitializer {

    private final EcomRemoteAreaMapper remoteAreaMapper;

    @Override
    public void initialize(Context context) {
        List<EcomRemoteArea> areas = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID,
                () -> remoteAreaMapper.selectList(Wrappers.<EcomRemoteArea>lambdaQuery().orderByAsc(EcomRemoteArea::getId)));
        areas.forEach(a -> remoteAreaMapper.insert(TenantInitializer.asNew(a)));
    }
}
