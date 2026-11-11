import request from '@/utils/request'

/**
 * 电商看板与利润分析（金额为本位币）。
 * 日期参数 beginDate / endDate 为 YYYY-MM-DD；store / sku / country / fees 返回数组（不分页）
 */
export const reportOverview = () => request.get('/ecom/report/overview')
export const reportTrend = (params) => request.get('/ecom/report/trend', { params })
export const reportStore = (params) => request.get('/ecom/report/store', { params })
export const reportSku = (params) => request.get('/ecom/report/sku', { params })
export const reportCountry = (params) => request.get('/ecom/report/country', { params })
export const reportFees = (params) => request.get('/ecom/report/fees', { params })
