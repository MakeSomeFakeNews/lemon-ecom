import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 采购计划 /ecom/purchase-plan：分页条件 status（PurchasePlanStatus）、keyword（计划单号 / 采购订单号 / 物料） */
export const pagePurchasePlan = (params) => request.get('/ecom/purchase-plan', { params })
/** 详情含 items（arrivedQty 为生成的采购订单中本物料的已入库数量，orderStatus 为空表示订单已删除） */
export const getPurchasePlan = (id) => request.get(`/ecom/purchase-plan/${id}`)
/** 由补货建议生成：{ warehouseId, remark, w7, w14, w30, w60, items: [{ materialId, qty }] }，返回计划 ID */
export const purchasePlanFromReplenish = (data) => request.post('/ecom/purchase-plan/from-replenish', data)
export const addPurchasePlan = (data) => request.post('/ecom/purchase-plan', data)
export const updatePurchasePlan = (id, data) => request.put(`/ecom/purchase-plan/${id}`, data)
export const deletePurchasePlan = (id) => request.delete(`/ecom/purchase-plan/${ids(id)}`)
export const approvePurchasePlan = (id) => request.post(`/ecom/purchase-plan/${id}/approve`)
export const unapprovePurchasePlan = (id) => request.post(`/ecom/purchase-plan/${id}/unapprove`)
export const closePurchasePlan = (id) => request.post(`/ecom/purchase-plan/${id}/close`)
/** 按供应商拆单生成采购订单（草稿），返回订单号数组 */
export const generatePurchaseOrders = (id) => request.post(`/ecom/purchase-plan/${id}/orders`)
