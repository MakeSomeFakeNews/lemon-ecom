package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.dto.LogisticsEstimate;
import com.lemon.erp.modules.ecom.dto.OrderEditRequest;
import com.lemon.erp.modules.ecom.dto.OrderEnhanceViews;
import com.lemon.erp.modules.ecom.dto.OrderEnhanceViews.AddressIssue;
import com.lemon.erp.modules.ecom.entity.EcomBuyerBlacklist;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomOrderItemPart;
import com.lemon.erp.modules.ecom.mapper.EcomOrderEnhanceMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 审单前的检查：收件地址校验（{@link EcomAddressValidator}，偏远地区按「偏远地区」邮编段）、买家黑名单、利润预估，结果写回订单
 * （address_issue、address_invalid、remote_area、blacklist_id、est_*），审单规则按它匹配「预估利润率低于、命中黑名单、地址异常、偏远地区」。
 * <ul>
 *   <li>订单写入执行审单规则时检查：新命中黑名单的订单挂起（原因写明命中的条目）</li>
 *   <li>审核时检查：新命中黑名单的挂起；开启了地址校验且地址不通过的进入异常（写明原因），在订单里修改地址后自动重新校验</li>
 *   <li>人工取消挂起后，已记录命中的黑名单不再重复挂起（人工确认过）</li>
 * </ul>
 * 利润预估（本位币）：运费按订单的物流渠道（没有时取费率最低的渠道）和重量（订单的预估重量，没有时按历史包裹的单件重量估算）试算；
 * 平台费用取订单上的佣金（按店铺佣金率估算或结算回写）+ 配送费 + 其他费用 + 税；商品成本按物料参考成本 × 数量；
 * 预估利润 = 本位币金额 − 平台费用 − 商品成本 − 运费，利润率 = 预估利润 ÷ 本位币金额。以「内部」标注的方法由调用方在自己的事务中调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomOrderCheckService {

    private static final int ISSUE_MAX = 500;

    private final EcomOrderMapper orderMapper;
    private final EcomOrderItemMapper itemMapper;
    private final EcomOrderEnhanceMapper enhanceMapper;
    private final EcomOutboundSupport outboundSupport;
    private final EcomLogisticsService logisticsService;
    private final WmsMaterialService materialService;
    private final EcomBuyerBlacklistService blacklistService;
    private final EcomRemoteAreaService remoteAreaService;
    private final EcomOrderSettingService settingService;
    private final EcomOrderService orderService;

    /**
     * 检查结果（审单规则匹配用）
     *
     * @param newBlacklist 本次新命中黑名单（订单之前没有记录命中）
     */
    public record Facts(List<AddressIssue> issues, boolean addressInvalid, boolean remote, EcomBuyerBlacklist blacklist, boolean newBlacklist,
                        Estimate estimate) {

        public BigDecimal estProfitRate() {
            return estimate == null ? null : estimate.profitRate();
        }

        /** 没有检查时（规则匹配新条件一律不命中） */
        public static Facts none() {
            return new Facts(List.of(), false, false, null, false, null);
        }
    }

    /** 利润预估（本位币） */
    public record Estimate(Long channelId, BigDecimal weight, BigDecimal shipping, BigDecimal fee, BigDecimal cost, BigDecimal profit, BigDecimal profitRate) {
    }

    /** 订单地址被修改：重新校验（EcomOrderService.update 发布，同一事务中同步处理） */
    public record AddressChangedEvent(Long orderId) {
    }

    // ---------------------------------------------------------------- 检查

    /**
     * 检查并写回订单（内部）
     *
     * @param items    订单行（需已带上组合商品的 parts）
     * @param countHit 是否累计黑名单命中次数
     */
    public Facts check(EcomOrder o, List<EcomOrderItem> items, boolean countHit) {
        boolean platform = o.getFulfillment() == FulfillmentType.PLATFORM;
        List<AddressIssue> issues = platform ? List.of() : EcomAddressValidator.validate(o, remoteAreaService::isRemote);
        boolean invalid = EcomAddressValidator.hasError(issues);
        boolean remote = issues.stream().anyMatch(i -> EcomAddressValidator.REMOTE_AREA.equals(i.code()));
        EcomBuyerBlacklist bl = blacklistService.match(o, countHit && o.getBlacklistId() == null);
        Estimate est = estimate(o, items);
        String text = issues.isEmpty() ? null : issues.stream().map(AddressIssue::message).collect(Collectors.joining("; "));
        LambdaUpdateWrapper<EcomOrder> u = Wrappers.<EcomOrder>lambdaUpdate().eq(EcomOrder::getId, o.getId())
                .set(EcomOrder::getAddressIssue, text == null || text.length() <= ISSUE_MAX ? text : text.substring(0, ISSUE_MAX - 3) + "...")
                .set(EcomOrder::getAddressInvalid, invalid ? 1 : 0).set(EcomOrder::getRemoteArea, remote ? 1 : 0)
                .set(EcomOrder::getEstChannelId, est.channelId()).set(EcomOrder::getEstWeight, est.weight())
                .set(EcomOrder::getEstShipping, est.shipping()).set(EcomOrder::getEstFee, est.fee()).set(EcomOrder::getEstCost, est.cost())
                .set(EcomOrder::getEstProfit, est.profit()).set(EcomOrder::getEstProfitRate, est.profitRate());
        // 已记录的黑名单只在新命中时更新，不因黑名单停用 / 删除而清除（保留人工确认过的痕迹）
        if (bl != null) {
            u.set(EcomOrder::getBlacklistId, bl.getId());
        }
        orderMapper.update(u);
        return new Facts(issues, invalid, remote, bl, bl != null && o.getBlacklistId() == null, est);
    }

    /**
     * 检查（内部，审单规则使用）：出错时只记日志、按没有检查结果处理，不影响订单写入与规则执行
     */
    public Facts checkQuietly(EcomOrder o, List<EcomOrderItem> items) {
        try {
            return check(o, items, true);
        } catch (RuntimeException e) {
            log.warn("Order check failed for order {}: {}", o.getId(), e.getMessage());
            return Facts.none();
        }
    }

    /** 带上组合商品展开结果的订单行 */
    private List<EcomOrderItem> itemsOf(Long orderId) {
        return outboundSupport.attachParts(itemMapper.selectByOrderId(orderId));
    }

    /**
     * 审单规则执行后（内部）：新命中黑名单且还没有挂起的订单挂起
     *
     * @return 处理后的状态
     */
    public EcomLocalStatus afterRules(EcomOrder o, EcomLocalStatus status, Facts f) {
        if (f.newBlacklist() && status != EcomLocalStatus.ON_HOLD && status != EcomLocalStatus.CLOSED) {
            hold(o.getId(), status, EcomBuyerBlacklistService.describe(f.blacklist()));
            return EcomLocalStatus.ON_HOLD;
        }
        return status;
    }

    /**
     * 审核前拦截（内部，SKU 已匹配）：新命中黑名单的挂起；开启了地址校验且地址不通过的进入异常
     *
     * @return 拦截原因，通过时为 null
     */
    public String auditBlock(EcomOrder o) {
        Facts f = check(o, itemsOf(o.getId()), true);
        if (f.newBlacklist()) {
            String reason = EcomBuyerBlacklistService.describe(f.blacklist());
            hold(o.getId(), EcomLocalStatus.PENDING, reason);
            return reason;
        }
        if (f.addressInvalid() && settingService.addressCheckEnabled()) {
            String reason = abbreviate(Messages.get("ecom.text.ord.addressInvalid", f.issues().stream().filter(AddressIssue::error)
                    .map(AddressIssue::message).collect(Collectors.joining("; "))));
            orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getLocalStatus, EcomLocalStatus.EXCEPTION)
                    .set(EcomOrder::getException, reason).eq(EcomOrder::getId, o.getId()));
            return reason;
        }
        return null;
    }

    private void hold(Long orderId, EcomLocalStatus prev, String reason) {
        orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getHoldPrevStatus, prev)
                .set(EcomOrder::getLocalStatus, EcomLocalStatus.ON_HOLD).set(EcomOrder::getHoldReason, abbreviate(reason))
                .set(EcomOrder::getHoldRuleId, null).eq(EcomOrder::getId, orderId));
    }

    /**
     * 地址修改后重新校验：之前因地址不通过进入异常、现在通过的订单重新匹配 SKU（匹配上回到待审核）
     */
    @EventListener
    public void onAddressChanged(AddressChangedEvent e) {
        EcomOrder before = orderMapper.selectById(e.orderId());
        if (before == null) {
            return;
        }
        Facts f = check(before, itemsOf(before.getId()), false);
        if (before.getLocalStatus() == EcomLocalStatus.EXCEPTION && Integer.valueOf(1).equals(before.getAddressInvalid()) && !f.addressInvalid()) {
            orderService.rematch(before.getId());
        }
    }

    /** 重新检查（详情「重新校验」）：重新校验地址、匹配黑名单、预估利润，返回结果 */
    @Transactional
    public OrderEnhanceViews.CheckResult recheck(Long orderId) {
        EcomOrder o = orderService.require(orderId);
        Facts f = check(o, itemsOf(orderId), false);
        if (o.getLocalStatus() == EcomLocalStatus.EXCEPTION && Integer.valueOf(1).equals(o.getAddressInvalid()) && !f.addressInvalid()) {
            orderService.rematch(orderId);
        }
        return view(f);
    }

    /** 订单当前的检查结果（不写库） */
    public OrderEnhanceViews.CheckResult result(Long orderId) {
        EcomOrder o = orderService.require(orderId);
        List<AddressIssue> issues = o.getFulfillment() == FulfillmentType.PLATFORM ? List.of() : EcomAddressValidator.validate(o, remoteAreaService::isRemote);
        EcomBuyerBlacklist bl = o.getBlacklistId() == null ? null : blacklistById(o.getBlacklistId());
        Estimate est = new Estimate(o.getEstChannelId(), o.getEstWeight(), o.getEstShipping(), o.getEstFee(), o.getEstCost(), o.getEstProfit(),
                o.getEstProfitRate());
        return view(new Facts(issues, EcomAddressValidator.hasError(issues), issues.stream().anyMatch(i -> EcomAddressValidator.REMOTE_AREA.equals(i.code())),
                bl, false, est));
    }

    private EcomBuyerBlacklist blacklistById(Long id) {
        try {
            return blacklistService.get(id);
        } catch (BizException e) {
            return null;
        }
    }

    private OrderEnhanceViews.CheckResult view(Facts f) {
        Estimate e = f.estimate();
        String channelName = null;
        if (e != null && e.channelId() != null) {
            try {
                channelName = logisticsService.get(e.channelId()).getName();
            } catch (BizException ex) {
                channelName = null;
            }
        }
        return new OrderEnhanceViews.CheckResult(f.issues(), f.addressInvalid(), f.remote(), f.blacklist() == null ? null : f.blacklist().getId(),
                f.blacklist() == null ? null : EcomBuyerBlacklistService.describe(f.blacklist()),
                e == null ? null : e.channelId(), channelName, e == null ? null : e.weight(), e == null ? null : e.shipping(),
                e == null ? null : e.fee(), e == null ? null : e.cost(), e == null ? null : e.profit(), e == null ? null : e.profitRate());
    }

    /** 修改收件地址：其余可修改的信息保持不变，保存后自动重新校验 */
    @Transactional
    public void updateAddress(Long orderId, OrderEnhanceViews.AddressRequest req) {
        EcomOrder o = orderService.require(orderId);
        orderService.update(orderId, new OrderEditRequest(req.shipName(), req.shipPhone(), req.shipCountry(), req.shipState(), req.shipCity(),
                req.shipPostcode(), req.shipAddress(), o.getFulfillment(), o.getRemark(), o.getWeight()));
    }

    // ---------------------------------------------------------------- 利润预估

    /**
     * 利润预估（本位币）：见类注释。没有重量、没有匹配的费率时运费为空（利润按 0 运费估算）
     */
    public Estimate estimate(EcomOrder o, List<EcomOrderItem> items) {
        Map<Long, BigDecimal> qtyByMaterial = new HashMap<>();
        for (EcomOrderItem it : items) {
            if (it.getParts() != null && !it.getParts().isEmpty()) {
                for (EcomOrderItemPart p : it.getParts()) {
                    qtyByMaterial.merge(p.getMaterialId(), Decimals.nz(p.getMaterialQty()), BigDecimal::add);
                }
            } else if (it.getMaterialId() != null) {
                qtyByMaterial.merge(it.getMaterialId(), Decimals.nz(it.getMaterialQty()), BigDecimal::add);
            }
        }
        BigDecimal cost = BigDecimal.ZERO;
        if (!qtyByMaterial.isEmpty()) {
            Map<Long, WmsMaterial> materials = materialService.mapByIds(qtyByMaterial.keySet());
            for (Map.Entry<Long, BigDecimal> e : qtyByMaterial.entrySet()) {
                WmsMaterial m = materials.get(e.getKey());
                if (m != null && m.getRefCost() != null) {
                    cost = cost.add(m.getRefCost().multiply(e.getValue()));
                }
            }
        }
        cost = Decimals.money(cost);
        BigDecimal weight = o.getWeight() != null && o.getWeight().signum() > 0 ? o.getWeight() : historicalWeight(qtyByMaterial);
        Long channel = o.getFulfillment() == FulfillmentType.PLATFORM ? null : o.getChannelId();
        BigDecimal shipping = null;
        if (o.getFulfillment() != FulfillmentType.PLATFORM && weight != null && o.getShipCountry() != null) {
            if (channel != null) {
                shipping = logisticsService.estimate(channel, o.getShipCountry(), weight);
            }
            if (shipping == null) {
                List<LogisticsEstimate> list = logisticsService.estimate(o.getShipCountry(), weight);
                if (!list.isEmpty()) {
                    channel = list.get(0).channelId();
                    shipping = list.get(0).fee();
                }
            }
        }
        // 用订单的副本按利润公式计算（税额按订单税率重算），成本、运费取预估值
        EcomOrder c = new EcomOrder();
        BeanUtils.copyProperties(o, c);
        c.setCostAmount(cost);
        c.setShippingCost(shipping == null ? BigDecimal.ZERO : shipping);
        c.setRefundAmount(BigDecimal.ZERO);
        BigDecimal profit = EcomOrderService.profitOf(c);
        BigDecimal feesFc = Decimals.nz(c.getWithheldTax()).add(Decimals.nz(c.getVatAmount())).add(Decimals.nz(c.getPlatformFee()))
                .add(Decimals.nz(c.getFulfillmentFee())).add(Decimals.nz(c.getOtherFee()));
        BigDecimal fee = Decimals.money(feesFc.multiply(Decimals.nz(c.getExchangeRate())));
        BigDecimal base = Decimals.nz(o.getTotalBase());
        BigDecimal rate = base.signum() > 0 ? profit.multiply(Decimals.HUNDRED).divide(base, 2, RoundingMode.HALF_UP) : null;
        return new Estimate(channel, weight == null ? null : weight.setScale(3, RoundingMode.HALF_UP), shipping, fee, cost, profit, rate);
    }

    /** 历史包裹的单件重量 × 物料数量（有物料没有历史重量时返回 null） */
    private BigDecimal historicalWeight(Map<Long, BigDecimal> qtyByMaterial) {
        if (qtyByMaterial.isEmpty()) {
            return null;
        }
        Map<Long, BigDecimal> unit = new HashMap<>();
        for (Map<String, Object> row : enhanceMapper.selectUnitWeights(qtyByMaterial.keySet())) {
            Object w = row.get("unitWeight");
            if (w != null) {
                unit.put(((Number) row.get("materialId")).longValue(), new BigDecimal(w.toString()));
            }
        }
        Set<Long> missing = new HashSet<>(qtyByMaterial.keySet());
        missing.removeAll(unit.keySet());
        if (!missing.isEmpty()) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> e : qtyByMaterial.entrySet()) {
            total = total.add(unit.get(e.getKey()).multiply(e.getValue()));
        }
        return total.signum() > 0 ? total : null;
    }

    private static String abbreviate(String s) {
        return s == null || s.length() <= 255 ? s : s.substring(0, 252) + "...";
    }
}
