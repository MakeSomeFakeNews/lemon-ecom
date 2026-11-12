package com.lemon.erp.modules.ecom.demo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.dto.OrderDraft;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.entity.EcomOrderItem;
import com.lemon.erp.modules.ecom.entity.EcomOrderItemPart;
import com.lemon.erp.modules.ecom.entity.EcomOrderRule;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.mapper.EcomInboundItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderItemMapper;
import com.lemon.erp.modules.ecom.mapper.EcomOrderMapper;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.service.EcomLogisticsService;
import com.lemon.erp.modules.ecom.service.EcomOrderRuleService;
import com.lemon.erp.modules.ecom.service.EcomOrderService;
import com.lemon.erp.modules.ecom.service.EcomOutboundSupport;
import com.lemon.erp.modules.ecom.service.EcomStoreService;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.ecom.entity.EcomTaxRate;
import com.lemon.erp.modules.ecom.service.EcomOrderTax;
import com.lemon.erp.modules.ecom.service.EcomTaxService;
import com.lemon.erp.modules.scm.dto.TradeDocItemRequest;
import com.lemon.erp.modules.scm.dto.TradeDocRequest;
import com.lemon.erp.modules.scm.entity.ScmTradeDoc;
import com.lemon.erp.modules.scm.model.TradeKind;
import com.lemon.erp.modules.scm.service.ScmTradeDocService;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.service.StockCostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 演示数据 · 平台订单（order 50）：按订单计划经 {@link EcomOrderService#saveDraft} 写入（匹配在线商品、汇率折算、佣金估算、税率、审单规则），
 * 三天前的已发货订单按「店铺 × 仓库 × 发货日」汇总生成销售出库（经 {@link EcomOutboundSupport#outbound}，库存、成本、应收与逐单出库一致），
 * 出库成本按行分摊回订单，平台仓订单的物流成本按头程单件成本分摊、自发货订单按渠道费率试算，利润同订单利润公式。
 * 先写历史订单，再建审单规则（发货渠道、截单时间、大额人工审核），最后写近期订单（规则对近期订单生效）；近期订单的作业环节见
 * {@link DemoOrderOpsContributor}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoOrderContributor implements DemoContributor {

    private static final int CHUNK = 500;

    private final EcomOrderService orderService;
    private final EcomOrderMapper orderMapper;
    private final EcomOrderItemMapper itemMapper;
    private final EcomOutboundSupport outboundSupport;
    private final EcomStoreService storeService;
    private final EcomLogisticsService logisticsService;
    private final EcomOrderRuleService ruleService;
    private final EcomInboundItemMapper inboundItemMapper;
    private final EcomTaxService taxService;
    private final ExchangeRateProvider rateProvider;
    private final ScmTradeDocService tradeDocService;
    private final StockCostService stockCostService;

    @Override
    public int order() {
        return 50;
    }

    @Override
    public String key() {
        return "orders";
    }

    @Override
    public void generate(DemoContext ctx) {
        DemoOrderPlan plan = ctx.get(DemoContributors.ORDER_PLAN);
        Map<String, Long> channels = ctx.get(DemoKeys.CHANNEL_BY_CODE);
        List<DemoOrderPlan.Order> history = plan.orders.stream().filter(o -> !o.recent()).toList();
        List<DemoOrderPlan.Order> recent = plan.orders.stream().filter(DemoOrderPlan.Order::recent).toList();
        long t0 = System.currentTimeMillis();
        List<Saved> saved = insertHistory(ctx, history);
        long t1 = System.currentTimeMillis();
        outboundHistory(ctx, saved, channels);
        log.info("Demo orders: {} saved in {} ms, outbound in {} ms", history.size(), t1 - t0, System.currentTimeMillis() - t1);
        rules(ctx, channels);
        for (DemoOrderPlan.Order o : recent) {
            orderService.saveDraft(draft(ctx, o), false);
        }
        assignIds(ctx, recent);
        ctx.put(DemoKeys.ORDER_IDS, plan.orders.stream().map(o -> o.id).filter(Objects::nonNull).toList());
    }

    private OrderDraft draft(DemoContext ctx, DemoOrderPlan.Order o) {
        OrderDraft d = new OrderDraft();
        d.setStoreId(o.storeId);
        d.setPlatformOrderNo(o.no);
        d.setOrderTime(o.time);
        d.setPaidTime(o.time.plusMinutes(ctx.between(0, 20)));
        d.setStatus(switch (o.stage) {
            case SHIPPED -> EcomOrderStatus.SHIPPED;
            case CANCELLED -> EcomOrderStatus.CANCELLED;
            default -> EcomOrderStatus.UNSHIPPED;
        });
        d.setFulfillment(o.fulfillment);
        d.setBuyerName(o.buyer);
        d.setBuyerEmail(o.email);
        d.setShipName(o.buyer);
        d.setShipPhone(o.store.platform() == EcomPlatform.AMAZON ? null : "+" + ctx.between(1, 99) + " " + ctx.between(200, 999) + "-" + ctx.between(1000, 9999));
        d.setShipCountry(o.country);
        d.setShipState(o.state);
        d.setShipCity(o.city);
        d.setShipPostcode(o.postcode);
        d.setShipAddress(ctx.between(10, 9999) + " " + ctx.pick("Main St", "Oak Ave", "Maple Dr", "Park Rd", "Hauptstraße", "Elm St", "Lake View Blvd"));
        d.setCurrency(o.store.currency());
        if (o.fulfillment == FulfillmentType.SELLER) {
            d.setShipBy(o.time.plusDays(o.stage == DemoOrderPlan.Stage.PENDING && ctx.chance(0.3) ? 1 : 2));
            d.setDeliverBy(o.time.plusDays(14));
        }
        boolean coupon = ctx.chance(0.1);
        BigDecimal subtotal = BigDecimal.ZERO;
        int n = 1;
        for (DemoOrderPlan.Line l : o.lines) {
            OrderDraft.Line line = new OrderDraft.Line();
            line.setPlatformLineId(o.no + "-" + n++);
            line.setSellerSku(l.listing().sku());
            line.setPlatformItemId(l.listing().platformItemId());
            line.setTitle(l.listing().title());
            line.setQty(BigDecimal.valueOf(l.qty()));
            line.setPrice(l.listing().price());
            BigDecimal amount = l.listing().price().multiply(BigDecimal.valueOf(l.qty()));
            subtotal = subtotal.add(amount);
            if (coupon) {
                line.setDiscount(amount.multiply(BigDecimal.valueOf(0.1)).setScale(DemoCatalog.scale(o.store.currency()), RoundingMode.HALF_UP));
            }
            d.getLines().add(line);
        }
        if (o.store.platform() == EcomPlatform.SHOPIFY && subtotal.compareTo(BigDecimal.valueOf(35)) < 0) {
            d.setShippingAmount(BigDecimal.valueOf(4.99));
        }
        return d;
    }

    /** 写入后取订单 ID（按 店铺 + 平台订单号） */
    private void assignIds(DemoContext ctx, List<DemoOrderPlan.Order> orders) {
        List<Long> storeIds = ctx.get(DemoKeys.STORE_IDS);
        Map<String, Long> ids = new HashMap<>();
        orderMapper.selectList(Wrappers.<EcomOrder>lambdaQuery().select(EcomOrder::getId, EcomOrder::getStoreId, EcomOrder::getPlatformOrderNo)
                .in(EcomOrder::getStoreId, storeIds)).forEach(o -> ids.put(o.getStoreId() + "|" + o.getPlatformOrderNo(), o.getId()));
        for (DemoOrderPlan.Order o : orders) {
            o.id = ids.get(o.storeId + "|" + o.no);
        }
    }

    // ---------------------------------------------------------------- 历史订单：批量写入与汇总出库

    /** 写入后的历史订单（内存中保留，出库时直接使用） */
    private record Saved(DemoOrderPlan.Order plan, EcomOrder order, List<EcomOrderItem> items) {
    }

    /**
     * 历史订单的批量写入路径：与 {@link EcomOrderService#saveDraft} 相同的字段与口径（汇率、佣金估算、税率、在线商品匹配与组合展开、利润），
     * 只是不再逐单查重和执行审单规则（规则在历史订单之后才建立）
     */
    private List<Saved> insertHistory(DemoContext ctx, List<DemoOrderPlan.Order> history) {
        Map<Long, EcomStore> stores = new HashMap<>();
        Map<String, BigDecimal> rates = new HashMap<>();
        Map<String, Optional<EcomTaxRate>> taxes = new HashMap<>();
        Long deptId = SecurityUtils.getLoginUser().deptId();
        List<Saved> out = new ArrayList<>();
        for (DemoOrderPlan.Order p : history) {
            OrderDraft d = draft(ctx, p);
            EcomStore store = stores.computeIfAbsent(p.storeId, storeService::get);
            EcomOrder o = new EcomOrder();
            o.setStoreId(store.getId());
            o.setPlatformOrderNo(d.getPlatformOrderNo());
            o.setOrderTime(d.getOrderTime());
            o.setPaidTime(d.getPaidTime());
            o.setStatus(d.getStatus());
            o.setFulfillment(d.getFulfillment());
            o.setBuyerName(d.getBuyerName());
            o.setBuyerEmail(d.getBuyerEmail());
            o.setShipName(d.getShipName());
            o.setShipPhone(d.getShipPhone());
            o.setShipCountry(d.getShipCountry());
            o.setShipState(d.getShipState());
            o.setShipCity(d.getShipCity());
            o.setShipPostcode(d.getShipPostcode());
            o.setShipAddress(d.getShipAddress());
            o.setCurrency(d.getCurrency());
            o.setShipBy(d.getShipBy());
            o.setDeliverBy(d.getDeliverBy());
            LocalDate day = o.getOrderTime().toLocalDate();
            o.setExchangeRate(rates.computeIfAbsent(o.getCurrency() + day.getYear() + "-" + day.getMonthValue(), k -> rateProvider.rate(o.getCurrency(), day)));
            List<EcomOrderItem> items = new ArrayList<>();
            BigDecimal item = BigDecimal.ZERO, shipping = BigDecimal.ZERO, discount = BigDecimal.ZERO;
            for (OrderDraft.Line l : d.getLines()) {
                EcomOrderItem it = new EcomOrderItem();
                it.setPlatformLineId(l.getPlatformLineId());
                it.setSellerSku(l.getSellerSku());
                it.setPlatformItemId(l.getPlatformItemId());
                it.setTitle(l.getTitle());
                it.setQty(Decimals.qty(l.getQty()));
                it.setPrice(l.getPrice());
                it.setAmount(Decimals.money(it.getQty().multiply(it.getPrice())));
                it.setShipping(BigDecimal.ZERO.setScale(2));
                it.setTax(BigDecimal.ZERO.setScale(2));
                it.setDiscount(Decimals.money(Decimals.nz(l.getDiscount())));
                items.add(it);
                item = item.add(it.getAmount());
                discount = discount.add(it.getDiscount());
            }
            if (d.getShippingAmount() != null) {
                shipping = Decimals.money(d.getShippingAmount());
            }
            o.setItemAmount(item);
            o.setShippingAmount(shipping);
            o.setTaxAmount(BigDecimal.ZERO.setScale(2));
            o.setDiscountAmount(discount);
            o.setTotalAmount(item.add(shipping).subtract(discount));
            o.setTotalBase(Decimals.money(o.getTotalAmount().multiply(o.getExchangeRate())));
            o.setPlatformFee(Decimals.money(o.getTotalAmount().multiply(Decimals.nz(store.getCommissionRate())).divide(Decimals.HUNDRED, 6, RoundingMode.HALF_UP)));
            o.setFulfillmentFee(BigDecimal.ZERO);
            o.setOtherFee(BigDecimal.ZERO);
            o.setFeeActual(0);
            o.setRefundAmount(BigDecimal.ZERO);
            o.setCostAmount(BigDecimal.ZERO);
            o.setShippingCost(BigDecimal.ZERO);
            o.setReturnFee(BigDecimal.ZERO);
            EcomOrderTax.assign(o, taxes.computeIfAbsent(store.getMarketplace() + "|" + o.getShipCountry(),
                    k -> Optional.ofNullable(taxService.resolve(store.getMarketplace(), o.getShipCountry()))).orElse(null));
            String exception = outboundSupport.match(store.getId(), items);
            o.setException(exception);
            o.setLocalStatus(o.getStatus() == EcomOrderStatus.CANCELLED ? EcomLocalStatus.CLOSED
                    : exception == null ? EcomLocalStatus.PENDING : EcomLocalStatus.EXCEPTION);
            o.setProfit(EcomOrderService.computeProfit(o));
            o.setDeptId(deptId);
            orderMapper.insert(o);
            for (EcomOrderItem it : items) {
                it.setOrderId(o.getId());
                List<EcomOrderItemPart> parts = it.getParts();
                itemMapper.insert(it);
                it.setParts(parts);
            }
            if (items.stream().anyMatch(EcomOutboundSupport::isBundle)) {
                outboundSupport.saveParts(o.getId(), items);
            }
            p.id = o.getId();
            out.add(new Saved(p, o, items));
        }
        return out;
    }

    /** 出库单分组：店铺 × 仓库 × 单据日期 × 汇率 */
    private record GroupKey(Long storeId, Long warehouseId, LocalDate docDate, BigDecimal rate) {
    }

    /** 最近这些天按发货日逐日出库，更早的按周汇总（单据日期为当周最后一个发货日所在周日，不晚于汇总截止日） */
    private static final int DAILY_DAYS = 28;

    private static LocalDate docDate(LocalDate today, LocalDate shipDate) {
        LocalDate cutoff = today.minusDays(DAILY_DAYS);
        if (!shipDate.isBefore(cutoff)) {
            return shipDate;
        }
        LocalDate sunday = shipDate.plusDays(7L - shipDate.getDayOfWeek().getValue());
        return sunday.isBefore(cutoff) ? sunday : cutoff.minusDays(1);
    }

    private void outboundHistory(DemoContext ctx, List<Saved> saved, Map<String, Long> channels) {
        Map<GroupKey, List<Saved>> groups = saved.stream()
                .filter(x -> x.plan().stage == DemoOrderPlan.Stage.SHIPPED && x.order().getLocalStatus() == EcomLocalStatus.PENDING)
                .collect(Collectors.groupingBy(x -> new GroupKey(x.order().getStoreId(), x.plan().warehouseId, docDate(ctx.today(), x.plan().shipDate),
                        x.order().getExchangeRate()), LinkedHashMap::new, Collectors.toList()));
        Map<Long, EcomStore> stores = new HashMap<>();
        Map<String, BigDecimal> landed = new HashMap<>();
        Map<String, BigDecimal> freight = new HashMap<>();
        List<GroupKey> keys = new ArrayList<>(groups.keySet());
        keys.sort(Comparator.comparing(GroupKey::docDate));
        for (GroupKey key : keys) {
            List<Saved> group = groups.get(key);
            EcomStore store = stores.computeIfAbsent(key.storeId(), storeService::get);
            // 同一物料合并为一行出库（单价为各行本位币金额的加权平均），成本按物料的出库成本单价分摊回各订单行
            Map<Long, BigDecimal[]> byMaterial = new LinkedHashMap<>();
            for (Saved x : group) {
                for (EcomOrderItem it : x.items()) {
                    for (EcomOutboundSupport.Line l : EcomOutboundSupport.Line.wholeExpanded(it)) {
                        BigDecimal lineBase = it.getAmount().add(Decimals.nz(it.getShipping())).add(Decimals.nz(it.getTax()))
                                .subtract(Decimals.nz(it.getDiscount())).multiply(key.rate());
                        if (l.part() != null) {
                            lineBase = lineBase.multiply(Decimals.nz(l.part().getShare()));
                        }
                        BigDecimal[] acc = byMaterial.computeIfAbsent(l.materialId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                        acc[0] = acc[0].add(l.materialQty());
                        acc[1] = acc[1].add(lineBase.max(BigDecimal.ZERO));
                    }
                }
            }
            List<TradeDocItemRequest> lines = new ArrayList<>();
            byMaterial.forEach((mid, acc) -> lines.add(new TradeDocItemRequest(null, null, null, null, null, acc[0], null, null, mid,
                    acc[1].divide(acc[0], 4, RoundingMode.HALF_UP), BigDecimal.ZERO, null)));
            String remark = Messages.get("ecom.text.demo.outboundRemark", store.getName(), group.size());
            Long docId = tradeDocService.create(TradeKind.SALES_OUT, new TradeDocRequest(null, key.warehouseId(), key.docDate(), remark, null, lines,
                    store.getCustomerId(), null));
            ScmTradeDoc doc = tradeDocService.detail(TradeKind.SALES_OUT, docId);
            Map<Long, StockCostService.CostSum> costs = doc.getWmsDocId() == null ? Map.of()
                    : stockCostService.docCosts(StockBizType.OUT, List.of(doc.getWmsDocId()));
            Map<Long, BigDecimal> unitCost = new HashMap<>();
            byMaterial.forEach((mid, acc) -> {
                StockCostService.CostSum c = costs.get(mid);
                unitCost.put(mid, c == null || c.amount() == null ? BigDecimal.ZERO : c.amount().abs().divide(acc[0], 6, RoundingMode.HALF_UP));
            });
            for (Saved x : group) {
                writeShipped(ctx, x, key, store, doc, unitCost, landed, freight, channels);
            }
        }
    }

    /** 回写已发货订单：出库单、各行与组件成本、物流成本、发货信息、利润 */
    private void writeShipped(DemoContext ctx, Saved x, GroupKey key, EcomStore store, ScmTradeDoc doc, Map<Long, BigDecimal> unitCost,
                              Map<String, BigDecimal> landed, Map<String, BigDecimal> freight, Map<String, Long> channels) {
        EcomOrder o = x.order();
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal weight = BigDecimal.valueOf(0.05);
        BigDecimal shippingCost = BigDecimal.ZERO;
        BigDecimal units = BigDecimal.ZERO;
        for (EcomOrderItem it : x.items()) {
            BigDecimal c = BigDecimal.ZERO;
            for (EcomOutboundSupport.Line l : EcomOutboundSupport.Line.wholeExpanded(it)) {
                BigDecimal lc = Decimals.money(unitCost.getOrDefault(l.materialId(), BigDecimal.ZERO).multiply(l.materialQty()));
                c = c.add(lc);
                if (l.part() != null && l.part().getId() != null) {
                    outboundSupport.addPartCost(l.part().getId(), lc);
                }
                DemoCatalog.Product prod = DemoInboundContributor.productOf(ctx, l.materialId());
                weight = weight.add(BigDecimal.valueOf(prod.weight()).multiply(l.materialQty()));
                if (o.getFulfillment() == FulfillmentType.PLATFORM) {
                    BigDecimal unit = landed.computeIfAbsent(o.getStoreId() + "|" + l.materialId(),
                            k -> Decimals.nz(inboundItemMapper.selectLatestUnitLanded(o.getStoreId(), l.materialId())));
                    shippingCost = shippingCost.add(unit.multiply(l.materialQty()));
                }
            }
            itemMapper.update(Wrappers.<EcomOrderItem>lambdaUpdate().set(EcomOrderItem::getCost, c).eq(EcomOrderItem::getId, it.getId()));
            cost = cost.add(c);
            units = units.add(it.getQty());
        }
        LocalDateTime shipTime = LocalDateTime.of(x.plan().shipDate, LocalTime.of(ctx.between(9, 18), ctx.between(0, 59)));
        if (shipTime.isBefore(o.getOrderTime())) {
            shipTime = o.getOrderTime().plusHours(ctx.between(2, 6));
        }
        o.setTradeDocId(doc.getId());
        o.setTradeDocNo(doc.getDocNo());
        o.setWarehouseId(key.warehouseId());
        o.setCostAmount(Decimals.money(cost));
        o.setWeight(weight.setScale(3, RoundingMode.HALF_UP));
        o.setStatus(EcomOrderStatus.SHIPPED);
        o.setLocalStatus(EcomLocalStatus.SHIPPED);
        o.setShipTime(shipTime);
        if (o.getFulfillment() == FulfillmentType.PLATFORM) {
            o.setShippingCost(Decimals.money(shippingCost));
            if (store.getPlatform() == EcomPlatform.AMAZON) {
                o.setFulfillmentFee(fbaFee(store.getCurrency(), units, o.getWeight()));
            }
        } else {
            String channel = channelOf(store, o);
            Long channelId = channels.get(channel);
            BigDecimal w = o.getWeight().setScale(1, RoundingMode.UP);
            BigDecimal fee = freight.computeIfAbsent(channelId + "|" + o.getShipCountry() + "|" + w,
                    k -> Decimals.nz(logisticsService.estimate(channelId, o.getShipCountry(), w)));
            o.setChannelId(channelId);
            o.setShippingCost(Decimals.money(fee));
            o.setCarrier(carrierName(channel));
            o.setTrackingNo(trackingNo(ctx, channel, o.getShipCountry()));
        }
        o.setProfit(EcomOrderService.computeProfit(o));
        orderMapper.updateById(o);
    }

    private static Map<Long, BigDecimal> materialQty(EcomOrderItem it) {
        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        if (EcomOutboundSupport.isBundle(it)) {
            for (EcomOrderItemPart part : it.getParts()) {
                out.merge(part.getMaterialId(), Decimals.nz(part.getMaterialQty()), BigDecimal::add);
            }
        } else if (it.getMaterialId() != null) {
            out.put(it.getMaterialId(), Decimals.nz(it.getMaterialQty()));
        }
        return out;
    }

    /** 亚马逊 FBA 配送费估算（店铺币种）：每件 3.2 + 每公斤 1.1 美元，结算确认后以实际为准 */
    private static BigDecimal fbaFee(String currency, BigDecimal units, BigDecimal weight) {
        double usd = 3.2 * units.doubleValue() + 1.1 * weight.doubleValue();
        double v = switch (currency) {
            case "EUR" -> usd * 0.95;
            case DemoCatalog.JPY -> usd * 140;
            default -> usd;
        };
        return BigDecimal.valueOf(v).setScale(DemoCatalog.scale(currency), RoundingMode.HALF_UP);
    }

    /** 自发货渠道：虾皮走 SLS，高货值走 DHL，带电（充电类）走 4PX，其他走云途 */
    static String channelOf(EcomStore store, EcomOrder o) {
        if (store.getPlatform() == EcomPlatform.SHOPEE) {
            return "DEMO-SLS";
        }
        if (Decimals.nz(o.getTotalBase()).compareTo(BigDecimal.valueOf(700)) > 0) {
            return "DEMO-DHL";
        }
        return o.getId() % 3 == 0 ? "DEMO-4PX" : "DEMO-YUN";
    }

    static String carrierName(String channel) {
        return switch (channel) {
            case "DEMO-SLS" -> "Shopee SLS";
            case "DEMO-DHL" -> "DHL";
            case "DEMO-4PX" -> "4PX";
            default -> "YunExpress";
        };
    }

    static String trackingNo(DemoContext ctx, String channel, String country) {
        return switch (channel) {
            case "DEMO-SLS" -> country + "2" + ctx.between(10000000, 99999999) + ctx.between(1000, 9999);
            case "DEMO-DHL" -> String.valueOf(ctx.between(1000000000, 1999999999));
            case "DEMO-4PX" -> "4PX30" + DemoListingContributor.code(ctx, 12);
            default -> "YT" + ctx.between(10000000, 99999999) + ctx.between(10000000, 99999999);
        };
    }

    // ---------------------------------------------------------------- 审单规则

    /** 审单规则：东南亚走 SLS（截单 16:00）、美国自发货走云途（截单 15:00）、高货值走 DHL、大额订单人工审核 */
    private void rules(DemoContext ctx, Map<String, Long> channels) {
        Map<String, Long> stores = ctx.get(DemoKeys.STORE_BY_CODE);
        // 演示规则只作用于演示店铺，不影响之后接入的真实店铺
        String all = stores.values().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        Long localWh = ctx.get(DemoKeys.LOCAL_WAREHOUSE_ID);
        rule("演示：大额订单人工审核", 1, all, null, null, BigDecimal.valueOf(2500), null, null, true, // i18n-ignore 演示数据存库
                "订单金额超过 2500 元，人工核对地址与风控后再发货", null, false); // i18n-ignore 演示数据存库
        rule("演示：东南亚走 Shopee SLS", 10, stores.get("DEMO-SHP-MY") + "," + stores.get("DEMO-SHP-SG"), null, localWh, null, channels.get("DEMO-SLS"), // i18n-ignore 演示数据存库
                LocalTime.of(16, 0), false, null, null, true);
        rule("演示：高货值走 DHL", 20, all, null, localWh, BigDecimal.valueOf(700), channels.get("DEMO-DHL"), LocalTime.of(15, 0), false, null, // i18n-ignore 演示数据存库
                null, true);
        rule("演示：美国自发货走云途专线", 30, all, "US", localWh, null, channels.get("DEMO-YUN"), LocalTime.of(15, 0), false, null, null, // i18n-ignore 演示数据存库
                true);
        rule("演示：其他国家走 4PX", 40, all, null, localWh, null, channels.get("DEMO-4PX"), LocalTime.of(14, 0), false, null, null, // i18n-ignore 演示数据存库
                true);
    }

    private void rule(String name, int priority, String storeIds, String countries, Long warehouseId, BigDecimal amountMin, Long channelId,
                      LocalTime cutoff, boolean hold, String holdReason, String remark, boolean stop) {
        EcomOrderRule r = new EcomOrderRule();
        r.setName(name);
        r.setPriority(priority);
        r.setStatus(EnableStatus.ENABLED);
        r.setStoreIds(storeIds);
        r.setCountries(countries);
        r.setFulfillment(FulfillmentType.SELLER);
        r.setAmountMin(amountMin);
        r.setWarehouseId(warehouseId);
        r.setChannelId(channelId);
        r.setCutoffTime(cutoff);
        r.setHoldOrder(hold ? 1 : 0);
        r.setHoldReason(holdReason);
        r.setAutoAudit(0);
        r.setStopOnMatch(stop ? 1 : 0);
        r.setRemark(remark);
        ruleService.create(r);
    }
}
