import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 电商税率：params { current, size, country, taxType, status } */
export const pageTaxRate = (params) => request.get('/ecom/tax/rate', { params })
export const addTaxRate = (data) => request.post('/ecom/tax/rate', data)
export const updateTaxRate = (id, data) => request.put(`/ecom/tax/rate/${id}`, data)
export const changeTaxRateStatus = (id, status) => request.put(`/ecom/tax/rate/${id}/status`, null, { params: { status } })
export const deleteTaxRate = (id) => request.delete(`/ecom/tax/rate/${ids(id)}`)
/** 按当前税率重算一段下单时间内订单的税额与利润：params { beginDate, endDate, storeId } → 处理的订单数 */
export const applyTax = (params) => request.post('/ecom/tax/apply', null, { params })
/** VAT 汇总（按收件国家 × 月份）：params { beginDate, endDate, storeId } */
export const vatSummary = (params) => request.get('/ecom/tax/vat-summary', { params })
