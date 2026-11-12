package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.modules.scm.dto.OrderItemRequest;
import com.lemon.erp.modules.scm.dto.OrderRequest;
import com.lemon.erp.modules.scm.dto.TradeDocItemRequest;
import com.lemon.erp.modules.scm.dto.TradeDocRequest;
import com.lemon.erp.modules.scm.entity.ScmOrder;
import com.lemon.erp.modules.scm.entity.ScmOrderItem;
import com.lemon.erp.modules.scm.model.OrderKind;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.scm.service.ScmOrderService;
import com.lemon.erp.modules.scm.service.ScmTradeDocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示数据 · 采购（order 35）：按订单计划的需求给国内发货仓备货——每家供应商三批已全部入库的采购订单（形成期初与补货库存，
 * 覆盖各周期的自发货出库和头程调拨），一张部分到货的订单，一张已审核未到货的订单（在途采购）。
 * 缺货演示物料只备历史需求，近期订单审核时缺货
 */
@Component
@RequiredArgsConstructor
public class DemoPurchaseContributor implements DemoContributor {

    private final ScmOrderService orderService;
    private final ScmTradeDocService tradeDocService;

    @Override
    public int order() {
        return 35;
    }

    @Override
    public String key() {
        return "purchase";
    }

    @Override
    public void generate(DemoContext ctx) {
        DemoOrderPlan plan = ctx.get(DemoContributors.ORDER_PLAN);
        Map<String, Long> materials = ctx.get(DemoKeys.MATERIAL_BY_CODE);
        Map<String, Long> suppliers = ctx.get(DemoContributors.SUPPLIER_BY_CODE);
        Long localWh = ctx.get(DemoKeys.LOCAL_WAREHOUSE_ID);
        Long shortId = materials.get(DemoOrderPlan.SHORT_MATERIAL);
        Map<Long, double[]> need = DemoInboundContributor.localNeed(plan, localWh);
        LocalDate today = ctx.today();
        for (int s = 0; s < DemoCatalog.SUPPLIERS.size(); s++) {
            DemoCatalog.Supplier sup = DemoCatalog.SUPPLIERS.get(s);
            Long supplierId = suppliers.get(sup.code());
            List<DemoCatalog.Product> products = new ArrayList<>();
            for (DemoCatalog.Product p : DemoCatalog.PRODUCTS) {
                if (p.supplier() == s) {
                    products.add(p);
                }
            }
            for (int k = 0; k < 3; k++) {
                LocalDate receive = today.minusDays(DemoOrderPlan.LOCAL_RECEIVE_DAYS[k] + (long) s % 3);
                Map<Long, BigDecimal> qty = new LinkedHashMap<>();
                for (DemoCatalog.Product p : products) {
                    Long mid = materials.get(p.code());
                    double[] n = need.getOrDefault(mid, new double[4]);
                    double q = n[k];
                    if (k == 2) {
                        // 最后一批：再加近期订单与安全库存（缺货演示物料不加）
                        q += mid.equals(shortId) ? 0 : n[3] + p.popularity() * 25;
                    }
                    q = mid.equals(shortId) ? Math.ceil(q) : Math.ceil(q * 1.08 / 10) * 10;
                    if (q > 0) {
                        qty.put(mid, BigDecimal.valueOf(q));
                    }
                }
                if (!qty.isEmpty()) {
                    receivedOrder(supplierId, products, materials, qty, receive.minusDays(sup.leadDays()), receive, localWh, ctx, 1.0);
                }
            }
            // 部分到货：10 天前到了六成；在途：已审核、下周到货
            Map<Long, BigDecimal> next = new LinkedHashMap<>();
            for (DemoCatalog.Product p : products) {
                Long mid = materials.get(p.code());
                if (!mid.equals(shortId) && p.popularity() >= 5) {
                    next.put(mid, BigDecimal.valueOf(p.popularity() * 20L));
                }
            }
            if (!next.isEmpty()) {
                receivedOrder(supplierId, products, materials, next, today.minusDays(sup.leadDays() + 10L), today.minusDays(10), localWh, ctx, 0.6);
                Map<Long, BigDecimal> open = new LinkedHashMap<>();
                next.forEach((k, v) -> open.put(k, v.multiply(BigDecimal.valueOf(1.5)).setScale(0, RoundingMode.HALF_UP)));
                if (products.stream().anyMatch(p -> DemoOrderPlan.SHORT_MATERIAL.equals(p.code()))) {
                    // 缺货的喂食器已经补单，下周到货
                    open.put(shortId, BigDecimal.valueOf(60));
                }
                approvedOrder(supplierId, products, materials, open, today.minusDays(4), today.plusDays(sup.leadDays() - 4L), localWh, ctx);
            }
        }
    }

    /** 新建、审核采购订单并按比例入库 */
    private void receivedOrder(Long supplierId, List<DemoCatalog.Product> products, Map<String, Long> materials, Map<Long, BigDecimal> qty,
                               LocalDate orderDate, LocalDate receiveDate, Long wh, DemoContext ctx, double ratio) {
        Long orderId = approvedOrder(supplierId, products, materials, qty, orderDate, receiveDate, wh, ctx);
        ScmOrder order = orderService.detail(OrderKind.PURCHASE, orderId);
        List<TradeDocItemRequest> lines = new ArrayList<>();
        for (ScmOrderItem it : order.getItems()) {
            BigDecimal q = ratio >= 1 ? it.getQty() : it.getQty().multiply(BigDecimal.valueOf(ratio)).setScale(0, RoundingMode.DOWN);
            if (q.signum() > 0) {
                lines.add(new TradeDocItemRequest(it.getId(), null, null, null, null, q, null, null));
            }
        }
        tradeDocService.create(TradeKind.PURCHASE_IN, new TradeDocRequest(orderId, wh, receiveDate, null, Boolean.FALSE, lines));
    }

    private Long approvedOrder(Long supplierId, List<DemoCatalog.Product> products, Map<String, Long> materials, Map<Long, BigDecimal> qty,
                               LocalDate orderDate, LocalDate deliveryDate, Long wh, DemoContext ctx) {
        Map<Long, DemoCatalog.Product> byId = new HashMap<>();
        for (DemoCatalog.Product p : products) {
            byId.put(materials.get(p.code()), p);
        }
        List<OrderItemRequest> items = new ArrayList<>();
        qty.forEach((mid, q) -> {
            double drift = 0.97 + ctx.random().nextDouble() * 0.06;
            BigDecimal price = BigDecimal.valueOf(byId.get(mid).cost() / 1.13 * drift).setScale(2, RoundingMode.HALF_UP);
            items.add(new OrderItemRequest(mid, q, price, BigDecimal.valueOf(13), deliveryDate, null));
        });
        Long id = orderService.create(OrderKind.PURCHASE, new OrderRequest(supplierId, orderDate, deliveryDate, wh, null, items));
        orderService.approve(OrderKind.PURCHASE, id);
        return id;
    }
}
