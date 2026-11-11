package com.lemon.erp.modules.ecom.dto;

import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.model.SettlementItemType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 电商看板与利润分析的返回结构。金额均为本位币（按订单汇率折算），利润 = 销售额 − 平台费用 − 退款 − 商品成本 − 物流成本
 */
public final class EcomReports {

    private EcomReports() {
    }

    /** 看板概览：今日 / 本月销售与订单，待处理、异常、逾期未发货订单数，紧急补货 SKU 数 */
    @Data
    public static class Overview {
        private Long todayOrders;
        private BigDecimal todaySales;
        private Long monthOrders;
        private BigDecimal monthSales;
        private BigDecimal monthProfit;
        private BigDecimal monthRefund;
        private BigDecimal monthFees;
        private BigDecimal monthCost;
        private Long pendingOrders;
        private Long exceptionOrders;
        private Long overdueOrders;
        private Long inTransitInbounds;
        private Long urgentReplenish;
    }

    /** 按天趋势 */
    @Data
    public static class TrendRow {
        private LocalDate day;
        private Long orders;
        private BigDecimal sales;
        private BigDecimal profit;
    }

    /** 按店铺 / 平台汇总 */
    @Data
    public static class StoreRow {
        private Long storeId;
        private String storeName;
        private EcomPlatform platform;
        private String marketplace;
        private Long orders;
        private BigDecimal qty;
        private BigDecimal sales;
        private BigDecimal fees;
        private BigDecimal refund;
        private BigDecimal cost;
        private BigDecimal shipping;
        private BigDecimal adSpend;
        private BigDecimal profit;
        /** 利润率（%）= 利润 / 销售额 */
        private BigDecimal margin;
    }

    /** 按平台 SKU 汇总（广告花费按店铺 + SKU 的广告日报汇总，折本位币） */
    @Data
    public static class SkuRow {
        private Long storeId;
        private String storeName;
        private String sellerSku;
        private String platformItemId;
        private String title;
        private String materialCode;
        private String materialName;
        private Long orders;
        private BigDecimal qty;
        private BigDecimal sales;
        private BigDecimal fees;
        private BigDecimal refund;
        private BigDecimal cost;
        private BigDecimal shipping;
        private BigDecimal adSpend;
        private BigDecimal profit;
        private BigDecimal margin;
    }

    /** 按国家 */
    @Data
    public static class CountryRow {
        private String country;
        private Long orders;
        private BigDecimal qty;
        private BigDecimal sales;
        private BigDecimal profit;
    }

    /** 结算费用构成 */
    @Data
    public static class FeeRow {
        private SettlementItemType itemType;
        private BigDecimal amount;
    }

    /** 广告汇总：ACOS = 花费 / 广告销售额，点击率 = 点击 / 曝光，单次点击成本 = 花费 / 点击 */
    @Data
    public static class AdRow {
        private Long storeId;
        private String storeName;
        private String campaign;
        private String sellerSku;
        private Long impressions;
        private Long clicks;
        private BigDecimal spend;
        private BigDecimal adSales;
        private Integer adOrders;
        private BigDecimal acos;
        private BigDecimal ctr;
        private BigDecimal cpc;
    }
}
