import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

// ---------------------------------------------------------------- 开发项目 /ecom/dev/project

/** 分页：status、keyword（编号 / 名称 / 卖点）、ownerId、platform、source、category */
export const pageDevProject = (params) => request.get('/ecom/dev/project', { params })
/** 看板：{ columns: [{ status, count, items }], limit } */
export const devBoard = (params) => request.get('/ecom/dev/project/board', { params })
/** 详情：含 samples、logs */
export const getDevProject = (id) => request.get(`/ecom/dev/project/${id}`)
export const addDevProject = (data) => request.post('/ecom/dev/project', data)
export const updateDevProject = (id, data) => request.put(`/ecom/dev/project/${id}`, data)
export const deleteDevProject = (id) => request.delete(`/ecom/dev/project/${ids(id)}`)
/** 手工流转 { status, remark } */
export const moveDevProject = (id, status, remark) => request.post(`/ecom/dev/project/${id}/move`, { status, remark })
/** 提交审批，返回是否发起了审批流程 */
export const submitDevProject = (id) => request.post(`/ecom/dev/project/${id}/submit`)
export const withdrawDevProject = (id) => request.post(`/ecom/dev/project/${id}/withdraw`)
export const approveDevProject = (id, remark) => request.post(`/ecom/dev/project/${id}/approve`, { remark })
export const rejectDevProject = (id, remark) => request.post(`/ecom/dev/project/${id}/reject`, { remark })
/** 生成物料 { code, unit, spec }，返回物料 ID */
export const generateDevMaterial = (id, data) => request.post(`/ecom/dev/project/${id}/material`, data || {})
export const uploadDevImage = (id, file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post(`/ecom/dev/project/${id}/images`, form)
}
export const deleteDevImage = (id, url) => request.delete(`/ecom/dev/project/${id}/images`, { params: { url } })
export const mainDevImage = (id, url) => request.put(`/ecom/dev/project/${id}/images/main`, null, { params: { url } })
export const addDevSample = (projectId, data) => request.post(`/ecom/dev/project/${projectId}/samples`, data)
export const updateDevSample = (sampleId, data) => request.put(`/ecom/dev/project/samples/${sampleId}`, data)
export const deleteDevSample = (sampleId) => request.delete(`/ecom/dev/project/samples/${sampleId}`)

// ---------------------------------------------------------------- 利润试算 /ecom/dev/profit

export const calcProfit = (data) => request.post('/ecom/dev/profit/calc', data)
/** 多平台对比 { base, targets: [{ platform, marketplace, category, salePrice, targetMargin, fulfillmentFee }] } */
export const compareProfit = (data) => request.post('/ecom/dev/profit/compare', data)
export const profitDefaults = (params) => request.get('/ecom/dev/profit/defaults', { params })
export const pageProfitScheme = (params) => request.get('/ecom/dev/profit/scheme', { params })
export const getProfitScheme = (id) => request.get(`/ecom/dev/profit/scheme/${id}`)
/** 保存方案 { name, params } */
export const saveProfitScheme = (data) => request.post('/ecom/dev/profit/scheme', data)
export const deleteProfitScheme = (id) => request.delete(`/ecom/dev/profit/scheme/${ids(id)}`)
export const listCommissionRate = (params) => request.get('/ecom/dev/profit/rate', { params })
export const addCommissionRate = (data) => request.post('/ecom/dev/profit/rate', data)
export const updateCommissionRate = (id, data) => request.put(`/ecom/dev/profit/rate/${id}`, data)
export const deleteCommissionRate = (id) => request.delete(`/ecom/dev/profit/rate/${ids(id)}`)

// ---------------------------------------------------------------- 1688 账号 /ecom/1688/account

export const listAli1688Account = () => request.get('/ecom/1688/account')
export const getAli1688Account = (id) => request.get(`/ecom/1688/account/${id}`)
export const addAli1688Account = (data) => request.post('/ecom/1688/account', data)
export const updateAli1688Account = (id, data) => request.put(`/ecom/1688/account/${id}`, data)
export const deleteAli1688Account = (id) => request.delete(`/ecom/1688/account/${ids(id)}`)
export const ali1688AuthorizeUrl = (id) => request.post(`/ecom/1688/account/${id}/authorize-url`)
export const refreshAli1688Token = (id) => request.post(`/ecom/1688/account/${id}/refresh`)
export const revokeAli1688 = (id) => request.post(`/ecom/1688/account/${id}/revoke`)

// ---------------------------------------------------------------- 1688 货源 /ecom/1688/offer

export const pageAli1688Offer = (params) => request.get('/ecom/1688/offer', { params })
export const ali1688OffersByMaterials = (materialIds) => request.get('/ecom/1688/offer/by-materials', { params: { materialIds: ids(materialIds) } })
export const parseAli1688Offer = (text) => request.get('/ecom/1688/offer/parse', { params: { text } })
export const previewAli1688Offer = (offer, accountId) => request.get('/ecom/1688/offer/preview', { params: { offer, accountId } })
export const linkAli1688Offer = (data) => request.post('/ecom/1688/offer', data)
export const updateAli1688Offer = (id, data) => request.put(`/ecom/1688/offer/${id}`, data)
export const deleteAli1688Offer = (id) => request.delete(`/ecom/1688/offer/${ids(id)}`)
/** 同步价格库存，ids 为空同步全部 */
export const syncAli1688Offer = (idList) => request.post('/ecom/1688/offer/sync', idList?.length ? { ids: idList } : {})
export const ackAli1688Offer = (idList) => request.post('/ecom/1688/offer/ack', { ids: idList })
export const ali1688PriceLogs = (id) => request.get(`/ecom/1688/offer/${id}/price-logs`)

// ---------------------------------------------------------------- 1688 采购单 /ecom/1688/order

export const pageAli1688Order = (params) => request.get('/ecom/1688/order', { params })
/** 详情 { order（含 items）, trace } */
export const getAli1688Order = (id) => request.get(`/ecom/1688/order/${id}`)
export const ali1688OrdersByPo = (scmOrderId) => request.get(`/ecom/1688/order/by-po/${scmOrderId}`)
export const previewAli1688Order = (scmOrderId) => request.get(`/ecom/1688/order/preview/${scmOrderId}`)
/** 一键下单 { scmOrderId, accountId, message }，返回 { created, failures } */
export const createAli1688Order = (data) => request.post('/ecom/1688/order', data)
export const syncAli1688Order = (id) => request.post(`/ecom/1688/order/${id}/sync`)
export const syncAli1688Orders = (idList) => request.post('/ecom/1688/order/sync', idList?.length ? { ids: idList } : {})
export const ali1688PayUrl = (idList) => request.post('/ecom/1688/order/pay-url', { ids: idList })
export const cancelAli1688Order = (id, remark) => request.post(`/ecom/1688/order/${id}/cancel`, { remark })
