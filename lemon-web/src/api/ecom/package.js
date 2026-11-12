import request from '@/utils/request'

/**
 * 包裹与打单 /ecom/package
 * 分页条件（PackageQuery）：storeId、warehouseId、channelId、status、keyword（包裹号 / 运单号 / 平台订单号 / 收件人）、beginDate、endDate
 * 批量接口返回 { 包裹号: 原因 }，全部成功时为空对象
 */
export const pageEcomPackage = (params) => request.get('/ecom/package', { params })
export const ecomPackageStatusCounts = (params) => request.get('/ecom/package/status-counts', { params })
/** 详情含 items（订单号、SKU、物料、件数） */
export const getEcomPackage = (id) => request.get(`/ecom/package/${id}`)
/** 批量申请运单号：{ ids, channelId?, weight? }（包裹没有渠道 / 重量时使用） */
export const labelEcomPackages = (data) => request.post('/ecom/package/label', data, { timeout: 300000 })
/** 重新获取面单 */
export const refetchEcomPackageLabel = (id) => request.post(`/ecom/package/${id}/label-file`, null, { timeout: 120000 })
/** 手工填写运单号：{ carrier, trackingNo, channelId?, weight?, shippingCost? } */
export const setEcomPackageTracking = (id, data) => request.put(`/ecom/package/${id}/tracking`, data)
/** 修改：{ channelId, weight, shippingCost, remark } */
export const updateEcomPackage = (id, data) => request.put(`/ecom/package/${id}`, data)
/** 单个面单 PDF（Blob） */
export const ecomPackageLabel = (id) => request.get(`/ecom/package/${id}/label`, { responseType: 'blob', timeout: 120000 })
/** 批量打印面单：合并的 PDF（Blob） */
export const ecomPackageLabels = (ids) => request.post('/ecom/package/labels', { ids }, { responseType: 'blob', timeout: 120000 })
/** 拣货单：[{ warehouseName, packageCount, totalQty, packageNos, rows: [{ locationCode, otherLocations, materialCode, materialName, spec, unit, qty, skus, packageCount }] }] */
export const ecomPickList = (ids) => request.post('/ecom/package/pick-list', { ids })
/** 批量发货 */
export const shipEcomPackages = (ids) => request.post('/ecom/package/ship', { ids }, { timeout: 300000 })
/** 扫描发货：运单号或包裹号，返回包裹详情 */
export const scanShipEcomPackage = (code) => request.post('/ecom/package/scan', { code })
/** 取消（已在物流商下单的同时取消物流商订单） */
export const cancelEcomPackage = (id) => request.post(`/ecom/package/${id}/cancel`)
/** 物流轨迹：[{ time, location, description }] */
export const trackEcomPackage = (id) => request.get(`/ecom/package/${id}/track`)
