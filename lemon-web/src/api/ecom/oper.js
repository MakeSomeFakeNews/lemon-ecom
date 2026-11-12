import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

// ---------------------------------------------------------------- ASIN / 商品监控 /ecom/oper/monitor

/** 监控列表：params { current, size, storeId, platform, marketplace, monitorType, status, keyword, alertOnly } */
export const pageMonitor = (params) => request.get('/ecom/oper/monitor', { params })
/** 支持接口取数的平台：EcomPlatform[] */
export const monitorFetchPlatforms = () => request.get('/ecom/oper/monitor/platforms')
export const getMonitor = (id) => request.get(`/ecom/oper/monitor/${id}`)
/** 趋势：{ snapshots（升序）, alerts }，days 默认 60 */
export const monitorTrend = (id, days = 60) => request.get(`/ecom/oper/monitor/${id}/trend`, { params: { days } })
/** 列表迷你趋势：一次取多个监控最近 days 天的排名 / 评分 [{ monitorId, dates, rankMain, rankSub, rating, reviewCount }] */
export const monitorTrends = (ids, days = 30) => request.get('/ecom/oper/monitor/trends', { params: { ids: [].concat(ids).join(','), days } })
export const addMonitor = (data) => request.post('/ecom/oper/monitor', data)
export const updateMonitor = (id, data) => request.put(`/ecom/oper/monitor/${id}`, data)
export const changeMonitorStatus = (id, status) => request.put(`/ecom/oper/monitor/${id}/status`, null, { params: { status } })
export const deleteMonitor = (v) => request.delete(`/ecom/oper/monitor/${ids(v)}`)
/** 接口取数：返回 { total, success, failed, alerts, message } */
export const fetchMonitors = (idList) => request.post('/ecom/oper/monitor/fetch', { ids: [].concat(idList) })
/** 手工录入快照，返回新产生的预警 */
export const recordSnapshot = (id, data) => request.post(`/ecom/oper/monitor/${id}/snapshot`, data)
export const importSnapshots = (data) => request.post('/ecom/oper/monitor/snapshot/import', data)
/** 预警列表：params { current, size, storeId, monitorId, alertType, status, beginDate, endDate, keyword } */
export const pageMonitorAlert = (params) => request.get('/ecom/oper/monitor/alert', { params })
/** 未处理预警数：[{ alertType, count }] */
export const monitorAlertCounts = (params) => request.get('/ecom/oper/monitor/alert/counts', { params })
/** 处理预警：{ ids, status（HANDLED / IGNORED）, note } */
export const handleMonitorAlerts = (data) => request.post('/ecom/oper/monitor/alert/handle', data)

// ---------------------------------------------------------------- 促销 /ecom/oper/promotion

/** 促销列表：params { current, size, storeId, promoType, status, phase, keyword, beginDate, endDate } */
export const pagePromotion = (params) => request.get('/ecom/oper/promotion', { params })
/** 促销日历：[{ id, name, storeId, storeName, platform, promoType, startTime, endTime, status, phase, itemCount }] */
export const promotionCalendar = (params) => request.get('/ecom/oper/promotion/calendar', { params })
/** 能推送的平台 */
export const promotionPushPlatforms = () => request.get('/ecom/oper/promotion/platforms')
export const getPromotion = (id) => request.get(`/ecom/oper/promotion/${id}`)
/** 效果分析：{ promotion, days, before, during, after, qtyLift, salesLift, profitLift, roi, daily, skus } */
export const promotionEffect = (id) => request.get(`/ecom/oper/promotion/${id}/effect`)
export const addPromotion = (data) => request.post('/ecom/oper/promotion', data)
export const updatePromotion = (id, data) => request.put(`/ecom/oper/promotion/${id}`, data)
export const deletePromotion = (v) => request.delete(`/ecom/oper/promotion/${ids(v)}`)
export const confirmPromotion = (id) => request.post(`/ecom/oper/promotion/${id}/confirm`)
export const cancelPromotion = (id) => request.post(`/ecom/oper/promotion/${id}/cancel`)
export const pushPromotion = (id) => request.post(`/ecom/oper/promotion/${id}/push`)

// ---------------------------------------------------------------- 运营目标与绩效 /ecom/oper/target

/** 目标设置：[{ refId, refName, code, platform, marketplace, ownerName, salesTarget, profitTarget, orderTarget, remark, lastSales, lastProfit, lastOrders }] */
export const targetEditRows = (month, targetType) => request.get('/ecom/oper/target/edit', { params: { month, targetType } })
/** 保存：{ month, targetType, rows: [{ refId, salesTarget, profitTarget, orderTarget, remark }] } */
export const saveTargets = (data) => request.put('/ecom/oper/target', data)
/** 复制上月目标：{ month, targetType }，返回复制条数 */
export const copyTargets = (data) => request.post('/ecom/oper/target/copy', data)
/** 达成率：{ month, timeProgress, total, rows }，dimension 为 EcomAchievementDimension */
export const targetAchievement = (month, dimension) => request.get('/ecom/oper/target/achievement', { params: { month, dimension } })
/** 运营人员绩效排行：params { beginDate, endDate } */
export const opsPerformance = (params) => request.get('/ecom/oper/target/performance', { params })

// ---------------------------------------------------------------- 店铺授权 /ecom/oper/store-auth

/** 店铺与授权成员：params { current, size, keyword, platform, userId } */
export const pageStoreAuth = (params) => request.get('/ecom/oper/store-auth', { params })
export const storeMembers = (storeId) => request.get(`/ecom/oper/store-auth/store/${storeId}`)
/** 保存店铺的授权成员：{ members: [{ userId, memberRole, remark }] } */
export const saveStoreMembers = (storeId, data) => request.put(`/ecom/oper/store-auth/store/${storeId}`, data)
/** 用户负责与被授权的店铺：{ userId, nickname, owned, granted } */
export const userStores = (userId) => request.get(`/ecom/oper/store-auth/user/${userId}`)
/** 按用户授权：{ userId, storeIds, memberRole } */
export const saveUserStores = (data) => request.put('/ecom/oper/store-auth/user', data)
/** 当前用户的店铺范围：{ restricted, storeIds } */
export const myStoreScope = () => request.get('/ecom/oper/store-auth/mine')
