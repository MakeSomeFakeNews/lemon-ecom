package com.lemon.erp.modules.ecom.fba.dto;

import com.lemon.erp.modules.ecom.fba.model.FbaAgeSource;
import com.lemon.erp.modules.ecom.fba.model.FbaHealthAdvice;
import com.lemon.erp.modules.ecom.fba.model.FbaSizeCategory;
import com.lemon.erp.modules.ecom.model.InboundStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * FBA 运营的查询结果
 */
public final class FbaViews {

    private FbaViews() {
    }

    /**
     * 库存健康的一行：快照数量、库龄分段（查询）+ 日均销量、可售天数、冗余、预估仓储费、处理建议（计算）。
     * 数量为平台 SKU 件数，费用为店铺币种
     */
    @Data
    public static class HealthRow {
        private Long id;
        private Long storeId;
        private String storeName;
        private String marketplace;
        private String currency;
        private String sellerSku;
        private String fnsku;
        private String asin;
        private String title;
        private LocalDate snapshotDate;
        private BigDecimal availableQty;
        private BigDecimal inboundQty;
        private BigDecimal reservedQty;
        private BigDecimal unfulfillableQty;
        private BigDecimal researchingQty;
        private BigDecimal age0To90;
        private BigDecimal age91To180;
        private BigDecimal age181To270;
        private BigDecimal age271To365;
        private BigDecimal age365Plus;
        private FbaAgeSource ageSource;
        private BigDecimal unitsShipped30;
        private BigDecimal reportStorageFee;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal price;
        /** 最近 30 天订单件数（不含取消） */
        private BigDecimal sold30;
        private BigDecimal lengthCm;
        private BigDecimal widthCm;
        private BigDecimal heightCm;
        private BigDecimal weightKg;

        /** 日均销量（订单优先，没有订单时取报告的 30 天发货数） */
        private BigDecimal dailySales;
        /** 可售天数 = 可售 ÷ 日均，日均为 0 时为空（无穷） */
        private BigDecimal daysOfSupply;
        /** 冗余数量 = 可售 − 日均 × 冗余阈值天数 */
        private BigDecimal excessQty;
        /** 超过 180 天的库存 */
        private BigDecimal agedQty;
        /** 单件体积（立方英尺） */
        private BigDecimal unitVolume;
        private FbaSizeCategory sizeCategory;
        private String tierName;
        /** 预估本月月度仓储费 */
        private BigDecimal monthlyFee;
        /** 预估本月超龄库存附加费（长期仓储费） */
        private BigDecimal agedFee;
        private FbaHealthAdvice advice;
    }

    /** 库存健康汇总（金额为本位币） */
    @Data
    public static class HealthSummary {
        private int skuCount;
        private BigDecimal availableQty = BigDecimal.ZERO;
        private BigDecimal inboundQty = BigDecimal.ZERO;
        private BigDecimal reservedQty = BigDecimal.ZERO;
        private BigDecimal unfulfillableQty = BigDecimal.ZERO;
        private BigDecimal researchingQty = BigDecimal.ZERO;
        private BigDecimal excessQty = BigDecimal.ZERO;
        private int excessSkus;
        private BigDecimal agedQty = BigDecimal.ZERO;
        /** 0–90、91–180、181–270、271–365、365 以上的件数 */
        private BigDecimal[] buckets = {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        private BigDecimal monthlyFeeBase = BigDecimal.ZERO;
        private BigDecimal agedFeeBase = BigDecimal.ZERO;
        private Map<FbaHealthAdvice, Integer> adviceCounts;
        /** 缺少尺寸（无法估算仓储费）的 SKU 数 */
        private int missingDims;
        /** 可索赔金额（待提交 + 已提交，本位币） */
        private BigDecimal claimableBase = BigDecimal.ZERO;
        /** 没有维护汇率、未折算进合计的币种 */
        private List<String> missingRates;
    }

    /** 配送费试算结果（重量单位随分段单位制：lb / kg） */
    public record FeeQuote(String tierCode, String tierName, FbaSizeCategory sizeCategory, BigDecimal shippingWeight, String weightUnit,
                           BigDecimal fee, String currency) {
    }

    /** 费用核查的一行（店铺币种）：结算实收配送费与按尺寸重量计算的应收配送费对比 */
    @Data
    public static class AuditRow {
        private Long storeId;
        private String storeName;
        private String marketplace;
        private String currency;
        private String sellerSku;
        private String materialCode;
        private String materialName;
        private Integer orderCount;
        private BigDecimal qty;
        private BigDecimal actualFee;
        private BigDecimal actualUnit;
        private BigDecimal lengthCm;
        private BigDecimal widthCm;
        private BigDecimal heightCm;
        private BigDecimal weightKg;
        private String tierName;
        private BigDecimal shippingWeight;
        private String weightUnit;
        private BigDecimal expectedUnit;
        private BigDecimal expectedFee;
        /** 亚马逊预估费用报告给出的每件配送费（参考） */
        private BigDecimal platformFee;
        private BigDecimal diffUnit;
        /** 多收金额（为正时） */
        private BigDecimal overcharge;
        /** 问题：MISSING_DIMS 缺少尺寸、NO_RATE 没有匹配的费率、OVER 多收、OK 正常、UNDER 少收 */
        private String flag;
        /** 已生成的多收配送费索赔单号 */
        private String claimNo;
    }

    /** 费用核查结果 */
    public record AuditResult(List<AuditRow> rows, int skuCount, int overSkus, int missingDims, BigDecimal overchargeBase,
                              List<String> missingRates) {
    }

    /** 尺寸维护列表的一行：平台仓配送的在线商品 + 已维护的尺寸 + 试算结果 */
    @Data
    public static class DimRow {
        private Long id;
        private Long storeId;
        private String storeName;
        private String marketplace;
        private String currency;
        private String sellerSku;
        private String title;
        private String materialCode;
        private String materialName;
        private BigDecimal lengthCm;
        private BigDecimal widthCm;
        private BigDecimal heightCm;
        private BigDecimal weightKg;
        private String source;
        private String platformTier;
        private BigDecimal platformFee;
        private String tierName;
        private BigDecimal expectedFee;
    }

    /** 货件对账的一行：头程货件明细的发货件数与亚马逊实收件数 */
    @Data
    public static class ReceiptRow {
        private Long inboundItemId;
        private Long inboundId;
        private String docNo;
        private String shipmentId;
        private Long storeId;
        private String storeName;
        private String currency;
        private InboundStatus status;
        private LocalDate shipDate;
        private LocalDate receiveDate;
        private String sellerSku;
        private String materialCode;
        private String materialName;
        private BigDecimal packQty;
        /** 发货件数（物料数量 ÷ 多件装） */
        private BigDecimal shippedQty;
        /** 亚马逊实收件数，没有对账记录时为空 */
        private BigDecimal receivedQty;
        private String source;
        private BigDecimal shortQty;
        private BigDecimal price;
        private BigDecimal commissionRate;
        private String claimNo;
    }

    /** 识别出的退款未退回的退货（每个退货单 × SKU 一行） */
    @Data
    public static class UnreturnedRefund {
        private Long returnId;
        private String docNo;
        private Long storeId;
        private String platformOrderNo;
        private String sellerSku;
        private String currency;
        private BigDecimal qty;
        private BigDecimal refundAmount;
        private LocalDate refundDate;
    }

    /** 估算索赔单价用的在线商品信息 */
    @Data
    public static class ListingValue {
        private Long storeId;
        private String sellerSku;
        private String fnsku;
        private BigDecimal price;
        private BigDecimal commissionRate;
        private String currency;
        private Long materialId;
        private BigDecimal packQty;
    }

    /** 索赔汇总（金额为本位币） */
    public record ClaimSummary(List<Map<String, Object>> byStatus, List<Map<String, Object>> byType, BigDecimal claimableBase,
                               BigDecimal paidBase, int expiringSoon, List<String> missingRates) {
    }

    /** 自动识别结果：按类型的新增条数 */
    public record DetectResult(Map<String, Integer> created, int total) {
    }

    /** 费用核查实际扣费的查询结果（店铺 × SKU） */
    @Data
    public static class FeeActual {
        private Long storeId;
        private String sellerSku;
        private Integer orderCount;
        private BigDecimal qty;
        private BigDecimal actualFee;
    }

    /** 头程签收记录（推算库龄用）：签收日期与件数 */
    @Data
    public static class ReceiptLot {
        private Long storeId;
        private String sellerSku;
        private LocalDate receiveDate;
        private BigDecimal qty;
    }

    /** 推算库龄用的库存基数：平台库存快照（有时优先）或系统平台仓库存 */
    @Data
    public static class StockBase {
        private String sellerSku;
        private String asin;
        private String fnsku;
        private String title;
        private LocalDate snapshotDate;
        private BigDecimal fulfillableQty;
        private BigDecimal inboundQty;
        private BigDecimal reservedQty;
        private BigDecimal unfulfillableQty;
        private BigDecimal systemQty;
    }
}
