package com.lemon.erp.modules.ecom.analytics.service;

import com.lemon.erp.modules.ecom.analytics.dto.AnalyticsRows;
import com.lemon.erp.modules.ecom.analytics.entity.EcomStockSnapshot;
import com.lemon.erp.modules.ecom.analytics.mapper.EcomAnalyticsMapper;
import com.lemon.erp.modules.ecom.analytics.mapper.EcomStockSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 断货日识别：每日库存快照（ecom_stock_snapshot，每天 23:50 记录）中按配送方式的可售数不大于阈值（默认 0）的那天记为断货日
 * （平台仓配送看平台仓可售，自发货看本地仓可售）。没有快照的日期不算断货（快照任务上线前的历史、未映射的 SKU）。
 * 供销量预测（断货日销量补偿）与补货建议（加权日均排除断货日）使用，算法见 {@link StockoutCalc}
 */
@Service
@RequiredArgsConstructor
public class EcomStockoutService {

    private static final int CHUNK = 500;

    private final EcomStockSnapshotMapper snapshotMapper;
    private final EcomAnalyticsMapper analyticsMapper;

    /**
     * 每个 SKU 在 [start, start + days) 内每天是否断货，没有断货日的 SKU 不在结果中
     *
     * @return 在线商品 ID → 与日期对齐的标记（下标 0 为 start）
     */
    public Map<Long, boolean[]> flags(Collection<Long> listingIds, LocalDate start, int days, BigDecimal threshold) {
        Map<Long, boolean[]> out = new HashMap<>();
        for (EcomStockSnapshot s : select(listingIds, start, start.plusDays(days - 1L), threshold)) {
            int idx = (int) ChronoUnit.DAYS.between(start, s.getSnapshotDate());
            if (idx >= 0 && idx < days) {
                out.computeIfAbsent(s.getListingId(), k -> new boolean[days])[idx] = true;
            }
        }
        return out;
    }

    /**
     * 最近 maxDays 天（到昨天）的断货日与断货日当天的销量，没有断货日的 SKU 不在结果中
     *
     * @param today 今天（不含）
     */
    public Map<Long, Window> windows(Collection<Long> listingIds, LocalDate today, int maxDays, BigDecimal threshold) {
        Map<Long, Window> out = new HashMap<>();
        for (EcomStockSnapshot s : select(listingIds, today.minusDays(maxDays), today.minusDays(1), threshold)) {
            out.computeIfAbsent(s.getListingId(), k -> new Window(new TreeMap<>())).sales().put(s.getSnapshotDate(), BigDecimal.ZERO);
        }
        if (out.isEmpty()) {
            return out;
        }
        List<Long> ids = new ArrayList<>(out.keySet());
        for (int i = 0; i < ids.size(); i += CHUNK) {
            for (AnalyticsRows.ListingDay d : analyticsMapper.selectListingDaily(ids.subList(i, Math.min(ids.size(), i + CHUNK)),
                    today.minusDays(maxDays).atStartOfDay(), today.atStartOfDay())) {
                Window w = out.get(d.getListingId());
                if (w != null && w.sales().containsKey(d.getSaleDay())) {
                    w.sales().put(d.getSaleDay(), d.getQty() == null ? BigDecimal.ZERO : d.getQty());
                }
            }
        }
        return out;
    }

    /**
     * 一个 SKU 最近一段时间的断货日
     *
     * @param sales 断货日 → 当天的销量（按日期排序）
     */
    public record Window(TreeMap<LocalDate, BigDecimal> sales) {

        /** 最近 days 天（到昨天）内的断货天数 */
        public int days(LocalDate today, int days) {
            return sales.subMap(today.minusDays(days), true, today, false).size();
        }

        /** 最近 days 天（到昨天）内断货日的销量合计 */
        public BigDecimal salesIn(LocalDate today, int days) {
            return sales.subMap(today.minusDays(days), true, today, false).values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /** 一个 SKU 在 [from, to) 内每天的销量（没有销量的日期不在结果中） */
    public Map<LocalDate, BigDecimal> dailySales(Long listingId, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> out = new TreeMap<>();
        for (AnalyticsRows.ListingDay d : analyticsMapper.selectListingDaily(List.of(listingId), from.atStartOfDay(), to.atStartOfDay())) {
            out.merge(d.getSaleDay(), d.getQty() == null ? BigDecimal.ZERO : d.getQty(), BigDecimal::add);
        }
        return out;
    }

    /**
     * 把这些日期的快照记为断货（平台仓 / 本地仓 / 可售都为 0；没有快照的日期补一条），演示数据与测试用
     */
    @org.springframework.transaction.annotation.Transactional
    public void markStockout(Long listingId, Long storeId, com.lemon.erp.modules.ecom.model.FulfillmentType fulfillment, Collection<LocalDate> dates) {
        List<EcomStockSnapshot> rows = new ArrayList<>();
        for (LocalDate d : dates) {
            snapshotMapper.deleteDay(d, List.of(listingId));
            EcomStockSnapshot s = new EcomStockSnapshot();
            s.setSnapshotDate(d);
            s.setListingId(listingId);
            s.setStoreId(storeId);
            s.setFulfillment(fulfillment);
            s.setPlatformQty(BigDecimal.ZERO);
            s.setLocalQty(BigDecimal.ZERO);
            s.setSellableQty(BigDecimal.ZERO);
            rows.add(s);
        }
        if (!rows.isEmpty()) {
            snapshotMapper.insertBatch(rows);
        }
    }

    private List<EcomStockSnapshot> select(Collection<Long> listingIds, LocalDate from, LocalDate to, BigDecimal threshold) {
        List<EcomStockSnapshot> out = new ArrayList<>();
        if (listingIds == null || listingIds.isEmpty() || to.isBefore(from)) {
            return out;
        }
        List<Long> ids = new ArrayList<>(listingIds);
        BigDecimal t = threshold == null ? BigDecimal.ZERO : threshold;
        for (int i = 0; i < ids.size(); i += CHUNK) {
            out.addAll(snapshotMapper.selectStockout(ids.subList(i, Math.min(ids.size(), i + CHUNK)), from, to, t));
        }
        return out;
    }
}
