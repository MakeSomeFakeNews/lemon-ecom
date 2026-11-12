import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 结算费用类型映射：params { current, size, platform, itemType, keyword } */
export const pageFeeMapping = (params) => request.get('/ecom/fee-mapping', { params })
export const addFeeMapping = (data) => request.post('/ecom/fee-mapping', data)
export const updateFeeMapping = (id, data) => request.put(`/ecom/fee-mapping/${id}`, data)
export const changeFeeMappingStatus = (id, status) => request.put(`/ecom/fee-mapping/${id}/status`, null, { params: { status } })
export const deleteFeeMapping = (id) => request.delete(`/ecom/fee-mapping/${ids(id)}`)
