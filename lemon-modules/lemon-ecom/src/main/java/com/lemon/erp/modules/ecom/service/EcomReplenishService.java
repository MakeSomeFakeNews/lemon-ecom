package com.lemon.erp.modules.ecom.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.dto.BundleComponent;
import com.lemon.erp.modules.ecom.dto.PurchaseSuggestRow;
import com.lemon.erp.modules.ecom.dto.ReplenishRow;
import com.lemon.erp.modules.ecom.dto.ReplenishTransit;
import com.lemon.erp.modules.ecom.dto.ReplenishWeights;
import com.lemon.erp.modules.ecom.analytics.dto.DailyForecast;
import com.lemon.erp.modules.ecom.analytics.service.EcomForecastService;
import com.lemon.erp.modules.ecom.entity.EcomReplenishSetting;
import com.lemon.erp.modules.ecom.mapper.EcomReplenishMapper;
import com.lemon.erp.modules.ecom.model.ReplenishLevel;
import com.lemon.erp.modules.scm.entity.ScmSupplierPrice;
import com.lemon.erp.modules.scm.service.ScmQueryService;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 补货建议，分两段：
 * <ol>
 *   <li><b>平台仓补货</b>（店铺 × 在线商品，平台 SKU 件数）：日均 = 最近 7 / 14 / 30 / 60 天日均按权重加权（默认 7 天 × 0.5 + 30 天 × 0.5）× 系数，
 *   设置了手工日均时取手工日均；建议补货 = 日均 × (补货周期 + 安全天数) − 平台仓库存 − 头程在途；
 *   可售天数 = (平台仓库存 + 头程在途) ÷ 日均，在补货周期内为紧急，在补货周期 + 安全天数内为预警</li>
 *   <li><b>本地仓采购</b>（物料）：建议采购 = 各店铺平台仓建议补货（折算为物料）+ 本地仓安全库存（日均 × 采购周期 + 物料安全库存）
 *   − 本地仓库存 − 采购在途（已审核、部分入库的采购订单未入库数量），不足默认供应商最小起订量时按起订量；
 *   采购周期取物料级补货设置 → 默认供应商的提前期 → 店铺补货周期</li>
 * </ol>
 * 补货周期、安全天数、系数、手工日均、不补货按「在线商品级设置 → 物料级设置 → 店铺设置」取第一个有值的。
 * 组合商品（{@link EcomBundleService#componentsOf}）：平台仓 / 本地仓库存、头程在途按组件折算为套数，
 * 本地仓采购把组合 SKU 的平台仓建议、日均按组件用量折算到各组件物料上
 */
@Service
@RequiredArgsConstructor
public class EcomReplenishService {

    /** 按预测时取的天数（与预测服务的上限一致） */
    static final int FORECAST_DAYS = 90;
    static final String SOURCE_WEIGHTED = "WEIGHTED";
    static final String SOURCE_FORECAST = "FORECAST";
    static final String SOURCE_MANUAL = "MANUAL";


    private static final int DEFAULT_LEAD_DAYS = 30;
    private static final int DEFAULT_SAFETY_DAYS = 15;
    /** 采购周期来源 */
    static final String FROM_SETTING = "SETTING";
    static final String FROM_SUPPLIER = "SUPPLIER";
    static final String FROM_STORE = "STORE";

    private final EcomReplenishMapper replenishMapper;
    private final EcomReplenishSettingService settingService;
    private final WmsMaterialService materialService;
    private final ScmQueryService scmQueryService;
    private final EcomBundleService bundleService;
    private final EcomForecastService forecastService;

    /** 平台仓补货建议（默认加权） */
    public List<ReplenishRow> rows(Long storeId, String keyword, ReplenishLevel level) {
        return rows(storeId, keyword, level, ReplenishWeights.DEFAULT);
    }

    /**
     * 平台仓补货建议
     *
     * @param level 只看某个紧急程度，为空全部
     */
    public List<ReplenishRow> rows(Long storeId, String keyword, ReplenishLevel level, ReplenishWeights weights) {
        List<ReplenishRow> rows = computeRows(storeId, keyword, weights, LocalDate.now());
        return rows.stream().filter(r -> level == null || r.getLevel() == level)
                .sorted(Comparator.comparing((ReplenishRow r) -> r.getLevel().ordinal()).reversed()
                        .thenComparing(r -> r.getCoverDays() == null ? BigDecimal.valueOf(Long.MAX_VALUE) : r.getCoverDays()))
                .toList();
    }

    private List<ReplenishRow> computeRows(Long storeId, String keyword, ReplenishWeights weights, LocalDate today) {
        LocalDateTime start = today.atStartOfDay();
        List<ReplenishRow> raw = replenishMapper.selectRows(storeId, keyword, start.minusDays(7), start.minusDays(14),
                start.minusDays(30), start.minusDays(60));
        // 组成物料：单品为 物料 × 多件装，组合为各组件 × 用量；没有映射（组合没有组成）的不参与补货
        Map<Long, List<BundleComponent>> components = bundleService.componentsOf(raw.stream().map(ReplenishRow::getListingId).toList());
        List<ReplenishRow> rows = raw.stream().filter(r -> components.containsKey(r.getListingId())).toList();
        rows.forEach(r -> r.setComponents(components.get(r.getListingId())));
        fillBundles(rows.stream().filter(EcomReplenishService::isBundle).toList());
        if (weights.forecast() && !rows.isEmpty()) {
            Map<Long, DailyForecast> forecasts = forecastService.dailyForecast(rows.stream().map(ReplenishRow::getListingId).toList(), FORECAST_DAYS);
            for (ReplenishRow r : rows) {
                DailyForecast f = forecasts.get(r.getListingId());
                if (f != null && !f.daily().isEmpty()) {
                    r.setForecastDaily(f.daily());
                    r.setForecastMethod(f.method() == null ? null : f.method().name());
                }
            }
        }
        Map<Long, EcomReplenishSetting> listingSettings = settingService.byListings(rows.stream().map(ReplenishRow::getListingId).toList());
        Map<Long, EcomReplenishSetting> materialSettings = settingService.byMaterials(
                rows.stream().map(ReplenishRow::getMaterialId).filter(Objects::nonNull).distinct().toList());
        for (ReplenishRow r : rows) {
            compute(r, today, weights, listingSettings.get(r.getListingId()), r.getMaterialId() == null ? null : materialSettings.get(r.getMaterialId()));
        }
        return rows;
    }

    /** 组合商品（组成不是「映射物料 × 多件装」）：组件不止一个，或组件不是映射的物料 */
    private static boolean isBundle(ReplenishRow r) {
        List<BundleComponent> c = r.getComponents();
        return c.size() != 1 || !c.get(0).materialId().equals(r.getMaterialId());
    }

    /**
     * 组合商品的平台仓库存、本地仓库存、头程在途按组件折算：各组件 数量 ÷ 用量 的最小值（凑得齐的套数）；
     * 在途的预计到达取各组件中最晚的（套件到齐的日期）
     */
    private void fillBundles(List<ReplenishRow> bundles) {
        if (bundles.isEmpty()) {
            return;
        }
        List<Long> materialIds = bundles.stream().flatMap(r -> r.getComponents().stream()).map(BundleComponent::materialId).distinct().toList();
        List<Long> warehouseIds = bundles.stream().flatMap(r -> Stream.of(r.getWarehouseId(), r.getPlatformWarehouseId()))
                .filter(Objects::nonNull).distinct().toList();
        Map<String, BigDecimal> stock = new HashMap<>();
        if (!warehouseIds.isEmpty()) {
            for (Map<String, Object> row : replenishMapper.selectStock(materialIds, warehouseIds)) {
                stock.put(row.get("materialId") + "@" + row.get("warehouseId"), dec(row.get("qty")));
            }
        }
        Map<String, BigDecimal> transit = new HashMap<>();
        Map<String, LocalDate> eta = new HashMap<>();
        for (Map<String, Object> row : replenishMapper.selectStoreInboundTransit(materialIds)) {
            String key = row.get("materialId") + "@" + row.get("storeId");
            transit.put(key, dec(row.get("qty")));
            LocalDate d = toDate(row.get("eta"));
            if (d != null) {
                eta.put(key, d);
            }
        }
        for (ReplenishRow r : bundles) {
            r.setBundle(true);
            r.setPlatformQty(sets(r, c -> stock.get(c.materialId() + "@" + r.getPlatformWarehouseId())));
            r.setLocalQty(sets(r, c -> stock.get(c.materialId() + "@" + r.getWarehouseId())));
            r.setInTransitQty(sets(r, c -> transit.get(c.materialId() + "@" + r.getStoreId())));
            r.setInTransitEta(r.getInTransitQty().signum() > 0 ? r.getComponents().stream().map(c -> eta.get(c.materialId() + "@" + r.getStoreId()))
                    .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null) : null);
        }
    }

    /** 能凑齐的套数：各组件 数量 ÷ 用量 的最小值（4 位小数，不为负） */
    private static BigDecimal sets(ReplenishRow r, Function<BundleComponent, BigDecimal> qtyOf) {
        BigDecimal min = null;
        for (BundleComponent c : r.getComponents()) {
            if (c.qty() == null || c.qty().signum() <= 0) {
                continue;
            }
            BigDecimal n = Decimals.nz(qtyOf.apply(c)).divide(c.qty(), 4, RoundingMode.DOWN);
            min = min == null || n.compareTo(min) < 0 ? n : min;
        }
        return min == null || min.signum() < 0 ? BigDecimal.ZERO : min;
    }

    /**
     * 计算一行平台仓补货建议
     *
     * @param listing  在线商品级设置，可为空
     * @param material 物料级设置，可为空
     */
    static void compute(ReplenishRow r, LocalDate today, ReplenishWeights weights, EcomReplenishSetting listing, EcomReplenishSetting material) {
        int lead = firstNonNull(pick(listing, EcomReplenishSetting::getLeadDays), pick(material, EcomReplenishSetting::getLeadDays),
                r.getStoreLeadDays(), DEFAULT_LEAD_DAYS);
        int safety = firstNonNull(pick(listing, EcomReplenishSetting::getSafetyDays), pick(material, EcomReplenishSetting::getSafetyDays),
                r.getStoreSafetyDays(), DEFAULT_SAFETY_DAYS);
        BigDecimal factor = firstNonNull(pick(listing, EcomReplenishSetting::getFactor), pick(material, EcomReplenishSetting::getFactor), BigDecimal.ONE);
        r.setLeadDays(lead);
        r.setSafetyDays(safety);
        r.setFactor(factor);
        r.setHasSetting(listing != null);
        r.setDisabled(isDisabled(listing) || isDisabled(material));
        BigDecimal weighted = weights.daily(r.getSales7(), r.getSales14(), r.getSales30(), r.getSales60());
        r.setWeightedDaily(weighted);
        BigDecimal override = pick(listing, EcomReplenishSetting::getDailySales);
        r.setDailyOverridden(override != null);
        BigDecimal base = weighted;
        r.setDailySource(SOURCE_WEIGHTED);
        if (r.getForecastDaily() != null) {
            // 按预测：取未来「补货周期 + 安全天数」天（最多 90 天）的预测平均，已含趋势与促销系数
            List<BigDecimal> f = r.getForecastDaily();
            int n = Math.max(1, Math.min(f.size(), lead + safety));
            base = f.subList(0, n).stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(n), 4, RoundingMode.HALF_UP);
            r.setDailySource(SOURCE_FORECAST);
        }
        if (override != null) {
            r.setDailySource(SOURCE_MANUAL);
        }
        BigDecimal daily = override != null ? override : base.multiply(factor).setScale(4, RoundingMode.HALF_UP);
        r.setDailySales(daily);
        BigDecimal platform = Decimals.nz(r.getPlatformQty());
        BigDecimal transit = Decimals.nz(r.getInTransitQty());
        BigDecimal available = platform.add(transit);
        BigDecimal target = daily.multiply(BigDecimal.valueOf((long) lead + safety));
        r.setTargetQty(target.setScale(0, RoundingMode.CEILING));
        if (daily.signum() <= 0) {
            r.setCoverDays(null);
            r.setSuggestQty(BigDecimal.ZERO);
            r.setStockoutDate(null);
            r.setLevel(ReplenishLevel.OK);
            return;
        }
        BigDecimal cover = available.divide(daily, 1, RoundingMode.DOWN);
        r.setCoverDays(cover);
        BigDecimal suggest = target.subtract(available).setScale(0, RoundingMode.CEILING);
        r.setSuggestQty(suggest.signum() > 0 && !r.isDisabled() ? suggest : BigDecimal.ZERO);
        r.setStockoutDate(today.plusDays(cover.longValue()));
        if (r.isDisabled()) {
            r.setLevel(ReplenishLevel.OK);
        } else if (cover.compareTo(BigDecimal.valueOf(lead)) <= 0) {
            r.setLevel(ReplenishLevel.URGENT);
        } else if (cover.compareTo(BigDecimal.valueOf((long) lead + safety)) <= 0) {
            r.setLevel(ReplenishLevel.WARN);
        } else {
            r.setLevel(ReplenishLevel.OK);
        }
    }

    public long countUrgent() {
        return rows(null, null, ReplenishLevel.URGENT).size();
    }

    // ---------------------------------------------------------------- 本地仓采购

    /**
     * 本地仓采购建议（物料）：汇总全部启用店铺的在售在线商品（设置了不补货的在线商品不计入）
     *
     * @param keyword 物料编码 / 名称、平台 SKU
     * @param level   只看某个紧急程度，为空全部
     */
    public List<PurchaseSuggestRow> purchaseRows(String keyword, ReplenishLevel level, ReplenishWeights weights) {
        LocalDate today = LocalDate.now();
        List<PurchaseSuggestRow> rows = computePurchase(computeRows(null, null, weights, today), today);
        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase(Locale.ROOT);
        return rows.stream()
                .filter(r -> level == null || r.getLevel() == level)
                .filter(r -> kw == null || contains(r.getMaterialCode(), kw) || contains(r.getMaterialName(), kw)
                        || r.getDemands().stream().anyMatch(d -> contains(d.sellerSku(), kw)))
                .sorted(Comparator.comparing((PurchaseSuggestRow r) -> r.getLevel().ordinal()).reversed()
                        .thenComparing(r -> r.getLocalCoverDays() == null ? BigDecimal.valueOf(Long.MAX_VALUE) : r.getLocalCoverDays()))
                .toList();
    }

    /** 指定物料的采购建议（生成采购计划用），不在建议中的物料不返回 */
    public Map<Long, PurchaseSuggestRow> purchaseRowsOf(Collection<Long> materialIds, ReplenishWeights weights) {
        LocalDate today = LocalDate.now();
        return computePurchase(computeRows(null, null, weights, today), today).stream()
                .filter(r -> materialIds.contains(r.getMaterialId()))
                .collect(Collectors.toMap(PurchaseSuggestRow::getMaterialId, Function.identity()));
    }

    private List<PurchaseSuggestRow> computePurchase(List<ReplenishRow> listingRows, LocalDate today) {
        Map<Long, PurchaseSuggestRow> byMaterial = new LinkedHashMap<>();
        Map<Long, Integer> storeLead = new HashMap<>();
        for (ReplenishRow r : listingRows) {
            if (r.isDisabled()) {
                continue;
            }
            for (BundleComponent c : r.getComponents()) {
                BigDecimal per = c.qty();
                PurchaseSuggestRow p = byMaterial.computeIfAbsent(c.materialId(), id -> {
                    PurchaseSuggestRow n = new PurchaseSuggestRow();
                    n.setMaterialId(id);
                    n.setDailySales(BigDecimal.ZERO);
                    n.setPlatformSuggestQty(BigDecimal.ZERO);
                    n.setPlatformQty(BigDecimal.ZERO);
                    return n;
                });
                BigDecimal qty = Decimals.nz(r.getSuggestQty()).multiply(per);
                BigDecimal daily = Decimals.nz(r.getDailySales()).multiply(per);
                p.setPlatformSuggestQty(p.getPlatformSuggestQty().add(qty));
                p.setPlatformQty(p.getPlatformQty().add(Decimals.nz(r.getPlatformQty()).multiply(per)));
                p.setDailySales(p.getDailySales().add(daily));
                p.getDemands().add(new PurchaseSuggestRow.Demand(r.getStoreId(), r.getStoreName(), r.getListingId(), r.getSellerSku(),
                        Decimals.qty(qty), daily.setScale(4, RoundingMode.HALF_UP), r.getWarehouseId()));
                storeLead.merge(c.materialId(), r.getLeadDays(), Math::max);
            }
        }
        if (byMaterial.isEmpty()) {
            return List.of();
        }
        List<Long> ids = List.copyOf(byMaterial.keySet());
        Map<Long, WmsMaterial> materials = materialService.mapByIds(ids);
        Map<Long, EcomReplenishSetting> settings = settingService.byMaterials(ids);
        Map<Long, BigDecimal> local = qtyMap(replenishMapper.selectLocalStock(ids));
        List<Map<String, Object>> purchaseTransit = replenishMapper.selectPurchaseTransit(ids);
        List<Map<String, Object>> inboundTransit = replenishMapper.selectInboundTransit(ids);
        Map<Long, BigDecimal> purchaseQty = qtyMap(purchaseTransit);
        Map<Long, LocalDate> purchaseEta = etaMap(purchaseTransit);
        Map<Long, BigDecimal> inboundQty = qtyMap(inboundTransit);
        Map<Long, LocalDate> inboundEta = etaMap(inboundTransit);
        Map<Long, ScmSupplierPrice> suppliers = scmQueryService.defaultSuppliers(ids);
        for (PurchaseSuggestRow p : byMaterial.values()) {
            Long id = p.getMaterialId();
            WmsMaterial m = materials.get(id);
            if (m != null) {
                p.setMaterialCode(m.getCode());
                p.setMaterialName(m.getName());
                p.setSpec(m.getSpec());
                p.setUnit(m.getUnit());
                p.setSafetyStock(m.getSafetyStock());
            }
            p.setLocalQty(local.getOrDefault(id, BigDecimal.ZERO));
            p.setPurchaseTransitQty(purchaseQty.getOrDefault(id, BigDecimal.ZERO));
            p.setPurchaseEta(purchaseEta.get(id));
            p.setInboundTransitQty(inboundQty.getOrDefault(id, BigDecimal.ZERO));
            p.setInboundEta(inboundEta.get(id));
            ScmSupplierPrice price = suppliers.get(id);
            if (price != null) {
                p.setSupplierId(price.getSupplierId());
                p.setSupplierName(price.getSupplierName());
                p.setSupplierLeadDays(price.getLeadDays());
                p.setMinQty(price.getMinQty());
                p.setTaxRate(price.getTaxRate());
            }
            computePurchase(p, settings.get(id), storeLead.getOrDefault(id, DEFAULT_LEAD_DAYS), today);
            if (price != null) {
                p.setPrice(scmQueryService.effectivePrice(price, p.getSuggestQty().signum() > 0 ? p.getSuggestQty() : null));
            }
        }
        return List.copyOf(byMaterial.values());
    }

    /**
     * 计算一个物料的采购建议（数量、日均、库存、在途、默认供应商已填好）
     *
     * @param setting   物料级设置，可为空
     * @param storeLead 需求店铺中最长的补货周期（没有设置采购周期、供应商提前期时使用）
     */
    static void computePurchase(PurchaseSuggestRow p, EcomReplenishSetting setting, int storeLead, LocalDate today) {
        BigDecimal override = pick(setting, EcomReplenishSetting::getDailySales);
        p.setHasSetting(setting != null);
        p.setDisabled(isDisabled(setting));
        p.setDailyOverridden(override != null);
        BigDecimal daily = (override != null ? override : Decimals.nz(p.getDailySales())).setScale(4, RoundingMode.HALF_UP);
        p.setDailySales(daily);
        p.setPlatformSuggestQty(Decimals.qty(Decimals.nz(p.getPlatformSuggestQty())));
        p.setPlatformQty(Decimals.qty(Decimals.nz(p.getPlatformQty())));
        Integer days = pick(setting, EcomReplenishSetting::getPurchaseDays);
        if (days != null) {
            p.setPurchaseDaysFrom(FROM_SETTING);
        } else if (p.getSupplierLeadDays() != null && p.getSupplierLeadDays() > 0) {
            days = p.getSupplierLeadDays();
            p.setPurchaseDaysFrom(FROM_SUPPLIER);
        } else {
            days = storeLead;
            p.setPurchaseDaysFrom(FROM_STORE);
        }
        p.setPurchaseDays(days);
        p.setArrivalDate(today.plusDays(days));
        BigDecimal localSafety = daily.multiply(BigDecimal.valueOf(days)).add(Decimals.nz(p.getSafetyStock()));
        p.setLocalSafetyQty(localSafety.setScale(0, RoundingMode.CEILING));
        BigDecimal local = Decimals.nz(p.getLocalQty());
        BigDecimal transit = Decimals.nz(p.getPurchaseTransitQty());
        BigDecimal suggest = p.getPlatformSuggestQty().add(localSafety).subtract(local).subtract(transit).setScale(0, RoundingMode.CEILING);
        if (suggest.signum() > 0 && p.getMinQty() != null && p.getMinQty().compareTo(suggest) > 0) {
            suggest = p.getMinQty().setScale(0, RoundingMode.CEILING);
        }
        suggest = suggest.signum() > 0 && !p.isDisabled() ? suggest : BigDecimal.ZERO;
        p.setSuggestQty(suggest);
        BigDecimal free = local.add(transit).subtract(p.getPlatformSuggestQty());
        p.setLocalCoverDays(daily.signum() > 0 ? free.max(BigDecimal.ZERO).divide(daily, 1, RoundingMode.DOWN) : null);
        if (suggest.signum() == 0) {
            p.setLevel(ReplenishLevel.OK);
        } else if (p.getLocalCoverDays() != null && p.getLocalCoverDays().compareTo(BigDecimal.valueOf(days)) <= 0) {
            p.setLevel(ReplenishLevel.URGENT);
        } else {
            p.setLevel(ReplenishLevel.WARN);
        }
    }

    // ---------------------------------------------------------------- 在途构成

    /**
     * 物料的在途构成：采购在途的采购订单、头程在途的货件
     *
     * @param storeId 头程货件只看某个店铺，为空全部
     */
    public ReplenishTransit transit(Long materialId, Long storeId) {
        return new ReplenishTransit(replenishMapper.selectPurchaseTransitDetail(materialId),
                replenishMapper.selectInboundTransitDetail(materialId, storeId));
    }

    // ---------------------------------------------------------------- 内部

    private static boolean isDisabled(EcomReplenishSetting s) {
        return s != null && s.getDisabled() != null && s.getDisabled() == 1;
    }

    private static <T> T pick(EcomReplenishSetting s, Function<EcomReplenishSetting, T> getter) {
        return s == null ? null : getter.apply(s);
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        return Stream.of(values).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static boolean contains(String text, String kw) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(kw);
    }

    private static BigDecimal dec(Object v) {
        return v == null ? BigDecimal.ZERO : new BigDecimal(v.toString());
    }

    private static Map<Long, BigDecimal> qtyMap(List<Map<String, Object>> rows) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object qty = row.get("qty");
            map.put(((Number) row.get("materialId")).longValue(), qty == null ? BigDecimal.ZERO : Decimals.qty(new BigDecimal(qty.toString())));
        }
        return map;
    }

    private static Map<Long, LocalDate> etaMap(List<Map<String, Object>> rows) {
        Map<Long, LocalDate> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            LocalDate eta = toDate(row.get("eta"));
            if (eta != null) {
                map.put(((Number) row.get("materialId")).longValue(), eta);
            }
        }
        return map;
    }

    private static LocalDate toDate(Object v) {
        if (v instanceof LocalDate d) {
            return d;
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (v instanceof LocalDateTime t) {
            return t.toLocalDate();
        }
        if (v instanceof java.util.Date d) {
            return new java.sql.Date(d.getTime()).toLocalDate();
        }
        return v == null ? null : LocalDate.parse(v.toString().substring(0, 10));
    }
}
