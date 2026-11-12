import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 发货计划 /ecom/shipment-plan：分页条件 storeId、status（ShipmentPlanStatus）、keyword（计划号 / 平台 SKU） */
export const pageShipmentPlan = (params) => request.get('/ecom/shipment-plan', { params })
/** 详情含 items（shippedQty 为已生成货件的件数）、inbounds（生成的头程货件） */
export const getShipmentPlan = (id) => request.get(`/ecom/shipment-plan/${id}`)
/** 新增草稿：{ storeId, fromWarehouseId, toWarehouseId, planDate, remark, items: [{ listingId, qty, remark }] } */
export const addShipmentPlan = (data) => request.post('/ecom/shipment-plan', data)
/** 由补货建议生成（草稿）：{ storeId, toWarehouseId, fromWarehouseId, remark, items: [{ listingId, qty }] } → 计划 ID */
export const shipmentPlanFromReplenish = (data) => request.post('/ecom/shipment-plan/from-replenish', data)
export const updateShipmentPlan = (id, data) => request.put(`/ecom/shipment-plan/${id}`, data)
export const deleteShipmentPlan = (id) => request.delete(`/ecom/shipment-plan/${ids(id)}`)
export const confirmShipmentPlan = (id) => request.post(`/ecom/shipment-plan/${id}/confirm`)
export const unconfirmShipmentPlan = (id) => request.post(`/ecom/shipment-plan/${id}/unconfirm`)
export const cancelShipmentPlan = (id) => request.post(`/ecom/shipment-plan/${id}/cancel`)
/** 生成头程货件：{ shipments: [{ shipmentId, channelId, lines: [{ planItemId, qty }] }] }，为空时全部未生成的件数生成一张 → 货件 ID */
export const generateShipmentPlan = (id, data) => request.post(`/ecom/shipment-plan/${id}/generate`, data || {})
