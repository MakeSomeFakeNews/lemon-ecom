import request from '@/utils/request'

/**
 * 平台订单 /ecom/order
 * 分页条件（OrderQuery）：storeId、platform、status（平台状态）、localStatus（处理状态）、fulfillment、keyword（平台订单号 / 买家 / 运单号 / 平台 SKU）、
 * country、beginDate、endDate（下单日期）、overdue（只看承诺发货时间已过且未发货的）
 */
export const pageEcomOrder = (params) => request.get('/ecom/order', { params })
/** 详情含 items（明细带映射物料） */
export const getEcomOrder = (id) => request.get(`/ecom/order/${id}`)
/** Excel 导入：{ rows: [{ rowNum, data: OrderImportRow }] }，一行一个订单行，同一订单按店铺编码 + 平台订单号合并 */
export const importEcomOrder = (data) => request.post('/ecom/order/import', data)
/** 修改收件信息、配送方式、备注（OrderEditRequest，未发货时） */
export const updateEcomOrder = (id, data) => request.put(`/ecom/order/${id}`, data)
/** 重新匹配 SKU → 物料（异常 / 待处理的订单） */
export const rematchEcomOrder = (id) => request.post(`/ecom/order/${id}/rematch`)
/** 批量出库：{ ids, warehouseId?, bizDate? }，返回失败的订单 { 平台订单号: 原因 }，全部成功时为空对象 */
export const outboundEcomOrders = (data) => request.post('/ecom/order/outbound', data)
/** 发货（ShipRequest）：carrier、trackingNo、shipTime、channelId、weight、shippingCost（本位币，为空时按渠道和重量估算） */
export const shipEcomOrder = (id, data) => request.post(`/ecom/order/${id}/ship`, data)
/** 取消：只能取消尚未出库的订单 */
export const cancelEcomOrder = (id) => request.post(`/ecom/order/${id}/cancel`)
