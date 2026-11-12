import request from '@/utils/request'

/** SKU 配对中心 /ecom/pairing */

/** 物料及其在线商品数、组合数、店铺数：keyword、pairedOnly */
export const pagePairingMaterials = (params) => request.get('/ecom/pairing/materials', { params })
/** 物料在各店铺、各平台的在线商品（单品映射与含它的组合，组合的 packQty 为用量） */
export const listingsOfMaterial = (materialId) => request.get(`/ecom/pairing/materials/${materialId}/listings`)
/** 未配对的平台 SKU（storeId、keyword），每条带 suggestion（推荐物料，可能为空） */
export const pageUnpaired = (params) => request.get('/ecom/pairing/unpaired', { params })
/** 确认配对：[{ listingId, materialId, packQty }] */
export const confirmPairs = (pairs) => request.post('/ecom/pairing/confirm', { pairs })
/** 解除配对 */
export const unpairListings = (ids) => request.post('/ecom/pairing/unpair', { ids: [].concat(ids) })
/** 导入配对：{ rows: [{ rowNum, data: { storeCode, sellerSku, materialCode, qty } }] } */
export const importPairs = (data) => request.post('/ecom/pairing/import', data)
/** 重新匹配异常订单 → { matched, remaining } */
export const rematchExceptions = (storeId) => request.post('/ecom/pairing/rematch', null, { params: { storeId } })
