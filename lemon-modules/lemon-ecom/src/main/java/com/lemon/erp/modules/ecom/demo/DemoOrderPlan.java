package com.lemon.erp.modules.ecom.demo;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 演示订单计划（只在内存中）：按增长趋势、周末效应、大促峰值生成 120 天的订单，并按库存周期统计各仓库的物料需求，
 * 采购入库和头程货件按需求备货，保证出库时库存充足（每个日期的结存都不为负）
 */
final class DemoOrderPlan {

    /** 订单的处理阶段 */
    enum Stage {
        /** 历史订单：已发货（按日汇总出库） */
        SHIPPED,
        /** 已取消 */
        CANCELLED,
        /** 平台仓配送、平台还没有发货（今天的 FBA 订单） */
        PLATFORM_PENDING,
        /** 自发货：经包裹打单发货 */
        PACKAGE_SHIPPED,
        /** 自发货：已打单待发货 */
        LABELED,
        /** 自发货：已审核待打单 */
        AUDITED,
        /** 自发货：已挂起 */
        HOLD,
        /** 待审核 */
        PENDING,
        /** 异常：含未配对的平台 SKU */
        EXCEPTION
    }

    /** 采购 / 头程的库存周期：周期 k 从 BOUNDS[k] 天前开始 */
    static final int[] PLATFORM_RECEIVE_DAYS = {121, 76, 41};
    static final int[] LOCAL_RECEIVE_DAYS = {137, 92, 56};
    /** 缺货演示：近期订单不备货的物料 */
    static final String SHORT_MATERIAL = "DM-PT07";

    record Line(DemoListings.Item listing, int qty) {
    }

    static final class Order {
        DemoCatalog.Store store;
        Long storeId;
        String no;
        LocalDateTime time;
        LocalDate shipDate;
        FulfillmentType fulfillment;
        Long warehouseId;
        String country;
        String state;
        String city;
        String postcode;
        String buyer;
        String email;
        Stage stage;
        final List<Line> lines = new ArrayList<>();
        /** 写入后的订单 ID */
        Long id;

        /** 预估重量（kg）：各行单件毛重 × 件数 + 包装 */
        BigDecimal weight() {
            double w = 0.05;
            for (Line l : lines) {
                w += l.listing().weight() * l.qty();
            }
            return BigDecimal.valueOf(w).setScale(3, java.math.RoundingMode.HALF_UP);
        }

        boolean recent() {
            return stage != Stage.SHIPPED && stage != Stage.CANCELLED;
        }
    }

    final List<Order> orders = new ArrayList<>();
    /** 平台仓 ID → 仓库编码 */
    final Map<Long, String> platformWarehouses = new LinkedHashMap<>();
    /** 仓库 ID → 物料 ID → 各周期需求（平台仓按 PLATFORM_RECEIVE_DAYS，本地仓按 LOCAL_RECEIVE_DAYS） */
    final Map<Long, Map<Long, double[]>> demand = new LinkedHashMap<>();
    /** 本地仓近期（未发货的自发货订单）需求，不含缺货演示物料 */
    final Map<Long, Double> localRecent = new HashMap<>();

    static DemoOrderPlan build(DemoContext ctx, Map<String, Long> stores, Map<Long, Map<String, DemoListings.Item>> listings,
                               Map<String, Long> warehouses, Long localWh, Long shortMaterialId) {
        DemoOrderPlan plan = new DemoOrderPlan();
        for (DemoCatalog.Store s : DemoCatalog.STORES) {
            if (s.platformWh() != null) {
                plan.platformWarehouses.put(warehouses.get(s.platformWh()), s.platformWh());
            }
        }
        LocalDate today = ctx.today();
        LocalDateTime now = LocalDateTime.now();
        double shareSum = DemoCatalog.STORES.stream().mapToDouble(DemoCatalog.Store::share).sum();
        Set<String> numbers = new HashSet<>();
        for (int d = ctx.days(); d >= 0; d--) {
            LocalDate day = today.minusDays(d);
            int idx = ctx.days() - d;
            double base = 20 + 34 * Math.pow(idx / (double) ctx.days(), 1.15);
            DayOfWeek dow = day.getDayOfWeek();
            double factor = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY ? 1.2 : dow == DayOfWeek.FRIDAY ? 1.06 : dow == DayOfWeek.MONDAY ? 0.92 : 1;
            // 大促：会员日（两天）、开学季、黑五预热、双十一
            if (d == 96 || d == 95) factor *= 2.4;
            if (d == 60) factor *= 1.6;
            if (d <= 3) factor *= 1.35;
            factor *= 0.88 + ctx.random().nextDouble() * 0.24;
            int count = (int) Math.round(base * factor);
            if (d == 0) {
                // 今天只到当前时刻
                count = Math.max(6, (int) Math.round(count * Math.max(0.25, now.toLocalTime().toSecondOfDay() / 86400.0)));
            }
            for (int i = 0; i < count; i++) {
                DemoCatalog.Store s = pickStore(ctx, shareSum);
                Order o = plan.order(ctx, s, stores.get(s.code()), listings.get(stores.get(s.code())), day, d, now, numbers, warehouses, localWh);
                if (o != null) {
                    plan.orders.add(o);
                }
            }
        }
        plan.extras(ctx, stores, listings, now, numbers, localWh);
        plan.orders.sort(Comparator.comparing(o -> o.time));
        plan.countDemand(today, localWh, shortMaterialId);
        return plan;
    }

    private Order order(DemoContext ctx, DemoCatalog.Store s, Long storeId, Map<String, DemoListings.Item> listings, LocalDate day, int d,
                        LocalDateTime now, Set<String> numbers, Map<String, Long> warehouses, Long localWh) {
        boolean platform = s.platformWh() != null && ctx.chance(s.platformShare());
        FulfillmentType ful = platform ? FulfillmentType.PLATFORM : FulfillmentType.SELLER;
        List<DemoListings.Item> pool = listings.values().stream().filter(l -> l.mapped() && l.fulfillment() == ful).toList();
        if (pool.isEmpty()) {
            return null;
        }
        Order o = new Order();
        o.store = s;
        o.storeId = storeId;
        o.fulfillment = ful;
        o.warehouseId = platform ? warehouses.get(s.platformWh()) : localWh;
        // 下单时间：白天到晚上较多
        int minute = (int) Math.min(1439, Math.max(0, 60 * (7 + ctx.random().nextGaussian() * 4.5 + 7)));
        o.time = LocalDateTime.of(day, LocalTime.of(minute / 60 % 24, minute % 60, ctx.between(0, 59)));
        if (d == 0 && o.time.isAfter(now.minusMinutes(5))) {
            o.time = now.minusMinutes(ctx.between(6, 600)).withNano(0);
            if (!o.time.toLocalDate().equals(day)) {
                o.time = day.atTime(0, ctx.between(1, 59));
            }
        }
        int lines = ctx.chance(0.86) ? 1 : ctx.chance(0.8) ? 2 : 3;
        Set<String> used = new HashSet<>();
        for (int i = 0; i < lines; i++) {
            DemoListings.Item l = weighted(ctx, pool);
            if (used.add(l.sku())) {
                o.lines.add(new Line(l, ctx.chance(0.8) ? 1 : ctx.chance(0.75) ? 2 : 3));
            }
        }
        fillBuyer(ctx, o, numbers);
        o.stage = stage(ctx, o, d);
        o.shipDate = shipDate(ctx, o, d);
        return o;
    }

    /** 处理阶段：3 天前的全部已发货（少量取消）；近 3 天的自发货订单分布在各作业环节 */
    private static Stage stage(DemoContext ctx, Order o, int d) {
        if (ctx.chance(0.015) && d > 0) {
            return Stage.CANCELLED;
        }
        if (o.fulfillment == FulfillmentType.PLATFORM) {
            return d == 0 ? Stage.PLATFORM_PENDING : Stage.SHIPPED;
        }
        if (d >= 3) {
            return Stage.SHIPPED;
        }
        double r = ctx.random().nextDouble();
        return switch (d) {
            case 2 -> r < 0.8 ? Stage.PACKAGE_SHIPPED : r < 0.92 ? Stage.LABELED : Stage.PENDING;
            case 1 -> r < 0.4 ? Stage.PACKAGE_SHIPPED : r < 0.62 ? Stage.LABELED : r < 0.8 ? Stage.AUDITED : r < 0.86 ? Stage.HOLD : Stage.PENDING;
            default -> r < 0.18 ? Stage.AUDITED : r < 0.24 ? Stage.HOLD : Stage.PENDING;
        };
    }

    /** 发货日：平台仓当天或次日，自发货 1–2 天（不晚于昨天） */
    private static LocalDate shipDate(DemoContext ctx, Order o, int d) {
        int lag = o.fulfillment == FulfillmentType.PLATFORM ? ctx.between(0, 1) : ctx.between(1, 2);
        return o.time.toLocalDate().plusDays(Math.min(lag, Math.max(0, d - 1)));
    }

    /** 额外的近期订单：缺货演示物料的自发货订单、含未配对 SKU 的异常订单 */
    private void extras(DemoContext ctx, Map<String, Long> stores, Map<Long, Map<String, DemoListings.Item>> listings, LocalDateTime now,
                        Set<String> numbers, Long localWh) {
        for (String code : new String[]{"DEMO-SHOPIFY", "DEMO-EBAY-US", "DEMO-AMZ-US"}) {
            Long storeId = stores.get(code);
            DemoListings.Item item = listings.get(storeId).values().stream()
                    .filter(l -> SHORT_MATERIAL.equals(l.productCode()) && l.components() == null && l.fulfillment() == FulfillmentType.SELLER)
                    .findFirst().orElse(null);
            if (item != null) {
                orders.add(extra(ctx, code, storeId, item, now.minusHours(ctx.between(3, 20)), Stage.AUDITED, numbers, localWh));
            }
        }
        for (Map.Entry<String, Long> e : stores.entrySet()) {
            for (DemoListings.Item item : listings.get(e.getValue()).values()) {
                if (!item.mapped()) {
                    orders.add(extra(ctx, e.getKey(), e.getValue(), item, now.minusHours(ctx.between(2, 40)), Stage.EXCEPTION, numbers, localWh));
                }
            }
        }
    }

    private Order extra(DemoContext ctx, String code, Long storeId, DemoListings.Item item, LocalDateTime time, Stage stage, Set<String> numbers,
                        Long localWh) {
        Order o = new Order();
        o.store = DemoCatalog.store(code);
        o.storeId = storeId;
        o.fulfillment = FulfillmentType.SELLER;
        o.warehouseId = localWh;
        o.time = time.withNano(0);
        o.lines.add(new Line(item, 1));
        fillBuyer(ctx, o, numbers);
        o.stage = stage;
        o.shipDate = o.time.toLocalDate();
        return o;
    }

    private void countDemand(LocalDate today, Long localWh, Long shortMaterialId) {
        for (Order o : orders) {
            if (o.stage == Stage.CANCELLED || o.stage == Stage.EXCEPTION || o.stage == Stage.PLATFORM_PENDING) {
                continue;
            }
            int ago = (int) (today.toEpochDay() - o.shipDate.toEpochDay());
            boolean local = o.fulfillment == FulfillmentType.SELLER;
            int[] bounds = local ? LOCAL_RECEIVE_DAYS : PLATFORM_RECEIVE_DAYS;
            int period = ago >= bounds[1] ? 0 : ago >= bounds[2] ? 1 : 2;
            for (Line l : o.lines) {
                for (Map.Entry<Long, BigDecimal> e : l.listing().perUnit().entrySet()) {
                    double q = e.getValue().doubleValue() * l.qty();
                    if (o.recent() && local) {
                        if (!e.getKey().equals(shortMaterialId)) {
                            localRecent.merge(e.getKey(), q, Double::sum);
                        }
                        continue;
                    }
                    demand.computeIfAbsent(o.warehouseId, k -> new HashMap<>()).computeIfAbsent(e.getKey(), k -> new double[3])[period] += q;
                }
            }
        }
    }

    private static DemoCatalog.Store pickStore(DemoContext ctx, double shareSum) {
        double r = ctx.random().nextDouble() * shareSum;
        for (DemoCatalog.Store s : DemoCatalog.STORES) {
            r -= s.share();
            if (r <= 0) {
                return s;
            }
        }
        return DemoCatalog.STORES.get(0);
    }

    private static DemoListings.Item weighted(DemoContext ctx, List<DemoListings.Item> pool) {
        int sum = pool.stream().mapToInt(l -> Math.max(1, l.popularity())).sum();
        int r = ctx.random().nextInt(sum);
        for (DemoListings.Item l : pool) {
            r -= Math.max(1, l.popularity());
            if (r < 0) {
                return l;
            }
        }
        return pool.get(0);
    }

    private static void fillBuyer(DemoContext ctx, Order o, Set<String> numbers) {
        DemoCatalog.Store s = o.store;
        o.country = s.countries().get(ctx.random().nextInt(s.countries().size()));
        String[][] cities = DemoCatalog.CITIES.getOrDefault(o.country, DemoCatalog.CITIES.get("US"));
        String[] c = cities[ctx.random().nextInt(cities.length)];
        o.state = c[0];
        o.city = c[1];
        o.postcode = c[2];
        o.buyer = switch (o.country) {
            case "DE", "AT" -> ctx.pick(DemoCatalog.DE_NAMES);
            case "JP" -> ctx.pick(DemoCatalog.JP_NAMES);
            case "MY", "SG" -> ctx.pick(DemoCatalog.SEA_NAMES);
            default -> ctx.pick(DemoCatalog.FIRST_NAMES) + " " + ctx.pick(DemoCatalog.LAST_NAMES);
        };
        String mail = DemoListingContributor.code(ctx, 10).toLowerCase();
        o.email = s.platform() == EcomPlatform.AMAZON ? mail + "@marketplace.amazon." + switch (s.marketplace()) {
            case "DE" -> "de";
            case "JP" -> "co.jp";
            default -> "com";
        } : s.platform() == EcomPlatform.SHOPIFY ? mail + "@gmail.com" : null;
        String no;
        do {
            no = orderNo(ctx, s, o.time);
        } while (!numbers.add(s.code() + "|" + no));
        o.no = no;
    }

    private static String orderNo(DemoContext ctx, DemoCatalog.Store s, LocalDateTime time) {
        return switch (s.platform()) {
            case AMAZON -> (switch (s.marketplace()) {
                case "DE" -> "302";
                case "JP" -> "503";
                default -> "11" + ctx.between(1, 4);
            }) + "-" + digits(ctx, 7) + "-" + digits(ctx, 7);
            case SHOPEE -> time.toLocalDate().toString().replace("-", "").substring(2) + DemoListingContributor.code(ctx, 8);
            case TIKTOK -> "5769" + digits(ctx, 14);
            case TEMU -> "PO-211-" + digits(ctx, 14);
            case ALIEXPRESS -> "81" + digits(ctx, 14);
            case SHOPIFY -> "#" + (10000 + ctx.random().nextInt(89999));
            case EBAY -> digits(ctx, 2) + "-" + digits(ctx, 5) + "-" + digits(ctx, 5);
            default -> digits(ctx, 12);
        };
    }

    private static String digits(DemoContext ctx, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(ctx.random().nextInt(10));
        }
        return sb.toString();
    }
}
