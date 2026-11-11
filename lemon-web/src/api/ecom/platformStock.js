import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 平台仓库存快照分页：storeId、keyword（SKU / 物料）、diffOnly（只看有差异） */
export const pagePlatformStock = (params) => request.get('/ecom/platform-stock', { params })
/** 导入平台库存报告：{ rows: [{ rowNum, data }] } → { total, imported, errors } */
export const importPlatformStock = (data) => request.post('/ecom/platform-stock/import', data)
export const deletePlatformStock = (id) => request.delete(`/ecom/platform-stock/${ids(id)}`)
