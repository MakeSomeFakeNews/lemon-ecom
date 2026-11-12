package com.lemon.erp.modules.ecom.analytics.dto;

import com.lemon.erp.modules.ecom.analytics.model.AbcClass;
import com.lemon.erp.modules.ecom.analytics.model.AbcMetric;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品分析的结果（金额为本位币，比率为百分数）
 */
public final class ProductViews {

    private ProductViews() {
    }

    // ---------------------------------------------------------------- ABC

    /** ABC 分析的一行（店铺 × SKU）：value 为依据的指标（销售额或毛利），share / cumShare 为占比与累计占比（含本行） */
    @Data
    public static class AbcRow {
        private Long storeId;
        private String storeName;
        private Long listingId;
        private String sellerSku;
        private String title;
        private String materialCode;
        private String materialName;
        private Long orders;
        private BigDecimal qty;
        private BigDecimal sales;
        private BigDecimal profit;
        private BigDecimal value;
        private BigDecimal sharePercent;
        private BigDecimal cumSharePercent;
        private AbcClass abcClass;
    }

    /** 各类的 SKU 数、指标合计与占比 */
    public record AbcSummary(AbcClass abcClass, int count, BigDecimal value, BigDecimal sharePercent, BigDecimal countPercent) {
    }

    public record AbcResult(AbcMetric metric, BigDecimal aPercent, BigDecimal bPercent, BigDecimal total, List<AbcSummary> summary,
                            List<AbcRow> rows) {
    }

    // ---------------------------------------------------------------- 滞销与库龄

    /**
     * 滞销物料：有库存但最近 days 天没有销量；库龄 = 今天 − 入库日期（批次物料取批次首次入库，其他按入库流水先进先出推算），
     * oldestAgeDays 最老一笔的库龄、avgAgeDays 按数量加权，age* 为各库龄段的数量
     */
    @Data
    public static class SlowRow {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String materialSpec;
        private String unit;
        private Integer skuCount;
        private BigDecimal stockQty;
        private BigDecimal unitCost;
        private BigDecimal stockAmount;
        private LocalDate lastSaleDate;
        /** 距最近一次销售的天数，从未销售为空 */
        private Long idleDays;
        private Integer oldestAgeDays;
        private BigDecimal avgAgeDays;
        private BigDecimal age0to30;
        private BigDecimal age31to90;
        private BigDecimal age91to180;
        private BigDecimal age181to365;
        private BigDecimal ageOver365;
    }

    /**
     * @param activeSkus        在售 SKU 数
     * @param movingSkus        其中最近 days 天有销量的
     * @param movingRatePercent 动销率 = movingSkus ÷ activeSkus × 100
     */
    public record SlowResult(int days, long activeSkus, long movingSkus, BigDecimal movingRatePercent, int slowCount, BigDecimal slowAmount,
                             List<SlowRow> rows) {
    }

    // ---------------------------------------------------------------- 售罄与周转

    /**
     * 物料的售罄率与周转：售罄率 = 期间销量 ÷ (期初库存 + 期间入库) × 100；周转天数 = 平均库存 ((期初 + 期末) ÷ 2) ÷ 日均销量；
     * 可售天数 = 当前库存 ÷ 日均销量。库存为全部仓库合计，期初 / 期末由当前库存倒推库存流水
     */
    @Data
    public static class SellThroughRow {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String materialSpec;
        private String unit;
        private Integer skuCount;
        private BigDecimal salesQty;
        private BigDecimal openingQty;
        private BigDecimal inboundQty;
        private BigDecimal closingQty;
        private BigDecimal stockQty;
        private BigDecimal stockAmount;
        private BigDecimal dailySales;
        private BigDecimal sellThroughPercent;
        private BigDecimal turnoverDays;
        private BigDecimal coverDays;
    }

    /** SKU 的断货天数（每日库存快照中可售 ≤ 0 的天数） */
    @Data
    public static class StockoutRow {
        private Long listingId;
        private Long storeId;
        private String storeName;
        private String sellerSku;
        private String title;
        private FulfillmentType fulfillment;
        private String materialCode;
        private String materialName;
        private Integer snapshotDays;
        private Integer stockoutDays;
        private BigDecimal stockoutPercent;
        private LocalDate lastStockoutDate;
    }

    /** 新品表现：首单日起 30 天（含首单日）的件数、订单数、销售额，observedDays 为截至今天已过的天数（最多 30） */
    @Data
    public static class NewProductRow {
        private Long storeId;
        private String storeName;
        private Long listingId;
        private String sellerSku;
        private String title;
        private String materialCode;
        private String materialName;
        private LocalDate firstOrderDate;
        private int observedDays;
        private long orders30;
        private BigDecimal qty30;
        private BigDecimal sales30;
        private BigDecimal dailyQty;
        private Integer stockoutDays30;
    }

    // ---------------------------------------------------------------- 退货率

    /** SKU 的退货率 = 退回件数 ÷ 销量 × 100（期间内下单的订单），reasons 为各原因的退回件数 */
    @Data
    public static class ReturnRow {
        private Long storeId;
        private String storeName;
        private Long listingId;
        private String sellerSku;
        private String title;
        private String materialCode;
        private String materialName;
        private BigDecimal soldQty;
        private BigDecimal returnQty;
        private BigDecimal returnRatePercent;
        private BigDecimal refund;
        private String topReason;
        private Map<String, BigDecimal> reasons = new LinkedHashMap<>();
    }

    public record ReasonShare(String reason, BigDecimal qty, BigDecimal sharePercent) {
    }

    public record ReturnResult(BigDecimal soldQty, BigDecimal returnQty, BigDecimal returnRatePercent, List<ReasonShare> reasons,
                               List<ReturnRow> rows) {
        public ReturnResult {
            reasons = reasons == null ? new ArrayList<>() : reasons;
        }
    }
}
