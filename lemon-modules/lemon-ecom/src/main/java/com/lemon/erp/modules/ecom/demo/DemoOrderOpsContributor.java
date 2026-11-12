package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.modules.ecom.dto.HoldRequest;
import com.lemon.erp.modules.ecom.dto.TrackingRequest;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageMapper;
import com.lemon.erp.modules.ecom.model.EcomPackageStatus;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.service.EcomOrderOpsService;
import com.lemon.erp.modules.ecom.service.EcomOrderService;
import com.lemon.erp.modules.ecom.service.EcomPackageService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 演示数据 · 订单作业（order 55）：近三天的自发货订单走真实的作业流程——审核（按可用库存生成包裹或缺货）、填写运单号（待发货）、
 * 包裹发货（逐单生成销售出库、回传发货）、人工挂起、几张待审核订单直接出库（已出库待填写物流，订单页可打开「发货」），
 * 最后整理订单操作日志（{@link DemoOrderLogs}）；每一步在独立事务中执行，一单失败不影响其他
 */
@Component
@RequiredArgsConstructor
public class DemoOrderOpsContributor implements DemoContributor {

    /** 直接出库、等待发货的订单数 */
    private static final int DIRECT_OUTBOUND = 6;
    private static final String[] HOLD_REASONS = {"买家留言要求修改收货地址", "买家要求合并下一单一起发货", "疑似刷单，等待风控确认"}; // i18n-ignore 演示数据存库

    private final EcomOrderOpsService opsService;
    private final EcomOrderService orderService;
    private final EcomPackageService packageService;
    private final EcomPackageMapper packageMapper;
    private final EcomOrderMapper orderMapper;
    private final EcomStoreService storeService;
    private final DemoTx tx;
    private final DemoOrderLogs orderLogs;

    @Override
    public int order() {
        return 55;
    }

    @Override
    public String key() {
        return "order-ops";
    }

    @Override
    public void generate(DemoContext ctx) {
        DemoOrderPlan plan = ctx.get(DemoContributors.ORDER_PLAN);
        Map<String, Long> channels = ctx.get(DemoKeys.CHANNEL_BY_CODE);
        List<DemoOrderPlan.Order> audit = new ArrayList<>();
        List<Long> hold = new ArrayList<>();
        for (DemoOrderPlan.Order o : plan.orders) {
            if (o.id == null) {
                continue;
            }
            switch (o.stage) {
                case AUDITED, LABELED, PACKAGE_SHIPPED -> audit.add(o);
                case HOLD -> hold.add(o.id);
                default -> {
                }
            }
        }
        // 审核：按发货仓可用库存生成包裹（缺货演示物料进入缺货）
        for (DemoOrderPlan.Order o : audit) {
            tx.run("audit", () -> opsService.audit(List.of(o.id)));
        }
        Map<Long, EcomStore> stores = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (DemoOrderPlan.Order o : audit) {
            if (o.stage == DemoOrderPlan.Stage.AUDITED) {
                continue;
            }
            List<EcomPackage> packages = packageMapper.selectByOrderId(o.id).stream().filter(p -> p.getStatus() == EcomPackageStatus.WAIT_LABEL).toList();
            if (packages.isEmpty()) {
                continue;
            }
            EcomOrder order = orderMapper.selectById(o.id);
            EcomStore store = stores.computeIfAbsent(o.storeId, storeService::get);
            String channel = DemoOrderContributor.channelOf(store, order);
            for (EcomPackage p : packages) {
                tx.run("label", () -> packageService.manualTracking(p.getId(), new TrackingRequest(DemoOrderContributor.carrierName(channel),
                        DemoOrderContributor.trackingNo(ctx, channel, o.country), channels.get(channel), o.weight(), null)));
                if (o.stage == DemoOrderPlan.Stage.PACKAGE_SHIPPED) {
                    LocalDateTime shipTime = o.time.plusHours(ctx.between(6, 22));
                    LocalDateTime at = shipTime.isAfter(now) ? now.minusMinutes(10) : shipTime;
                    tx.run("ship", () -> packageService.ship(List.of(p.getId()), at));
                }
            }
        }
        for (int i = 0; i < hold.size(); i++) {
            Long id = hold.get(i);
            String reason = HOLD_REASONS[i % HOLD_REASONS.length];
            tx.run("hold", () -> opsService.hold(new HoldRequest(List.of(id), reason)));
        }
        // 直接出库（不经过包裹）：几张待审核的自发货订单生成销售出库，停在「已出库」等待填写物流发货
        int outbound = 0;
        for (DemoOrderPlan.Order o : plan.orders) {
            if (outbound >= DIRECT_OUTBOUND) {
                break;
            }
            if (o.id == null || o.stage != DemoOrderPlan.Stage.PENDING || o.fulfillment != FulfillmentType.SELLER
                    || o.lines.stream().anyMatch(l -> !l.listing().mapped() || DemoOrderPlan.SHORT_MATERIAL.equals(l.listing().productCode()))) {
                continue;
            }
            if (tx.run("direct-outbound", () -> orderService.outboundOne(o.id, null, null))) {
                outbound++;
            }
        }
        // 操作日志：近期订单按下单时间重排，最近已发货的历史订单补写日志
        List<Long> recentIds = plan.orders.stream().filter(DemoOrderPlan.Order::recent).map(o -> o.id).filter(Objects::nonNull).toList();
        tx.run("order-logs", () -> orderLogs.generate(ctx, recentIds));
    }
}
