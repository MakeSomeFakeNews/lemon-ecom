import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 在线商品分页：storeId、keyword（SKU / 商品 ID / 标题）、status、fulfillment、mapped（false 只看未映射）、materialId */
export const pageListing = (params) => request.get('/ecom/listing', { params })
export const getListing = (id) => request.get(`/ecom/listing/${id}`)
export const addListing = (data) => request.post('/ecom/listing', data)
export const updateListing = (id, data) => request.put(`/ecom/listing/${id}`, data)
/** 只修改物料映射：packQty 为空或不大于 0 时按 1 */
export const mapListing = (id, materialId, packQty) => request.put(`/ecom/listing/${id}/map`, null, { params: { materialId, packQty } })
export const deleteListing = (id) => request.delete(`/ecom/listing/${ids(id)}`)
/** 导入：{ rows: [{ rowNum, data }] } → { total, imported, errors } */
export const importListing = (data) => request.post('/ecom/listing/import', data)
