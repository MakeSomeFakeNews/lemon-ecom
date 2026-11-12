package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.ecom.dto.ReplenishSettingRequest;
import com.lemon.erp.modules.ecom.entity.EcomReplenishSetting;
import com.lemon.erp.modules.ecom.mapper.EcomReplenishSettingMapper;
import com.lemon.erp.modules.ecom.model.ReplenishScope;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 补货设置：在线商品级（listingId）、物料级（materialId）各一份，保存时按级别 + 对象覆盖；全部项为空且未勾选不补货时删除设置
 */
@Service
@RequiredArgsConstructor
public class EcomReplenishSettingService {

    private final EcomReplenishSettingMapper settingMapper;
    private final EcomListingService listingService;
    private final WmsMaterialService materialService;

    /** 在线商品级或物料级的设置，没有时返回 null */
    public EcomReplenishSetting find(ReplenishScope scope, Long listingId, Long materialId) {
        EcomReplenishSetting s = findRaw(scope, listingId, materialId);
        return s == null ? null : settingMapper.selectDetail(s.getId());
    }

    private EcomReplenishSetting findRaw(ReplenishScope scope, Long listingId, Long materialId) {
        if (scope == ReplenishScope.LISTING) {
            return listingId == null ? null : settingMapper.selectOne(Wrappers.<EcomReplenishSetting>lambdaQuery()
                    .eq(EcomReplenishSetting::getScope, ReplenishScope.LISTING).eq(EcomReplenishSetting::getListingId, listingId));
        }
        return materialId == null ? null : settingMapper.selectOne(Wrappers.<EcomReplenishSetting>lambdaQuery()
                .eq(EcomReplenishSetting::getScope, ReplenishScope.MATERIAL).eq(EcomReplenishSetting::getMaterialId, materialId));
    }

    /**
     * 保存（新增或覆盖）
     *
     * @return 设置 ID；全部项为空时删除已有设置并返回 null
     */
    @Transactional
    public Long save(ReplenishSettingRequest req) {
        if (req.scope() == ReplenishScope.LISTING) {
            if (req.listingId() == null) {
                throw new BizException("ecom.error.purchase.settingTargetRequired");
            }
            listingService.get(req.listingId());
        } else {
            if (req.materialId() == null) {
                throw new BizException("ecom.error.purchase.settingTargetRequired");
            }
            materialService.get(req.materialId());
        }
        EcomReplenishSetting old = findRaw(req.scope(), req.listingId(), req.materialId());
        boolean disabled = Boolean.TRUE.equals(req.disabled());
        // 采购周期只用于物料级
        Integer purchaseDays = req.scope() == ReplenishScope.MATERIAL ? req.purchaseDays() : null;
        boolean empty = !disabled && req.leadDays() == null && req.safetyDays() == null && purchaseDays == null
                && req.dailySales() == null && req.factor() == null && (req.remark() == null || req.remark().isBlank());
        if (empty) {
            if (old != null) {
                settingMapper.deleteById(old.getId());
            }
            return null;
        }
        EcomReplenishSetting s = old != null ? old : new EcomReplenishSetting();
        s.setScope(req.scope());
        s.setListingId(req.scope() == ReplenishScope.LISTING ? req.listingId() : null);
        s.setMaterialId(req.scope() == ReplenishScope.MATERIAL ? req.materialId() : null);
        s.setLeadDays(req.leadDays());
        s.setSafetyDays(req.safetyDays());
        s.setPurchaseDays(purchaseDays);
        s.setDailySales(req.dailySales());
        s.setFactor(req.factor());
        s.setDisabled(disabled ? 1 : 0);
        s.setRemark(req.remark());
        if (old == null) {
            settingMapper.insert(s);
        } else {
            // 为空的项要清除（updateById 会跳过 null）
            settingMapper.update(Wrappers.<EcomReplenishSetting>lambdaUpdate()
                    .set(EcomReplenishSetting::getLeadDays, s.getLeadDays())
                    .set(EcomReplenishSetting::getSafetyDays, s.getSafetyDays())
                    .set(EcomReplenishSetting::getPurchaseDays, s.getPurchaseDays())
                    .set(EcomReplenishSetting::getDailySales, s.getDailySales())
                    .set(EcomReplenishSetting::getFactor, s.getFactor())
                    .set(EcomReplenishSetting::getDisabled, s.getDisabled())
                    .set(EcomReplenishSetting::getRemark, s.getRemark())
                    .set(EcomReplenishSetting::getUpdateTime, LocalDateTime.now())
                    .eq(EcomReplenishSetting::getId, s.getId()));
        }
        return s.getId();
    }

    public void delete(Long id) {
        settingMapper.deleteById(id);
    }

    /** 在线商品级设置：listingId → 设置 */
    public Map<Long, EcomReplenishSetting> byListings(Collection<Long> listingIds) {
        if (listingIds.isEmpty()) {
            return Map.of();
        }
        return settingMapper.selectList(Wrappers.<EcomReplenishSetting>lambdaQuery()
                        .eq(EcomReplenishSetting::getScope, ReplenishScope.LISTING).in(EcomReplenishSetting::getListingId, listingIds))
                .stream().collect(Collectors.toMap(EcomReplenishSetting::getListingId, Function.identity(), (a, b) -> a));
    }

    /** 物料级设置：materialId → 设置 */
    public Map<Long, EcomReplenishSetting> byMaterials(Collection<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        List<EcomReplenishSetting> list = settingMapper.selectList(Wrappers.<EcomReplenishSetting>lambdaQuery()
                .eq(EcomReplenishSetting::getScope, ReplenishScope.MATERIAL).in(EcomReplenishSetting::getMaterialId, materialIds));
        return list.stream().collect(Collectors.toMap(EcomReplenishSetting::getMaterialId, Function.identity(), (a, b) -> a));
    }
}
