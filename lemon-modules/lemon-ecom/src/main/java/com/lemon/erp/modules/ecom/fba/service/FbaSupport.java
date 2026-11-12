package com.lemon.erp.modules.ecom.fba.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * FBA 运营的公共方法：取亚马逊店铺、折算本位币
 */
@Component
@RequiredArgsConstructor
public class FbaSupport {

    private final EcomStoreService storeService;
    private final ExchangeRateProvider rateProvider;

    /** 亚马逊店铺，其他平台提示 */
    public EcomStore amazonStore(Long storeId) {
        EcomStore s = storeService.get(storeId);
        if (s.getPlatform() != EcomPlatform.AMAZON) {
            throw new BizException("ecom.error.fba.notAmazonStore", s.getName());
        }
        return s;
    }

    public EcomStore store(Long storeId) {
        return storeService.get(storeId);
    }

    /** 店铺 ID → 店铺 */
    public Map<Long, EcomStore> stores(Collection<Long> ids) {
        Map<Long, EcomStore> map = new HashMap<>();
        storeService.byIds(new TreeSet<>(ids)).forEach(s -> map.put(s.getId(), s));
        return map;
    }

    /** 全部亚马逊店铺（含停用） */
    public List<EcomStore> amazonStores() {
        return storeService.all().stream().filter(s -> s.getPlatform() == EcomPlatform.AMAZON).toList();
    }

    /**
     * 汇率：日期所在月（没有时取之前最近一个月）的汇率，仍没有时取最近维护的汇率；都没有时报错
     */
    public BigDecimal rate(String currency, LocalDate date) {
        BigDecimal r = rateOrNull(currency, date);
        if (r == null) {
            LocalDate d = date == null ? LocalDate.now() : date;
            throw new BizException("ecom.error.exchangeRateMissing", currency, d.getYear() + "-" + String.format("%02d", d.getMonthValue()));
        }
        return r;
    }

    /** 汇率，没有维护时为 null */
    public BigDecimal rateOrNull(String currency, LocalDate date) {
        if (currency == null || currency.isBlank()) {
            return null;
        }
        LocalDate d = date == null ? LocalDate.now() : date;
        BigDecimal r = rateProvider.rate(currency, d);
        if (r == null && d.isBefore(LocalDate.now())) {
            r = rateProvider.rate(currency, LocalDate.now());
        }
        return r;
    }

    /** 折算本位币（2 位小数） */
    public BigDecimal toBase(BigDecimal amount, String currency, LocalDate date) {
        if (amount == null || amount.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return amount.multiply(rate(currency, date)).setScale(2, RoundingMode.HALF_UP);
    }
}
