package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantInitializer;
import com.lemon.erp.modules.ecom.entity.EcomFeeMapping;
import com.lemon.erp.modules.ecom.entity.EcomTaxRate;
import com.lemon.erp.modules.ecom.mapper.EcomFeeMappingMapper;
import com.lemon.erp.modules.ecom.mapper.EcomTaxRateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 新租户：从平台复制内置的费用类型映射和示例税率（税率保持平台中的启用状态，示例税率默认停用）
 */
@Component
@RequiredArgsConstructor
public class EcomProfitTenantInitializer implements TenantInitializer {

    private final EcomFeeMappingMapper mappingMapper;
    private final EcomTaxRateMapper rateMapper;

    @Override
    public void initialize(Context context) {
        List<EcomFeeMapping> mappings = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID,
                () -> mappingMapper.selectList(Wrappers.<EcomFeeMapping>lambdaQuery().eq(EcomFeeMapping::getBuiltin, 1)));
        mappings.forEach(m -> mappingMapper.insert(TenantInitializer.asNew(m)));
        List<EcomTaxRate> rates = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID,
                () -> rateMapper.selectList(Wrappers.<EcomTaxRate>lambdaQuery().eq(EcomTaxRate::getBuiltin, 1)));
        rates.forEach(r -> rateMapper.insert(TenantInitializer.asNew(r)));
    }
}
