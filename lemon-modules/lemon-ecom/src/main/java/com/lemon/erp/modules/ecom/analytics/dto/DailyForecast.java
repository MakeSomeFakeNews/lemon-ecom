package com.lemon.erp.modules.ecom.analytics.dto;

import com.lemon.erp.modules.ecom.analytics.model.ForecastMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 一个 SKU（在线商品）未来每天的预测销量（平台 SKU 件数），供补货建议等按预测计算
 *
 * @param startDate 第一天（今天）
 * @param daily     从 startDate 起每天的预测量，长度为请求的天数
 * @param stored    true 为定时任务生成的结果；false 为当时没有结果（新 SKU、还没生成）现算的
 */
public record DailyForecast(Long listingId, ForecastMethod method, LocalDate startDate, List<BigDecimal> daily, boolean stored) {

    /** 预测期合计 */
    public BigDecimal total() {
        return daily.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 预测期日均（4 位小数） */
    public BigDecimal average() {
        return daily.isEmpty() ? BigDecimal.ZERO : total().divide(BigDecimal.valueOf(daily.size()), 4, RoundingMode.HALF_UP);
    }
}
