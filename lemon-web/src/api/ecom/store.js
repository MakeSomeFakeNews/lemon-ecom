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
