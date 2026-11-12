import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 审单规则 /ecom/order-rule：分页 keyword、status；多值条件（storeIds、platforms、countries、skus、materialIds）为逗号分隔的字符串 */
export const pageOrderRule = (params) => request.get('/ecom/order-rule', { params })
export const getOrderRule = (id) => request.get(`/ecom/order-rule/${id}`)
export const addOrderRule = (data) => request.post('/ecom/order-rule', data)
export const updateOrderRule = (id, data) => request.put(`/ecom/order-rule/${id}`, data)
export const changeOrderRuleStatus = (id, status) => request.put(`/ecom/order-rule/${id}/status`, null, { params: { status } })
export const deleteOrderRule = (id) => request.delete(`/ecom/order-rule/${ids(id)}`)
