import request from '@/utils/request'

/** 电商看板（金额为本位币，增长率为百分数） */
/** 关键指标：[{ key: TODAY | YESTERDAY | MONTH, begin, end, current, previous, lastYear, mom, yoy }]，params { storeId } */
export const dashboardKpi = (params) => request.get('/ecom/dashboard/kpi', { params })
/** 实时销量：{ today: [{ hour, orders, sales }], yesterday, currentHour }，params { storeId } */
export const dashboardHourly = (params) => request.get('/ecom/dashboard/hourly', { params })
/** Top SKU：params { beginDate, endDate, storeId, by: QTY | SALES | PROFIT, limit } */
export const dashboardTopSku = (params) => request.get('/ecom/dashboard/top-sku', { params })
/** 店铺排行：params { beginDate, endDate } */
export const dashboardStoreRank = (params) => request.get('/ecom/dashboard/store-rank', { params })
/** 平台占比：params { beginDate, endDate } */
export const dashboardPlatformShare = (params) => request.get('/ecom/dashboard/platform-share', { params })
/** 待处理事项：[{ key, title, count, unit, path, query, tone, icon, sort }] */
export const dashboardTodos = () => request.get('/ecom/dashboard/todos')
