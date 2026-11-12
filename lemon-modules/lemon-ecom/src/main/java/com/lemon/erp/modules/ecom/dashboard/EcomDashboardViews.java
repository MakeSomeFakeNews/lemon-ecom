package com.lemon.erp.modules.ecom.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lemon.erp.modules.ecom.model.EcomPlatform;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 电商看板的返回结构（金额为本位币，增长率为百分数）
 */
public final class EcomDashboardViews {

    private EcomDashboardViews() {
    }

    /**
     * 一个区间的指标
     *
     * @param avgOrder 客单价 = 销售额 ÷ 订单数
     * @param profit   订单利润（未扣广告与不挂订单的费用分摊）
     */
    public record Metric(long orders, BigDecimal sales, BigDecimal units, BigDecimal avgOrder, BigDecimal profit) {
    }

    /**
     * 今日 / 昨日 / 本月的指标与对比
     *
     * @param key      TODAY / YESTERDAY / MONTH
     * @param current  本期
     * @param previous 环比期（今日：昨日同一时段；昨日：前一天；本月：上月同期）
     * @param lastYear 同比期（去年同期）
     * @param mom      环比增长率（%），按指标名（orders、sales、units、avgOrder、profit），上期为 0 时为空
     * @param yoy      同比增长率（%）
     */
    public record KpiPeriod(String key, @JsonFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime begin,
                            @JsonFormat(pattern = "yyyy-MM-dd HH:mm") LocalDateTime end, Metric current, Metric previous, Metric lastYear,
                            Map<String, BigDecimal> mom, Map<String, BigDecimal> yoy) {
    }

    /** 某个小时的订单数与销售额 */
    public record HourPoint(int hour, long orders, BigDecimal sales) {
    }

    /**
     * 实时销量：今日与昨日每小时（0–23 点，今日只到当前小时）
     *
     * @param currentHour 当前小时
     */
    public record Hourly(List<HourPoint> today, List<HourPoint> yesterday, int currentHour) {
    }

    /** SKU 排行的一行 */
    public record SkuRank(Long storeId, String storeName, String sellerSku, String title, BigDecimal qty, BigDecimal sales, BigDecimal profit,
                          BigDecimal margin) {
    }

    /** 店铺排行的一行（利润扣除广告花费） */
    public record StoreRank(Long storeId, String storeName, EcomPlatform platform, String marketplace, long orders, BigDecimal sales,
                            BigDecimal profit, BigDecimal margin, BigDecimal adSpend) {
    }

    /**
     * 平台占比
     *
     * @param percent 销售额占比（%）
     */
    public record PlatformShare(EcomPlatform platform, long orders, BigDecimal sales, BigDecimal profit, BigDecimal margin, BigDecimal percent) {
    }
}
