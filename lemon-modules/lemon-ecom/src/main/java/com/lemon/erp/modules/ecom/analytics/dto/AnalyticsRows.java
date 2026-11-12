package com.lemon.erp.modules.ecom.analytics.dto;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.FulfillmentType;
import com.lemon.erp.modules.ecom.model.ListingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 数据分析查询（EcomAnalyticsMapper.xml）的原始结果行，由服务组装成页面数据
 */
public final class AnalyticsRows {

    private AnalyticsRows() {
    }

    /** 销量统计的一格：维度字段（按维度只填相关的）、日期（按天时）、订单数、件数、销售额（本位币） */
    @Data
    public static class SalesCell {
        private Long storeId;
        private String storeName;
        private EcomPlatform platform;
        private String sellerSku;
        private Long listingId;
        private String title;
        private String spu;
        private Integer skuCount;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String materialSpec;
        private String country;
        private LocalDate saleDay;
        private Long orders;
        private BigDecimal qty;
        private BigDecimal amount;
    }

    @Data
    public static class ListingDay {
        private Long listingId;
        private LocalDate saleDay;
        private BigDecimal qty;
    }

    /** 在售 SKU：店铺的默认发货仓（本地仓）与平台仓 */
    @Data
    public static class ListingRef {
        private Long listingId;
        private Long storeId;
        private String sellerSku;
        private FulfillmentType fulfillment;
        private Long warehouseId;
        private Long platformWarehouseId;
    }

    @Data
    public static class ListingInfo {
        private Long listingId;
        private Long storeId;
        private String storeName;
        private EcomPlatform platform;
        private String sellerSku;
        private String title;
        private FulfillmentType fulfillment;
        private ListingStatus status;
        private String materialCode;
        private String materialName;
    }

    @Data
    public static class SkuSales {
        private Long storeId;
        private String storeName;
        private String sellerSku;
        private Long listingId;
        private String title;
        private String materialCode;
        private String materialName;
        private Long orders;
        private BigDecimal qty;
        private BigDecimal sales;
        private BigDecimal profit;
    }

    @Data
    public static class SkuReturn {
        private Long storeId;
        private String sellerSku;
        private String reason;
        private BigDecimal qty;
        private BigDecimal refund;
    }

    @Data
    public static class MaterialStock {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String materialSpec;
        private String unit;
        private BigDecimal stockQty;
        private BigDecimal unitCost;
        private Integer skuCount;
        private LocalDateTime lastSale;
    }

    /** 一笔库存（批次或入库流水）：入库时间、数量 */
    @Data
    public static class AgeLot {
        private Long materialId;
        private LocalDateTime inTime;
        private BigDecimal qty;
    }

    @Data
    public static class MaterialPeriod {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String materialSpec;
        private String unit;
        private Integer skuCount;
        private BigDecimal salesQty;
        private BigDecimal stockQty;
        private BigDecimal unitCost;
    }

    @Data
    public static class FlowSum {
        private Long materialId;
        private BigDecimal sinceBegin;
        private BigDecimal sinceEnd;
        private BigDecimal inbound;
    }

    @Data
    public static class StockoutStat {
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
        private LocalDate lastStockoutDate;
    }

    @Data
    public static class FirstSale {
        private Long storeId;
        private String storeName;
        private String sellerSku;
        private Long listingId;
        private String title;
        private String materialCode;
        private String materialName;
        private LocalDateTime firstTime;
    }
}
