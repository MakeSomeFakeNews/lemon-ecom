package com.lemon.erp.modules.ecom.analytics.service;

import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.ecom.analytics.dto.AnalyticsRows;
import com.lemon.erp.modules.ecom.analytics.dto.GenerateResult;
import com.lemon.erp.modules.ecom.analytics.entity.EcomStockSnapshot;
import com.lemon.erp.modules.ecom.analytics.mapper.EcomAnalyticsMapper;
import com.lemon.erp.modules.ecom.analytics.mapper.EcomStockSnapshotMapper;
import com.lemon.erp.modules.ecom.dto.BundleComponent;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.service.EcomBundleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 每日库存快照：记录每个在售 SKU（启用店铺、已映射物料或有组成）当天平台仓、本地仓的可售数（空闲库存 = 库存 − 冻结，
 * 单品 ÷ 多件装、组合取各组件 ⌊空闲库存 ÷ 用量⌋ 的最小值，向下取整为整件），按配送方式取可售数，用于断货天数与历史库存曲线。
 * 分批（每批 {@link #BATCH} 个 SKU）读取与写入，每批一个事务，一批失败不影响其他批；同一天重复执行覆盖当天的快照；保留 {@link #KEEP_DAYS} 天
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EcomStockSnapshotService {

    static final int BATCH = 500;
    static final int KEEP_DAYS = 730;

    private final EcomAnalyticsMapper analyticsMapper;
    private final EcomStockSnapshotMapper snapshotMapper;
    private final EcomBundleService bundleService;
    private final TransactionTemplate transactionTemplate;

    /** 记录某天的快照（取当前库存） */
    public GenerateResult record(LocalDate date) {
        int total = 0;
        int failed = 0;
        String firstError = null;
        long after = 0;
        while (true) {
            List<AnalyticsRows.ListingRef> refs = analyticsMapper.selectActiveListings(null, null, after, BATCH);
            if (refs.isEmpty()) {
                break;
            }
            after = refs.get(refs.size() - 1).getListingId();
            try {
                total += recordBatch(date, refs);
            } catch (RuntimeException e) {
                log.warn("Stock snapshot failed for listings after {}", refs.get(0).getListingId(), e);
                failed += refs.size();
                firstError = firstError == null ? e.getMessage() : firstError;
            }
            if (refs.size() < BATCH) {
                break;
            }
        }
        snapshotMapper.deleteBefore(date.minusDays(KEEP_DAYS));
        return new GenerateResult(total, failed, firstError);
    }

    private int recordBatch(LocalDate date, List<AnalyticsRows.ListingRef> refs) {
        Map<Long, List<BundleComponent>> components = bundleService.componentsOf(refs.stream().map(AnalyticsRows.ListingRef::getListingId).toList());
        List<AnalyticsRows.ListingRef> mapped = refs.stream().filter(r -> components.containsKey(r.getListingId())).toList();
        if (mapped.isEmpty()) {
            return 0;
        }
        List<Long> materialIds = mapped.stream().flatMap(r -> components.get(r.getListingId()).stream()).map(BundleComponent::materialId).distinct().toList();
        List<Long> warehouseIds = mapped.stream().flatMap(r -> Stream.of(r.getWarehouseId(), r.getPlatformWarehouseId()))
                .filter(Objects::nonNull).distinct().toList();
        Map<String, BigDecimal> stock = new HashMap<>();
        if (!warehouseIds.isEmpty()) {
            for (Map<String, Object> row : analyticsMapper.selectFreeStock(materialIds, warehouseIds)) {
                stock.put(row.get("materialId") + "@" + row.get("warehouseId"), new BigDecimal(String.valueOf(row.get("qty"))));
            }
        }
        List<EcomStockSnapshot> rows = new ArrayList<>();
        for (AnalyticsRows.ListingRef r : mapped) {
            List<BundleComponent> parts = components.get(r.getListingId());
            EcomStockSnapshot s = new EcomStockSnapshot();
            s.setSnapshotDate(date);
            s.setListingId(r.getListingId());
            s.setStoreId(r.getStoreId());
            s.setFulfillment(r.getFulfillment());
            s.setPlatformQty(r.getPlatformWarehouseId() == null ? BigDecimal.ZERO : sets(parts, stock, r.getPlatformWarehouseId()));
            s.setLocalQty(r.getWarehouseId() == null ? BigDecimal.ZERO : sets(parts, stock, r.getWarehouseId()));
            s.setSellableQty(r.getFulfillment() == FulfillmentType.PLATFORM ? s.getPlatformQty() : s.getLocalQty());
            rows.add(s);
        }
        List<Long> ids = rows.stream().map(EcomStockSnapshot::getListingId).toList();
        transactionTemplate.executeWithoutResult(st -> {
            snapshotMapper.deleteDay(date, ids);
            snapshotMapper.insertBatch(rows);
        });
        return rows.size();
    }

    /** 能凑齐的整件数：各组件 ⌊空闲库存 ÷ 用量⌋ 的最小值，不为负 */
    static BigDecimal sets(List<BundleComponent> parts, Map<String, BigDecimal> stock, Long warehouseId) {
        BigDecimal min = null;
        for (BundleComponent c : parts) {
            if (c.qty() == null || c.qty().signum() <= 0) {
                continue;
            }
            BigDecimal n = Decimals.nz(stock.get(c.materialId() + "@" + warehouseId)).divide(c.qty(), 0, RoundingMode.DOWN);
            min = min == null || n.compareTo(min) < 0 ? n : min;
        }
        return min == null || min.signum() < 0 ? BigDecimal.ZERO : min;
    }
}
