package com.lemon.erp.modules.ecom.dev.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantInitializer;
import com.lemon.erp.modules.ecom.dev.entity.EcomCommissionRate;
import com.lemon.erp.modules.ecom.dev.mapper.EcomCommissionRateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 新租户：从平台复制内置的平台佣金费率
 */
@Component
@RequiredArgsConstructor
public class DevTenantInitializer implements TenantInitializer {

    private final EcomCommissionRateMapper rateMapper;

    @Override
    public void initialize(Context context) {
        List<EcomCommissionRate> rates = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID,
                () -> rateMapper.selectList(Wrappers.<EcomCommissionRate>lambdaQuery().eq(EcomCommissionRate::getBuiltin, 1)));
        rates.forEach(r -> rateMapper.insert(TenantInitializer.asNew(r)));
    }
}
