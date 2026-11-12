import request from '@/utils/request'

/**
 * 数据分析（金额为本位币，比率为百分数）：销量统计、销量预测、商品分析。
 * 日期参数 beginDate / endDate 为 YYYY-MM-DD（含首尾）
 */

// ---------------------------------------------------------------- 销量统计

/** 销量矩阵：dimension（SalesDimension）、grain（SalesGrain）、beginDate、endDate、storeId、platform、keyword → SalesMatrix */
export const salesMatrix = (params) => request.get('/ecom/analytics/sales/matrix', { params })
/** 单个 SKU 的销量走势（叠加库存快照）：listingId、beginDate、endDate */
export const skuTrend = (params) => request.get('/ecom/analytics/sales/sku-trend', { params })

// ---------------------------------------------------------------- 销量预测

/** 预测列表（分页）：current、size、storeId、keyword、method */
export const pageForecast = (params) => request.get('/ecom/analytics/forecast', { params })
/** 单个 SKU 的预测详情 */
export const getForecast = (listingId) => request.get(`/ecom/analytics/forecast/${listingId}`)
/** 手工指定预测方法（method 为空恢复自动选择） */
export const fixForecastMethod = (listingId, method) => request.put(`/ecom/analytics/forecast/${listingId}/method`, { method: method || null })
/** 立即生成预测（storeId 为空时全部在售 SKU）→ { total, failed, firstError } */
export const generateForecast = (storeId) => request.post('/ecom/analytics/forecast/generate', null, { params: { storeId } })
export const getForecastSetting = () => request.get('/ecom/analytics/forecast/setting')
export const saveForecastSetting = (data) => request.put('/ecom/analytics/forecast/setting', data)
/** 促销 / 节假日：from、to、storeId */
export const listForecastEvents = (params) => request.get('/ecom/analytics/forecast/event', { params })
export const addForecastEvent = (data) => request.post('/ecom/analytics/forecast/event', data)
export const updateForecastEvent = (id, data) => request.put(`/ecom/analytics/forecast/event/${id}`, data)
export const deleteForecastEvent = (id) => request.delete(`/ecom/analytics/forecast/event/${id}`)

// ---------------------------------------------------------------- 商品分析

/** ABC：beginDate、endDate、storeId、keyword、metric（AbcMetric）、aPercent、bPercent */
export const productAbc = (params) => request.get('/ecom/analytics/product/abc', { params })
/** 滞销与动销率：days、storeId、keyword */
export const productSlow = (params) => request.get('/ecom/analytics/product/slow', { params })
/** 售罄率与周转（物料） */
export const productSellThrough = (params) => request.get('/ecom/analytics/product/sell-through', { params })
/** 断货天数（SKU，按每日快照） */
export const productStockout = (params) => request.get('/ecom/analytics/product/stockout', { params })
/** 新品表现（首单日起 30 天） */
export const productNew = (params) => request.get('/ecom/analytics/product/new-products', { params })
/** 退货率排行 */
export const productReturnRate = (params) => request.get('/ecom/analytics/product/return-rate', { params })
