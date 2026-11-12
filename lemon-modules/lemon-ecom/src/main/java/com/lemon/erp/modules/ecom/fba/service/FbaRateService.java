package com.lemon.erp.modules.ecom.fba.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaFeeRate;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaSizeTier;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaStorageRate;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaFeeRateMapper;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaSizeTierMapper;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaStorageRateMapper;
import com.lemon.erp.modules.ecom.fba.model.FbaStorageFeeType;
import com.lemon.erp.modules.ecom.model.EcomConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * FBA 费率：仓储费费率（月度 / 超龄附加费）、配送费尺寸分段与重量段费率，按站点维护；内置美国、德国、英国、日本站（来源见 docs/development.md「FBA 运营 → 费率来源与维护」）
 */
@Service
@RequiredArgsConstructor
public class FbaRateService {

    private final EcomFbaStorageRateMapper storageRateMapper;
    private final EcomFbaSizeTierMapper tierMapper;
    private final EcomFbaFeeRateMapper feeRateMapper;
    private final DictLookup dictLookup;

    // ---------------------------------------------------------------- 仓储费费率

    public List<EcomFbaStorageRate> storageRates(String marketplace) {
        return storageRateMapper.selectList(Wrappers.<EcomFbaStorageRate>lambdaQuery()
                .eq(marketplace != null && !marketplace.isBlank(), EcomFbaStorageRate::getMarketplace, marketplace)
                .orderByAsc(EcomFbaStorageRate::getMarketplace).orderByAsc(EcomFbaStorageRate::getFeeType)
                .orderByAsc(EcomFbaStorageRate::getSizeCategory).orderByAsc(EcomFbaStorageRate::getMonthFrom).orderByAsc(EcomFbaStorageRate::getAgeFrom));
    }

    /** 站点 → 仓储费费率 */
    public Map<String, List<EcomFbaStorageRate>> storageRatesByMarketplace(Collection<String> marketplaces) {
        if (marketplaces.isEmpty()) {
            return Map.of();
        }
        return storageRateMapper.selectList(Wrappers.<EcomFbaStorageRate>lambdaQuery().in(EcomFbaStorageRate::getMarketplace, marketplaces))
                .stream().collect(Collectors.groupingBy(EcomFbaStorageRate::getMarketplace));
    }

    public Long saveStorageRate(Long id, EcomFbaStorageRate r) {
        dictLookup.check(EcomConstants.MARKETPLACE_DICT, r.getMarketplace(), "ecom.label.marketplace");
        if (r.getFeeType() == FbaStorageFeeType.MONTHLY) {
            if (r.getMonthFrom() == null || r.getMonthTo() == null) {
                throw new BizException("ecom.error.fba.monthRequired");
            }
            r.setAgeFrom(null);
            r.setAgeTo(null);
        } else {
            if (r.getAgeFrom() == null) {
                throw new BizException("ecom.error.fba.ageRequired");
            }
            if (r.getAgeTo() != null && r.getAgeTo() < r.getAgeFrom()) {
                throw new BizException("ecom.error.fba.ageRangeInvalid");
            }
            r.setMonthFrom(null);
            r.setMonthTo(null);
        }
        if (id == null) {
            r.setId(null);
            r.setBuiltin(0);
            storageRateMapper.insert(r);
            return r.getId();
        }
        EcomFbaStorageRate old = storageRateMapper.selectById(id);
        if (old == null) {
            throw new BizException("ecom.error.fba.rateNotFound");
        }
        r.setId(id);
        r.setBuiltin(old.getBuiltin());
        storageRateMapper.updateById(r);
        return id;
    }

    public void deleteStorageRates(List<Long> ids) {
        storageRateMapper.deleteByIds(ids);
    }

    // ---------------------------------------------------------------- 配送费分段与费率

    /** 站点的尺寸分段（含重量段费率），按匹配顺序 */
    public List<EcomFbaSizeTier> tiers(String marketplace) {
        List<EcomFbaSizeTier> tiers = tierMapper.selectList(Wrappers.<EcomFbaSizeTier>lambdaQuery()
                .eq(marketplace != null && !marketplace.isBlank(), EcomFbaSizeTier::getMarketplace, marketplace)
                .orderByAsc(EcomFbaSizeTier::getMarketplace).orderByAsc(EcomFbaSizeTier::getSort).orderByAsc(EcomFbaSizeTier::getId));
        if (tiers.isEmpty()) {
            return tiers;
        }
        List<String> markets = tiers.stream().map(EcomFbaSizeTier::getMarketplace).distinct().toList();
        Map<String, List<EcomFbaFeeRate>> rates = feeRateMapper.selectList(Wrappers.<EcomFbaFeeRate>lambdaQuery()
                        .in(EcomFbaFeeRate::getMarketplace, markets).orderByAsc(EcomFbaFeeRate::getWeightFrom))
                .stream().collect(Collectors.groupingBy(r -> r.getMarketplace() + "|" + r.getTierCode()));
        tiers.forEach(t -> t.setRates(rates.getOrDefault(t.getMarketplace() + "|" + t.getCode(), new ArrayList<>())));
        return tiers;
    }

    /** 站点 → 尺寸分段（含费率），一次查询供批量试算 */
    public Map<String, List<EcomFbaSizeTier>> tiersByMarketplace() {
        return tiers(null).stream().collect(Collectors.groupingBy(EcomFbaSizeTier::getMarketplace, HashMap::new, Collectors.toList()));
    }

    /** 保存分段与它的重量段费率（费率整体替换） */
    @Transactional
    public Long saveTier(Long id, EcomFbaSizeTier t) {
        dictLookup.check(EcomConstants.MARKETPLACE_DICT, t.getMarketplace(), "ecom.label.marketplace");
        String code = t.getCode().trim();
        EcomFbaSizeTier dup = tierMapper.selectOne(Wrappers.<EcomFbaSizeTier>lambdaQuery()
                .eq(EcomFbaSizeTier::getMarketplace, t.getMarketplace()).eq(EcomFbaSizeTier::getCode, code));
        if (dup != null && !dup.getId().equals(id)) {
            throw new BizException("ecom.error.fba.tierCodeExists", code);
        }
        t.setCode(code);
        String oldMarket = t.getMarketplace();
        String oldCode = code;
        if (id == null) {
            t.setId(null);
            t.setBuiltin(0);
            tierMapper.insert(t);
        } else {
            EcomFbaSizeTier old = tierMapper.selectById(id);
            if (old == null) {
                throw new BizException("ecom.error.fba.tierNotFound");
            }
            oldMarket = old.getMarketplace();
            oldCode = old.getCode();
            t.setId(id);
            t.setBuiltin(old.getBuiltin());
            tierMapper.updateById(t);
        }
        feeRateMapper.delete(Wrappers.<EcomFbaFeeRate>lambdaQuery().eq(EcomFbaFeeRate::getMarketplace, oldMarket).eq(EcomFbaFeeRate::getTierCode, oldCode));
        if (t.getRates() != null) {
            for (EcomFbaFeeRate r : t.getRates()) {
                if (r.getWeightTo() != null && r.getWeightTo().compareTo(r.getWeightFrom()) <= 0) {
                    throw new BizException("ecom.error.fba.weightRangeInvalid");
                }
                r.setId(null);
                r.setMarketplace(t.getMarketplace());
                r.setTierCode(code);
                r.setBuiltin(t.getBuiltin());
                feeRateMapper.insert(r);
            }
        }
        return t.getId();
    }

    @Transactional
    public void deleteTiers(List<Long> ids) {
        for (EcomFbaSizeTier t : tierMapper.selectByIds(ids)) {
            feeRateMapper.delete(Wrappers.<EcomFbaFeeRate>lambdaQuery().eq(EcomFbaFeeRate::getMarketplace, t.getMarketplace())
                    .eq(EcomFbaFeeRate::getTierCode, t.getCode()));
            tierMapper.deleteById(t.getId());
        }
    }

    /** 配送费试算 */
    public FbaViews.FeeQuote quote(String marketplace, FbaFeeCalculator.Dims dims) {
        return FbaFeeCalculator.quote(dims, tiers(marketplace));
    }
}
