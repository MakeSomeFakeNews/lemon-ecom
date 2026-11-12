package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomOrderRule;
import com.lemon.erp.modules.ecom.mapper.EcomOrderRuleMapper;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 审单规则：维护与匹配。匹配逻辑见 {@link #evaluate}
 */
@Service
@RequiredArgsConstructor
public class EcomOrderRuleService {

    private final EcomOrderRuleMapper ruleMapper;
    private final EcomStoreService storeService;
    private final EcomLogisticsService logisticsService;
    private final WmsWarehouseService warehouseService;

    public PageResult<EcomOrderRule> page(long current, long size, String keyword, EnableStatus status) {
        Page<EcomOrderRule> page = Page.of(current, size);
        ruleMapper.selectPageList(page, keyword, status);
        return PageResult.of(page);
    }

    public EcomOrderRule get(Long id) {
        EcomOrderRule r = id == null ? null : ruleMapper.selectById(id);
        if (r == null) {
            throw new BizException("ecom.error.ops.ruleNotFound");
        }
        return r;
    }

    public Long create(EcomOrderRule r) {
        r.setId(null);
        normalize(r);
        if (r.getStatus() == null) {
            r.setStatus(EnableStatus.ENABLED);
        }
        ruleMapper.insert(r);
        return r.getId();
    }

    /** 修改：条件、动作整体替换（为空的条件表示不限） */
    public void update(Long id, EcomOrderRule r) {
        get(id);
        normalize(r);
        r.setId(id);
        if (r.getStatus() == null) {
            r.setStatus(get(id).getStatus());
        }
        ruleMapper.updateById(r);
        // 清空的条件、动作（updateById 不更新空值）
        ruleMapper.update(Wrappers.<EcomOrderRule>lambdaUpdate()
                .set(EcomOrderRule::getStoreIds, r.getStoreIds()).set(EcomOrderRule::getPlatforms, r.getPlatforms())
                .set(EcomOrderRule::getFulfillment, r.getFulfillment()).set(EcomOrderRule::getCountries, r.getCountries())
                .set(EcomOrderRule::getSkus, r.getSkus()).set(EcomOrderRule::getMaterialIds, r.getMaterialIds())
                .set(EcomOrderRule::getAmountMin, r.getAmountMin()).set(EcomOrderRule::getAmountMax, r.getAmountMax())
                .set(EcomOrderRule::getWeightMin, r.getWeightMin()).set(EcomOrderRule::getWeightMax, r.getWeightMax())
                .set(EcomOrderRule::getWarehouseId, r.getWarehouseId()).set(EcomOrderRule::getChannelId, r.getChannelId())
                .set(EcomOrderRule::getHoldReason, r.getHoldReason()).set(EcomOrderRule::getCutoffTime, r.getCutoffTime())
                .set(EcomOrderRule::getRemark, r.getRemark())
                .eq(EcomOrderRule::getId, id));
    }

    public void changeStatus(Long id, EnableStatus status) {
        get(id);
        ruleMapper.update(Wrappers.<EcomOrderRule>lambdaUpdate().set(EcomOrderRule::getStatus, status).eq(EcomOrderRule::getId, id));
    }

    public void delete(List<Long> ids) {
        ids.forEach(this::get);
        ruleMapper.deleteByIds(ids);
    }

    private void normalize(EcomOrderRule r) {
        r.setName(r.getName().trim());
        r.setStoreIds(idList(r.getStoreIds(), "ecom.label.ops.ruleStores"));
        r.setMaterialIds(idList(r.getMaterialIds(), "ecom.label.ops.ruleMaterials"));
        r.setCountries(textList(r.getCountries(), true));
        r.setSkus(textList(r.getSkus(), false));
        String platforms = textList(r.getPlatforms(), true);
        if (platforms != null) {
            for (String p : platforms.split(",")) {
                try {
                    EcomPlatform.valueOf(p);
                } catch (IllegalArgumentException e) {
                    throw new BizException("ecom.error.ops.rulePlatformInvalid", p);
                }
            }
        }
        r.setPlatforms(platforms);
        if (r.getStoreIds() != null) {
            for (String id : r.getStoreIds().split(",")) {
                storeService.get(Long.valueOf(id));
            }
        }
        if (r.getAmountMin() != null && r.getAmountMax() != null && r.getAmountMin().compareTo(r.getAmountMax()) > 0
                || r.getWeightMin() != null && r.getWeightMax() != null && r.getWeightMin().compareTo(r.getWeightMax()) > 0) {
            throw new BizException("ecom.error.ops.ruleRangeInvalid");
        }
        if (r.getWarehouseId() != null) {
            warehouseService.get(r.getWarehouseId());
        }
        if (r.getChannelId() != null) {
            logisticsService.get(r.getChannelId());
        }
        r.setHoldOrder(flag(r.getHoldOrder()));
        r.setAutoAudit(flag(r.getAutoAudit()));
        r.setStopOnMatch(flag(r.getStopOnMatch()));
        if (r.getHoldOrder() == 1 && r.getAutoAudit() == 1) {
            throw new BizException("ecom.error.ops.ruleHoldAndAudit");
        }
        r.setHoldReason(StringUtils.hasText(r.getHoldReason()) ? r.getHoldReason().trim() : null);
        if (r.getWarehouseId() == null && r.getChannelId() == null && r.getHoldOrder() == 0 && r.getAutoAudit() == 0 && r.getCutoffTime() == null) {
            throw new BizException("ecom.error.ops.ruleNoAction");
        }
    }

    private static int flag(Integer v) {
        return v != null && v == 1 ? 1 : 0;
    }

    /** 逗号分隔的 ID，去重；非数字时提示 */
    private static String idList(String raw, String labelKey) {
        String s = textList(raw, false);
        if (s == null) {
            return null;
        }
        for (String part : s.split(",")) {
            if (!part.matches("\\d{1,18}")) {
                throw new BizException("ecom.error.ops.ruleIdListInvalid", Messages.get(labelKey), part);
            }
        }
        return s;
    }

    /** 逗号（中英文）、空白分隔的值，去重、去空，为空返回 null */
    static String textList(String raw, boolean upper) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        Set<String> values = new LinkedHashSet<>();
        for (String part : raw.split("[,，;；\\s]+")) {
            String v = part.trim();
            if (!v.isEmpty()) {
                values.add(upper ? v.toUpperCase(Locale.ROOT) : v);
            }
        }
        return values.isEmpty() ? null : String.join(",", values);
    }

    private static Set<String> set(String csv, boolean upper) {
        if (csv == null) {
            return Set.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(v -> !v.isEmpty()).map(v -> upper ? v.toUpperCase(Locale.ROOT) : v)
                .collect(Collectors.toSet());
    }

    // ---------------------------------------------------------------- 匹配

    /**
     * 规则匹配的结果
     *
     * @param warehouseId 第一条设置了发货仓的命中规则
     * @param channelId   第一条设置了物流渠道的命中规则
     * @param cutoffTime  第一条设置了截单时间的命中规则
     * @param holdRule    第一条要求人工审核的命中规则（有它时不自动审核）
     * @param autoAudit   有命中规则要求自动审核
     * @param matched     命中的规则名称
     */
    public record Outcome(Long warehouseId, Long channelId, LocalTime cutoffTime, EcomOrderRule holdRule, boolean autoAudit, List<String> matched) {

        /** 计划发货时间：付款（没有时按下单）在截单时间之前当天，之后次日，时间为截单时间 */
        public LocalDateTime planShipTime(EcomOrder o) {
            if (cutoffTime == null) {
                return null;
            }
            LocalDateTime base = o.getPaidTime() != null ? o.getPaidTime() : o.getOrderTime();
            if (base == null) {
                return null;
            }
            LocalDate day = base.toLocalTime().isAfter(cutoffTime) ? base.toLocalDate().plusDays(1) : base.toLocalDate();
            return day.atTime(cutoffTime);
        }
    }

    /**
     * 按优先级逐条匹配启用的规则：规则中填写的条件都满足才命中（为空的条件不限）——店铺、平台、配送方式、收件国家在列表中；
     * 平台 SKU / 物料：订单任一行在列表中；订单金额（本位币）、重量（kg）在区间内（含两端，订单没有重量时重量条件不命中）。
     * 命中规则的动作：发货仓、物流渠道、截单时间取第一条设置了它的规则；任何一条要求人工审核即挂起（原因取第一条）；
     * 没有挂起且有规则要求自动审核时自动审核；规则勾选了「命中后停止」时不再匹配后面的规则
     */
    public Outcome evaluate(EcomOrder o, EcomPlatform platform, List<EcomOrderItem> items) {
        Long warehouse = null, channel = null;
        LocalTime cutoff = null;
        EcomOrderRule hold = null;
        boolean audit = false;
        List<String> matched = new ArrayList<>();
        for (EcomOrderRule r : ruleMapper.selectEnabled()) {
            if (!matches(r, o, platform, items)) {
                continue;
            }
            matched.add(r.getName());
            if (warehouse == null) warehouse = r.getWarehouseId();
            if (channel == null) channel = r.getChannelId();
            if (cutoff == null) cutoff = r.getCutoffTime();
            if (hold == null && Integer.valueOf(1).equals(r.getHoldOrder())) hold = r;
            if (Integer.valueOf(1).equals(r.getAutoAudit())) audit = true;
            if (Integer.valueOf(1).equals(r.getStopOnMatch())) {
                break;
            }
        }
        return new Outcome(warehouse, channel, cutoff, hold, audit && hold == null, matched);
    }

    static boolean matches(EcomOrderRule r, EcomOrder o, EcomPlatform platform, List<EcomOrderItem> items) {
        if (r.getStoreIds() != null && !set(r.getStoreIds(), false).contains(String.valueOf(o.getStoreId()))) {
            return false;
        }
        if (r.getPlatforms() != null && (platform == null || !set(r.getPlatforms(), true).contains(platform.name()))) {
            return false;
        }
        if (r.getFulfillment() != null && r.getFulfillment() != o.getFulfillment()) {
            return false;
        }
        if (r.getCountries() != null && (o.getShipCountry() == null || !set(r.getCountries(), true).contains(o.getShipCountry().toUpperCase(Locale.ROOT)))) {
            return false;
        }
        if (r.getSkus() != null) {
            Set<String> skus = set(r.getSkus(), true);
            if (items.stream().map(EcomOrderItem::getSellerSku).filter(Objects::nonNull).noneMatch(s -> skus.contains(s.toUpperCase(Locale.ROOT)))) {
                return false;
            }
        }
        if (r.getMaterialIds() != null) {
            Set<String> ids = set(r.getMaterialIds(), false);
            if (items.stream().map(EcomOrderItem::getMaterialId).filter(Objects::nonNull).noneMatch(id -> ids.contains(String.valueOf(id)))) {
                return false;
            }
        }
        if (!inRange(Decimals.nz(o.getTotalBase()), r.getAmountMin(), r.getAmountMax())) {
            return false;
        }
        if (r.getWeightMin() != null || r.getWeightMax() != null) {
            return o.getWeight() != null && inRange(o.getWeight(), r.getWeightMin(), r.getWeightMax());
        }
        return true;
    }

    private static boolean inRange(BigDecimal v, BigDecimal min, BigDecimal max) {
        return (min == null || v.compareTo(min) >= 0) && (max == null || v.compareTo(max) <= 0);
    }
}
