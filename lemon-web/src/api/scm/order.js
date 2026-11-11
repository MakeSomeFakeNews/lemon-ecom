import request from '@/utils/request'

/** 采购 / 销售订单：kind = purchase | sales */
export const pageOrder = (kind, params) => request.get(`/scm/${kind}/order`, { params })
export const getOrder = (kind, id) => request.get(`/scm/${kind}/order/${id}`)
/** 返回新订单 ID */
export const addOrder = (kind, data) => request.post(`/scm/${kind}/order`, data)
export const updateOrder = (kind, id, data) => request.put(`/scm/${kind}/order/${id}`, data)
/** 返回 true 已发起审批流程，false 已直接审核 */
export const approveOrder = (kind, id) => request.post(`/scm/${kind}/order/${id}/approve`)
export const withdrawOrder = (kind, id) => request.post(`/scm/${kind}/order/${id}/withdraw`)
export const unapproveOrder = (kind, id) => request.post(`/scm/${kind}/order/${id}/unapprove`)
export const closeOrder = (kind, id) => request.post(`/scm/${kind}/order/${id}/close`)
/** 修改销售订单的业务员 */
export const changeSalesman = (id, salesmanId) => request.put(`/scm/sales/order/${id}/salesman`, { salesmanId })
export const deleteOrder = (kind, ids) => request.delete(`/scm/${kind}/order/${[].concat(ids).join(',')}`)
/** 订单变更：data { reason, lines: [{ action: MODIFY | REMOVE | ADD, orderItemId, materialId, qty, price, taxRate, deliveryDate }] } */
export const changeOrder = (kind, id, data) => request.post(`/scm/${kind}/order/${id}/change`, data)
export const orderChanges = (kind, id) => request.get(`/scm/${kind}/order/${id}/changes`)
