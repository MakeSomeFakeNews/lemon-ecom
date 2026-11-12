package com.lemon.erp.modules.ecom.assistant.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantInitializer;
import com.lemon.erp.modules.ecom.assistant.entity.EcomBannedWord;
import com.lemon.erp.modules.ecom.assistant.mapper.EcomBannedWordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 新租户：从平台复制内置的违禁词 */
@Component
@RequiredArgsConstructor
public class EcomBannedWordTenantInitializer implements TenantInitializer {

    private final EcomBannedWordMapper wordMapper;

    @Override
    public void initialize(Context context) {
        List<EcomBannedWord> words = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID,
                () -> wordMapper.selectList(Wrappers.<EcomBannedWord>lambdaQuery().eq(EcomBannedWord::getBuiltin, 1)));
        if (!words.isEmpty()) {
            words.forEach(w -> {
                TenantInitializer.asNew(w);
                w.setCreateBy(null);
            });
            wordMapper.insertBatch(words);
        }
    }
}
