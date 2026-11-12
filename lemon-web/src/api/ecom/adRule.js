import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 广告规则分页：params { current, size, keyword, status } */
export const pageAdRules = (params) => request.get('/ecom/ad-rule', { params })
export const getAdRule = (id) => request.get(`/ecom/ad-rule/${id}`)
export const addAdRule = (data) => request.post('/ecom/ad-rule', data)
export const updateAdRule = (id, data) => request.put(`/ecom/ad-rule/${id}`, data)
export const deleteAdRule = (id) => request.delete(`/ecom/ad-rule/${ids(id)}`)
export const changeAdRuleStatus = (id, status) => request.put(`/ecom/ad-rule/${id}/status`, null, { params: { status } })
/** 预览建议（不保存） */
export const previewAdRule = (id) => request.get(`/ecom/ad-rule/${id}/preview`, { timeout: 60000 })
/** 立即执行，返回 { matched, applied, pending, failed, notified, skipped, message } */
export const runAdRule = (id) => request.post(`/ecom/ad-rule/${id}/run`, null, { timeout: 120000 })
/** 执行日志：params { current, size, ruleId, storeId, status, source, beginDate, endDate, keyword } */
export const pageAdRuleLogs = (params) => request.get('/ecom/ad-rule/logs', { params })
export const applyAdRuleLogs = (ids) => request.post('/ecom/ad-rule/logs/apply', { ids }, { timeout: 120000 })
export const markAdRuleLogsManual = (ids) => request.post('/ecom/ad-rule/logs/manual', { ids })
export const ignoreAdRuleLogs = (ids) => request.post('/ecom/ad-rule/logs/ignore', { ids })
