package com.lemon.erp.modules.ecom.assistant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** AI 运营助手的查询结果行（{@code EcomAssistantQueryMapper}） */
public final class AssistantRows {

    private AssistantRows() {
    }

    /** 广告日报按店铺汇总（店铺币种） */
    @Data
    public static class AdTotal {
        private Long storeId;
        private String currency;
        private long impressions;
        private long clicks;
        private BigDecimal spend;
        private BigDecimal sales;
        private long orders;
    }

    @Data
    public static class SkuReturn {
        private Long storeId;
        private String sellerSku;
        private BigDecimal qty;
        private BigDecimal refundBase;
        private long returns;
    }

    @Data
    public static class ReasonRow {
        private String reason;
        private String reasonLabel;
        private long returns;
        private BigDecimal qty;
        private BigDecimal refundBase;
    }

    @Data
    public static class SkuReasonRow {
        private Long storeId;
        private String storeName;
        private String sellerSku;
        private String reason;
        private String reasonLabel;
        private long returns;
        private BigDecimal qty;
        private BigDecimal refundBase;
    }

    @Data
    public static class ReviewRow {
        private Long id;
        private Long storeId;
        private String storeName;
        private EcomPlatform platform;
        private String sellerSku;
        private String platformItemId;
        private Integer rating;
        private String title;
        private String content;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate reviewDate;
        private Integer replied;
        private String handleStatus;
    }

    @Data
    public static class ReviewSkuRow {
        private Long storeId;
        private String storeName;
        private String sellerSku;
        private long reviews;
        private BigDecimal avgRating;
        private long badReviews;
    }

    @Data
    public static class OrderIssue {
        private Long id;
        private Long storeId;
        private String storeName;
        private String platformOrderNo;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        private LocalDateTime orderTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        private LocalDateTime shipBy;
        private String localStatus;
        private String fulfillment;
        private String exception;
        private BigDecimal totalAmount;
        private String currency;
        /** 距承诺发货截止的小时数（负数为已超时） */
        private Long hoursLeft;
    }

    @Data
    public static class OrderIssueSummary {
        private Long storeId;
        private String storeName;
        private long exceptions;
        private long overdue;
        private long dueSoon;
        private long pending;
    }

    @Data
    public static class CampaignInfo {
        private Long id;
        private Long storeId;
        private String storeName;
        private String currency;
        private String name;
        private String status;
        private BigDecimal dailyBudget;
    }

    /** 搜索词：花费、销售额为店铺币种（不同站点不直接相加，只作参考） */
    @Data
    public static class KeywordRow {
        private String searchTerm;
        private long impressions;
        private long clicks;
        private long orders;
        private BigDecimal spend;
        private BigDecimal sales;
        /** 转化率（%）= 订单 ÷ 点击 */
        private BigDecimal cvrPercent;
        private String marketplaces;
    }
}
