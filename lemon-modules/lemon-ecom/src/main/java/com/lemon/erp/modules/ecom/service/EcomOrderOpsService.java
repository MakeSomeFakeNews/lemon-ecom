package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.modules.ecom.dto.HoldRequest;
import com.lemon.erp.modules.ecom.dto.SplitRequest;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomOrderItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * 审单作业：审单规则（订单写入后自动执行，也可在列表上重新应用）、审核（按发货仓可用库存检查，通过生成包裹，不足为缺货）、
 * 重新检查库存、挂起 / 取消挂起、合并 / 拆分订单（包裹见 {@link EcomPackageService}）。
 * <p>
 * 批量操作逐张订单一个事务，返回 {平台订单号: 原因}（审核缺货的订单也在其中，状态已改为缺货）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomOrderOpsService {

    /** 可以审核的处理状态（异常的先匹配 SKU） */
    private static final Set<EcomLocalStatus> AUDIT_ALLOWED = EnumSet.of(EcomLocalStatus.PENDING, EcomLocalStatus.EXCEPTION);
    /** 可以（重新）应用审单规则的处理状态 */
    private static final Set<EcomLocalStatus> RULE_ALLOWED = EnumSet.of(EcomLocalStatus.PENDING, EcomLocalStatus.ON_HOLD, EcomLocalStatus.EXCEPTION,
            EcomLocalStatus.OUT_OF_STOCK);
    /** 可以挂起的处理状态 */
    private static final Set<EcomLocalStatus> HOLD_ALLOWED = EnumSet.of(EcomLocalStatus.PENDING, EcomLocalStatus.EXCEPTION, EcomLocalStatus.OUT_OF_STOCK,
            EcomLocalStatus.AUDITED, EcomLocalStatus.LABELED);
    /** 可以重新检查库存的处理状态 */
    private static final Set<EcomLocalStatus> RECHECK_ALLOWED = EnumSet.of(EcomLocalStatus.OUT_OF_STOCK, EcomLocalStatus.AUDITED, EcomLocalStatus.PARTIAL);
    /** 可以拆分的处理状态 */
    private static final Set<EcomLocalStatus> SPLIT_ALLOWED = EnumSet.of(EcomLocalStatus.AUDITED, EcomLocalStatus.OUT_OF_STOCK, EcomLocalStatus.PARTIAL);
    private static final int TEXT_MAX = 255;

    private final EcomOrderService orderService;
    private final EcomOrderMapper orderMapper;
    private final EcomOrderItemMapper itemMapper;
    private final EcomStoreService storeService;
    private final EcomOutboundSupport outboundSupport;
    private final EcomOrderRuleService ruleService;
    private final EcomPackageService packageService;
    private final TransactionTemplate transactionTemplate;

    // ---------------------------------------------------------------- 审单规则

    /**
     * 新订单写入后执行审单规则：与写入同一个事务、同步执行；规则执行出错只记日志，不影响订单写入
     * （这里调用的都是不开启事务的内部方法，异常不会把导入 / 同步的事务标记为回滚）
     */
    @EventListener
    public void onOrderSaved(EcomOrderService.OrderSavedEvent e) {
        try {
            applyRulesInternal(orderMapper.selectById(e.orderId()));
        } catch (BizException ex) {
            log.warn("Order rules failed for order {}: {}", e.orderId(), ex.getMessage());
        }
    }

    /** 对选中的订单重新应用审单规则（待审核、挂起、异常、缺货的订单） */
    public Map<String, String> applyRules(List<Long> ids) {
        return batch(ids, o -> {
            if (!RULE_ALLOWED.contains(o.getLocalStatus())) {
                throw new BizException("ecom.error.orderStatusNotAllow", o.getPlatformOrderNo());
            }
            if (o.getLocalStatus() == EcomLocalStatus.ON_HOLD && o.getHoldRuleId() == null) {
                throw new BizException("ecom.error.ops.manualHoldKeep", o.getPlatformOrderNo());
            }
            return applyRulesInternal(o);
        });
    }

    /**
     * 执行审单规则（内部）：记录命中的规则，自发货订单按规则指定发货仓、物流渠道，按截单时间推算计划发货时间；
     * 规则要求人工审核时挂起（写原因），之前被规则挂起、现在不再命中的恢复；规则要求自动审核且没有挂起时审核
     *
     * @return 自动审核时缺货 / 异常的说明，否则 null
     */
    String applyRulesInternal(EcomOrder o) {
        if (o == null || !RULE_ALLOWED.contains(o.getLocalStatus())) {
            return null;
        }
        if (o.getLocalStatus() == EcomLocalStatus.ON_HOLD && o.getHoldRuleId() == null) {
            return null;
        }
        EcomStore store = storeService.get(o.getStoreId());
        List<EcomOrderItem> items = itemMapper.selectByOrderId(o.getId());
        EcomOrderRuleService.Outcome r = ruleService.evaluate(o, store.getPlatform(), items);
        LambdaUpdateWrapper<EcomOrder> u = Wrappers.<EcomOrder>lambdaUpdate().eq(EcomOrder::getId, o.getId())
                .set(EcomOrder::getMatchedRules, r.matched().isEmpty() ? null : abbreviate(String.join(", ", r.matched())))
                .set(EcomOrder::getPlanShipTime, r.planShipTime(o));
        if (o.getFulfillment() == FulfillmentType.SELLER) {
            if (r.warehouseId() != null) {
                u.set(EcomOrder::getWarehouseId, r.warehouseId());
            }
            if (r.channelId() != null) {
                u.set(EcomOrder::getChannelId, r.channelId());
            }
        }
        EcomLocalStatus status = o.getLocalStatus();
        if (r.holdRule() != null) {
            String reason = StringUtils.hasText(r.holdRule().getHoldReason()) ? r.holdRule().getHoldReason()
                    : Messages.get("ecom.text.ops.ruleHoldDefault", r.holdRule().getName());
            if (status != EcomLocalStatus.ON_HOLD) {
                u.set(EcomOrder::getHoldPrevStatus, status).set(EcomOrder::getLocalStatus, EcomLocalStatus.ON_HOLD);
            }
            u.set(EcomOrder::getHoldReason, abbreviate(reason)).set(EcomOrder::getHoldRuleId, r.holdRule().getId());
            status = EcomLocalStatus.ON_HOLD;
        } else if (status == EcomLocalStatus.ON_HOLD) {
            status = o.getHoldPrevStatus() == null ? EcomLocalStatus.PENDING : o.getHoldPrevStatus();
            u.set(EcomOrder::getLocalStatus, status).set(EcomOrder::getHoldPrevStatus, null).set(EcomOrder::getHoldReason, null)
                    .set(EcomOrder::getHoldRuleId, null);
        }
        orderMapper.update(u);
        if (r.autoAudit() && status == EcomLocalStatus.PENDING) {
            EcomOrder fresh = orderMapper.selectById(o.getId());
            if (auditable(fresh)) {
                return auditInternal(fresh);
            }
        }
        return null;
    }

    // ---------------------------------------------------------------- 审核与库存

    /** 批量审核（待审核、异常的自发货订单） */
    public Map<String, String> audit(List<Long> ids) {
        return batch(ids, o -> {
            checkAuditable(o);
            return auditInternal(o);
        });
    }

    private void checkAuditable(EcomOrder o) {
        if (!AUDIT_ALLOWED.contains(o.getLocalStatus())) {
            throw new BizException("ecom.error.orderStatusNotAllow", o.getPlatformOrderNo());
        }
        if (o.getStatus() == EcomOrderStatus.PENDING) {
            throw new BizException("ecom.error.orderUnpaid", o.getPlatformOrderNo());
        }
        if (o.getStatus() == EcomOrderStatus.CANCELLED || o.getStatus() == EcomOrderStatus.REFUNDED) {
            throw new BizException("ecom.error.orderCancelledCannotOutbound", o.getPlatformOrderNo());
        }
        if (o.getFulfillment() != FulfillmentType.SELLER) {
            throw new BizException("ecom.error.ops.auditPlatformOrder", o.getPlatformOrderNo());
        }
    }

    private static boolean auditable(EcomOrder o) {
        return o != null && AUDIT_ALLOWED.contains(o.getLocalStatus()) && o.getStatus() == EcomOrderStatus.UNSHIPPED
                && o.getFulfillment() == FulfillmentType.SELLER;
    }

    /**
     * 审核（内部）：匹配 SKU（未匹配为异常），确定发货仓（规则指定的，否则店铺默认发货仓），按可用库存
     * （空闲库存 − 其他未发货包裹占用）检查，够则生成包裹（待打单），不够为缺货并写明缺口
     *
     * @return 异常 / 缺货的说明，通过时为 null
     */
    private String auditInternal(EcomOrder o) {
        EcomStore store = storeService.requireEnabled(o.getStoreId());
        List<EcomOrderItem> items = itemMapper.selectByOrderId(o.getId());
        String exception = outboundSupport.match(store.getId(), items);
        items.forEach(itemMapper::updateById);
        if (exception != null) {
            orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getLocalStatus, EcomLocalStatus.EXCEPTION)
                    .set(EcomOrder::getException, exception).eq(EcomOrder::getId, o.getId()));
            return exception;
        }
        Long wh = o.getWarehouseId() != null ? o.getWarehouseId() : store.getWarehouseId();
        orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getWarehouseId, wh).set(EcomOrder::getLocalStatus, EcomLocalStatus.PENDING)
                .eq(EcomOrder::getId, o.getId()));
        o.setWarehouseId(wh);
        String shortage = packageService.allocateRemainder(o, wh);
        if (shortage != null) {
            orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getLocalStatus, EcomLocalStatus.OUT_OF_STOCK)
                    .set(EcomOrder::getException, abbreviate(shortage)).eq(EcomOrder::getId, o.getId()));
            return shortage;
        }
        orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getAuditTime, LocalDateTime.now()).eq(EcomOrder::getId, o.getId()));
        return null;
    }

    /**
     * 重新检查库存：缺货的订单库存够了生成包裹（待打单）；拆分后还有件数没有分配包裹的订单，为剩余件数生成包裹
     */
    public Map<String, String> recheck(List<Long> ids) {
        return batch(ids, o -> {
            if (!RECHECK_ALLOWED.contains(o.getLocalStatus())) {
                throw new BizException("ecom.error.orderStatusNotAllow", o.getPlatformOrderNo());
            }
            EcomStore store = storeService.requireEnabled(o.getStoreId());
            Long wh = o.getWarehouseId() != null ? o.getWarehouseId() : store.getWarehouseId();
            o.setWarehouseId(wh);
            String shortage = packageService.allocateRemainder(o, wh);
            if (shortage != null) {
                orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getException, abbreviate(shortage)).eq(EcomOrder::getId, o.getId()));
                return shortage;
            }
            if (o.getLocalStatus() == EcomLocalStatus.OUT_OF_STOCK) {
                orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getAuditTime, LocalDateTime.now()).set(EcomOrder::getWarehouseId, wh)
                        .eq(EcomOrder::getId, o.getId()));
            }
            return null;
        });
    }

    // ---------------------------------------------------------------- 挂起

    /** 挂起（人工拦截）：打单、发货时会拦截挂起的订单 */
    @Transactional
    public void hold(HoldRequest req) {
        for (Long id : req.ids()) {
            EcomOrder o = orderService.require(id);
            if (!HOLD_ALLOWED.contains(o.getLocalStatus())) {
                throw new BizException("ecom.error.orderStatusNotAllow", o.getPlatformOrderNo());
            }
            orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getHoldPrevStatus, o.getLocalStatus())
                    .set(EcomOrder::getLocalStatus, EcomLocalStatus.ON_HOLD).set(EcomOrder::getHoldReason, req.reason().trim())
                    .set(EcomOrder::getHoldRuleId, null).eq(EcomOrder::getId, id));
        }
    }

    /** 取消挂起：回到挂起前的状态（期间包裹有变化时按包裹重新汇总；平台已取消且没有包裹的关闭） */
    @Transactional
    public void unhold(List<Long> ids) {
        for (Long id : ids) {
            EcomOrder o = orderService.require(id);
            if (o.getLocalStatus() != EcomLocalStatus.ON_HOLD) {
                throw new BizException("ecom.error.orderStatusNotAllow", o.getPlatformOrderNo());
            }
            boolean hasPackages = packageService.hasActive(id);
            EcomLocalStatus prev = o.getHoldPrevStatus() == null ? EcomLocalStatus.PENDING : o.getHoldPrevStatus();
            if (!hasPackages && (prev == EcomLocalStatus.AUDITED || prev == EcomLocalStatus.LABELED || prev == EcomLocalStatus.PARTIAL)) {
                prev = EcomLocalStatus.PENDING;
            }
            if (!hasPackages && (o.getStatus() == EcomOrderStatus.CANCELLED || o.getStatus() == EcomOrderStatus.REFUNDED)) {
                prev = EcomLocalStatus.CLOSED;
            }
            orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getLocalStatus, prev).set(EcomOrder::getHoldPrevStatus, null)
                    .set(EcomOrder::getHoldReason, null).set(EcomOrder::getHoldRuleId, null).eq(EcomOrder::getId, id));
            if (hasPackages) {
                packageService.recomputeOrder(id);
            }
        }
    }

    // ---------------------------------------------------------------- 合并 / 拆分

    /**
     * 合并订单为一个包裹：同店铺、同收件人地址、都是待打单（包裹都还没有打单）的订单
     *
     * @return 新包裹 ID
     */
    @Transactional
    public Long merge(List<Long> ids) {
        List<Long> distinct = ids.stream().distinct().toList();
        distinct.forEach(orderService::require);
        return packageService.merge(orderMapper.selectByIdsForUpdate(distinct));
    }

    /**
     * 拆分订单为多个包裹（部分发货 / 缺货先发有货的），每个包裹单独打单、出库、发货
     *
     * @return 新包裹 ID
     */
    @Transactional
    public List<Long> split(Long id, SplitRequest req) {
        orderService.require(id);
        EcomOrder o = orderMapper.selectByIdsForUpdate(List.of(id)).get(0);
        if (!SPLIT_ALLOWED.contains(o.getLocalStatus())) {
            throw new BizException("ecom.error.orderStatusNotAllow", o.getPlatformOrderNo());
        }
        EcomStore store = storeService.requireEnabled(o.getStoreId());
        Long wh = o.getWarehouseId() != null ? o.getWarehouseId() : store.getWarehouseId();
        o.setWarehouseId(wh);
        List<Long> created = packageService.split(o, req, wh);
        orderMapper.update(Wrappers.<EcomOrder>lambdaUpdate().set(EcomOrder::getWarehouseId, wh)
                .set(EcomOrder::getAuditTime, o.getAuditTime() == null ? LocalDateTime.now() : o.getAuditTime()).eq(EcomOrder::getId, id));
        return created;
    }

    // ---------------------------------------------------------------- 工具

    /** 逐张订单一个事务；返回值不为空（缺货、异常说明）或抛出业务异常的记入结果 */
    private Map<String, String> batch(List<Long> ids, Function<EcomOrder, String> action) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Long id : ids.stream().distinct().toList()) {
            try {
                String msg = transactionTemplate.execute(s -> action.apply(orderService.require(id)));
                if (msg != null) {
                    out.put(orderNo(id), msg);
                }
            } catch (BizException e) {
                out.put(orderNo(id), e.getMessage());
            }
        }
        return out;
    }

    private String orderNo(Long id) {
        EcomOrder o = orderMapper.selectById(id);
        return o == null ? String.valueOf(id) : o.getPlatformOrderNo();
    }

    private static String abbreviate(String s) {
        return s == null || s.length() <= TEXT_MAX ? s : s.substring(0, TEXT_MAX - 3) + "...";
    }
}
