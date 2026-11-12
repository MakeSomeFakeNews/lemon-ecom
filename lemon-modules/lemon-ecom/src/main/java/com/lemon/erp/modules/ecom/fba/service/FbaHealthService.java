package com.lemon.erp.modules.ecom.fba.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.entity.EcomStore;
import com.lemon.erp.modules.ecom.fba.connector.TsvReport;
import com.lemon.erp.modules.ecom.fba.dto.FbaViews;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaInventory;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaSizeTier;
import com.lemon.erp.modules.ecom.fba.entity.EcomFbaStorageRate;
import com.lemon.erp.modules.ecom.fba.mapper.EcomFbaInventoryMapper;
import com.lemon.erp.modules.ecom.fba.model.AmzReportType;
import com.lemon.erp.modules.ecom.fba.model.FbaAgeSource;
import com.lemon.erp.modules.ecom.fba.model.FbaHealthAdvice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * FBA 库存健康：
 * <ul>
 *   <li>数据：库存规划报告（GET_FBA_INVENTORY_PLANNING_DATA，库龄分段、最近 30 天发货、预估仓储费）和 FBA 库存报告
 *       （GET_FBA_MYI_UNSUPPRESSED_INVENTORY_DATA，可售、在途、预留、不可售、调查中）写入 {@code ecom_fba_inventory}（店铺 × SKU 只保留最近一次）；
 *       没有接口授权或报告时，「按头程推算」用平台库存快照（没有时用系统平台仓库存）作为在库数量，按已签收头程货件先进先出推算库龄分段</li>
 *   <li>计算（查询时）：日均销量 = 最近 30 天订单件数 ÷ 30（没有订单时取报告的 30 天发货数）；可售天数 = 可售 ÷ 日均；
 *       冗余 = 可售 − 日均 × 冗余阈值天数（默认 90）；月度仓储费 = 在库件数 × 单件体积 × 当月费率（按尺寸类别、淡旺季）；
 *       超龄附加费 = Σ 各库龄分段件数 × 单件体积 × 与分段重叠的库龄段费率（按重叠天数占比，见 {@link FbaHealthCalc#agedFee}）；处理建议见 {@link FbaHealthCalc#advice}</li>
 * </ul>
 * 单件体积和尺寸类别取「FBA 费用核查」中维护的商品尺寸（按配送费尺寸分段匹配），没有尺寸时不估算仓储费
 */
@Service
@RequiredArgsConstructor
public class FbaHealthService implements AmzReportHandler {

    public static final int DEFAULT_EXCESS_DAYS = 90;
    /** 报告数据在多少天内有效：按头程推算时不覆盖这段时间内报告带来的库龄 */
    static final int REPORT_FRESH_DAYS = 7;

    private final EcomFbaInventoryMapper inventoryMapper;
    private final FbaRateService rateService;
    private final FbaSupport support;
    private final FbaClaimService claimService;
    private final TransactionTemplate transactionTemplate;

    /** 查询条件 */
    public record Query(Long storeId, String keyword, FbaHealthAdvice advice, Boolean excessOnly, Boolean agedOnly, Integer excessDays) {

        int threshold() {
            return excessDays == null || excessDays <= 0 ? DEFAULT_EXCESS_DAYS : excessDays;
        }
    }

    public PageResult<FbaViews.HealthRow> page(long current, long size, Query q) {
        List<FbaViews.HealthRow> rows = rows(q).stream()
                .filter(r -> q.advice() == null || r.getAdvice() == q.advice())
                .filter(r -> q.excessOnly() == null || !q.excessOnly() || Decimals.positive(r.getExcessQty()))
                .filter(r -> q.agedOnly() == null || !q.agedOnly() || Decimals.positive(r.getAgedQty()))
                .toList();
        int from = (int) Math.min(rows.size(), Math.max(0, (current - 1) * size));
        int to = (int) Math.min(rows.size(), from + size);
        return new PageResult<>(new ArrayList<>(rows.subList(from, to)), rows.size());
    }

    /** 全部行（按店铺、SKU 排序）并计算 */
    public List<FbaViews.HealthRow> rows(Query q) {
        List<FbaViews.HealthRow> rows = inventoryMapper.selectHealthRows(q.storeId(), q.keyword(), LocalDateTime.now().minusDays(FbaHealthCalc.VELOCITY_DAYS));
        if (rows.isEmpty()) {
            return rows;
        }
        Set<String> markets = rows.stream().map(FbaViews.HealthRow::getMarketplace).collect(Collectors.toCollection(TreeSet::new));
        Map<String, List<EcomFbaSizeTier>> tiers = rateService.tiersByMarketplace();
        Map<String, List<EcomFbaStorageRate>> rates = rateService.storageRatesByMarketplace(markets);
        LocalDate today = LocalDate.now();
        int threshold = q.threshold();
        for (FbaViews.HealthRow r : rows) {
            FbaFeeCalculator.Dims dims = new FbaFeeCalculator.Dims(r.getLengthCm(), r.getWidthCm(), r.getHeightCm(), r.getWeightKg());
            FbaViews.FeeQuote quote = FbaFeeCalculator.quote(dims, tiers.getOrDefault(r.getMarketplace(), List.of()));
            FbaHealthCalc.compute(r, threshold, quote, rates.getOrDefault(r.getMarketplace(), List.of()), today);
        }
        return rows;
    }

    /** 汇总：件数、库龄分段、冗余、仓储费（折算本位币）、建议分布、可索赔金额 */
    public FbaViews.HealthSummary summary(Long storeId, Integer excessDays) {
        List<FbaViews.HealthRow> rows = rows(new Query(storeId, null, null, null, null, excessDays));
        FbaViews.HealthSummary s = new FbaViews.HealthSummary();
        Map<FbaHealthAdvice, Integer> advice = new EnumMap<>(FbaHealthAdvice.class);
        for (FbaHealthAdvice a : FbaHealthAdvice.values()) {
            advice.put(a, 0);
        }
        Set<String> missingRates = new TreeSet<>();
        Map<String, BigDecimal> rateCache = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (FbaViews.HealthRow r : rows) {
            s.setSkuCount(s.getSkuCount() + 1);
            s.setAvailableQty(s.getAvailableQty().add(Decimals.nz(r.getAvailableQty())));
            s.setInboundQty(s.getInboundQty().add(Decimals.nz(r.getInboundQty())));
            s.setReservedQty(s.getReservedQty().add(Decimals.nz(r.getReservedQty())));
            s.setUnfulfillableQty(s.getUnfulfillableQty().add(Decimals.nz(r.getUnfulfillableQty())));
            s.setResearchingQty(s.getResearchingQty().add(Decimals.nz(r.getResearchingQty())));
            s.setExcessQty(s.getExcessQty().add(Decimals.nz(r.getExcessQty())));
            if (Decimals.positive(r.getExcessQty())) {
                s.setExcessSkus(s.getExcessSkus() + 1);
            }
            s.setAgedQty(s.getAgedQty().add(Decimals.nz(r.getAgedQty())));
            BigDecimal[] b = FbaHealthCalc.buckets(r);
            for (int i = 0; i < b.length; i++) {
                s.getBuckets()[i] = s.getBuckets()[i].add(b[i]);
            }
            advice.merge(r.getAdvice(), 1, Integer::sum);
            if (r.getUnitVolume() == null) {
                s.setMissingDims(s.getMissingDims() + 1);
            }
            if (r.getMonthlyFee() != null || r.getAgedFee() != null) {
                BigDecimal rate = rateCache.computeIfAbsent(r.getCurrency(), c -> support.rateOrNull(c, today));
                if (rate == null) {
                    missingRates.add(r.getCurrency());
                } else {
                    s.setMonthlyFeeBase(s.getMonthlyFeeBase().add(Decimals.nz(r.getMonthlyFee()).multiply(rate)));
                    s.setAgedFeeBase(s.getAgedFeeBase().add(Decimals.nz(r.getAgedFee()).multiply(rate)));
                }
            }
        }
        s.setMonthlyFeeBase(Decimals.money(s.getMonthlyFeeBase()));
        s.setAgedFeeBase(Decimals.money(s.getAgedFeeBase()));
        s.setAdviceCounts(advice);
        FbaViews.ClaimSummary claims = claimService.summary(storeId);
        s.setClaimableBase(claims.claimableBase());
        missingRates.addAll(claims.missingRates());
        s.setMissingRates(new ArrayList<>(missingRates));
        return s;
    }

    // ---------------------------------------------------------------- 报告写入

    @Override
    public Set<AmzReportType> types() {
        return Set.of(AmzReportType.FBA_INVENTORY_PLANNING, AmzReportType.FBA_MYI_INVENTORY);
    }

    @Override
    public int apply(EcomStore store, AmzReportType type, TsvReport report, LocalDate snapshotDate) {
        Integer n = transactionTemplate.execute(st -> {
            Map<String, EcomFbaInventory> existing = existing(store.getId());
            int count = 0;
            for (TsvReport.Row row : report.rows()) {
                String sku = row.text("sku", "seller-sku", "msku");
                if (sku == null) {
                    continue;
                }
                EcomFbaInventory inv = existing.get(sku);
                boolean isNew = inv == null;
                if (isNew) {
                    inv = blank(store, sku, snapshotDate);
                    existing.put(sku, inv);
                }
                if (type == AmzReportType.FBA_INVENTORY_PLANNING) {
                    applyPlanning(inv, row, snapshotDate);
                } else {
                    applyInventory(inv, row, snapshotDate);
                }
                if (isNew) {
                    inventoryMapper.insert(inv);
                } else {
                    inventoryMapper.updateById(inv);
                }
                count++;
            }
            return count;
        });
        return n == null ? 0 : n;
    }

    /** 库存规划报告：库龄分段（兼容 0–30 / 31–60 / 61–90 与 366–455 / 456 以上的细分列）、可售、30 天发货、预估仓储费 */
    static void applyPlanning(EcomFbaInventory inv, TsvReport.Row row, LocalDate snapshotDate) {
        LocalDate date = row.date("snapshot-date");
        inv.setSnapshotDate(date == null ? snapshotDate : date);
        inv.setFnsku(first(row.text("fnsku"), inv.getFnsku()));
        inv.setAsin(first(row.text("asin"), inv.getAsin()));
        inv.setTitle(abbreviate(first(row.text("product-name"), inv.getTitle())));
        BigDecimal available = row.decimal("available", "afn-fulfillable-quantity");
        if (available != null) {
            inv.setAvailableQty(available);
        }
        BigDecimal age0 = row.decimal("inv-age-0-to-90-days");
        if (age0 == null) {
            age0 = row.qty("inv-age-0-to-30-days").add(row.qty("inv-age-31-to-60-days")).add(row.qty("inv-age-61-to-90-days"));
        }
        inv.setAge0To90(age0);
        inv.setAge91To180(row.qty("inv-age-91-to-180-days"));
        inv.setAge181To270(row.qty("inv-age-181-to-270-days"));
        inv.setAge271To365(row.qty("inv-age-271-to-365-days"));
        BigDecimal plus = row.decimal("inv-age-365-plus-days");
        if (plus == null) {
            plus = row.qty("inv-age-366-to-455-days").add(row.qty("inv-age-456-plus-days"));
        }
        inv.setAge365Plus(plus);
        inv.setAgeSource(FbaAgeSource.REPORT);
        inv.setUnitsShipped30(row.decimal("units-shipped-t30", "units-shipped-last-30-days"));
        inv.setReportStorageFee(row.decimal("estimated-storage-cost-next-month"));
        inv.setCurrency(first(row.text("currency"), inv.getCurrency()));
        BigDecimal inbound = row.decimal("inbound-quantity");
        if (inbound != null) {
            inv.setInboundQty(inbound);
        }
        BigDecimal reserved = row.decimal("total-reserved-quantity", "reserved-quantity");
        if (reserved != null) {
            inv.setReservedQty(reserved);
        }
        BigDecimal unfulfillable = row.decimal("unfulfillable-quantity");
        if (unfulfillable != null) {
            inv.setUnfulfillableQty(unfulfillable);
        }
    }

    /** FBA 库存报告：可售、在途（处理中 + 已发货 + 接收中）、预留、不可售、调查中 */
    static void applyInventory(EcomFbaInventory inv, TsvReport.Row row, LocalDate snapshotDate) {
        inv.setSnapshotDate(snapshotDate);
        inv.setFnsku(first(row.text("fnsku"), inv.getFnsku()));
        inv.setAsin(first(row.text("asin"), inv.getAsin()));
        inv.setTitle(abbreviate(first(row.text("product-name"), inv.getTitle())));
        inv.setAvailableQty(row.qty("afn-fulfillable-quantity"));
        inv.setInboundQty(row.qty("afn-inbound-working-quantity").add(row.qty("afn-inbound-shipped-quantity"))
                .add(row.qty("afn-inbound-receiving-quantity")));
        inv.setReservedQty(row.qty("afn-reserved-quantity"));
        inv.setUnfulfillableQty(row.qty("afn-unsellable-quantity"));
        inv.setResearchingQty(row.qty("afn-researching-quantity"));
    }

    // ---------------------------------------------------------------- 按头程推算

    /**
     * 按头程签收先进先出推算库龄：在库数量取平台库存快照（可售 + 预留），没有快照时取系统平台仓库存；
     * 最近 {@value #REPORT_FRESH_DAYS} 天内由库存规划报告写入的 SKU 不覆盖库龄。返回写入的条数
     */
    public int rebuildFromInbound(Long storeId) {
        EcomStore store = support.amazonStore(storeId);
        Integer n = transactionTemplate.execute(st -> {
            Map<String, EcomFbaInventory> existing = existing(store.getId());
            Map<String, List<FbaHealthCalc.Lot>> lots = inventoryMapper.selectReceiptLots(store.getId()).stream()
                    .collect(Collectors.groupingBy(FbaViews.ReceiptLot::getSellerSku, Collectors.collectingAndThen(Collectors.toList(), FbaHealthCalc::lots)));
            LocalDate today = LocalDate.now();
            int count = 0;
            for (FbaViews.StockBase b : inventoryMapper.selectStockBase(store.getId())) {
                EcomFbaInventory inv = existing.get(b.getSellerSku());
                boolean snapshot = b.getFulfillableQty() != null;
                BigDecimal available = snapshot ? b.getFulfillableQty() : Decimals.nz(b.getSystemQty()).setScale(0, java.math.RoundingMode.FLOOR);
                if (inv == null && available.signum() <= 0) {
                    continue;
                }
                boolean isNew = inv == null;
                if (isNew) {
                    inv = blank(store, b.getSellerSku(), today);
                }
                boolean freshReport = !isNew && inv.getAgeSource() == FbaAgeSource.REPORT && inv.getSnapshotDate() != null
                        && !inv.getSnapshotDate().isBefore(today.minusDays(REPORT_FRESH_DAYS));
                if (!freshReport) {
                    inv.setFnsku(first(inv.getFnsku(), b.getFnsku()));
                    inv.setAsin(first(inv.getAsin(), b.getAsin()));
                    inv.setTitle(abbreviate(first(inv.getTitle(), b.getTitle())));
                    inv.setSnapshotDate(today);
                    inv.setAvailableQty(available);
                    if (snapshot) {
                        inv.setInboundQty(Decimals.nz(b.getInboundQty()));
                        inv.setReservedQty(Decimals.nz(b.getReservedQty()));
                        inv.setUnfulfillableQty(Decimals.nz(b.getUnfulfillableQty()));
                    }
                    BigDecimal onHand = available.add(Decimals.nz(inv.getReservedQty()));
                    BigDecimal[] buckets = FbaHealthCalc.fifoBuckets(onHand, lots.getOrDefault(b.getSellerSku(), List.of()), today);
                    inv.setAge0To90(buckets[0]);
                    inv.setAge91To180(buckets[1]);
                    inv.setAge181To270(buckets[2]);
                    inv.setAge271To365(buckets[3]);
                    inv.setAge365Plus(buckets[4]);
                    inv.setAgeSource(FbaAgeSource.FIFO);
                    if (isNew) {
                        inventoryMapper.insert(inv);
                    } else {
                        inventoryMapper.updateById(inv);
                    }
                    count++;
                }
            }
            return count;
        });
        return n == null ? 0 : n;
    }

    /** 直接写入一条快照（演示数据、测试使用），店铺 + SKU 已有时覆盖 */
    public void saveSnapshot(EcomFbaInventory inv) {
        EcomFbaInventory old = inventoryMapper.selectOne(Wrappers.<EcomFbaInventory>lambdaQuery()
                .eq(EcomFbaInventory::getStoreId, inv.getStoreId()).eq(EcomFbaInventory::getSellerSku, inv.getSellerSku()));
        if (old == null) {
            inventoryMapper.insert(inv);
        } else {
            inv.setId(old.getId());
            inventoryMapper.updateById(inv);
        }
    }

    private Map<String, EcomFbaInventory> existing(Long storeId) {
        Map<String, EcomFbaInventory> map = new HashMap<>();
        inventoryMapper.selectList(Wrappers.<EcomFbaInventory>lambdaQuery().eq(EcomFbaInventory::getStoreId, storeId))
                .forEach(i -> map.put(i.getSellerSku(), i));
        return map;
    }

    private static EcomFbaInventory blank(EcomStore store, String sku, LocalDate date) {
        EcomFbaInventory inv = new EcomFbaInventory();
        inv.setStoreId(store.getId());
        inv.setSellerSku(sku);
        inv.setSnapshotDate(date);
        inv.setCurrency(store.getCurrency());
        inv.setAvailableQty(BigDecimal.ZERO);
        inv.setInboundQty(BigDecimal.ZERO);
        inv.setReservedQty(BigDecimal.ZERO);
        inv.setUnfulfillableQty(BigDecimal.ZERO);
        inv.setResearchingQty(BigDecimal.ZERO);
        inv.setAge0To90(BigDecimal.ZERO);
        inv.setAge91To180(BigDecimal.ZERO);
        inv.setAge181To270(BigDecimal.ZERO);
        inv.setAge271To365(BigDecimal.ZERO);
        inv.setAge365Plus(BigDecimal.ZERO);
        inv.setAgeSource(FbaAgeSource.REPORT);
        return inv;
    }

    private static String first(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private static String abbreviate(String s) {
        return s == null || s.length() <= 255 ? s : s.substring(0, 255);
    }

}
