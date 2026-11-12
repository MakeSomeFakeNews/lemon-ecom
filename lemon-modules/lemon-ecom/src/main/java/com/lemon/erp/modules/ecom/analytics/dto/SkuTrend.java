package com.lemon.erp.modules.ecom.analytics.dto;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 单个 SKU 的销量走势：每天的件数、销售额（本位币），叠加当天的库存快照（没有快照的日期库存为空），可售 ≤ 0 为断货
 *
 * @param stockoutDays 区间内断货的天数（有快照的日期中）
 */
public record SkuTrend(Long listingId, Long storeId, String storeName, EcomPlatform platform, String sellerSku, String title,
                       FulfillmentType fulfillment, String materialCode, String materialName,
                       BigDecimal totalQty, BigDecimal totalAmount, int snapshotDays, int stockoutDays, List<Point> days) {

    public record Point(LocalDate date, BigDecimal qty, BigDecimal amount, BigDecimal platformQty, BigDecimal localQty,
                        BigDecimal sellableQty, Boolean stockout) {
    }
}
