import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/**
 * 订单增强：手工建单、补发单、标签与备注、地址校验 / 利润预估、批量指定仓库与物流、物流跟踪、买家黑名单、偏远地区、订单作业设置
 */

// ---------------------------------------------------------------- 订单

/** 手工创建订单（ManualOrder），返回订单 ID */
export const createManualOrder = (data) => request.post('/ecom/order/manual', data)
/** 从原订单创建补发单：{ lines: [{ orderItemId, qty }], ticketId, returnId, reason, ship*（为空取原订单收件信息） }，返回补发单 ID */
export const createResendOrder = (id, data) => request.post(`/ecom/order/${id}/resend`, data)
/** 原订单的补发单与售后成本 { resends, afterSaleCost } */
export const getOrderResends = (id) => request.get(`/ecom/order/${id}/resends`)
/** 批量打标 / 去标：{ ids, tagIds } */
export const tagOrders = (orderIds, tagIds) => request.post('/ecom/order/tag', { ids: orderIds, tagIds })
export const untagOrders = (orderIds, tagIds) => request.post('/ecom/order/untag', { ids: orderIds, tagIds })
/** 内部备注历史 */
export const listOrderNotes = (id) => request.get(`/ecom/order/${id}/notes`)
/** 订单操作日志（新的在前，带操作人） */
export const listOrderLogs = (id) => request.get(`/ecom/order/${id}/logs`)
export const addOrderNote = (id, content) => request.post(`/ecom/order/${id}/notes`, { content })
/** 地址校验、黑名单、利润预估的结果；POST 为重新校验 */
export const getOrderCheck = (id) => request.get(`/ecom/order/${id}/check`)
export const recheckOrder = (id) => request.post(`/ecom/order/${id}/check`)
/** 修改收件地址（保存后自动重新校验） */
export const updateOrderAddress = (id, data) => request.put(`/ecom/order/${id}/address`, data)
/** 批量指定发货仓 / 物流渠道，返回 { 平台订单号: 原因 } */
export const assignOrders = (data) => request.post('/ecom/order/assign', data)
/** 订单已发货包裹的轨迹 [{ pkg, events }] */
export const getOrderTracks = (id) => request.get(`/ecom/order/${id}/tracks`)
/** 按包裹重新回传发货（部分发货） */
export const confirmPackageShipment = (id) => request.post(`/ecom/package/${id}/ship-confirm`)

// ---------------------------------------------------------------- 标签

export const listOrderTags = (params) => request.get('/ecom/order-tag', { params })
export const orderTagOptions = () => request.get('/ecom/order-tag/options')
export const addOrderTag = (data) => request.post('/ecom/order-tag', data)
export const updateOrderTag = (id, data) => request.put(`/ecom/order-tag/${id}`, data)
export const deleteOrderTag = (id) => request.delete(`/ecom/order-tag/${ids(id)}`)

// ---------------------------------------------------------------- 买家黑名单

export const pageBlacklist = (params) => request.get('/ecom/buyer-blacklist', { params })
export const addBlacklist = (data) => request.post('/ecom/buyer-blacklist', data)
export const updateBlacklist = (id, data) => request.put(`/ecom/buyer-blacklist/${id}`, data)
export const deleteBlacklist = (id) => request.delete(`/ecom/buyer-blacklist/${ids(id)}`)

// ---------------------------------------------------------------- 偏远地区

export const pageRemoteArea = (params) => request.get('/ecom/remote-area', { params })
export const addRemoteArea = (data) => request.post('/ecom/remote-area', data)
export const updateRemoteArea = (id, data) => request.put(`/ecom/remote-area/${id}`, data)
export const deleteRemoteArea = (id) => request.delete(`/ecom/remote-area/${ids(id)}`)

// ---------------------------------------------------------------- 物流跟踪

/** 已发货包裹的轨迹列表：storeId、channelId、country、trackStatus、alert、alertOnly、keyword、beginDate、endDate */
export const pageTrack = (params) => request.get('/ecom/track', { params })
/** 各状态 / 预警的包裹数 { "S:状态": n, "A:预警": n } */
export const trackCounts = (params) => request.get('/ecom/track/counts', { params })
export const getPackageTracks = (packageId) => request.get(`/ecom/track/${packageId}`)
export const pullPackageTracks = (packageId) => request.post(`/ecom/track/${packageId}/pull`)
export const pullTracks = (packageIds) => request.post('/ecom/track/pull', { ids: packageIds })
/** 订单作业设置：地址校验、17TRACK 密钥（只写）与地址、预警时限 */
export const getOrderSetting = () => request.get('/ecom/track/setting')
export const saveOrderSetting = (data) => request.put('/ecom/track/setting', data)
