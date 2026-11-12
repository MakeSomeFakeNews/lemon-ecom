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
/** SPU 视图：店铺 + 父商品 ID 分组（storeId、keyword、mapped=false 只看有未配对变体的） */
export const spuListing = (params) => request.get('/ecom/listing/spu', { params })
/** 按款式批量映射预览：{ templateId, rule: StyleMatchRule, storeId, parentItemId, listingIds, prefix, suffix, includeMapped } → 配对结果（不保存） */
export const previewStyleMap = (data) => request.post('/ecom/listing/map-style/preview', data)
/** 批量映射为单品：[{ listingId, materialId, packQty }] → 映射数 */
export const mapListingBatch = (pairs) => request.post('/ecom/listing/map-batch', { pairs })
