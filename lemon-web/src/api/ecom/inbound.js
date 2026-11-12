import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 头程货件 /ecom/inbound：分页条件 storeId、status（InboundStatus）、keyword（单据号 / 货件号 / 运单号） */
export const pageEcomInbound = (params) => request.get('/ecom/inbound', { params })
/** 各状态的货件数（状态标签页）：{ 状态: 数量 }，条件 storeId、keyword */
export const ecomInboundStatusCounts = (params) => request.get('/ecom/inbound/status-counts', { params })
/** 详情含 items（unitLanded 为单件头程成本） */
export const getEcomInbound = (id) => request.get(`/ecom/inbound/${id}`)
/** 新增草稿（InboundRequest），返回 ID */
export const addEcomInbound = (data) => request.post('/ecom/inbound', data)
/** 修改草稿 */
export const updateEcomInbound = (id, data) => request.put(`/ecom/inbound/${id}`, data)
/** 已发运 / 已签收后补录物流信息与费用（仍提交完整的 InboundRequest，只有物流与费用字段生效） */
export const updateEcomInboundCosts = (id, data) => request.put(`/ecom/inbound/${id}/costs`, data)
export const deleteEcomInbound = (id) => request.delete(`/ecom/inbound/${ids(id)}`)
/** 提交发运：返回 true 表示进入审批，false 表示已直接发运（生成在途调拨） */
export const submitEcomInbound = (id) => request.post(`/ecom/inbound/${id}/submit`)
/** 签收：receiveDate（yyyy-MM-dd）为空取当天 */
export const receiveEcomInbound = (id, receiveDate) => request.post(`/ecom/inbound/${id}/receive`, null, { params: { receiveDate } })
/** 取消草稿 / 审批中的货件 */
export const cancelEcomInbound = (id) => request.post(`/ecom/inbound/${id}/cancel`)
/** 装箱（整体替换）：{ boxes: [{ length, width, height, weight, remark, items: [{ sellerSku, qty }] }] } */
export const saveEcomInboundBoxes = (id, boxes) => request.put(`/ecom/inbound/${id}/boxes`, { boxes })
