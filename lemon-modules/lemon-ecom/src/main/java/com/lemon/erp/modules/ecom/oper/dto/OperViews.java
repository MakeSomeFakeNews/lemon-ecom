package com.lemon.erp.modules.ecom.oper.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.ecom.model.EcomPlatform;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitorAlert;
import com.lemon.erp.modules.ecom.oper.entity.EcomMonitorSnapshot;
import com.lemon.erp.modules.ecom.oper.entity.EcomPromotion;
import com.lemon.erp.modules.ecom.oper.entity.EcomStoreMember;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 运营监控与团队的返回结构
 */
public final class OperViews {

    private OperViews() {
    }

    // ---------------------------------------------------------------- 商品监控

    /** 监控详情：快照趋势（按日期升序）与最近的预警 */
    public record MonitorTrend(List<EcomMonitorSnapshot> snapshots, List<EcomMonitorAlert> alerts) {
    }

    /** 接口取数的结果 */
    public record FetchOutcome(int total, int success, int failed, int alerts, String message) {
    }

    /** 未处理预警数：按类型 */
    public record AlertCount(String alertType, long count) {
    }

    // ---------------------------------------------------------------- 店铺授权

    /** 店铺授权页的一行：店铺、负责人与授权成员 */
    @Data
    public static class StoreAuthRow {
        private Long storeId;
        private String storeCode;
        private String storeName;
        private EcomPlatform platform;
        private String marketplace;
        private Long ownerId;
        private String ownerName;
        private Integer status;
        private List<EcomStoreMember> members;
    }

    /** 按用户查看：用户负责的店铺与被授权的店铺 */
    public record UserStores(Long userId, String nickname, boolean unrestricted, List<StoreBrief> owned, List<EcomStoreMember> granted) {
    }

    public record StoreBrief(Long id, String code, String name, EcomPlatform platform, String marketplace) {
    }

    // ---------------------------------------------------------------- 促销

    /** 促销日历中的一个活动 */
    public record CalendarItem(Long id, String name, Long storeId, String storeName, EcomPlatform platform, String promoType,
                               @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                               @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                               String status, String phase, Integer itemCount) {
    }

    /** 效果分析中一个时间段的汇总（本位币）；profit 已扣除该时段分摊的活动费用（只在活动期） */
    @Data
    public static class Window {
        private String key;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime begin;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime end;
        /** 时段尚未结束（统计到当前时间） */
        private boolean partial;
        private long orders;
        private BigDecimal qty = BigDecimal.ZERO;
        private BigDecimal sales = BigDecimal.ZERO;
        private BigDecimal grossProfit = BigDecimal.ZERO;
        private BigDecimal fee = BigDecimal.ZERO;
        private BigDecimal profit = BigDecimal.ZERO;
        /** 日均销量、日均销售额 */
        private BigDecimal dailyQty = BigDecimal.ZERO;
        private BigDecimal dailySales = BigDecimal.ZERO;
        /** 利润率 % */
        private BigDecimal profitRate;
    }

    /** 按天的销量（参与 SKU 合计） */
    public record DailyRow(@JsonFormat(pattern = "yyyy-MM-dd") LocalDate day, long orders, BigDecimal qty, BigDecimal sales, BigDecimal profit) {
    }

    /** 按 SKU 的活动前 / 中 / 后销量与销售额 */
    @Data
    public static class SkuEffect {
        private String sellerSku;
        private String title;
        private BigDecimal beforeQty = BigDecimal.ZERO;
        private BigDecimal duringQty = BigDecimal.ZERO;
        private BigDecimal afterQty = BigDecimal.ZERO;
        private BigDecimal beforeSales = BigDecimal.ZERO;
        private BigDecimal duringSales = BigDecimal.ZERO;
        private BigDecimal afterSales = BigDecimal.ZERO;
        /** 活动期销量较活动前的增长 %，活动前为 0 时为空 */
        private BigDecimal qtyLift;
    }

    /**
     * 效果分析：活动前 / 中 / 后同等天数的对比（days 为活动天数），销量增长、销售额增长为活动期较活动前的百分比，
     * roi = (活动期利润 − 活动前利润) ÷ 活动费用
     */
    public record PromotionEffect(EcomPromotion promotion, int days, Window before, Window during, Window after,
                                  BigDecimal qtyLift, BigDecimal salesLift, BigDecimal profitLift, BigDecimal roi,
                                  List<DailyRow> daily, List<SkuEffect> skus) {
    }

    // ---------------------------------------------------------------- 目标与绩效

    /** 目标设置页的一行：对象与本月目标（没有设置为 0） */
    @Data
    public static class TargetEditRow {
        private Long refId;
        private String refName;
        private String code;
        private EcomPlatform platform;
        private String marketplace;
        private Long ownerId;
        private String ownerName;
        private BigDecimal salesTarget = BigDecimal.ZERO;
        private BigDecimal profitTarget = BigDecimal.ZERO;
        private Integer orderTarget = 0;
        private String remark;
        /** 上月实际（参考） */
        private BigDecimal lastSales = BigDecimal.ZERO;
        private BigDecimal lastProfit = BigDecimal.ZERO;
        private long lastOrders;
    }

    /** 达成率的一行（本位币）：rate 为百分比，没有目标时为空 */
    @Data
    public static class AchievementRow {
        private String key;
        private String name;
        private EcomPlatform platform;
        private BigDecimal salesTarget = BigDecimal.ZERO;
        private BigDecimal salesActual = BigDecimal.ZERO;
        private BigDecimal salesRate;
        private BigDecimal profitTarget = BigDecimal.ZERO;
        private BigDecimal profitActual = BigDecimal.ZERO;
        private BigDecimal profitRate;
        private long orderTarget;
        private long orderActual;
        private BigDecimal orderRate;
        /** 店铺数（按负责人、按平台） */
        private int storeCount;
    }

    /** 达成率报表：timeProgress 为月份已过天数占比（%，用于对比进度） */
    public record Achievement(String month, BigDecimal timeProgress, AchievementRow total, List<AchievementRow> rows) {
    }

    /** 运营人员绩效（本位币）：按负责人汇总其负责的店铺 */
    @Data
    public static class PerformanceRow {
        private int rank;
        private Long userId;
        private String userName;
        private int storeCount;
        private long orders;
        private BigDecimal sales = BigDecimal.ZERO;
        private BigDecimal profit = BigDecimal.ZERO;
        /** 利润率 % */
        private BigDecimal profitRate;
        private BigDecimal adSpend = BigDecimal.ZERO;
        private BigDecimal adSales = BigDecimal.ZERO;
        /** 广告 ACOS % = 广告花费 ÷ 广告销售额 */
        private BigDecimal acos;
        private long returns;
        /** 退货率 % = 退货单数 ÷ 订单数 */
        private BigDecimal returnRate;
        /** 差评数（1–2 星） */
        private long badReviews;
        private BigDecimal salesTarget;
        /** 销售额达成率 %（只按月查询时有） */
        private BigDecimal salesRate;
    }
}
