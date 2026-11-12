package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.modules.ecom.dto.GenerateInboundRequest;
import com.lemon.erp.modules.ecom.dto.InboundRequest;
import com.lemon.erp.modules.ecom.dto.ShipmentPlanRequest;
import com.lemon.erp.modules.ecom.entity.EcomShipmentPlan;
import com.lemon.erp.modules.ecom.entity.EcomShipmentPlanItem;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ShipmentPlanStatus;
import com.lemon.erp.modules.ecom.service.EcomInboundService;
import com.lemon.erp.modules.ecom.service.EcomShipmentPlanService;
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
 * 演示数据 · 头程货件（order 40）：国内仓 → FBA 美 / 德 / 日仓、美国海外仓，每个平台仓三批已签收的货件（按各周期的平台仓订单需求备货，
 * 头程运费 / 关税 / 杂费分摊为单件头程成本）、两批在途货件，以及发货计划（草稿、已确认，和一张已生成草稿货件的计划）
 */
@Component
@RequiredArgsConstructor
public class DemoInboundContributor implements DemoContributor {

    /**
     * 一批头程
     *
     * @param channel    物流渠道编码
     * @param shipAgo    发运日（几天前）
     * @param receiveAgo 签收日（几天前，在途为空）
     * @param etaAhead   在途货件的预计到达（几天后）
     */
    record Batch(String channel, int shipAgo, Integer receiveAgo, int etaAhead) {
    }

    /** 平台仓 → 所属店铺 */
    static final Map<String, String> WAREHOUSE_STORE = Map.of(
            DemoCatalog.FBA_US.code(), "DEMO-AMZ-US", DemoCatalog.FBA_DE.code(), "DEMO-AMZ-DE",
            DemoCatalog.FBA_JP.code(), "DEMO-AMZ-JP", DemoCatalog.US_3PL.code(), "DEMO-TT-US");

    private final EcomInboundService inboundService;
    private final EcomShipmentPlanService planService;

    @Override
    public int order() {
        return 40;
    }

    @Override
    public String key() {
        return "inbound";
    }

    /** 各平台仓的头程批次：前三批对应订单计划的三个周期（签收日 = 周期开始），后两批在途 */
    static List<Batch> batches(String warehouseCode) {
        int[] r = DemoOrderPlan.PLATFORM_RECEIVE_DAYS;
        return switch (warehouseCode) {
            case "DEMO-FBA-DE" -> List.of(new Batch("DEMO-AIR-EU", r[0] + 12, r[0], 0), new Batch("DEMO-AIR-EU", r[1] + 12, r[1], 0),
                    new Batch("DEMO-AIR-EU", r[2] + 12, r[2], 0), new Batch("DEMO-AIR-EU", 6, null, 7));
            case "DEMO-FBA-JP" -> List.of(new Batch("DEMO-SEA-JP", r[0] + 10, r[0], 0), new Batch("DEMO-SEA-JP", r[1] + 10, r[1], 0),
                    new Batch("DEMO-SEA-JP", r[2] + 10, r[2], 0), new Batch("DEMO-SEA-JP", 5, null, 5));
            case "DEMO-US-3PL" -> List.of(new Batch("DEMO-AIR-US", r[0] + 9, r[0], 0), new Batch("DEMO-SEA-US", r[1] + 22, r[1], 0),
                    new Batch("DEMO-AIR-US", r[2] + 9, r[2], 0), new Batch("DEMO-SEA-US", 12, null, 10));
            default -> List.of(new Batch("DEMO-AIR-US", r[0] + 9, r[0], 0), new Batch("DEMO-SEA-US", r[1] + 22, r[1], 0),
                    new Batch("DEMO-AIR-US", r[2] + 10, r[2], 0), new Batch("DEMO-SEA-US", 14, null, 8), new Batch("DEMO-AIR-US", 4, null, 6));
        };
    }

    /** 每批的物料数量：前三批 = 该周期需求 × 1.15（最近一批 × 1.5，留出安全库存），在途批次按最近周期的日均销量 */
    static Map<Long, BigDecimal> batchQty(DemoOrderPlan plan, Long warehouseId, int index, Batch b) {
        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        Map<Long, double[]> demand = plan.demand.getOrDefault(warehouseId, Map.of());
        int days = DemoOrderPlan.PLATFORM_RECEIVE_DAYS[2];
        demand.forEach((mid, d) -> {
            double q = b.receiveAgo() != null ? d[index] * (index == 2 ? 1.5 : 1.15) + 2 : d[2] / days * (b.etaAhead() >= 8 ? 24 : 12);
            if (q >= 1) {
                out.put(mid, BigDecimal.valueOf(Math.ceil(q)));
            }
        });
        return out;
    }

    /**
     * 国内仓各周期的物料需求：[0..2] 自发货出库 + 当期发运的头程，[3] 近期未发货的自发货订单
     */
    static Map<Long, double[]> localNeed(DemoOrderPlan plan, Long localWh) {
        Map<Long, double[]> need = new HashMap<>();
        plan.demand.getOrDefault(localWh, Map.of()).forEach((mid, d) -> {
            double[] n = need.computeIfAbsent(mid, k -> new double[4]);
            for (int i = 0; i < 3; i++) {
                n[i] += d[i];
            }
        });
        plan.localRecent.forEach((mid, q) -> need.computeIfAbsent(mid, k -> new double[4])[3] += q);
        int[] bounds = DemoOrderPlan.LOCAL_RECEIVE_DAYS;
        for (Map.Entry<Long, String> wh : plan.platformWarehouses.entrySet()) {
            List<Batch> list = batches(wh.getValue());
            for (int i = 0; i < list.size(); i++) {
                Batch b = list.get(i);
                int period = b.shipAgo() >= bounds[1] ? 0 : b.shipAgo() >= bounds[2] ? 1 : 2;
                final int p = period;
                batchQty(plan, wh.getKey(), Math.min(i, 2), b).forEach((mid, q) -> need.computeIfAbsent(mid, k -> new double[4])[p] += q.doubleValue());
            }
        }
        return need;
    }

    @Override
    public void generate(DemoContext ctx) {
        DemoOrderPlan plan = ctx.get(DemoContributors.ORDER_PLAN);
        Map<String, Long> stores = ctx.get(DemoKeys.STORE_BY_CODE);
        Map<String, Long> channels = ctx.get(DemoKeys.CHANNEL_BY_CODE);
        Map<Long, Map<String, DemoListings.Item>> listings = ctx.get(DemoContributors.LISTINGS);
        Long localWh = ctx.get(DemoKeys.LOCAL_WAREHOUSE_ID);
        LocalDate today = ctx.today();
        for (Map.Entry<Long, String> wh : plan.platformWarehouses.entrySet()) {
            String storeCode = WAREHOUSE_STORE.get(wh.getValue());
            Long storeId = stores.get(storeCode);
            Map<Long, String> skuOf = skuOfMaterial(listings.get(storeId));
            List<Batch> list = batches(wh.getValue());
            for (int i = 0; i < list.size(); i++) {
                Batch b = list.get(i);
                Map<Long, BigDecimal> qty = batchQty(plan, wh.getKey(), Math.min(i, 2), b);
                if (qty.isEmpty()) {
                    continue;
                }
                List<InboundRequest.Item> items = new ArrayList<>();
                double weight = 0;
                double value = 0;
                for (Map.Entry<Long, BigDecimal> e : qty.entrySet()) {
                    items.add(new InboundRequest.Item(e.getKey(), skuOf.get(e.getKey()), e.getValue(), null));
                    DemoCatalog.Product p = productOf(ctx, e.getKey());
                    weight += p.weight() * e.getValue().doubleValue();
                    value += p.cost() * e.getValue().doubleValue();
                }
                LocalDate ship = today.minusDays(b.shipAgo());
                LocalDate eta = b.receiveAgo() != null ? today.minusDays(b.receiveAgo()) : today.plusDays(b.etaAhead());
                int boxes = (int) Math.ceil(weight / 18);
                double perKg = switch (b.channel()) {
                    case "DEMO-SEA-US" -> 13.5;
                    case "DEMO-AIR-EU" -> 52;
                    case "DEMO-SEA-JP" -> 9.5;
                    default -> 46;
                };
                boolean amazon = storeCode.startsWith("DEMO-AMZ");
                String shipmentId = amazon ? "FBA1" + DemoListingContributor.code(ctx, 8) : "3PL-" + ship.toString().replace("-", "") + "-" + (i + 1);
                InboundRequest req = new InboundRequest(storeId, shipmentId, localWh, wh.getKey(), channels.get(b.channel()),
                        b.channel().contains("SEA") ? "Matson" : "UPS", "1Z" + DemoListingContributor.code(ctx, 16), ship, eta, boxes,
                        BigDecimal.valueOf(weight).setScale(3, RoundingMode.HALF_UP), BigDecimal.valueOf(boxes * 0.06).setScale(4, RoundingMode.HALF_UP),
                        money(weight * perKg), money(DemoCatalog.scale(DemoCatalog.store(storeCode).currency()) == 0 ? 0 : value * 0.035), money(180 + boxes * 12.0), null, items);
                Long id = inboundService.create(req);
                boolean approving = inboundService.submit(id);
                if (!approving && b.receiveAgo() != null) {
                    inboundService.receive(id, today.minusDays(b.receiveAgo()));
                }
            }
        }
        plans(ctx, plan, stores, listings, channels, localWh);
    }

    /** 发货计划：美国站已确认（待生成货件）、德国站草稿、日本站已生成一张草稿货件 */
    private void plans(DemoContext ctx, DemoOrderPlan plan, Map<String, Long> stores, Map<Long, Map<String, DemoListings.Item>> listings,
                       Map<String, Long> channels, Long localWh) {
        Object[][] defs = {{"DEMO-AMZ-US", "DEMO-FBA-US", ShipmentPlanStatus.CONFIRMED}, {"DEMO-AMZ-DE", "DEMO-FBA-DE", ShipmentPlanStatus.DRAFT},
                {"DEMO-AMZ-JP", "DEMO-FBA-JP", ShipmentPlanStatus.GENERATED}};
        for (Object[] def : defs) {
            Long storeId = stores.get((String) def[0]);
            Long toWh = plan.platformWarehouses.entrySet().stream().filter(e -> e.getValue().equals(def[1])).map(Map.Entry::getKey).findFirst().orElse(null);
            List<ShipmentPlanRequest.Item> items = new ArrayList<>();
            for (DemoListings.Item l : listings.get(storeId).values()) {
                if (l.fulfillment() == FulfillmentType.PLATFORM && l.mapped() && l.popularity() >= 5 && items.size() < 8) {
                    items.add(new ShipmentPlanRequest.Item(l.id(), BigDecimal.valueOf(l.popularity() * 12L), null));
                }
            }
            if (items.isEmpty() || toWh == null) {
                continue;
            }
            Long id = planService.create(new ShipmentPlanRequest(storeId, localWh, toWh, ctx.today().plusDays(3),
                    "下一批补货（按补货建议）", items)); // i18n-ignore 演示数据存库
            if (def[2] == ShipmentPlanStatus.DRAFT) {
                continue;
            }
            planService.confirm(id);
            if (def[2] == ShipmentPlanStatus.GENERATED) {
                EcomShipmentPlan p = planService.detail(id);
                List<GenerateInboundRequest.Line> lines = new ArrayList<>();
                for (EcomShipmentPlanItem it : p.getItems()) {
                    lines.add(new GenerateInboundRequest.Line(it.getId(), it.getQty()));
                }
                planService.generate(id, new GenerateInboundRequest(List.of(new GenerateInboundRequest.Shipment(
                        "FBA1" + DemoListingContributor.code(ctx, 8), channels.get("DEMO-SEA-JP"), lines))));
            }
        }
    }

    /** 物料 → 店铺平台仓配送的单品平台 SKU */
    private static Map<Long, String> skuOfMaterial(Map<String, DemoListings.Item> listings) {
        Map<Long, String> out = new HashMap<>();
        for (DemoListings.Item l : listings.values()) {
            if (l.materialId() != null && l.fulfillment() == FulfillmentType.PLATFORM && l.packQty().compareTo(BigDecimal.ONE) == 0) {
                out.putIfAbsent(l.materialId(), l.sku());
            }
        }
        return out;
    }

    static DemoCatalog.Product productOf(DemoContext ctx, Long materialId) {
        Map<String, Long> materials = ctx.get(DemoKeys.MATERIAL_BY_CODE);
        for (Map.Entry<String, Long> e : materials.entrySet()) {
            if (e.getValue().equals(materialId)) {
                return DemoCatalog.product(e.getKey());
            }
        }
        throw new IllegalStateException("material " + materialId); // i18n-ignore 内部错误
    }

    private static BigDecimal money(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
