import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 广告日报：params { current, size, storeId, beginDate, endDate, keyword } */
export const pageAd = (params) => request.get('/ecom/ad', { params })
/** 汇总：params { storeId, beginDate, endDate, groupBy: store | campaign | sku }，返回 AdRow 数组 */
export const adSummary = (params) => request.get('/ecom/ad/summary', { params })
/** 导入广告报告：{ rows: [{ rowNum, data }] } → { total, imported, errors } */
export const importAds = (data) => request.post('/ecom/ad/import', data)
export const deleteAd = (id) => request.delete(`/ecom/ad/${ids(id)}`)
