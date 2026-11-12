package com.lemon.erp.modules.ecom.owh.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.demo.DemoContext;
import com.lemon.erp.modules.ecom.demo.DemoContributor;
import com.lemon.erp.modules.ecom.demo.DemoKeys;
import com.lemon.erp.modules.ecom.demo.DemoTx;
import com.lemon.erp.modules.ecom.dto.OrderDraft;
import com.lemon.erp.modules.ecom.entity.EcomInbound;
import com.lemon.erp.modules.ecom.entity.EcomInboundItem;
import com.lemon.erp.modules.ecom.entity.EcomListing;
import com.lemon.erp.modules.ecom.entity.EcomOrderRule;
import com.lemon.erp.modules.ecom.entity.EcomPackage;
import com.lemon.erp.modules.ecom.entity.EcomPackageItem;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomPackageItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomPackageMapper;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomPackageStatus;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.InboundStatus;
import com.lemon.erp.modules.ecom.model.ListingComposeType;
import com.lemon.erp.modules.ecom.owh.connector.OwhData;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.InboundCreateRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.MappingRequest;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhAccount;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhOutbound;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhProduct;
import com.lemon.erp.modules.ecom.owh.entity.EcomOwhWarehouse;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhAccountMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhOutboundMapper;
import com.lemon.erp.modules.ecom.owh.mapper.EcomOwhProductMapper;
import com.lemon.erp.modules.ecom.owh.model.OwhFeeType;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundSource;
import com.lemon.erp.modules.ecom.owh.model.OwhInboundStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhOutboundStatus;
import com.lemon.erp.modules.ecom.owh.model.OwhProvider;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhAccountService;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhDocNos;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhFeeService;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhInboundService;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhOutboundService;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhStockService;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhSkuService;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhReturnService;
import com.lemon.erp.modules.ecom.owh.service.EcomOwhTransferService;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.ReturnCreateRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.SkuRequest;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.TransferLine;
import com.lemon.erp.modules.ecom.owh.dto.OwhRequests.TransferRequest;
import com.lemon.erp.modules.ecom.owh.model.OwhReturnStatus;
import com.lemon.erp.modules.ecom.dto.ReturnRequest;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.model.ReturnType;
import com.lemon.erp.modules.ecom.service.EcomReturnService;
import com.lemon.erp.modules.ecom.service.EcomInboundService;
import com.lemon.erp.modules.ecom.service.EcomListingService;
import com.lemon.erp.modules.ecom.service.EcomOrderRuleService;
import com.lemon.erp.modules.ecom.service.EcomOrderService;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import com.lemon.erp.modules.wms.dto.OptionItem;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.entity.WmsWarehouse;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.WarehouseType;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.StockReservationService;
import com.lemon.erp.modules.wms.service.WmsStockDocService;
import com.lemon.erp.modules.wms.service.WmsWarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 演示数据 · 海外仓（order 130）：两个海外仓账号（谷仓美国 → 美国海外仓（洛杉矶），万邑通英国 → 新建的英国伯明翰仓）、仓库映射与物流产品；
 * <ul>
 *   <li>入库：国内仓发往海外仓的在途调拨与 TikTok 美国店发往美国海外仓的头程货件生成入库单，分别处于已收货（ERP 已转可用）、收货中、已推送、草稿；</li>
 *   <li>出库：Shopify / eBay 店铺的美国、英国订单经审单规则指定海外仓发货，推送后分别为已发货（海外仓返回运单号后自动出库、回传）、
 *   已推送、截单中、已取消、推送失败，另有待推送的包裹；</li>
 *   <li>费用：已发货出库单的尾程运费与操作费（计入订单物流成本）、未发货出库单的操作费（待入账）、近 4 个月各 SKU 的仓租、入库单的卸货上架费；</li>
 *   <li>库存：按 ERP 账面生成海外仓库存快照（含锁定、在途），其中几个 SKU 有盘盈 / 盘亏差异，另有一个海外仓有、系统没有的 SKU。</li>
 * </ul>
 * 服务商接口没有真实账号，演示中不调用服务商：推送 / 同步的结果直接按服务商返回的结构写入（EcomOwhInboundService.applyRemote、
 * EcomOwhOutboundService.applyState），之后的库存、出库、回传、费用入账都走正常流程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OwhDemoContributor implements DemoContributor {

    private static final String US_WAREHOUSE = "DEMO-US-3PL";
    private static final String UK_WAREHOUSE = "DEMO-OWH-UK";
    private static final String[] OUT_STORES = {"DEMO-SHOPIFY", "DEMO-EBAY-US"};
    private static final String[][] US_ADDR = {{"Los Angeles", "CA", "90012", "418 S Main St"}, {"Phoenix", "AZ", "85004", "22 E Monroe St"},
            {"Dallas", "TX", "75201", "1700 Pacific Ave"}, {"Seattle", "WA", "98101", "1420 5th Ave"}, {"Denver", "CO", "80202", "1600 Glenarm Pl"},
            {"Chicago", "IL", "60601", "233 N Michigan Ave"}, {"Atlanta", "GA", "30303", "55 Park Pl NE"}};
    private static final String[][] UK_ADDR = {{"London", "Greater London", "SW1A 1AA", "10 Downing St"}, {"Manchester", "Greater Manchester", "M1 1AE", "3 Piccadilly Pl"},
            {"Birmingham", "West Midlands", "B1 1BB", "8 Brindleyplace"}, {"Leeds", "West Yorkshire", "LS1 4AP", "1 City Sq"}, {"Bristol", "Bristol", "BS1 4DJ", "12 Queen Sq"}};
    private static final String[] NAMES = {"Emily Johnson", "Michael Brown", "Sarah Davis", "James Wilson", "Olivia Taylor", "Daniel Moore",
            "Sophie Clark", "Harry Walker", "Grace Hall", "Oliver Young"};

    private final EcomOwhAccountService accountService;
    private final EcomOwhStockService stockService;
    private final EcomOwhInboundService inboundService;
    private final EcomOwhOutboundService outboundService;
    private final EcomOwhFeeService feeService;
    private final EcomOwhSkuService skuService;
    private final EcomOwhReturnService owhReturnService;
    private final EcomOwhTransferService transferService;
    private final EcomReturnService ecomReturnService;
    private final EcomOwhAccountMapper accountMapper;
    private final EcomOwhProductMapper productMapper;
    private final EcomOwhOutboundMapper outboundMapper;
    private final EcomStoreService storeService;
    private final EcomListingService listingService;
    private final EcomInboundService ecomInboundService;
    private final EcomOrderService orderService;
    private final EcomOrderRuleService ruleService;
    private final EcomPackageMapper packageMapper;
    private final EcomPackageItemMapper packageItemMapper;
    private final WmsWarehouseService warehouseService;
    private final WmsStockDocService stockDocService;
    private final StockReservationService reservationService;
    private final DocNoService docNoService;
    private final DemoTx tx;

    /** 一个海外仓：账号、映射、币种、服务商单号前缀、物流产品 */
    private record Owh(EcomOwhAccount account, EcomOwhWarehouse mapping, String currency, String prefix, String country, String[][] products) {
    }

    @Override
    public int order() {
        return 130;
    }

    @Override
    public String key() {
        return "owh";
    }

    @Override
    public void generate(DemoContext ctx) {
        Long local = ctx.get(DemoKeys.LOCAL_WAREHOUSE_ID);
        Map<String, Long> channels = ctx.get(DemoKeys.CHANNEL_BY_CODE);
        Map<String, Long> whIds = new HashMap<>();
        for (OptionItem o : warehouseService.options()) {
            whIds.put(o.code(), o.id());
        }
        if (local == null || whIds.get(US_WAREHOUSE) == null) {
            log.warn("Demo owh skipped: base warehouses missing");
            return;
        }
        List<Owh> owhs = new ArrayList<>();
        tx.run("owh-setup", () -> owhs.addAll(setup(whIds, channels)));
        if (owhs.size() < 2) {
            return;
        }
        Owh us = owhs.get(0);
        Owh uk = owhs.get(1);

        // 出库店铺的单品在线商品（自发货、映射了物料、1 件装）
        Map<Long, List<EcomListing>> listings = new LinkedHashMap<>();
        List<Long> materials = new ArrayList<>();
        for (String code : OUT_STORES) {
            EcomStore s = storeService.findByCode(code);
            if (s == null) {
                continue;
            }
            List<EcomListing> list = listingService.page(1, 200, s.getId(), null, null, FulfillmentType.SELLER, true, null).records().stream()
                    .filter(l -> l.getMaterialId() != null && (l.getComposeType() == null || l.getComposeType() == ListingComposeType.SINGLE)
                            && l.getPackQty() != null && l.getPackQty().compareTo(BigDecimal.ONE) == 0)
                    .limit(8).toList();
            listings.put(s.getId(), list);
            list.forEach(l -> {
                if (!materials.contains(l.getMaterialId())) {
                    materials.add(l.getMaterialId());
                }
            });
        }
        List<Long> picked = materials.stream().limit(8).toList();
        // 万邑通账号的前 3 个物料在海外仓使用自己的 SKU（WN- + 物料编码），其余按物料编码
        tx.run("owh-sku", () -> {
            Map<Long, String> codes = skuService.skus(uk.account().getId(), picked);
            picked.stream().limit(3).forEach(m -> skuService.save(uk.account().getId(), new SkuRequest(m, "WN-" + codes.get(m), "演示：万邑通商品 SKU"))); // i18n-ignore 演示数据存库
        });

        // 入库：国内仓 → 海外仓的在途调拨，生成入库单（已收货 / 收货中 / 已推送 / 草稿）
        transferInbound(ctx, local, us, picked, 42, OwhInboundStatus.RECEIVED, 1.0);
        transferInbound(ctx, local, us, picked, 12, OwhInboundStatus.RECEIVING, 0.6);
        // 英国仓的这批少收 1 件：ERP 按实收入库，产生一条待处理的收货差异
        transferInbound(ctx, local, uk, picked, 38, OwhInboundStatus.RECEIVED, 0.999);
        transferInbound(ctx, local, uk, picked, 7, OwhInboundStatus.PUSHED, 0);
        transferInbound(ctx, local, uk, picked, 2, OwhInboundStatus.DRAFT, 0);
        shipmentInbounds(ctx, us);

        // 出库：审单规则指定海外仓 → 订单自动审核生成包裹 → 推送 / 发货
        tx.run("owh-rules", () -> rules(us, uk));
        List<Long> usPackages = orders(ctx, us, listings, picked, 16);
        List<Long> ukPackages = orders(ctx, uk, listings, picked, 10);
        List<Long> shipped = new ArrayList<>();
        List<Long> pushed = new ArrayList<>();
        outbounds(ctx, us, usPackages, shipped, pushed);
        outbounds(ctx, uk, ukPackages, shipped, pushed);

        // 退件与海外仓调拨
        returns(ctx, us, shipped, OwhReturnStatus.RECEIVED, OwhReturnStatus.PUSHED);
        returns(ctx, uk, shipped, OwhReturnStatus.RECEIVED, OwhReturnStatus.DRAFT);
        transfers(ctx, local, us, uk, picked);

        // 费用与库存快照
        tx.run("owh-fees-us", () -> fees(ctx, us, shipped, pushed));
        tx.run("owh-fees-uk", () -> fees(ctx, uk, shipped, pushed));
        tx.run("owh-stock-us", () -> snapshot(ctx, us));
        tx.run("owh-stock-uk", () -> snapshot(ctx, uk));
    }

    // ---------------------------------------------------------------- 账号、仓库、映射、产品

    private List<Owh> setup(Map<String, Long> whIds, Map<String, Long> channels) {
        Long ukWh = whIds.get(UK_WAREHOUSE);
        if (ukWh == null) {
            WmsWarehouse w = new WmsWarehouse();
            w.setCode(UK_WAREHOUSE);
            w.setName("万邑通英国仓（伯明翰）"); // i18n-ignore 演示数据存库
            w.setAddress("Unit 3, Tyburn Road, Birmingham B24 8NB, UK");
            w.setType(WarehouseType.FINISHED);
            w.setSort(40);
            w.setStatus(EnableStatus.ENABLED);
            warehouseService.create(w);
            ukWh = w.getId();
        }
        String[][] usProducts = {{"UPS-GROUND", "UPS Ground（2–5 日）", "UPS", "DEMO-EXP"}, {"USPS-PRIORITY", "USPS Priority Mail（1–3 日）", "USPS", null}, // i18n-ignore 演示数据存库
                {"FEDEX-HOME", "FedEx Home Delivery", "FedEx", null}};
        String[][] ukProducts = {{"RM-TRACKED48", "Royal Mail Tracked 48", "Royal Mail", null}, {"DPD-NEXTDAY", "DPD Next Day", "DPD", null},
                {"EVRI-STD", "Evri Standard", "Evri", null}};
        Owh us = owh(OwhProvider.GOODCANG, "DEMO-GC-US", "谷仓美国", "USD", whIds.get(US_WAREHOUSE), "USWE", "Goodcang US West (Ontario, CA)", "US", // i18n-ignore 演示数据存库
                usProducts, "GC", channels, Map.of("appKey", "demo-gc-key", "appToken", "demo-gc-token"), null);
        Owh uk = owh(OwhProvider.WINIT, "DEMO-WINIT-UK", "万邑通英国", "GBP", ukWh, "UKBH", "WINIT UK Birmingham", "GB", ukProducts, "WO", // i18n-ignore 演示数据存库
                channels, Map.of("appKey", "demo-winit-key", "token", "demo-winit-token"), whIds.get("DEMO-DEFECT"));
        return List.of(us, uk);
    }

    private Owh owh(OwhProvider provider, String code, String name, String currency, Long warehouseId, String owhCode, String owhName, String country,
                    String[][] products, String prefix, Map<String, Long> channels, Map<String, String> auth, Long defectWarehouseId) {
        EcomOwhAccount a = new EcomOwhAccount();
        a.setCode(code);
        a.setName(name);
        a.setProvider(provider);
        a.setSandbox(1);
        a.setAutoSync(0);
        a.setCurrency(currency);
        a.setAuth(auth);
        a.setRemark("演示账号：没有真实授权，已关闭自动同步（手工推送、同步会失败）"); // i18n-ignore 演示数据存库
        Long id = accountService.create(a);
        boolean gb = "GB".equals(country);
        Long mappingId = accountService.saveMapping(null, new MappingRequest(id, warehouseId, owhCode, owhName, country, products[0][0], 0,
                EnableStatus.ENABLED, null, gb ? defectWarehouseId : null, gb ? "WINIT Receiving" : "Goodcang Receiving", gb ? "+44 121 555 0100" : "+1 909 555 0100",
                gb ? "West Midlands" : "CA", gb ? "Birmingham" : "Ontario", gb ? "B24 8NB" : "91761",
                gb ? "Unit 3, Tyburn Road" : "5600 E Santa Ana St, Building 2"));
        for (String[] p : products) {
            EcomOwhProduct x = new EcomOwhProduct();
            x.setAccountId(id);
            x.setProductCode(p[0]);
            x.setProductName(p[1]);
            x.setOwhCode(owhCode);
            x.setCarrierName(p[2]);
            x.setChannelId(p[3] == null || channels == null ? null : channels.get(p[3]));
            x.setStatus(EnableStatus.ENABLED);
            x.setSyncTime(LocalDateTime.now());
            productMapper.insert(x);
        }
        return new Owh(accountService.get(id), accountService.mapping(mappingId), currency, prefix, country, products);
    }

    // ---------------------------------------------------------------- 入库

    /** 国内仓 → 海外仓的在途调拨（daysAgo 天前调出），生成入库单并按 status 写入服务商状态；收货中按 ratio 部分收货 */
    private void transferInbound(DemoContext ctx, Long local, Owh o, List<Long> materials, int daysAgo, OwhInboundStatus status, double ratio) {
        if (materials.isEmpty()) {
            return;
        }
        tx.run("owh-inbound-transfer", () -> {
            LocalDate date = ctx.today().minusDays(daysAgo);
            Map<Long, BigDecimal> free = reservationService.free(local, materials);
            List<StockDocItemRequest> items = new ArrayList<>();
            for (Long m : materials) {
                BigDecimal f = Decimals.nz(free.get(m));
                int qty = Math.min(f.divide(BigDecimal.valueOf(6), 0, RoundingMode.DOWN).intValue(), 20 + ctx.between(0, 30));
                if (qty >= 5) {
                    items.add(new StockDocItemRequest(m, null, null, null, null, null, BigDecimal.valueOf(qty), null, null));
                }
            }
            if (items.isEmpty()) {
                return;
            }
            WmsStockDoc doc = stockDocService.createConfirmed(StockBizType.TRANSFER, new StockDocRequest(null, local, o.mapping().getWarehouseId(), date,
                    "补货发往" + o.account().getName() + "海外仓", items, true, null), "OWH-DEMO"); // i18n-ignore 演示数据存库
            Long id = inboundService.createFrom(new InboundCreateRequest(OwhInboundSource.TRANSFER, doc.getId()), false);
            if (status == OwhInboundStatus.DRAFT) {
                return;
            }
            Map<String, BigDecimal> received = new LinkedHashMap<>();
            boolean shortFirst = status == OwhInboundStatus.RECEIVED && ratio < 1;
            for (var it : inboundService.detail(id).getItems()) {
                BigDecimal q = status == OwhInboundStatus.RECEIVED ? it.getQty() : it.getQty().multiply(BigDecimal.valueOf(ratio)).setScale(0, RoundingMode.DOWN);
                if (shortFirst && received.isEmpty()) {
                    q = q.subtract(BigDecimal.ONE);
                }
                received.merge(it.getSku(), q, BigDecimal::add);
            }
            LocalDateTime receiveTime = date.plusDays(Math.min(daysAgo - 1, 18)).atTime(15, 30);
            OwhData.InboundState state = new OwhData.InboundState(status, status == OwhInboundStatus.RECEIVED ? "5" : status == OwhInboundStatus.RECEIVING ? "3" : "1",
                    status == OwhInboundStatus.PUSHED ? Map.of() : received, status == OwhInboundStatus.RECEIVED ? receiveTime : null);
            inboundService.applyRemote(id, o.prefix() + "I" + (20000000 + ctx.between(0, 9999999)), state);
        });
    }

    /** TikTok 美国店发往美国海外仓的头程货件：已发运的生成入库单并推送，已签收的补录为已收货（最多各 2 张） */
    private void shipmentInbounds(DemoContext ctx, Owh us) {
        EcomStore tt = storeService.findByCode("DEMO-TT-US");
        if (tt == null) {
            return;
        }
        List<EcomInbound> list = ecomInboundService.page(1, 50, tt.getId(), null, null).records().stream()
                .filter(i -> Objects.equals(i.getToWarehouseId(), us.mapping().getWarehouseId())).toList();
        int shippedN = 0;
        int receivedN = 0;
        for (EcomInbound ib : list) {
            boolean shipped = ib.getStatus() == InboundStatus.SHIPPED && shippedN < 2;
            boolean received = ib.getStatus() == InboundStatus.RECEIVED && receivedN < 2;
            if (!shipped && !received) {
                continue;
            }
            if (shipped) {
                shippedN++;
            } else {
                receivedN++;
            }
            tx.run("owh-inbound-shipment", () -> {
                Long id = inboundService.createFrom(new InboundCreateRequest(OwhInboundSource.INBOUND, ib.getId()), received);
                Map<String, BigDecimal> qty = new LinkedHashMap<>();
                if (received) {
                    for (var it : inboundService.detail(id).getItems()) {
                        qty.merge(it.getSku(), it.getQty(), BigDecimal::add);
                    }
                }
                LocalDateTime t = ib.getReceiveDate() == null ? null : ib.getReceiveDate().atTime(11, 0);
                inboundService.applyRemote(id, "GCI" + (30000000 + ctx.between(0, 9999999)),
                        new OwhData.InboundState(received ? OwhInboundStatus.RECEIVED : OwhInboundStatus.PUSHED, received ? "5" : "1", qty, t));
            });
        }
    }

    // ---------------------------------------------------------------- 出库

    private void rules(Owh us, Owh uk) {
        StringBuilder stores = new StringBuilder();
        for (String code : OUT_STORES) {
            EcomStore s = storeService.findByCode(code);
            if (s != null) {
                stores.append(stores.isEmpty() ? "" : ",").append(s.getId());
            }
        }
        if (stores.isEmpty()) {
            return;
        }
        ruleService.create(rule("美国订单走谷仓海外仓", 3, stores.toString(), "US", us.mapping().getWarehouseId())); // i18n-ignore 演示数据存库
        ruleService.create(rule("英国订单走万邑通海外仓", 4, stores.toString(), "GB", uk.mapping().getWarehouseId())); // i18n-ignore 演示数据存库
    }

    private static EcomOrderRule rule(String name, int priority, String storeIds, String country, Long warehouseId) {
        EcomOrderRule r = new EcomOrderRule();
        r.setName(name);
        r.setPriority(priority);
        r.setStatus(EnableStatus.ENABLED);
        r.setStoreIds(storeIds);
        r.setFulfillment(FulfillmentType.SELLER);
        r.setCountries(country);
        r.setWarehouseId(warehouseId);
        r.setAutoAudit(1);
        r.setRemark("海外仓一件代发：审核后推送海外仓出库单"); // i18n-ignore 演示数据存库
        return r;
    }

    /** 近 14 天的订单（收件国家为海外仓所在国家），写入后按审单规则自动审核，返回生成的海外仓包裹 */
    private List<Long> orders(DemoContext ctx, Owh o, Map<Long, List<EcomListing>> listings, List<Long> stockMaterials, int count) {
        List<Long> packages = new ArrayList<>();
        List<Map.Entry<Long, EcomListing>> pool = new ArrayList<>();
        listings.forEach((storeId, list) -> list.stream().filter(l -> stockMaterials.contains(l.getMaterialId()))
                .forEach(l -> pool.add(Map.entry(storeId, l))));
        if (pool.isEmpty()) {
            return packages;
        }
        boolean uk = "GB".equals(o.country());
        for (int i = 0; i < count; i++) {
            Map.Entry<Long, EcomListing> pick = pool.get(ctx.random().nextInt(pool.size()));
            EcomStore store = storeService.get(pick.getKey());
            EcomListing l = pick.getValue();
            int daysAgo = Math.max(0, 13 - i * 14 / count);
            LocalDateTime time = ctx.today().minusDays(daysAgo).atTime(ctx.between(1, 22), ctx.between(0, 59));
            String[] addr = (uk ? UK_ADDR : US_ADDR)[ctx.random().nextInt((uk ? UK_ADDR : US_ADDR).length)];
            String name = NAMES[ctx.random().nextInt(NAMES.length)];
            OrderDraft d = new OrderDraft();
            d.setStoreId(store.getId());
            d.setPlatformOrderNo((uk ? "UK" : "US") + "-OW" + (100000 + ctx.between(0, 899999)));
            d.setOrderTime(time);
            d.setPaidTime(time.plusMinutes(3));
            d.setStatus(EcomOrderStatus.UNSHIPPED);
            d.setFulfillment(FulfillmentType.SELLER);
            d.setBuyerName(name);
            d.setBuyerEmail(name.toLowerCase().replace(' ', '.') + "@example.com");
            d.setShipName(name);
            d.setShipPhone(uk ? "+44 20 7946 0" + ctx.between(100, 999) : "+1 206 555 0" + ctx.between(100, 199));
            d.setShipCountry(o.country());
            d.setShipCity(addr[0]);
            d.setShipState(addr[1]);
            d.setShipPostcode(addr[2]);
            d.setShipAddress(addr[3]);
            d.setCurrency(store.getCurrency());
            d.setShipBy(time.plusDays(2));
            OrderDraft.Line line = new OrderDraft.Line();
            line.setPlatformLineId(d.getPlatformOrderNo() + "-1");
            line.setSellerSku(l.getSellerSku());
            line.setTitle(l.getTitle());
            int qty = ctx.chance(0.2) ? 2 : 1;
            BigDecimal price = l.getPrice() == null ? BigDecimal.valueOf(19.99) : l.getPrice();
            line.setQty(BigDecimal.valueOf(qty));
            line.setPrice(price);
            line.setAmount(price.multiply(BigDecimal.valueOf(qty)));
            d.getLines().add(line);
            List<Long> created = new ArrayList<>();
            tx.run("owh-order", () -> created.add(orderService.createManual(d, null)));
            for (Long orderId : created) {
                packageMapper.selectByOrderId(orderId).stream()
                        .filter(p -> p.getStatus() == EcomPackageStatus.WAIT_LABEL && Objects.equals(p.getWarehouseId(), o.mapping().getWarehouseId()))
                        .forEach(p -> packages.add(p.getId()));
            }
        }
        return packages;
    }

    /**
     * 包裹按顺序分到各状态：早的已发货，之后已推送、截单中、已取消、推送失败，最近的留在待推送
     */
    private void outbounds(DemoContext ctx, Owh o, List<Long> packages, List<Long> shipped, List<Long> pushed) {
        int n = packages.size();
        int shippedEnd = n / 2;
        int pushedEnd = shippedEnd + Math.max(1, n * 15 / 100);
        int failedEnd = pushedEnd + 2 + Math.max(1, n * 15 / 100);
        for (int i = 0; i < n; i++) {
            Long pkg = packages.get(i);
            OwhOutboundStatus target = i < shippedEnd ? OwhOutboundStatus.SHIPPED : i < pushedEnd ? OwhOutboundStatus.PUSHED
                    : i == pushedEnd ? OwhOutboundStatus.INTERCEPTING : i == pushedEnd + 1 ? OwhOutboundStatus.CANCELLED
                    : i < failedEnd ? OwhOutboundStatus.FAILED : null;
            if (target == null) {
                continue;
            }
            tx.run("owh-outbound", () -> {
                EcomPackage p = packageMapper.selectById(pkg);
                LocalDateTime pushTime = p.getCreateTime() == null ? LocalDateTime.now() : p.getCreateTime().plusHours(2);
                String[] product = o.products()[ctx.random().nextInt(o.products().length)];
                EcomOwhOutbound ob = new EcomOwhOutbound();
                ob.setOutboundNo(docNoService.next(EcomOwhDocNos.OUTBOUND));
                ob.setPackageId(pkg);
                ob.setStoreId(p.getStoreId());
                ob.setAccountId(o.account().getId());
                ob.setMappingId(o.mapping().getId());
                ob.setWarehouseId(p.getWarehouseId());
                ob.setProductCode(product[0]);
                ob.setPushCount(1);
                if (target == OwhOutboundStatus.FAILED) {
                    ob.setStatus(OwhOutboundStatus.FAILED);
                    ob.setErrorMsg(o.account().getProvider() == OwhProvider.GOODCANG
                            ? "Goodcang 拒绝了请求：收件人邮编与城市不匹配（zipcode does not match city）" // i18n-ignore 演示数据存库
                            : "WINIT 拒绝了请求：SKU 可用库存不足（Insufficient available inventory）"); // i18n-ignore 演示数据存库
                    outboundMapper.insert(ob);
                    return;
                }
                ob.setOwhOrderNo(o.prefix() + (60000000 + ctx.between(0, 9999999)));
                ob.setStatus(OwhOutboundStatus.PUSHED);
                ob.setOwhStatus("W");
                ob.setPushTime(pushTime);
                ob.setSyncTime(pushTime.plusHours(1));
                outboundMapper.insert(ob);
                if (target == OwhOutboundStatus.SHIPPED) {
                    String tracking = switch (product[2]) {
                        case "UPS" -> "1Z" + (100000000L + ctx.between(0, 899999999)) + "US";
                        case "Royal Mail" -> "RM" + (100000000L + ctx.between(0, 899999999)) + "GB";
                        default -> "9400" + (100000000000L + ctx.between(0, 899999999));
                    };
                    outboundService.applyState(ob.getId(), new OwhData.OutboundState(OwhOutboundStatus.OUTBOUND, "D", tracking, product[2],
                            pushTime.plusHours(18 + ctx.between(0, 20))));
                    shipped.add(ob.getId());
                } else if (target == OwhOutboundStatus.PUSHED) {
                    pushed.add(ob.getId());
                } else if (target == OwhOutboundStatus.INTERCEPTING) {
                    outboundService.applyState(ob.getId(), new OwhData.OutboundState(OwhOutboundStatus.INTERCEPTING, "P", null, null, null));
                    pushed.add(ob.getId());
                } else {
                    outboundService.applyState(ob.getId(), new OwhData.OutboundState(OwhOutboundStatus.CANCELLED, "X", null, null, null));
                }
            });
        }
    }

    // ---------------------------------------------------------------- 费用与库存

    private void fees(DemoContext ctx, Owh o, List<Long> shipped, List<Long> pushed) {
        List<OwhData.Fee> fees = new ArrayList<>();
        boolean us = "USD".equals(o.currency());
        for (EcomOwhOutbound ob : outboundMapper.selectList(Wrappers.<EcomOwhOutbound>lambdaQuery().eq(EcomOwhOutbound::getAccountId, o.account().getId())
                .in(EcomOwhOutbound::getId, ids(shipped, pushed)))) {
            LocalDate d = (ob.getOutboundTime() != null ? ob.getOutboundTime() : ob.getPushTime()).toLocalDate();
            fees.add(new OwhData.Fee("OP-" + ob.getOwhOrderNo(), OwhFeeType.OPERATION, d, ob.getOwhOrderNo(), o.mapping().getOwhCode(), null, o.currency(),
                    money(us ? 1.6 + ctx.random().nextDouble() : 1.1 + ctx.random().nextDouble() * 0.6), "出库操作费（拣货、打包）")); // i18n-ignore 演示数据存库
            if (shipped.contains(ob.getId())) {
                fees.add(new OwhData.Fee("FR-" + ob.getOwhOrderNo(), OwhFeeType.FREIGHT, d, ob.getOwhOrderNo(), o.mapping().getOwhCode(), null, o.currency(),
                        money(us ? 6.5 + ctx.random().nextDouble() * 5 : 3.2 + ctx.random().nextDouble() * 2.5), "尾程运费 " + ob.getProductCode())); // i18n-ignore 演示数据存库
            }
        }
        // 已收货入库单的卸货上架费（按箱）
        for (var in : inboundService.page(1, 50, o.account().getId(), null, OwhInboundStatus.RECEIVED, null).records()) {
            if (in.getOwhOrderNo() == null) {
                continue;
            }
            LocalDate d = in.getReceiveTime() == null ? ctx.today() : in.getReceiveTime().toLocalDate();
            int boxes = in.getBoxCount() == null || in.getBoxCount() == 0 ? 1 : in.getBoxCount();
            fees.add(new OwhData.Fee("IN-" + in.getOwhOrderNo(), OwhFeeType.INBOUND, d, in.getOwhOrderNo(), o.mapping().getOwhCode(), null, o.currency(),
                    money(boxes * (us ? 3.5 : 2.8) + in.getTotalQty().doubleValue() * 0.12), "卸货上架费 " + boxes + " 箱")); // i18n-ignore 演示数据存库
        }
        // 近 4 个月各 SKU 的仓租（月末计费）
        List<Map<String, Object>> stock = stockService.erpStock(o.mapping().getWarehouseId());
        Map<Long, String> skus = skuService.skus(o.account().getId(), stock.stream().map(x -> ((Number) x.get("materialId")).longValue()).toList());
        for (int k = 3; k >= 0; k--) {
            YearMonth m = YearMonth.from(ctx.today()).minusMonths(k);
            LocalDate d = k == 0 ? ctx.today() : m.atEndOfMonth();
            for (Map<String, Object> s : stock) {
                String sku = skus.get(((Number) s.get("materialId")).longValue());
                double qty = ((Number) s.get("qty")).doubleValue();
                fees.add(new OwhData.Fee("ST-" + m + "-" + sku, OwhFeeType.STORAGE, d, null, o.mapping().getOwhCode(), sku, o.currency(),
                        money(Math.max(0.3, qty * (us ? 0.045 : 0.035) * (k == 0 ? 0.4 : 1))), m + " 仓租")); // i18n-ignore 演示数据存库
            }
        }
        if (!fees.isEmpty()) {
            feeService.save(o.account(), fees);
        }
        accountMapper.update(Wrappers.<EcomOwhAccount>lambdaUpdate().set(EcomOwhAccount::getFeeSyncTime, ctx.today().atTime(3, 30))
                .eq(EcomOwhAccount::getId, o.account().getId()));
    }

    /** 按 ERP 账面生成海外仓库存快照：锁定 = 已推送未出库的数量，在途 = 在途调拨数量；前两个 SKU 盘盈 3 / 盘亏 2，另有一个系统没有的 SKU */
    private void snapshot(DemoContext ctx, Owh o) {
        Map<Long, BigDecimal> locked = new HashMap<>();
        for (EcomOwhOutbound ob : outboundMapper.selectList(Wrappers.<EcomOwhOutbound>lambdaQuery().eq(EcomOwhOutbound::getAccountId, o.account().getId())
                .in(EcomOwhOutbound::getStatus, OwhOutboundStatus.PUSHED, OwhOutboundStatus.INTERCEPTING))) {
            for (EcomPackageItem it : packageItemMapper.selectByPackageIds(List.of(ob.getPackageId()))) {
                locked.merge(it.getMaterialId(), it.getMaterialQty(), BigDecimal::add);
            }
        }
        List<OwhData.Stock> rows = new ArrayList<>();
        int k = 0;
        List<Map<String, Object>> erpRows = stockService.erpStock(o.mapping().getWarehouseId());
        Map<Long, String> skus = skuService.skus(o.account().getId(), erpRows.stream().map(x -> ((Number) x.get("materialId")).longValue()).toList());
        for (Map<String, Object> s : erpRows) {
            Long m = ((Number) s.get("materialId")).longValue();
            BigDecimal erp = new BigDecimal(String.valueOf(s.get("qty")));
            BigDecimal lock = Decimals.nz(locked.get(m)).min(erp);
            BigDecimal delta = k == 0 ? BigDecimal.valueOf(3) : k == 1 ? BigDecimal.valueOf(-2) : BigDecimal.ZERO;
            BigDecimal avail = erp.subtract(lock).add(delta).max(BigDecimal.ZERO);
            BigDecimal transit = k % 3 == 2 ? BigDecimal.valueOf(ctx.between(5, 30)) : BigDecimal.ZERO;
            rows.add(new OwhData.Stock(skus.get(m), avail, transit, lock));
            k++;
        }
        rows.add(new OwhData.Stock(o.prefix() + "-RETURN-001", BigDecimal.valueOf(2), BigDecimal.ZERO, BigDecimal.ZERO));
        stockService.saveSnapshot(o.mapping(), rows);
        accountMapper.update(Wrappers.<EcomOwhAccount>lambdaUpdate().set(EcomOwhAccount::getStockSyncTime, LocalDateTime.now().minusMinutes(ctx.between(5, 50)))
                .eq(EcomOwhAccount::getId, o.account().getId()));
    }

    /** 已发货出库单的订单生成退货退款（退回海外仓）与海外仓退件单，按 states 依次为已收货（含不良品）/ 已推送 / 草稿 */
    private void returns(DemoContext ctx, Owh o, List<Long> shipped, OwhReturnStatus... states) {
        List<EcomOwhOutbound> obs = outboundMapper.selectList(Wrappers.<EcomOwhOutbound>lambdaQuery().eq(EcomOwhOutbound::getAccountId, o.account().getId())
                .eq(EcomOwhOutbound::getStatus, OwhOutboundStatus.SHIPPED).orderByAsc(EcomOwhOutbound::getId));
        int n = 0;
        for (EcomOwhOutbound ob : obs) {
            if (n >= states.length) {
                break;
            }
            OwhReturnStatus target = states[n++];
            tx.run("owh-return", () -> {
                Long orderId = packageItemMapper.selectByPackageIds(List.of(ob.getPackageId())).get(0).getOrderId();
                EcomOrderItem oi = orderService.items(orderId).get(0);
                Long erId = ecomReturnService.create(new ReturnRequest(orderId, ReturnType.RETURN_REFUND, target == OwhReturnStatus.RECEIVED ? "QUALITY" : "WRONG_ITEM",
                        null, o.mapping().getWarehouseId(), "买家退货寄回海外仓", // i18n-ignore 演示数据存库
                        List.of(new ReturnRequest.Item(oi.getId(), oi.getQty(), oi.getAmount() == null ? BigDecimal.TEN : oi.getAmount()))));
                Long id = owhReturnService.create(new ReturnCreateRequest(erId, "USPS", "9205" + (100000000000L + ctx.between(0, 899999999)), null));
                if (target == OwhReturnStatus.DRAFT) {
                    return;
                }
                Map<String, BigDecimal> good = new LinkedHashMap<>();
                Map<String, BigDecimal> bad = new LinkedHashMap<>();
                if (target == OwhReturnStatus.RECEIVED) {
                    for (var it : owhReturnService.detail(id).getItems()) {
                        // 英国仓演示不良品（转入不良品仓）；美国仓多件时 1 件不良（报废出库），单件全部良品
                        boolean defect = bad.isEmpty() && ("GB".equals(o.country()) || it.getQty().compareTo(BigDecimal.ONE) > 0);
                        BigDecimal b = defect ? BigDecimal.ONE : BigDecimal.ZERO;
                        bad.put(it.getSku(), b);
                        good.put(it.getSku(), it.getQty().subtract(b));
                    }
                }
                owhReturnService.applyRemote(id, o.prefix() + "R" + (40000000 + ctx.between(0, 9999999)), new OwhData.ReturnState(target,
                        target == OwhReturnStatus.RECEIVED ? "3" : "1", good, bad, target == OwhReturnStatus.RECEIVED ? LocalDateTime.now().minusDays(1) : null));
            });
        }
    }

    /**
     * 海外仓调拨：美国 → 英国海外仓（在途，英国入库单已推送）、英国海外仓 → 国内仓（调出仓处理中）、国内仓 → 英国海外仓（已收货）、美国海外仓 → 国内仓（草稿）
     */
    private void transfers(DemoContext ctx, Long local, Owh us, Owh uk, List<Long> materials) {
        if (materials.size() < 2) {
            return;
        }
        Long usWh = us.mapping().getWarehouseId();
        Long ukWh = uk.mapping().getWarehouseId();
        List<TransferLine> lines = List.of(new TransferLine(materials.get(0), BigDecimal.valueOf(3)), new TransferLine(materials.get(1), BigDecimal.valueOf(2)));
        tx.run("owh-transfer-us-uk", () -> {
            Long id = transferService.create(new TransferRequest(usWh, ukWh, null, "美国仓滞销调往英国仓", lines)); // i18n-ignore 演示数据存库
            transferService.recordPushed(id, "GCT" + (50000000 + ctx.between(0, 9999999)));
            transferService.applyOutState(id, new OwhData.OutboundState(OwhOutboundStatus.OUTBOUND, "D", "1Z" + (300000000L + ctx.between(0, 99999999)), "UPS Freight",
                    LocalDateTime.now().minusDays(3)));
            Long inboundId = transferService.detail(id).getInboundId();
            if (inboundId != null) {
                inboundService.applyRemote(inboundId, "WOI" + (60000000 + ctx.between(0, 9999999)), new OwhData.InboundState(OwhInboundStatus.PUSHED, "1", Map.of(), null));
            }
        });
        tx.run("owh-transfer-uk-local", () -> {
            Long id = transferService.create(new TransferRequest(ukWh, local, null, "英国仓不良品退回国内", List.of(lines.get(1)))); // i18n-ignore 演示数据存库
            transferService.recordPushed(id, "WOT" + (50000000 + ctx.between(0, 9999999)));
        });
        tx.run("owh-transfer-local-uk", () -> {
            Long id = transferService.create(new TransferRequest(local, ukWh, null, "国内仓补货英国仓", lines)); // i18n-ignore 演示数据存库
            transferService.submit(id);
            Long inboundId = transferService.detail(id).getInboundId();
            if (inboundId != null) {
                Map<String, BigDecimal> received = new LinkedHashMap<>();
                for (var it : inboundService.detail(inboundId).getItems()) {
                    received.merge(it.getSku(), it.getQty(), BigDecimal::add);
                }
                inboundService.applyRemote(inboundId, "WOI" + (70000000 + ctx.between(0, 9999999)),
                        new OwhData.InboundState(OwhInboundStatus.RECEIVED, "FN", received, LocalDateTime.now().minusDays(1)));
            }
        });
        tx.run("owh-transfer-draft", () -> transferService.create(new TransferRequest(usWh, local, "UPS-GROUND", "美国仓退回国内（待确认）", // i18n-ignore 演示数据存库
                List.of(lines.get(0)))));
    }

    private static List<Long> ids(List<Long> a, List<Long> b) {
        List<Long> out = new ArrayList<>(a);
        out.addAll(b);
        if (out.isEmpty()) {
            out.add(-1L);
        }
        return out;
    }

    private static BigDecimal money(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
