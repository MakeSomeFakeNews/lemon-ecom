import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 平台店铺分页：keyword（编码 / 名称）、platform、status */
export const pageStore = (params) => request.get('/ecom/store', { params })
/** 启用的店铺（登录即可访问）：[{ id, code, name, platform, marketplace, currency, warehouseId, platformWarehouseId, customerId }] */
export const storeOptions = () => request.get('/ecom/store/options')
/** 详情：不返回 credentials，只返回 hasCredentials */
export const getStore = (id) => request.get(`/ecom/store/${id}`)
export const addStore = (data) => request.post('/ecom/store', data)
/** 修改：credentials 留空表示不修改 */
export const updateStore = (id, data) => request.put(`/ecom/store/${id}`, data)
export const changeStoreStatus = (id, status) => request.put(`/ecom/store/${id}/status`, null, { params: { status } })
/** 清除渠道授权，之后该店铺只能手工导入订单 */
export const clearStoreCredentials = (id) => request.delete(`/ecom/store/${id}/credentials`)
export const deleteStore = (id) => request.delete(`/ecom/store/${ids(id)}`)
/** 测试渠道授权是否有效 */
export const testStore = (id) => request.post(`/ecom/store/${id}/test`, null, { timeout: 60000 })
/** 手工同步订单，since（yyyy-MM-ddTHH:mm:ss）为空时从上次同步时间开始；返回 { storeId, storeName, fetched, created, updated, message } */
export const syncStore = (id, since) => request.post(`/ecom/store/${id}/sync`, null, { params: { since }, timeout: 300000 })

// ---------------------------------------------------------------- 渠道同步中心

/** 已有渠道接口的平台（登录即可访问）：[{ platform, authType, fields: [{ key, label, secret, required, oauth }], productionBaseUrl, sandboxBaseUrl, hasSandbox, capabilities, docUrl }] */
export const storeConnectors = () => request.get('/ecom/store/connectors')
/** 店铺已填写的授权字段名（不返回值） */
export const storeCredentialKeys = (id) => request.get(`/ecom/store/${id}/credential-keys`)
/** OAuth 授权页地址（新窗口打开） */
export const storeAuthorizeUrl = (id) => request.post(`/ecom/store/${id}/authorize-url`)
/** 手工拉取在线商品：返回 { fetched, created, updated, message } */
export const syncStoreListings = (id) => request.post(`/ecom/store/${id}/sync-listings`, null, { timeout: 300000 })
/** 手工回传可售库存，all 为 true 时全部回传：返回 { fetched（SKU 数）, updated（回传数）, message } */
export const pushStoreInventory = (id, all) => request.post(`/ecom/store/${id}/push-inventory`, null, { params: { all }, timeout: 300000 })
/** 同步日志分页：storeId、syncType、success（1 / 0） */
export const pageStoreSyncLogs = (params) => request.get('/ecom/store/sync-logs', { params })
