package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.DetailLines;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.modules.ecom.carrier.CarrierConnector;
import com.lemon.erp.modules.ecom.carrier.CarrierContext;
import com.lemon.erp.modules.ecom.carrier.CarrierField;
import com.lemon.erp.modules.ecom.carrier.CarrierInfo;
import com.lemon.erp.modules.ecom.carrier.CarrierRegistry;
import com.lemon.erp.modules.ecom.connector.CredentialCipher;
import com.lemon.erp.modules.ecom.dto.LogisticsEstimate;
import com.lemon.erp.modules.ecom.entity.EcomOrderRule;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.entity.EcomLogisticsChannel;
import com.lemon.erp.modules.ecom.entity.EcomLogisticsRate;
import com.lemon.erp.modules.ecom.mapper.EcomLogisticsChannelMapper;
import com.lemon.erp.modules.ecom.mapper.EcomLogisticsRateMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderRuleMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageMapper;
import com.lemon.erp.modules.ecom.model.EcomConstants;
import com.lemon.erp.modules.scm.service.ScmSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 国际物流渠道与费率：运费 = 固定费 + 每公斤 × 计费重量，按目的国精确匹配优先、通用（*）其次。
 * 渠道可对接物流商面单接口（carrier/CarrierConnector）：物流商、产品代码、环境、自定义地址与授权（CredentialCipher 加密，只写不读）
 */
@Service
@RequiredArgsConstructor
public class EcomLogisticsService {

    private final EcomLogisticsChannelMapper channelMapper;
    private final EcomLogisticsRateMapper rateMapper;
    private final ScmSupplierService supplierService;
    private final DictLookup dictLookup;
    private final CredentialCipher cipher;
    private final CarrierRegistry carrierRegistry;
    private final EcomPackageMapper packageMapper;
    private final EcomOrderRuleMapper ruleMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        c.setCarrierCredentials(mergeCredentials(null, c));
        channelMapper.insert(c);
        saveRates(c);
        return c.getId();
    }

    @Transactional
    public void update(Long id, EcomLogisticsChannel c) {
        EcomLogisticsChannel old = get(id);
        normalize(c);
        c.setCarrierCredentials(mergeCredentials(old, c));
        if (channelMapper.exists(Wrappers.<EcomLogisticsChannel>lambdaQuery().eq(EcomLogisticsChannel::getCode, c.getCode()).ne(EcomLogisticsChannel::getId, id))) {
            throw new BizException("ecom.error.channelCodeExists", c.getCode());
        }
        c.setId(id);
        c.setStatus(null);
        channelMapper.updateById(c);
        // 取消对接、清空产品代码 / 自定义地址时置空（updateById 不更新空值）
        channelMapper.update(Wrappers.<EcomLogisticsChannel>lambdaUpdate().set(EcomLogisticsChannel::getCarrier, c.getCarrier())
                .set(EcomLogisticsChannel::getCarrierService, c.getCarrierService()).set(EcomLogisticsChannel::getCarrierApiUrl, c.getCarrierApiUrl())
                .eq(EcomLogisticsChannel::getId, id));
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
            EcomLogisticsChannel c = get(id);
            if (packageMapper.exists(Wrappers.<EcomPackage>lambdaQuery().eq(EcomPackage::getChannelId, id))
                    || ruleMapper.exists(Wrappers.<EcomOrderRule>lambdaQuery().eq(EcomOrderRule::getChannelId, id))) {
                throw new BizException("ecom.error.ops.channelInUse", c.getName());
            }
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
        if (c.getCarrier() != null) {
            carrierRegistry.require(c.getCarrier());
        }
        c.setCarrierService(StringUtils.hasText(c.getCarrierService()) ? c.getCarrierService().trim() : null);
        c.setCarrierApiUrl(StringUtils.hasText(c.getCarrierApiUrl()) ? c.getCarrierApiUrl().trim() : null);
        if (c.getCarrierApiUrl() != null && !(c.getCarrierApiUrl().startsWith("http://") || c.getCarrierApiUrl().startsWith("https://"))) {
            throw new BizException("ecom.error.ops.carrierUrlInvalid", c.getName());
        }
        if (c.getCarrierSandbox() == null) {
            c.setCarrierSandbox(0);
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

    // ---------------------------------------------------------------- 物流商面单对接

    /** 物流商接口的说明（物流渠道表单按授权字段生成输入框），名称按当前语言 */
    public record CarrierView(String carrier, String name, String displayName, List<FieldView> fields, String productionUrl, String sandboxUrl,
                              String serviceHint, String docUrl) {
    }

    public record FieldView(String key, String label, boolean secret, boolean required) {
    }

    public List<CarrierView> carriers() {
        List<CarrierView> out = new ArrayList<>();
        for (CarrierConnector c : carrierRegistry.all()) {
            CarrierInfo i = c.info();
            List<FieldView> fields = new ArrayList<>();
            for (CarrierField f : i.fields()) {
                fields.add(new FieldView(f.key(), Messages.get(f.labelKey()), f.secret(), f.required()));
            }
            out.add(new CarrierView(i.carrier().name(), Messages.get(i.nameKey()), i.displayName(), fields, i.productionUrl(), i.sandboxUrl(),
                    Messages.get(i.serviceKey()), i.docUrl()));
        }
        return out;
    }

    /** 清除物流商授权 */
    public void clearCarrierCredentials(Long id) {
        get(id);
        channelMapper.update(Wrappers.<EcomLogisticsChannel>lambdaUpdate().set(EcomLogisticsChannel::getCarrierCredentials, null)
                .eq(EcomLogisticsChannel::getId, id));
    }

    /** 渠道的物流商接口上下文（解密授权），渠道没有对接物流商时提示手工填写运单号 */
    public CarrierContext carrierContext(EcomLogisticsChannel c) {
        if (c.getCarrier() == null) {
            throw new BizException("ecom.error.ops.channelNoCarrier", c.getName());
        }
        return new CarrierContext(c.getName(), decrypt(c.getCarrierCredentials()), Integer.valueOf(1).equals(c.getCarrierSandbox()),
                c.getCarrierApiUrl(), c.getCarrierService());
    }

    /** 保存时提交的授权与已保存的合并（空值不修改），加密后返回；没有对接物流商时清空 */
    private String mergeCredentials(EcomLogisticsChannel old, EcomLogisticsChannel c) {
        if (c.getCarrier() == null) {
            return old == null ? null : old.getCarrierCredentials() == null ? null : "";
        }
        Map<String, String> merged = new HashMap<>(old == null || old.getCarrier() != c.getCarrier() ? Map.of() : decrypt(old.getCarrierCredentials()));
        if (c.getCarrierAuth() != null) {
            c.getCarrierAuth().forEach((k, v) -> {
                if (StringUtils.hasText(k) && StringUtils.hasText(v)) {
                    merged.put(k.trim(), v.trim());
                }
            });
        }
        if (merged.isEmpty()) {
            return old != null && old.getCarrierCredentials() != null ? "" : null;
        }
        return cipher.encrypt(objectMapper.writeValueAsString(merged));
    }

    private Map<String, String> decrypt(String stored) {
        String json = cipher.decrypt(stored);
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {
            });
        } catch (JacksonException e) {
            return Map.of();
        }
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
