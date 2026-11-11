package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.modules.ecom.dto.LogisticsEstimate;
import com.lemon.erp.modules.ecom.entity.EcomLogisticsChannel;
import com.lemon.erp.modules.ecom.entity.EcomLogisticsRate;
import com.lemon.erp.modules.ecom.mapper.EcomLogisticsChannelMapper;
import com.lemon.erp.modules.ecom.mapper.EcomLogisticsRateMapper;
import com.lemon.erp.modules.ecom.model.EcomConstants;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 国际物流渠道与费率：运费 = 固定费 + 每公斤 × 计费重量，按目的国精确匹配优先、通用（*）其次
 */
@Service
@RequiredArgsConstructor
public class EcomLogisticsService {

    private final EcomLogisticsChannelMapper channelMapper;
    private final EcomLogisticsRateMapper rateMapper;
    private final ScmSupplierService supplierService;
    private final DictLookup dictLookup;

    public PageResult<EcomLogisticsChannel> page(long current, long size, String keyword, EnableStatus status, String transportMode) {
        Page<EcomLogisticsChannel> page = Page.of(current, size);
        channelMapper.selectPageList(page, keyword, status, transportMode);
        return PageResult.of(page);
    }

    public EcomLogisticsChannel detail(Long id) {
        EcomLogisticsChannel c = channelMapper.selectDetail(id);
        if (c == null) {
            throw new BizException("ecom.error.channelNotFound");
        }
        c.setRates(rateMapper.selectList(Wrappers.<EcomLogisticsRate>lambdaQuery().eq(EcomLogisticsRate::getChannelId, id)
                .orderByAsc(EcomLogisticsRate::getCountry).orderByAsc(EcomLogisticsRate::getMinWeight)));
        return c;
    }

    public EcomLogisticsChannel get(Long id) {
        EcomLogisticsChannel c = id == null ? null : channelMapper.selectById(id);
        if (c == null) {
            throw new BizException("ecom.error.channelNotFound");
        }
        return c;
    }

    /** 启用的渠道（选择用） */
    public List<EcomLogisticsChannel> options() {
        return channelMapper.selectList(Wrappers.<EcomLogisticsChannel>lambdaQuery()
                .select(EcomLogisticsChannel::getId, EcomLogisticsChannel::getCode, EcomLogisticsChannel::getName, EcomLogisticsChannel::getProvider,
                        EcomLogisticsChannel::getTransportMode, EcomLogisticsChannel::getDaysMin, EcomLogisticsChannel::getDaysMax)
                .eq(EcomLogisticsChannel::getStatus, EnableStatus.ENABLED).orderByAsc(EcomLogisticsChannel::getId));
    }

    @Transactional
    public Long create(EcomLogisticsChannel c) {
        c.setId(null);
        normalize(c);
        if (channelMapper.exists(Wrappers.<EcomLogisticsChannel>lambdaQuery().eq(EcomLogisticsChannel::getCode, c.getCode()))) {
            throw new BizException("ecom.error.channelCodeExists", c.getCode());
        }
        if (c.getStatus() == null) c.setStatus(EnableStatus.ENABLED);
        channelMapper.insert(c);
        saveRates(c);
        return c.getId();
    }

    @Transactional
    public void update(Long id, EcomLogisticsChannel c) {
        get(id);
        normalize(c);
        if (channelMapper.exists(Wrappers.<EcomLogisticsChannel>lambdaQuery().eq(EcomLogisticsChannel::getCode, c.getCode()).ne(EcomLogisticsChannel::getId, id))) {
            throw new BizException("ecom.error.channelCodeExists", c.getCode());
        }
        c.setId(id);
        c.setStatus(null);
        channelMapper.updateById(c);
        if (c.getRates() != null) {
            saveRates(c);
        }
    }

    public void changeStatus(Long id, EnableStatus status) {
        get(id);
        channelMapper.update(Wrappers.<EcomLogisticsChannel>lambdaUpdate().set(EcomLogisticsChannel::getStatus, status).eq(EcomLogisticsChannel::getId, id));
    }

    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            get(id);
            rateMapper.delete(Wrappers.<EcomLogisticsRate>lambdaQuery().eq(EcomLogisticsRate::getChannelId, id));
        }
        channelMapper.deleteByIds(ids);
    }

    private void normalize(EcomLogisticsChannel c) {
        c.setCode(c.getCode().trim().toUpperCase());
        c.setName(c.getName().trim());
        dictLookup.check(EcomConstants.TRANSPORT_MODE_DICT, c.getTransportMode(), "ecom.label.transportMode");
        if (c.getSupplierId() != null) {
            supplierService.get(c.getSupplierId());
        }
        if (c.getDaysMin() != null && c.getDaysMax() != null && c.getDaysMax() < c.getDaysMin()) {
            throw new BizException("ecom.error.daysRangeInvalid");
        }
        if (c.getRates() != null) {
            for (EcomLogisticsRate r : c.getRates()) {
                r.setCountry(r.getCountry().trim().toUpperCase());
                r.setMinWeight(Decimals.nz(r.getMinWeight()));
                r.setBaseFee(Decimals.money(Decimals.nz(r.getBaseFee())));
                r.setPerKg(Decimals.money(Decimals.nz(r.getPerKg())));
                if (r.getMaxWeight() != null && r.getMaxWeight().compareTo(r.getMinWeight()) <= 0) {
                    throw new BizException("ecom.error.rateWeightRangeInvalid", r.getCountry());
                }
            }
        }
    }

    private void saveRates(EcomLogisticsChannel c) {
        List<EcomLogisticsRate> rates = c.getRates() == null ? List.of() : c.getRates();
        DetailLines.replace(rateMapper, EcomLogisticsRate::getChannelId, c.getId(), rates, EcomLogisticsRate::setChannelId, rateMapper::insertBatch);
    }

    /**
     * 运费试算：每条启用渠道取匹配目的国的费率（没有时取通用费率），运费 = 固定费 + 每公斤 × 重量
     */
    public List<LogisticsEstimate> estimate(String country, BigDecimal weight) {
        if (country == null || country.isBlank() || weight == null || weight.signum() < 0) {
            return List.of();
        }
        Map<Long, EcomLogisticsRate> best = new LinkedHashMap<>();
        for (EcomLogisticsRate r : rateMapper.selectCandidates(country.trim().toUpperCase(), weight)) {
            EcomLogisticsRate cur = best.get(r.getChannelId());
            if (cur == null || ("*".equals(cur.getCountry()) && !"*".equals(r.getCountry()))) {
                best.put(r.getChannelId(), r);
            }
        }
        List<LogisticsEstimate> out = new ArrayList<>();
        for (EcomLogisticsRate r : best.values()) {
            out.add(new LogisticsEstimate(r.getChannelId(), r.getChannelCode(), r.getChannelName(), r.getProvider(), r.getTransportMode(),
                    fee(r, weight), r.getDaysMin(), r.getDaysMax(), r.getCountry()));
        }
        out.sort((a, b) -> a.fee().compareTo(b.fee()));
        return out;
    }

    /** 指定渠道的运费，没有匹配费率时为 null */
    public BigDecimal estimate(Long channelId, String country, BigDecimal weight) {
        if (channelId == null) {
            return null;
        }
        return estimate(country, weight).stream().filter(e -> e.channelId().equals(channelId)).map(LogisticsEstimate::fee).findFirst().orElse(null);
    }

    private static BigDecimal fee(EcomLogisticsRate r, BigDecimal weight) {
        return Decimals.money(Decimals.nz(r.getBaseFee()).add(Decimals.nz(r.getPerKg()).multiply(weight)));
    }
}
