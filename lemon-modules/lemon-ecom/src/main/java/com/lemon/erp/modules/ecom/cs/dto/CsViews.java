package com.lemon.erp.modules.ecom.cs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.ecom.cs.entity.EcomCsTicket;
import com.lemon.erp.modules.ecom.cs.entity.EcomMessage;
import com.lemon.erp.modules.ecom.cs.entity.EcomMessageThread;
import com.lemon.erp.modules.ecom.cs.model.CsRequestStatus;
import com.lemon.erp.modules.ecom.entity.EcomOrder;
import com.lemon.erp.modules.ecom.model.EcomLocalStatus;
import com.lemon.erp.modules.ecom.model.EcomOrderStatus;
import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客服与评价的返回结构
 */
public final class CsViews {

    private CsViews() {
    }

    /**
     * 会话详情：会话、消息（按时间升序）、平台能力（能否发送）、售后工单
     *
     * @param canSend 平台是否支持发送回复（不支持时只能登记）
     */
    public record ThreadDetail(EcomMessageThread thread, List<EcomMessage> messages, boolean canSend, int replyHours, List<EcomCsTicket> tickets) {
    }

    /**
     * 订单侧栏：关联订单（含明细、包裹，按订单的数据权限读取，没有权限时为空）与该买家在店铺中的最近订单
     */
    public record OrderPanel(EcomOrder order, List<EcomOrder> recentOrders) {
    }

    /**
     * 模板替换结果
     *
     * @param missing 没有取到值的变量（如订单还没有运单号），页面提示人工补充
     */
    public record Rendered(String content, List<String> missing) {
    }

    /** 评价概况 */
    public record ReviewSummary(long total, BigDecimal avgRating, long lowCount, BigDecimal lowRatePercent, long pendingCount, long repliedCount) {
    }

    /** 按 SKU 的评价统计 */
    public record SkuStat(Long storeId, String storeName, String sellerSku, String title, long reviewCount, BigDecimal avgRating, long lowCount,
                          BigDecimal lowRatePercent, @JsonFormat(pattern = "yyyy-MM-dd") LocalDate lastDate) {
    }

    /** 按天的评分趋势 */
    public record TrendPoint(@JsonFormat(pattern = "yyyy-MM-dd") LocalDate date, long reviewCount, BigDecimal avgRating, long lowCount) {
    }

    /** 可以请求评价的订单（已发货 5–30 天）及请求记录 */
    @lombok.Data
    public static class RequestCandidate {
        private Long orderId;
        private Long storeId;
        private String storeName;
        private EcomPlatform platform;
        private String platformOrderNo;
        private String buyerName;
        private EcomOrderStatus status;
        private EcomLocalStatus localStatus;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime shipTime;
        private CsRequestStatus requestStatus;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime requestTime;
        private String requestMessage;
    }

    /** 一张订单请求评价的结果 */
    public record RequestOutcome(Long orderId, String platformOrderNo, CsRequestStatus status, String message) {
    }

    /** 一个店铺一次同步的结果 */
    public record SyncOutcome(Long storeId, String storeName, int fetched, int created, int updated, String message) {
    }
}
