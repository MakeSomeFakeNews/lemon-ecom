package com.lemon.erp.modules.ecom.fba.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantInitializer;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaFeeRate;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaSizeTier;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaStorageRate;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaFeeRateMapper;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaSizeTierMapper;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaStorageRateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 新租户：从平台复制内置的 FBA 仓储费费率、配送费尺寸分段与费率
 */
@Component
@RequiredArgsConstructor
public class FbaTenantInitializer implements TenantInitializer {

    private final EcomFbaStorageRateMapper storageRateMapper;
    private final EcomFbaSizeTierMapper tierMapper;
    private final EcomFbaFeeRateMapper feeRateMapper;

    @Override
    public void initialize(Context context) {
        List<EcomFbaStorageRate> storage = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID,
                () -> storageRateMapper.selectList(Wrappers.<EcomFbaStorageRate>lambdaQuery().eq(EcomFbaStorageRate::getBuiltin, 1)));
        storage.forEach(r -> storageRateMapper.insert(TenantInitializer.asNew(r)));
        List<EcomFbaSizeTier> tiers = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID,
                () -> tierMapper.selectList(Wrappers.<EcomFbaSizeTier>lambdaQuery().eq(EcomFbaSizeTier::getBuiltin, 1)));
        tiers.forEach(t -> tierMapper.insert(TenantInitializer.asNew(t)));
        List<EcomFbaFeeRate> rates = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID,
                () -> feeRateMapper.selectList(Wrappers.<EcomFbaFeeRate>lambdaQuery().eq(EcomFbaFeeRate::getBuiltin, 1)));
        rates.forEach(r -> feeRateMapper.insert(TenantInitializer.asNew(r)));
    }
}
