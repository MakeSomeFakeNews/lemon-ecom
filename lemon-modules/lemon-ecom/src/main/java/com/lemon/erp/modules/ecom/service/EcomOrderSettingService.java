package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.modules.ecom.connector.CredentialCipher;
import com.lemon.erp.modules.ecom.dto.OrderEnhanceViews;
import com.lemon.erp.modules.ecom.entity.EcomOrderSetting;
import com.lemon.erp.modules.ecom.mapper.EcomOrderSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 订单作业设置（每个租户一行）：审核前地址校验、17TRACK 密钥（加密保存，不返回前端）与接口地址、物流跟踪的预警时限。
 * 第一次读取时按默认值建立
 */
@Service
@RequiredArgsConstructor
public class EcomOrderSettingService {

    private static final int DEFAULT_NO_SCAN_HOURS = 72;
    private static final int DEFAULT_TRANSIT_DAYS = 20;
    private static final int DEFAULT_STALE_DAYS = 7;

    private final EcomOrderSettingMapper mapper;
    private final CredentialCipher cipher;

    /** 当前租户的设置（密钥不返回，hasTrackKey 表示是否已填写） */
    public EcomOrderSetting get() {
        EcomOrderSetting s = current();
        s.setHasTrackKey(StringUtils.hasText(s.getTrackKey()));
        return s;
    }

    /** 当前租户的设置（含加密的密钥），没有时建立 */
    EcomOrderSetting current() {
        EcomOrderSetting s = Queries.first(mapper, Wrappers.<EcomOrderSetting>lambdaQuery().orderByAsc(EcomOrderSetting::getId));
        if (s != null) {
            return s;
        }
        EcomOrderSetting d = new EcomOrderSetting();
        d.setAddressCheck(1);
        d.setTrackEnabled(1);
        d.setNoScanHours(DEFAULT_NO_SCAN_HOURS);
        d.setTransitDays(DEFAULT_TRANSIT_DAYS);
        d.setStaleDays(DEFAULT_STALE_DAYS);
        try {
            mapper.insert(d);
            return d;
        } catch (DuplicateKeyException e) {
            return Queries.first(mapper, Wrappers.<EcomOrderSetting>lambdaQuery().orderByAsc(EcomOrderSetting::getId));
        }
    }

    public void save(OrderEnhanceViews.SettingRequest req) {
        EcomOrderSetting s = current();
        var u = Wrappers.<EcomOrderSetting>lambdaUpdate().eq(EcomOrderSetting::getId, s.getId())
                .set(EcomOrderSetting::getAddressCheck, req.addressCheck() == 1 ? 1 : 0)
                .set(EcomOrderSetting::getTrackEnabled, req.trackEnabled() == 1 ? 1 : 0)
                .set(EcomOrderSetting::getNoScanHours, req.noScanHours())
                .set(EcomOrderSetting::getTransitDays, req.transitDays())
                .set(EcomOrderSetting::getStaleDays, req.staleDays())
                .set(EcomOrderSetting::getTrackApiUrl, StringUtils.hasText(req.trackApiUrl()) ? req.trackApiUrl().trim() : null);
        if (req.trackKey() != null && !req.trackKey().isEmpty()) {
            // 传空白清除，传值替换
            u.set(EcomOrderSetting::getTrackKey, req.trackKey().isBlank() ? null : cipher.encrypt(req.trackKey().trim()));
        }
        mapper.update(u);
    }

    /** 审核前是否校验地址 */
    public boolean addressCheckEnabled() {
        Integer v = current().getAddressCheck();
        return v == null || v == 1;
    }

    /** 解密后的 17TRACK 密钥，没有填写为 null */
    String trackKey(EcomOrderSetting s) {
        return StringUtils.hasText(s.getTrackKey()) ? cipher.decrypt(s.getTrackKey()) : null;
    }
}
