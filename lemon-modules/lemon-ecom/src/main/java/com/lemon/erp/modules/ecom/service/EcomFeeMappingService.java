package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.ecom.entity.EcomFeeMapping;
import com.lemon.erp.modules.ecom.mapper.EcomFeeMappingMapper;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.SettlementItemType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 结算费用类型映射：平台 + 原始类型名 → 系统结算明细类型。导入结算报告时没有填写类型的行按店铺平台识别，
 * 平台没有对应映射时用通用映射（平台 OTHER）；匹配忽略首尾空格和大小写
 */
@Service
@RequiredArgsConstructor
public class EcomFeeMappingService {

    private final EcomFeeMappingMapper mappingMapper;

    public PageResult<EcomFeeMapping> page(long current, long size, EcomPlatform platform, SettlementItemType itemType, String keyword) {
        Page<EcomFeeMapping> page = Page.of(current, size);
        mappingMapper.selectPage(page, Wrappers.<EcomFeeMapping>lambdaQuery()
                .eq(platform != null, EcomFeeMapping::getPlatform, platform)
                .eq(itemType != null, EcomFeeMapping::getItemType, itemType)
                .like(StringUtils.hasText(keyword), EcomFeeMapping::getRawKey, keyOf(keyword))
                .orderByAsc(EcomFeeMapping::getPlatform).orderByAsc(EcomFeeMapping::getItemType).orderByAsc(EcomFeeMapping::getId));
        return PageResult.of(page);
    }

    public Long create(EcomFeeMapping m) {
        m.setId(null);
        normalize(m);
        checkUnique(m, null);
        m.setBuiltin(0);
        if (m.getStatus() == null) {
            m.setStatus(EnableStatus.ENABLED);
        }
        mappingMapper.insert(m);
        return m.getId();
    }

    public void update(Long id, EcomFeeMapping m) {
        EcomFeeMapping old = get(id);
        normalize(m);
        checkUnique(m, id);
        m.setId(id);
        m.setBuiltin(old.getBuiltin());
        mappingMapper.updateById(m);
    }

    public void changeStatus(Long id, EnableStatus status) {
        get(id);
        mappingMapper.update(Wrappers.<EcomFeeMapping>lambdaUpdate().set(EcomFeeMapping::getStatus, status).eq(EcomFeeMapping::getId, id));
    }

    public void delete(List<Long> ids) {
        mappingMapper.deleteByIds(ids);
    }

    private EcomFeeMapping get(Long id) {
        EcomFeeMapping m = mappingMapper.selectById(id);
        if (m == null) {
            throw new BizException("ecom.error.fee.mappingNotFound");
        }
        return m;
    }

    private static void normalize(EcomFeeMapping m) {
        m.setRawType(m.getRawType().trim());
        m.setRawKey(keyOf(m.getRawType()));
    }

    private void checkUnique(EcomFeeMapping m, Long excludeId) {
        if (mappingMapper.exists(Wrappers.<EcomFeeMapping>lambdaQuery().eq(EcomFeeMapping::getPlatform, m.getPlatform())
                .eq(EcomFeeMapping::getRawKey, m.getRawKey()).ne(excludeId != null, EcomFeeMapping::getId, excludeId))) {
            throw new BizException("ecom.error.fee.mappingExists", m.getRawType());
        }
    }

    static String keyOf(String raw) {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }

    /** 一次导入使用的识别器：启用的映射一次读出 */
    public Resolver resolver() {
        Map<EcomPlatform, Map<String, SettlementItemType>> byPlatform = new HashMap<>();
        for (EcomFeeMapping m : mappingMapper.selectList(Wrappers.<EcomFeeMapping>lambdaQuery().eq(EcomFeeMapping::getStatus, EnableStatus.ENABLED))) {
            byPlatform.computeIfAbsent(m.getPlatform(), k -> new HashMap<>()).put(m.getRawKey(), m.getItemType());
        }
        return new Resolver(byPlatform);
    }

    /** 原始类型名识别：先按店铺平台，再按通用（OTHER）；也接受直接填写的系统类型名（如 STORAGE） */
    public record Resolver(Map<EcomPlatform, Map<String, SettlementItemType>> byPlatform) {

        public SettlementItemType resolve(EcomPlatform platform, String raw) {
            String key = keyOf(raw);
            if (key == null || key.isEmpty()) {
                return null;
            }
            SettlementItemType t = byPlatform.getOrDefault(platform, Map.of()).get(key);
            if (t == null) {
                t = byPlatform.getOrDefault(EcomPlatform.OTHER, Map.of()).get(key);
            }
            if (t == null) {
                for (SettlementItemType v : SettlementItemType.values()) {
                    if (v.name().equalsIgnoreCase(key)) {
                        return v;
                    }
                }
            }
            return t;
        }
    }
}
