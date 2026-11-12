package com.lemon.erp.modules.ecom.analytics.dto;

import com.lemon.erp.modules.ecom.analytics.entity.EcomForecastEvent;
import com.lemon.erp.modules.ecom.analytics.entity.EcomForecastSetting;
import com.lemon.erp.modules.ecom.analytics.model.ForecastMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 单个 SKU 的预测详情（按当前参数与事件现算）：最近的历史日销量、各方法从今天起的预测与回测误差、所选方法的 80% 预测区间
 *
 * @param info          SKU（店铺、标题、物料）
 * @param method        选用的方法（手工指定优先，否则回测 MAPE 最小）
 * @param fixedMethod   手工指定的方法
 * @param historyStart  history 第一天
 * @param history       每天的实际销量
 * @param forecastStart 预测第一天（今天）
 * @param lower         所选方法每天的区间下限
 * @param upper         所选方法每天的区间上限
 * @param backtest      所选方法对最近 28 天的回测预测（与 history 末尾 28 天对齐），历史不足时为空
 * @param events        影响预测期或历史的促销 / 节假日
 */
public record ForecastDetail(AnalyticsRows.ListingInfo info, ForecastMethod method, ForecastMethod fixedMethod, EcomForecastSetting setting,
                             LocalDate historyStart, List<BigDecimal> history, LocalDate forecastStart, List<MethodLine> methods,
                             List<BigDecimal> lower, List<BigDecimal> upper, BigDecimal sigma, List<BigDecimal> backtest,
                             List<EcomForecastEvent> events) {

    /**
     * 一种方法的结果
     *
     * @param mape  回测 MAPE（%），无法计算为空
     * @param mae   回测 28 天绝对误差合计
     * @param daily 从今天起每天的预测
     */
    public record MethodLine(ForecastMethod method, BigDecimal mape, BigDecimal mae, BigDecimal total30, BigDecimal total60,
                             BigDecimal total90, boolean selected, List<BigDecimal> daily) {
    }
}
