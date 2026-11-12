package com.lemon.erp.modules.ecom.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.framework.data.Queries;
import com.lemon.erp.modules.ecom.carrier.CarrierTrackEvent;
import com.lemon.erp.modules.ecom.carrier.CarrierTrackInfo;
import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.demo.DemoContributor;
import com.lemon.erp.modules.ecom.demo.DemoKeys;
import com.lemon.erp.modules.ecom.demo.DemoTx;
import com.lemon.erp.modules.ecom.dto.OrderEnhanceViews;
import com.lemon.erp.modules.ecom.dto.SplitRequest;
import com.lemon.erp.modules.ecom.dto.TrackingRequest;
import com.lemon.erp.modules.ecom.entity.EcomBuyerBlacklist;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomOrderRule;
import com.lemon.erp.modules.ecom.entity.EcomOrderTag;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.entity.EcomReturn;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomListingMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderRuleMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderTagMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageMapper;
import com.lemon.erp.modules.ecom.mapper.EcomReturnMapper;
import com.lemon.erp.modules.ecom.model.BlacklistType;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderType;
import com.lemon.erp.modules.ecom.model.EcomPackageStatus;
import com.lemon.erp.modules.ecom.model.EcomTrackStatus;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ListingComposeType;
import com.lemon.erp.modules.ecom.model.TrackSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示数据 · 订单增强（order 140）：订单标签与审单规则自动打标、已打标订单、买家黑名单（含命中挂起的订单）、地址异常订单（PO Box、邮编格式、
 * 缺城市、电话格式、偏远地区）、手工订单（独立站 / 线下，含按包裹拆分的部分发货）、补发单（关联退货，出库成本计入售后成本）、
 * 内部备注、全部订单的利润预估，以及已发货包裹的物流轨迹（暂无轨迹 / 已揽收 / 运输中 / 到达待取 / 已签收 / 异常 / 退回，含超时未上网与运输超时预警）。
 * 基础数据（店铺、在线商品、订单、库存）由基础组成部分生成，这里按店铺编码 DEMO- 查询；每一步在独立事务中执行，一步失败不影响其他
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EcomOrderEnhanceDemo implements DemoContributor {

    private static final String[][] TAGS = { // i18n-ignore 演示数据存库
            {"加急", "red"}, {"大客户", "gold"}, {"低利润", "volcano"}, {"偏远地区", "purple"}, {"黑名单复核", "magenta"}, {"礼品包装", "cyan"}, // i18n-ignore 演示数据存库
            {"已联系买家", "green"}}; // i18n-ignore 演示数据存库
    private static final String[] NOTES = { // i18n-ignore 演示数据存库
            "买家来信确认收货地址无误", "已电话联系买家，同意延迟两天发货", "仓库反馈外箱破损，已换新包装", "大客户，优先安排 DHL 发货", // i18n-ignore 演示数据存库
            "买家要求附赠品卡片", "平台客服提醒：请在承诺时间内上传运单号", "已核对 SKU 颜色，与买家留言一致"}; // i18n-ignore 演示数据存库
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** 演示轨迹的场景 */
    private enum Scenario {
        NONE, PICKED, TRANSIT, STALLED, PICKUP, DELIVERED, EXCEPTION, RETURNED
    }

    /** 收件地址：国家、州、城市、邮编、地址、电话 */
    private record Addr(String country, String state, String city, String postcode, String street, String phone) {
    }

    private static final Addr[] GOOD = {
            new Addr("US", "CA", "Los Angeles", "90012", "350 S Grand Ave Apt 1204", "(213) 555-0147"),
            new Addr("US", "NY", "Brooklyn", "11201", "75 Henry St", "718-555-0192"),
            new Addr("US", "TX", "Austin", "78701", "500 W 2nd St Suite 1900", "512 555 0133"),
            new Addr("US", "WA", "Seattle", "98101", "1301 2nd Ave", "+1 206 555 0110"),
            new Addr("GB", "", "London", "SW1A 1AA", "10 Downing Street", "+44 20 7946 0958"),
            new Addr("DE", "", "Berlin", "10115", "Invalidenstraße 43", "+49 30 901820"),
            new Addr("AU", "NSW", "Sydney", "2000", "200 George St", "+61 2 9374 4000")};
    private static final String[] BUYERS = {"Emily Johnson", "Michael Brown", "Sophia Martinez", "James Wilson", "Olivia Taylor", "Daniel Anderson",
            "Charlotte Thomas", "Benjamin Moore", "Amelia White", "Lucas Harris"};

    private final EcomOrderTagMapper tagMapper;
    private final EcomOrderTagService tagService;
    private final EcomOrderRuleMapper ruleMapper;
    private final EcomOrderRuleService ruleService;
    private final EcomBuyerBlacklistService blacklistService;
    private final EcomOrderManualService manualService;
    private final EcomOrderOpsService opsService;
    private final EcomOrderCheckService checkService;
    private final EcomOrderNoteService noteService;
    private final EcomPackageService packageService;
    private final EcomTrackService trackService;
    private final EcomStoreService storeService;
    private final EcomOutboundSupport outboundSupport;
    private final EcomOrderMapper orderMapper;
    private final EcomOrderItemMapper itemMapper;
    private final EcomPackageMapper packageMapper;
    private final EcomListingMapper listingMapper;
    private final EcomReturnMapper returnMapper;
    private final DemoTx tx;

    @Override
    public int order() {
        return 140;
    }

    @Override
    public String key() {
        return "order-enhance";
    }

    @Override
    public void generate(DemoContext ctx) {
        Map<String, Long> tags = tags();
        blacklist();
        Map<String, Long> byCode = ctx.get(DemoKeys.STORE_BY_CODE);
        String demoStoreIds = byCode == null ? null : byCode.values().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        rules(tags, demoStoreIds);
        List<EcomStore> stores = sellerStores(ctx);
        Map<Long, List<EcomListing>> listings = new LinkedHashMap<>();
        for (EcomStore s : stores) {
            List<EcomListing> l = listingMapper.selectList(Wrappers.<EcomListing>lambdaQuery().eq(EcomListing::getStoreId, s.getId())
                    .eq(EcomListing::getFulfillment, FulfillmentType.SELLER).eq(EcomListing::getComposeType, ListingComposeType.SINGLE)
                    .isNotNull(EcomListing::getMaterialId).orderByAsc(EcomListing::getId));
            if (!l.isEmpty()) {
                listings.put(s.getId(), l);
            }
        }
        List<EcomStore> usable = stores.stream().filter(s -> listings.containsKey(s.getId())).toList();
        if (!usable.isEmpty()) {
            manualAndAbnormalOrders(ctx, usable, listings);
            trackedManualOrders(ctx, usable, listings);
            partialShipment(ctx, usable, listings);
        }
        estimateAndTag(ctx, tags);
        resends(ctx);
        notes(ctx);
        tracks(ctx);
    }

    // ---------------------------------------------------------------- 标签、黑名单、规则

    private Map<String, Long> tags() {
        Map<String, Long> out = new LinkedHashMap<>();
        for (int i = 0; i < TAGS.length; i++) {
            String name = TAGS[i][0];
            EcomOrderTag t = tagMapper.selectOne(Wrappers.<EcomOrderTag>lambdaQuery().eq(EcomOrderTag::getName, name));
            if (t == null) {
                t = new EcomOrderTag();
                t.setName(name);
                t.setColor(TAGS[i][1]);
                t.setSort(i + 1);
                t.setStatus(EnableStatus.ENABLED);
                EcomOrderTag nt = t;
                tx.run("tag", () -> tagService.create(nt));
            }
            if (t.getId() != null) {
                out.put(name, t.getId());
            }
        }
        return out;
    }

    private void blacklist() {
        Object[][] rows = { // i18n-ignore 演示数据存库
                {BlacklistType.BUYER, "fraud_buyer_2024", "多次签收后谎称未收到，申请退款"}, // i18n-ignore 演示数据存库
                {BlacklistType.EMAIL, "chargeback.king@mailinator.com", "信用卡拒付 3 次"}, // i18n-ignore 演示数据存库
                {BlacklistType.PHONE, "+1 (305) 555-0199", "恶意差评并索要赔偿"}, // i18n-ignore 演示数据存库
                {BlacklistType.ADDRESS, "1200 Brickell Bay Dr Unit 3305 33131", "转运仓地址，疑似刷单"}, // i18n-ignore 演示数据存库
                {BlacklistType.BUYER, "Kevin Spam", "批量下单后集中取消"}, // i18n-ignore 演示数据存库
                {BlacklistType.EMAIL, "refund.only@guerrillamail.com", "仅退款不退货，已向平台申诉"}}; // i18n-ignore 演示数据存库
        for (Object[] r : rows) {
            EcomBuyerBlacklist b = new EcomBuyerBlacklist();
            b.setMatchType((BlacklistType) r[0]);
            b.setMatchValue((String) r[1]);
            b.setReason((String) r[2]);
            b.setStatus(EnableStatus.ENABLED);
            tx.run("blacklist", () -> blacklistService.create(b));
        }
    }

    private void rules(Map<String, Long> tags, String storeIds) {
        rule("低利润订单打标（预估利润率 < 8%）", 900, storeIds, r -> { // i18n-ignore 演示数据存库
            r.setProfitRateMax(new BigDecimal("8"));
            r.setTagIds(idOf(tags, "低利润")); // i18n-ignore 演示数据存库
        });
        rule("偏远地区打标", 910, storeIds, r -> { // i18n-ignore 演示数据存库
            r.setRemoteArea(1);
            r.setTagIds(idOf(tags, "偏远地区")); // i18n-ignore 演示数据存库
        });
        rule("黑名单订单打标并人工审核", 920, storeIds, r -> { // i18n-ignore 演示数据存库
            r.setBlacklistHit(1);
            r.setTagIds(idOf(tags, "黑名单复核")); // i18n-ignore 演示数据存库
            r.setHoldOrder(1);
            r.setHoldReason("命中买家黑名单，请人工复核"); // i18n-ignore 演示数据存库
        });
    }

    private void rule(String name, int priority, String storeIds, java.util.function.Consumer<EcomOrderRule> setup) {
        if (ruleMapper.exists(Wrappers.<EcomOrderRule>lambdaQuery().eq(EcomOrderRule::getName, name))) {
            return;
        }
        EcomOrderRule r = new EcomOrderRule();
        r.setName(name);
        r.setPriority(priority);
        r.setStatus(EnableStatus.ENABLED);
        // 演示规则只作用于演示店铺，不影响之后接入的真实店铺
        r.setStoreIds(storeIds);
        setup.accept(r);
        if (r.getTagIds() == null && (r.getHoldOrder() == null || r.getHoldOrder() == 0)) {
            return;
        }
        tx.run("rule", () -> ruleService.create(r));
    }

    private static String idOf(Map<String, Long> tags, String name) {
        Long id = tags.get(name);
        return id == null ? null : String.valueOf(id);
    }

    // ---------------------------------------------------------------- 手工订单、地址异常、黑名单命中

    private List<EcomStore> sellerStores(DemoContext ctx) {
        Map<String, Long> byCode = ctx.get(DemoKeys.STORE_BY_CODE);
        List<EcomStore> out = new ArrayList<>();
        if (byCode == null) {
            return out;
        }
        for (Long id : byCode.values()) {
            EcomStore s = storeService.get(id);
            if (s.getWarehouseId() != null && s.getStatus() == EnableStatus.ENABLED) {
                out.add(s);
            }
        }
        return out;
    }

    private OrderEnhanceViews.ManualOrder manual(DemoContext ctx, EcomStore store, List<EcomListing> listings, String no, LocalDateTime time,
                                                 String buyer, String email, Addr a, String remark) {
        List<OrderEnhanceViews.ManualLine> lines = new ArrayList<>();
        int n = ctx.chance(0.3) ? 2 : 1;
        for (int i = 0; i < n; i++) {
            EcomListing l = listings.get(ctx.random().nextInt(listings.size()));
            BigDecimal price = l.getPrice() != null && l.getPrice().signum() > 0 ? l.getPrice() : BigDecimal.valueOf(ctx.between(15, 60));
            lines.add(new OrderEnhanceViews.ManualLine(l.getSellerSku(), l.getTitle(), BigDecimal.valueOf(ctx.between(1, 2)), price));
        }
        return new OrderEnhanceViews.ManualOrder(store.getId(), no, FulfillmentType.SELLER, time, buyer, email, buyer, a.phone(), a.country(),
                a.state(), a.city(), a.postcode(), a.street(), null, null, new BigDecimal("0.6"), remark, lines);
    }

    private Long create(OrderEnhanceViews.ManualOrder req) {
        Long[] id = {null};
        tx.run("manual", () -> id[0] = manualService.createManual(req));
        return id[0];
    }

    private void manualAndAbnormalOrders(DemoContext ctx, List<EcomStore> stores, Map<Long, List<EcomListing>> listings) {
        LocalDateTime now = LocalDateTime.now();
        // 地址异常：审核时进入异常并写明原因
        Addr[] bad = {
                new Addr("US", "FL", "Miami", "33101", "PO Box 4521", "305-555-0101"),
                new Addr("US", "CA", "San Diego", "9210", "1450 Harbor Island Dr", "619-555-0165"),
                new Addr("GB", "", "", "EC1A 1BB", "1 Clerkenwell Rd", "+44 20 7946 0000"),
                new Addr("DE", "", "Hamburg", "20095", "Packstation 112", "+49 40 123456"),
                new Addr("US", "TX", "Houston", "77002", "901 Bagby St", "phone: call me"),
                new Addr("US", "AK", "Anchorage", "99501", "632 W 6th Ave", "907-555-0118"),
                new Addr("AU", "NT", "Darwin", "0800", "21 Knuckey St", "+61 8 8981 1234")};
        for (int i = 0; i < bad.length; i++) {
            EcomStore s = stores.get(i % stores.size());
            Long id = create(manual(ctx, s, listings.get(s.getId()), "ADDR-" + now.format(DateTimeFormatter.ofPattern("MMdd")) + "-" + (101 + i),
                    now.minusHours(ctx.between(2, 30)), BUYERS[i % BUYERS.length], null, bad[i], null));
            if (id != null) {
                audit(id);
            }
        }
        // 命中黑名单：写入时由审单规则挂起
        String[][] black = {{"fraud_buyer_2024", null}, {"Ava Clark", "chargeback.king@mailinator.com"}, {"Noah Lewis", null}};
        for (int i = 0; i < black.length; i++) {
            EcomStore s = stores.get((i + 1) % stores.size());
            Addr a = i == 2 ? new Addr("US", "FL", "Miami", "33131", "1200 Brickell Bay Dr Unit 3305", "305-555-0199") : GOOD[i];
            create(manual(ctx, s, listings.get(s.getId()), "BLK-" + now.format(DateTimeFormatter.ofPattern("MMdd")) + "-" + (201 + i),
                    now.minusHours(ctx.between(1, 20)), black[i][0], black[i][1], a, null));
        }
        // 普通手工订单（线下展会、独立站、批发客户）：部分审核
        String[] remarks = {"线下展会订单", "独立站订单（Shopify 外部店）", "老客户微信下单", "批发客户样品单"}; // i18n-ignore 演示数据存库
        for (int i = 0; i < 8; i++) {
            EcomStore s = stores.get(i % stores.size());
            Long id = create(manual(ctx, s, listings.get(s.getId()), null, now.minusHours(ctx.between(1, 48)), BUYERS[(i + 3) % BUYERS.length],
                    null, GOOD[i % GOOD.length], remarks[i % remarks.length]));
            if (id != null && i % 2 == 0) {
                audit(id);
            }
        }
    }

    /** 已发货的手工订单（2–30 天前发货）：审核生成包裹、填写运单号、按历史时间发货，供物流跟踪演示 */
    private void trackedManualOrders(DemoContext ctx, List<EcomStore> stores, Map<Long, List<EcomListing>> listings) {
        LocalDateTime now = LocalDateTime.now();
        String[] carriers = {"YunExpress", "4PX", "DHL", "USPS"};
        for (int i = 0; i < 24; i++) {
            EcomStore s = stores.get(i % stores.size());
            LocalDateTime time = now.minusDays(2 + i).minusHours(ctx.between(0, 12));
            Long id = create(manual(ctx, s, listings.get(s.getId()), null, time, BUYERS[i % BUYERS.length], null, GOOD[i % GOOD.length],
                    "独立站订单")); // i18n-ignore 演示数据存库
            if (id == null) {
                continue;
            }
            audit(id);
            String carrier = carriers[i % carriers.length];
            LocalDateTime shipTime = time.plusHours(ctx.between(8, 30));
            shipPackages(ctx, id, carrier, shipTime.isAfter(now) ? now.minusHours(1) : shipTime);
        }
    }

    /** 审核（只审核待审核 / 异常的订单，已被审单规则自动审核的跳过） */
    private void audit(Long id) {
        EcomOrder o = orderMapper.selectById(id);
        if (o != null && (o.getLocalStatus() == EcomLocalStatus.PENDING || o.getLocalStatus() == EcomLocalStatus.EXCEPTION)) {
            tx.run("audit", () -> opsService.audit(List.of(id)));
        }
    }

    private void shipPackages(DemoContext ctx, Long orderId, String carrier, LocalDateTime shipTime) {
        for (EcomPackage p : packageMapper.selectByOrderId(orderId)) {
            if (p.getStatus() != EcomPackageStatus.WAIT_LABEL) {
                continue;
            }
            String tracking = trackingNo(ctx, carrier);
            tx.run("label", () -> packageService.manualTracking(p.getId(), new TrackingRequest(carrier, tracking, null, new BigDecimal("0.6"),
                    BigDecimal.valueOf(ctx.between(28, 65)))));
            tx.run("ship", () -> packageService.ship(List.of(p.getId()), shipTime));
        }
    }

    private static String trackingNo(DemoContext ctx, String carrier) {
        return switch (carrier) {
            case "YunExpress" -> "YT" + ctx.between(1000, 9999) + ctx.between(10000000, 99999999);
            case "4PX" -> "4PX30" + ctx.between(10000000, 99999999) + ctx.between(10, 99) + "CN";
            case "DHL" -> String.valueOf(ctx.between(1000000000, 1999999999));
            default -> "9400" + ctx.between(100000000, 999999999) + ctx.between(100000000, 999999999);
        };
    }

    /** 部分发货：两行的手工订单拆成两个包裹，先发一个 */
    private void partialShipment(DemoContext ctx, List<EcomStore> stores, Map<Long, List<EcomListing>> listings) {
        EcomStore s = stores.get(0);
        List<EcomListing> l = listings.get(s.getId());
        if (l.size() < 2) {
            return;
        }
        LocalDateTime time = LocalDateTime.now().minusDays(3);
        OrderEnhanceViews.ManualOrder req = new OrderEnhanceViews.ManualOrder(s.getId(), null, FulfillmentType.SELLER, time, "Grace Walker",
                null, "Grace Walker", "617-555-0123", "US", "MA", "Boston", "02108", "24 Beacon St", null, null, new BigDecimal("1.2"),
                "两件商品分两个包裹发出", List.of( // i18n-ignore 演示数据存库
                new OrderEnhanceViews.ManualLine(l.get(0).getSellerSku(), l.get(0).getTitle(), BigDecimal.ONE, BigDecimal.valueOf(39)),
                new OrderEnhanceViews.ManualLine(l.get(1).getSellerSku(), l.get(1).getTitle(), BigDecimal.ONE, BigDecimal.valueOf(25))));
        Long id = create(req);
        if (id == null) {
            return;
        }
        audit(id);
        List<EcomOrderItem> items = itemMapper.selectByOrderId(id);
        if (items.size() < 2) {
            return;
        }
        tx.run("split", () -> opsService.split(id, new SplitRequest(List.of(List.of(new SplitRequest.Line(items.get(0).getId(), BigDecimal.ONE)),
                List.of(new SplitRequest.Line(items.get(1).getId(), BigDecimal.ONE))))));
        List<EcomPackage> pkgs = packageMapper.selectByOrderId(id).stream().filter(p -> p.getStatus() == EcomPackageStatus.WAIT_LABEL).toList();
        if (!pkgs.isEmpty()) {
            EcomPackage p = pkgs.get(0);
            tx.run("label", () -> packageService.manualTracking(p.getId(), new TrackingRequest("USPS", trackingNo(ctx, "USPS"), null,
                    new BigDecimal("0.6"), new BigDecimal("32"))));
            tx.run("ship", () -> packageService.ship(List.of(p.getId()), time.plusHours(20)));
        }
    }

    // ---------------------------------------------------------------- 利润预估与打标

    /** 全部演示订单（最近 500 张）：重新预估利润、按规则自动打标；部分订单人工打标 */
    private void estimateAndTag(DemoContext ctx, Map<String, Long> tags) {
        List<EcomOrder> orders = demoOrders(500);
        Map<Long, EcomStore> stores = new LinkedHashMap<>();
        for (EcomOrder o : orders) {
            EcomStore store = stores.computeIfAbsent(o.getStoreId(), storeService::get);
            tx.run("estimate", () -> {
                List<EcomOrderItem> items = outboundSupport.attachParts(itemMapper.selectByOrderId(o.getId()));
                EcomOrderCheckService.Facts f = checkService.check(o, items, false);
                EcomOrderRuleService.Outcome r = ruleService.evaluate(o, store.getPlatform(), items, f);
                tagService.autoTag(o.getId(), r.tagIds());
            });
        }
        List<Long> manualTags = new ArrayList<>();
        for (String name : List.of("加急", "大客户", "礼品包装", "已联系买家")) { // i18n-ignore 演示数据存库
            if (tags.containsKey(name)) {
                manualTags.add(tags.get(name));
            }
        }
        if (manualTags.isEmpty()) {
            return;
        }
        for (EcomOrder o : orders) {
            if (ctx.chance(0.12)) {
                Long tag = manualTags.get(ctx.random().nextInt(manualTags.size()));
                tx.run("tag-order", () -> tagService.tag(List.of(o.getId()), List.of(tag)));
            }
        }
    }

    private List<EcomOrder> demoOrders(int limit) {
        List<Long> storeIds = storeService.all().stream().filter(s -> s.getCode() != null && s.getCode().startsWith(DemoKeys.STORE_CODE_PREFIX))
                .map(EcomStore::getId).toList();
        if (storeIds.isEmpty()) {
            return List.of();
        }
        return Queries.limit(orderMapper, Wrappers.<EcomOrder>lambdaQuery().in(EcomOrder::getStoreId, storeIds)
                .orderByDesc(EcomOrder::getOrderTime), limit);
    }

    // ---------------------------------------------------------------- 补发、备注

    /** 补发单：已发货的订单（优先有退货的）补发一件，部分审核、打单发货（出库成本计入售后成本） */
    private void resends(DemoContext ctx) {
        List<EcomReturn> returns = Queries.limit(returnMapper, Wrappers.<EcomReturn>lambdaQuery().orderByDesc(EcomReturn::getId), 50);
        List<Long> done = new ArrayList<>();
        int created = 0;
        String[] reasons = {"买家反馈少件", "运输破损", "发错颜色", "物流丢件，平台判赔后补发"}; // i18n-ignore 演示数据存库
        for (EcomReturn r : returns) {
            if (created >= 4 || done.contains(r.getOrderId())) {
                continue;
            }
            EcomOrder src = orderMapper.selectById(r.getOrderId());
            if (src == null || src.getOrderType() == EcomOrderType.RESEND) {
                continue;
            }
            Long id = resendOne(r.getOrderId(), r.getId(), reasons[created % reasons.length]);
            if (id != null) {
                done.add(r.getOrderId());
                created++;
                if (created % 2 == 1) {
                    audit(id);
                    shipPackages(ctx, id, "YunExpress", LocalDateTime.now().minusHours(ctx.between(2, 30)));
                }
            }
        }
        for (EcomOrder o : demoOrders(300)) {
            if (created >= 7) {
                break;
            }
            if (o.getLocalStatus() != EcomLocalStatus.SHIPPED || o.getOrderType() == EcomOrderType.RESEND || done.contains(o.getId()) || !ctx.chance(0.2)) {
                continue;
            }
            Long id = resendOne(o.getId(), null, reasons[created % reasons.length]);
            if (id != null) {
                done.add(o.getId());
                created++;
                if (created % 2 == 0) {
                    audit(id);
                }
            }
        }
    }

    private Long resendOne(Long orderId, Long returnId, String reason) {
        List<EcomOrderItem> items = itemMapper.selectByOrderId(orderId);
        if (items.isEmpty() || items.stream().anyMatch(i -> i.getMaterialId() == null && !Integer.valueOf(1).equals(i.getBundle()))) {
            return null;
        }
        EcomOrderItem first = items.get(0);
        Long[] id = {null};
        tx.run("resend", () -> id[0] = manualService.createResend(orderId, new OrderEnhanceViews.Resend(
                List.of(new OrderEnhanceViews.ResendLine(first.getId(), BigDecimal.ONE)), null, returnId, null, null, null, null, null, null, null, reason)));
        return id[0];
    }

    private void notes(DemoContext ctx) {
        for (EcomOrder o : demoOrders(120)) {
            if (!ctx.chance(0.18)) {
                continue;
            }
            int n = ctx.between(1, 3);
            for (int i = 0; i < n; i++) {
                String text = NOTES[ctx.random().nextInt(NOTES.length)];
                tx.run("note", () -> noteService.add(o.getId(), text));
            }
        }
    }

    // ---------------------------------------------------------------- 物流轨迹

    /** 演示店铺已发货的包裹：按发货天数生成轨迹（各状态、超时未上网、运输停滞） */
    private void tracks(DemoContext ctx) {
        List<Long> storeIds = storeService.all().stream().filter(s -> s.getCode() != null && s.getCode().startsWith(DemoKeys.STORE_CODE_PREFIX))
                .map(EcomStore::getId).toList();
        if (storeIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<EcomPackage> shipped = packageMapper.selectList(Wrappers.<EcomPackage>lambdaQuery().in(EcomPackage::getStoreId, storeIds)
                .eq(EcomPackage::getStatus, EcomPackageStatus.SHIPPED).isNotNull(EcomPackage::getShipTime));
        // 按发货天数轮流分配场景，保证各状态、各预警都有
        Scenario[] older = {Scenario.DELIVERED, Scenario.DELIVERED, Scenario.STALLED, Scenario.DELIVERED, Scenario.EXCEPTION, Scenario.DELIVERED,
                Scenario.RETURNED, Scenario.DELIVERED};
        Scenario[] middle = {Scenario.TRANSIT, Scenario.PICKUP, Scenario.TRANSIT, Scenario.NONE, Scenario.TRANSIT, Scenario.EXCEPTION};
        int oi = 0, mi = 0;
        for (EcomPackage p : shipped) {
            long hours = java.time.Duration.between(p.getShipTime(), now).toHours();
            Scenario scenario;
            double r = ctx.random().nextDouble();
            if (hours < 20) {
                scenario = r < 0.5 ? Scenario.NONE : Scenario.PICKED;
            } else if (hours < 72) {
                scenario = r < 0.15 ? Scenario.NONE : r < 0.5 ? Scenario.PICKED : Scenario.TRANSIT;
            } else if (hours < 24 * 8) {
                scenario = middle[mi++ % middle.length];
            } else {
                scenario = older[oi++ % older.length];
            }
            List<CarrierTrackEvent> events = timeline(ctx, p, scenario, now);
            EcomTrackStatus status = switch (scenario) {
                case PICKED -> EcomTrackStatus.PICKED_UP;
                case TRANSIT, STALLED -> EcomTrackStatus.IN_TRANSIT;
                case PICKUP -> EcomTrackStatus.PICKUP;
                case DELIVERED -> EcomTrackStatus.DELIVERED;
                case EXCEPTION -> EcomTrackStatus.EXCEPTION;
                case RETURNED -> EcomTrackStatus.RETURNED;
                case NONE -> EcomTrackStatus.NOT_FOUND;
            };
            TrackSource source = p.getCarrier() != null ? TrackSource.CARRIER : TrackSource.TRACK17;
            tx.run("track", () -> trackService.saveTracks(p.getId(), new CarrierTrackInfo(status, events), source));
        }
    }

    /** 一个包裹的轨迹（只保留不晚于现在的节点） */
    private List<CarrierTrackEvent> timeline(DemoContext ctx, EcomPackage p, Scenario scenario, LocalDateTime now) {
        List<CarrierTrackEvent> out = new ArrayList<>();
        if (scenario == Scenario.NONE) {
            return out;
        }
        String dest = destination(p.getShipCountry());
        LocalDateTime t = p.getShipTime().plusHours(ctx.between(3, 8));
        out.add(ev(t, "Shenzhen, CN", "Shipment picked up by carrier / 已揽收")); // i18n-ignore 演示数据存库
        if (scenario == Scenario.PICKED) {
            return keep(out, now);
        }
        t = t.plusHours(ctx.between(10, 20));
        out.add(ev(t, "Shenzhen, CN", "Departed sorting facility"));
        t = t.plusHours(ctx.between(18, 30));
        out.add(ev(t, "Hong Kong, HK", "Departed from origin airport"));
        if (scenario == Scenario.STALLED) {
            // 停滞：之后再没有更新（时间提前到 10 天以前）
            LocalDateTime shift = now.minusDays(ctx.between(10, 14));
            List<CarrierTrackEvent> shifted = new ArrayList<>();
            long back = java.time.Duration.between(shift, t).toHours();
            for (CarrierTrackEvent e : out) {
                shifted.add(ev(LocalDateTime.parse(e.time(), ISO).minusHours(Math.max(0, back)), e.location(), e.description()));
            }
            return shifted;
        }
        t = t.plusHours(ctx.between(30, 60));
        out.add(ev(t, gateway(p.getShipCountry()), "Arrived at destination country, customs cleared"));
        if (scenario == Scenario.TRANSIT) {
            return keep(out, now);
        }
        t = t.plusHours(ctx.between(20, 40));
        out.add(ev(t, dest, "Arrived at local delivery facility"));
        t = t.plusHours(ctx.between(6, 14));
        out.add(ev(t, dest, "Out for delivery"));
        switch (scenario) {
            case PICKUP -> {
                return keep(out, now);
            }
            case EXCEPTION -> out.add(ev(t.plusHours(ctx.between(4, 9)), dest, "Delivery attempt failed - incorrect address, contact carrier"));
            case RETURNED -> {
                out.add(ev(t.plusHours(ctx.between(4, 9)), dest, "Delivery attempt failed - recipient refused"));
                out.add(ev(t.plusDays(ctx.between(2, 4)), dest, "Returned to sender"));
            }
            default -> out.add(ev(t.plusHours(ctx.between(2, 8)), dest, "Delivered, left at front door"));
        }
        return keep(out, now);
    }

    private static List<CarrierTrackEvent> keep(List<CarrierTrackEvent> events, LocalDateTime now) {
        return events.stream().filter(e -> !LocalDateTime.parse(e.time(), ISO).isAfter(now)).toList();
    }

    private static CarrierTrackEvent ev(LocalDateTime t, String location, String description) {
        return new CarrierTrackEvent(t.format(ISO), location, description);
    }

    private static String gateway(String country) {
        return switch (country == null ? "" : country) {
            case "GB" -> "London Heathrow, GB";
            case "DE" -> "Frankfurt, DE";
            case "AU" -> "Sydney, AU";
            case "JP" -> "Tokyo Narita, JP";
            case "MY", "SG" -> "Kuala Lumpur, MY";
            default -> "Los Angeles, US";
        };
    }

    private static String destination(String country) {
        return switch (country == null ? "" : country) {
            case "GB" -> "Manchester, GB";
            case "DE" -> "Berlin, DE";
            case "AU" -> "Melbourne, AU";
            case "JP" -> "Osaka, JP";
            case "MY" -> "Penang, MY";
            case "SG" -> "Singapore, SG";
            default -> "Chicago, US";
        };
    }
}
