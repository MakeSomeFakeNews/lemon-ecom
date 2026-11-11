import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 退货退款 /ecom/return：分页条件 storeId、status（EcomReturnStatus）、keyword（单据号 / 平台订单号 / 买家） */
export const pageEcomReturn = (params) => request.get('/ecom/return', { params })
/** 详情含 items */
export const getEcomReturn = (id) => request.get(`/ecom/return/${id}`)
/** 新增（ReturnRequest）：orderId、returnType、reason、refundAmount（为空按明细合计）、warehouseId（退货退款必填）、remark、items[{ orderItemId, qty, refundAmount }]，返回 ID */
export const addEcomReturn = (data) => request.post('/ecom/return', data)
/** 退货入库（退货退款、待处理）：date 为入库日期（yyyy-MM-dd），为空取当天 */
export const receiveEcomReturn = (id, date) => request.post(`/ecom/return/${id}/receive`, null, { params: { date } })
/** 登记退款：仅退款的待处理单、退货退款的已入库单 */
export const refundEcomReturn = (id) => request.post(`/ecom/return/${id}/refund`)
/** 关闭待处理的单 */
export const closeEcomReturn = (id) => request.post(`/ecom/return/${id}/close`)
/** 删除待处理 / 已关闭的单 */
export const deleteEcomReturn = (id) => request.delete(`/ecom/return/${ids(id)}`)
