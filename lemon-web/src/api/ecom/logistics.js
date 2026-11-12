import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 物流渠道分页：keyword（编码 / 名称 / 物流商）、status、transportMode */
export const pageLogistics = (params) => request.get('/ecom/logistics', { params })
/** 启用的渠道（登录即可访问） */
export const logisticsOptions = () => request.get('/ecom/logistics/options')
/** 运费试算：目的国 + 重量（kg）→ [{ channelId, channelCode, channelName, provider, transportMode, fee, daysMin, daysMax, country }] */
export const estimateFreight = (country, weight) => request.get('/ecom/logistics/estimate', { params: { country, weight } })
/** 详情含 rates */
export const getLogistics = (id) => request.get(`/ecom/logistics/${id}`)
/** 新增 / 修改时整体提交 rates 数组 */
export const addLogistics = (data) => request.post('/ecom/logistics', data)
export const updateLogistics = (id, data) => request.put(`/ecom/logistics/${id}`, data)
export const changeLogisticsStatus = (id, status) => request.put(`/ecom/logistics/${id}/status`, null, { params: { status } })
export const deleteLogistics = (id) => request.delete(`/ecom/logistics/${ids(id)}`)
/** 可对接的物流商：[{ carrier, name, displayName, fields: [{ key, label, secret, required }], productionUrl, sandboxUrl, serviceHint, docUrl }] */
export const logisticsCarriers = () => request.get('/ecom/logistics/carriers')
/** 清除物流商授权 */
export const clearCarrierCredentials = (id) => request.delete(`/ecom/logistics/${id}/carrier-credentials`)
