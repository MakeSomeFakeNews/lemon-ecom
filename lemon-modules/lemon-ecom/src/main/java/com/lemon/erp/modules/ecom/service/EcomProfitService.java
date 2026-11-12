package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.finance.ExchangeRateProvider;
import com.lemon.erp.modules.ecom.dto.EcomProfitViews.Cell;
import com.lemon.erp.modules.ecom.dto.EcomProfitViews.ProfitRow;
import com.lemon.erp.modules.ecom.dto.EcomProfitViews.SkuAttr;
import com.lemon.erp.modules.ecom.dto.EcomProfitViews.StoreAttr;
import com.lemon.erp.modules.ecom.dto.EcomProfitViews.TypedCell;
import com.lemon.erp.modules.ecom.mapper.EcomProfitMapper;
import com.lemon.erp.modules.ecom.model.ProfitBasis;
import com.lemon.erp.modules.ecom.model.ProfitDimension;
import com.lemon.erp.modules.ecom.model.SettlementItemType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 多维利润报表（金额为本位币）。先汇总出 店铺 × 平台 SKU × 收件国家 × 月份 的统计单元，再按所选维度合计：
 * <ul>
 *     <li><b>订单口径</b>：下单时间在期间内、未取消的订单；含税销售额 = 买家实付（本位币），平台代扣税和需申报的 VAT 从收入中扣除；
 *     佣金、配送费、退货处理费、其他费用、退款取订单上的值（结算回写前为估算），订单级金额按行金额占比分摊到 SKU；
 *     商品成本取出库成本；物流成本按配送方式分为头程（平台配送）和尾程（自发货）</li>
 *     <li><b>结算口径</b>：结算明细的入账日期（没有时取结算周期结束日）在期间内；收入、退款、各项费用取结算明细的实际金额
 *     （按结算单汇率折本位币，挂订单的按订单行占比分摊到 SKU，不挂订单的订单收入 / 退款按店铺计入）；平台代收代缴税从收入中扣除；
 *     件数、商品成本、头程、尾程、需申报 VAT 取首次结算在期间内的订单</li>
 * </ul>
 * 两种口径都扣除：不挂订单费用的分摊结果（ecom_fee_allocation，按费用日期）和广告花费（ecom_ad_daily，按广告日期，店铺币种按当月汇率折算）。
 * 分摊结果和广告花费只到 店铺 × SKU × 月份，再按销售额占比分到收件国家；结算明细中的广告费不计入（避免与广告日报重复）。
 * 毛利 = 不含税销售额 − 佣金 − 配送费 − 仓储费 − 广告 − 退款 − 退货处理费 − 其他费用 − 商品成本 − 头程 − 尾程，
 * 毛利率 = 毛利 ÷ 不含税销售额，ROI = 毛利 ÷ (商品成本 + 头程)
 */
@Service
@RequiredArgsConstructor
public class EcomProfitService {

    private final EcomProfitMapper profitMapper;
    private final ExchangeRateProvider rateProvider;

    /**
     * @param keyword 按平台 SKU、商品标题、物料编码 / 名称过滤（只保留匹配的 SKU 单元）
     */
    public List<ProfitRow> profit(ProfitBasis basis, ProfitDimension dimension, LocalDate begin, LocalDate end, Long storeId, String keyword) {
        if (begin == null || end == null || end.isBefore(begin)) {
            throw new BizException("ecom.error.profit.rangeRequired");
        }
        Map<String, Cell> cells = basis == ProfitBasis.SETTLEMENT ? settlementCells(begin, end, storeId) : orderCells(begin, end, storeId);
        for (TypedCell t : profitMapper.selectAllocations(begin, end, storeId)) {
            SettlementItemType.ProfitColumn col = t.getItemType() == null ? null : t.getItemType().profitColumn();
            if (col != null) {
                distribute(cells, t, Decimals.nz(t.getAmount()).negate(), setterOf(col));
            }
        }
        Map<String, BigDecimal> rates = new HashMap<>();
        for (TypedCell t : profitMapper.selectAdSpend(begin, end, storeId)) {
            BigDecimal rate = rates.computeIfAbsent(t.getCurrency() + "|" + t.getPeriod(), k -> rateOf(t.getCurrency(), t.getPeriod()));
            distribute(cells, t, Decimals.money(Decimals.nz(t.getAmount()).multiply(rate)), (c, v) -> c.setAdSpend(add(c.getAdSpend(), v)));
        }
        Map<Long, StoreAttr> stores = profitMapper.selectStores().stream().collect(Collectors.toMap(StoreAttr::getStoreId, Function.identity()));
        Map<String, SkuAttr> skus = new HashMap<>();
        for (SkuAttr a : profitMapper.selectSkuAttrs(storeId)) {
            skus.put(a.getStoreId() + "|" + a.getSellerSku(), a);
        }
        Map<Long, SkuAttr> materials = materials(cells.values(), skus);
        String kw = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;

        Map<String, ProfitRow> rows = new LinkedHashMap<>();
        for (Cell c : cells.values()) {
            SkuAttr sku = skus.get(c.getStoreId() + "|" + c.getSellerSku());
            Long materialId = c.getMaterialId() != null ? c.getMaterialId() : sku == null ? null : sku.getMaterialId();
            SkuAttr material = materialId == null ? null : materials.get(materialId);
            if (kw != null && !matches(kw, c.getSellerSku(), sku, material)) {
                continue;
            }
            StoreAttr store = stores.get(c.getStoreId());
            String key = keyOf(dimension, c, store, sku, materialId);
            ProfitRow r = rows.computeIfAbsent(key, k -> label(dimension, k, c, store, sku, materialId, material));
            boolean orderLevel = dimension != ProfitDimension.SKU && dimension != ProfitDimension.SPU && dimension != ProfitDimension.MATERIAL;
            r.setOrders(nz(r.getOrders()) + (orderLevel ? nz(c.getOrderHeads()) : nz(c.getOrders())));
            r.setQty(add(r.getQty(), c.getQty()));
            r.setSalesGross(add(r.getSalesGross(), c.getSalesGross()));
            r.setWithheldTax(add(r.getWithheldTax(), c.getWithheld()));
            r.setVat(add(r.getVat(), c.getVat()));
            r.setCommission(add(r.getCommission(), c.getCommission()));
            r.setFbaFee(add(r.getFbaFee(), c.getFbaFee()));
            r.setStorageFee(add(r.getStorageFee(), c.getStorageFee()));
            r.setAdSpend(add(r.getAdSpend(), c.getAdSpend()));
            r.setRefund(add(r.getRefund(), c.getRefund()));
            r.setReturnFee(add(r.getReturnFee(), c.getReturnFee()));
            r.setOtherFee(add(r.getOtherFee(), c.getOtherFee()));
            r.setCost(add(r.getCost(), c.getCost()));
            r.setHeadCost(add(r.getHeadCost(), c.getHeadCost()));
            r.setLastMile(add(r.getLastMile(), c.getLastMile()));
        }
        List<ProfitRow> list = new ArrayList<>(rows.values());
        list.forEach(EcomProfitService::finish);
        if (dimension == ProfitDimension.MONTH) {
            list.sort(Comparator.comparing(ProfitRow::getMonth));
        } else {
            list.sort(Comparator.comparing(ProfitRow::getSalesNet).reversed().thenComparing(ProfitRow::getDimKey));
        }
        return list;
    }

    /** 合计（看板的利润卡片） */
    public ProfitRow total(ProfitBasis basis, LocalDate begin, LocalDate end, Long storeId) {
        ProfitRow t = new ProfitRow();
        for (ProfitRow r : profit(basis, ProfitDimension.STORE, begin, end, storeId, null)) {
            t.setOrders(nz(t.getOrders()) + nz(r.getOrders()));
            t.setSalesGross(add(t.getSalesGross(), r.getSalesGross()));
            t.setSalesNet(add(t.getSalesNet(), r.getSalesNet()));
            t.setProfit(add(t.getProfit(), r.getProfit()));
        }
        t.setSalesGross(Decimals.money(Decimals.nz(t.getSalesGross())));
        t.setSalesNet(Decimals.money(Decimals.nz(t.getSalesNet())));
        t.setProfit(Decimals.money(Decimals.nz(t.getProfit())));
        return t;
    }

    // ---------------------------------------------------------------- 单元

    private Map<String, Cell> orderCells(LocalDate begin, LocalDate end, Long storeId) {
        Map<String, Cell> cells = new LinkedHashMap<>();
        for (Cell c : profitMapper.selectOrderCells(begin.atStartOfDay(), end.plusDays(1).atStartOfDay(), storeId)) {
            c.setSellerSku(c.getSellerSku() == null ? "" : c.getSellerSku());
            cells.put(cellKey(c.getStoreId(), c.getSellerSku(), c.getCountry(), c.getPeriod()), c);
        }
        return cells;
    }

    private Map<String, Cell> settlementCells(LocalDate begin, LocalDate end, Long storeId) {
        Map<String, Cell> cells = new LinkedHashMap<>();
        for (Cell c : profitMapper.selectSettledOrderCells(begin, end, storeId)) {
            c.setSellerSku(c.getSellerSku() == null ? "" : c.getSellerSku());
            cells.put(cellKey(c.getStoreId(), c.getSellerSku(), c.getCountry(), c.getPeriod()), c);
        }
        List<TypedCell> typed = new ArrayList<>(profitMapper.selectSettlementOrderItems(begin, end, storeId));
        typed.addAll(profitMapper.selectSettlementUnlinked(begin, end, storeId));
        for (TypedCell t : typed) {
            SettlementItemType.ProfitColumn col = t.getItemType() == null ? null : t.getItemType().profitColumn();
            if (col == null) {
                continue;
            }
            BigDecimal amount = Decimals.nz(t.getAmount());
            Cell c = cell(cells, t.getStoreId(), t.getSellerSku(), t.getCountry(), t.getPeriod());
            // 收入为正；退款、费用、代收代缴税在结算中为负，报表中记为正数的减项（赔偿等正数冲减其他费用）
            if (col == SettlementItemType.ProfitColumn.SALES) {
                c.setSalesGross(add(c.getSalesGross(), amount));
            } else {
                setterOf(col).accept(c, amount.negate());
            }
        }
        return cells;
    }

    private static BiConsumer<Cell, BigDecimal> setterOf(SettlementItemType.ProfitColumn col) {
        return switch (col) {
            case SALES -> (c, v) -> c.setSalesGross(add(c.getSalesGross(), v.negate()));
            case REFUND -> (c, v) -> c.setRefund(add(c.getRefund(), v));
            case COMMISSION -> (c, v) -> c.setCommission(add(c.getCommission(), v));
            case FULFILLMENT -> (c, v) -> c.setFbaFee(add(c.getFbaFee(), v));
            case STORAGE -> (c, v) -> c.setStorageFee(add(c.getStorageFee(), v));
            case RETURN_FEE -> (c, v) -> c.setReturnFee(add(c.getReturnFee(), v));
            case TAX -> (c, v) -> c.setWithheld(add(c.getWithheld(), v));
            case OTHER -> (c, v) -> c.setOtherFee(add(c.getOtherFee(), v));
        };
    }

    /**
     * 把 店铺 × SKU × 月份 的金额分到收件国家：按同一 店铺 × SKU × 月份 单元的含税销售额占比（销售额为 0 时平均）；
     * SKU 为空（留在店铺的费用）时在店铺当月的全部单元中分；没有单元时新建一个（收件国家为空）
     */
    private static void distribute(Map<String, Cell> cells, TypedCell t, BigDecimal amount, BiConsumer<Cell, BigDecimal> setter) {
        if (amount.signum() == 0) {
            return;
        }
        String sku = t.getSellerSku() == null ? "" : t.getSellerSku();
        List<Cell> targets = cells.values().stream().filter(c -> Objects.equals(c.getStoreId(), t.getStoreId()) && Objects.equals(c.getPeriod(), t.getPeriod())
                && (sku.isEmpty() || sku.equals(c.getSellerSku()))).toList();
        if (targets.isEmpty()) {
            setter.accept(cell(cells, t.getStoreId(), sku, "", t.getPeriod()), amount);
            return;
        }
        BigDecimal total = targets.stream().map(c -> Decimals.nz(c.getSalesGross()).max(BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rest = amount;
        for (int i = 0; i < targets.size(); i++) {
            Cell c = targets.get(i);
            BigDecimal part;
            if (i == targets.size() - 1) {
                part = rest;
            } else if (total.signum() > 0) {
                part = Decimals.money(amount.multiply(Decimals.nz(c.getSalesGross()).max(BigDecimal.ZERO)).divide(total, 10, RoundingMode.HALF_UP));
            } else {
                part = Decimals.money(amount.divide(BigDecimal.valueOf(targets.size()), 10, RoundingMode.HALF_UP));
            }
            rest = rest.subtract(part);
            setter.accept(c, part);
        }
    }

    private static Cell cell(Map<String, Cell> cells, Long storeId, String sku, String country, Integer period) {
        String s = sku == null ? "" : sku;
        String ct = country == null ? "" : country;
        return cells.computeIfAbsent(cellKey(storeId, s, ct, period), k -> {
            Cell c = new Cell();
            c.setStoreId(storeId);
            c.setSellerSku(s);
            c.setCountry(ct);
            c.setPeriod(period);
            return c;
        });
    }

    private static String cellKey(Long storeId, String sku, String country, Integer period) {
        return storeId + "|" + (sku == null ? "" : sku) + "|" + (country == null ? "" : country) + "|" + period;
    }

    private BigDecimal rateOf(String currency, Integer period) {
        LocalDate day = LocalDate.of(period / 100, period % 100, 1);
        BigDecimal rate = rateProvider.rate(currency, day);
        if (rate == null) {
            throw new BizException("ecom.error.exchangeRateMissing", currency, period / 100 + "-" + String.format("%02d", period % 100));
        }
        return rate;
    }

    private Map<Long, SkuAttr> materials(java.util.Collection<Cell> cells, Map<String, SkuAttr> skus) {
        Set<Long> ids = new HashSet<>();
        for (Cell c : cells) {
            if (c.getMaterialId() != null) {
                ids.add(c.getMaterialId());
            }
        }
        skus.values().stream().map(SkuAttr::getMaterialId).filter(Objects::nonNull).forEach(ids::add);
        Map<Long, SkuAttr> map = new HashMap<>();
        List<Long> list = new ArrayList<>(ids);
        for (int i = 0; i < list.size(); i += 500) {
            profitMapper.selectMaterials(list.subList(i, Math.min(list.size(), i + 500))).forEach(m -> map.put(m.getMaterialId(), m));
        }
        return map;
    }

    private static boolean matches(String kw, String sku, SkuAttr attr, SkuAttr material) {
        return contains(sku, kw) || attr != null && (contains(attr.getTitle(), kw) || contains(attr.getParentItemId(), kw))
                || material != null && (contains(material.getMaterialCode(), kw) || contains(material.getMaterialName(), kw));
    }

    private static boolean contains(String s, String kw) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(kw);
    }

    // ---------------------------------------------------------------- 维度

    private static String keyOf(ProfitDimension d, Cell c, StoreAttr store, SkuAttr sku, Long materialId) {
        return switch (d) {
            case STORE -> String.valueOf(c.getStoreId());
            case PLATFORM -> store == null || store.getPlatform() == null ? "" : store.getPlatform().name();
            case MARKETPLACE -> store == null || store.getMarketplace() == null ? "" : store.getMarketplace();
            case SKU -> c.getStoreId() + "|" + c.getSellerSku();
            case SPU -> c.getStoreId() + "|" + spuOf(c, sku);
            case MATERIAL -> materialId == null ? "" : String.valueOf(materialId);
            case OWNER -> store == null || store.getOwnerId() == null ? "" : String.valueOf(store.getOwnerId());
            case COUNTRY -> c.getCountry() == null ? "" : c.getCountry();
            case MONTH -> String.valueOf(c.getPeriod());
        };
    }

    private static String spuOf(Cell c, SkuAttr sku) {
        return sku != null && StringUtils.hasText(sku.getParentItemId()) ? sku.getParentItemId() : c.getSellerSku();
    }

    private static ProfitRow label(ProfitDimension d, String key, Cell c, StoreAttr store, SkuAttr sku, Long materialId, SkuAttr material) {
        ProfitRow r = new ProfitRow();
        r.setDimKey(key);
        switch (d) {
            case STORE, SKU, SPU -> {
                r.setStoreId(c.getStoreId());
                if (store != null) {
                    r.setStoreName(store.getStoreName());
                    r.setPlatform(store.getPlatform());
                    r.setMarketplace(store.getMarketplace());
                    r.setOwnerName(store.getOwnerName());
                }
                if (d == ProfitDimension.SKU) {
                    r.setSellerSku(c.getSellerSku());
                    r.setTitle(sku == null ? null : sku.getTitle());
                    r.setSpu(spuOf(c, sku));
                    r.setMaterialId(materialId);
                    if (material != null) {
                        r.setMaterialCode(material.getMaterialCode());
                        r.setMaterialName(material.getMaterialName());
                    }
                } else if (d == ProfitDimension.SPU) {
                    r.setSpu(spuOf(c, sku));
                    r.setTitle(sku == null ? null : sku.getTitle());
                }
            }
            case PLATFORM -> r.setPlatform(store == null ? null : store.getPlatform());
            case MARKETPLACE -> r.setMarketplace(store == null ? null : store.getMarketplace());
            case MATERIAL -> {
                r.setMaterialId(materialId);
                if (material != null) {
                    r.setMaterialCode(material.getMaterialCode());
                    r.setMaterialName(material.getMaterialName());
                }
            }
            case OWNER -> {
                r.setOwnerId(store == null ? null : store.getOwnerId());
                r.setOwnerName(store == null ? null : store.getOwnerName());
            }
            case COUNTRY -> r.setCountry(c.getCountry());
            case MONTH -> r.setMonth(c.getPeriod() / 100 + "-" + String.format("%02d", c.getPeriod() % 100));
        }
        return r;
    }

    /** 金额保留两位，计算不含税销售额、毛利、毛利率、ROI */
    private static void finish(ProfitRow r) {
        r.setOrders(nz(r.getOrders()));
        r.setQty(Decimals.qty(Decimals.nz(r.getQty())));
        r.setSalesGross(m(r.getSalesGross()));
        r.setWithheldTax(m(r.getWithheldTax()));
        r.setVat(m(r.getVat()));
        r.setCommission(m(r.getCommission()));
        r.setFbaFee(m(r.getFbaFee()));
        r.setStorageFee(m(r.getStorageFee()));
        r.setAdSpend(m(r.getAdSpend()));
        r.setRefund(m(r.getRefund()));
        r.setReturnFee(m(r.getReturnFee()));
        r.setOtherFee(m(r.getOtherFee()));
        r.setCost(m(r.getCost()));
        r.setHeadCost(m(r.getHeadCost()));
        r.setLastMile(m(r.getLastMile()));
        r.setSalesNet(r.getSalesGross().subtract(r.getWithheldTax()).subtract(r.getVat()));
        r.setProfit(r.getSalesNet().subtract(r.getCommission()).subtract(r.getFbaFee()).subtract(r.getStorageFee()).subtract(r.getAdSpend())
                .subtract(r.getRefund()).subtract(r.getReturnFee()).subtract(r.getOtherFee()).subtract(r.getCost()).subtract(r.getHeadCost())
                .subtract(r.getLastMile()));
        r.setMargin(Decimals.percent(r.getProfit(), r.getSalesNet(), 2));
        r.setRoi(Decimals.percent(r.getProfit(), r.getCost().add(r.getHeadCost()), 2));
    }

    private static BigDecimal m(BigDecimal v) {
        return Decimals.money(Decimals.nz(v));
    }

    private static BigDecimal add(BigDecimal a, BigDecimal b) {
        return Decimals.nz(a).add(Decimals.nz(b));
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }
}
