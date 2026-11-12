package com.lemon.erp.modules.ecom.analytics.model;

/**
 * 销量预测方法（系统字典 ecom_forecast_method）：每个 SKU 默认用回测误差（MAPE）最小的方法，可手工指定
 */
public enum ForecastMethod {
    /** 移动平均：最近 N 天（默认 28）日均 */
    MA,
    /** 加权移动平均：最近 N 天按 1..N 线性加权，越近权重越大 */
    WMA,
    /** 指数平滑（Holt 线性趋势）：水平 α、趋势 β */
    HOLT,
    /** 星期季节性：去掉星期因素后的加权移动平均 × 周内各天的销量系数 */
    SEASONAL
}
