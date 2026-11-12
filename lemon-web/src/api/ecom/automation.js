import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 目录：触发器（含字段）、动作、各字段类型可用的运算符、公共变量 */
export const automationCatalog = () => request.get('/ecom/automation/catalog')
/** 内置规则模板 */
export const automationTemplates = () => request.get('/ecom/automation/templates')
/** 通知接收人可选的角色 */
export const automationRoles = () => request.get('/ecom/automation/roles')

/** 规则分页：params { current, size, keyword, trigger, status } */
export const pageAutoRules = (params) => request.get('/ecom/automation/rule', { params })
export const getAutoRule = (id) => request.get(`/ecom/automation/rule/${id}`)
export const addAutoRule = (data) => request.post('/ecom/automation/rule', data)
export const updateAutoRule = (id, data) => request.put(`/ecom/automation/rule/${id}`, data)
export const deleteAutoRule = (id) => request.delete(`/ecom/automation/rule/${ids(id)}`)
export const changeAutoRuleStatus = (id, status) => request.put(`/ecom/automation/rule/${id}/status`, null, { params: { status } })
export const copyAutoRule = (id) => request.post(`/ecom/automation/rule/${id}/copy`)
/** 一键从模板创建：data { storeIds } */
export const autoRuleFromTemplate = (code, data) => request.post(`/ecom/automation/rule/template/${code}`, data || {})
/** 试运行（规则可以是未保存的编辑内容），返回 { samples, summary } */
export const testAutoRule = (data) => request.post('/ecom/automation/rule/test', data, { timeout: 60000 })
/** 立即执行（扫描类触发器），返回 { events, executed, message } */
export const runAutoRule = (id) => request.post(`/ecom/automation/rule/${id}/run`, null, { timeout: 120000 })

/** 执行日志：params { current, size, ruleId, trigger, result, executed, storeId, beginDate, endDate, keyword } */
export const pageAutoLogs = (params) => request.get('/ecom/automation/log', { params })
export const getAutoLog = (id) => request.get(`/ecom/automation/log/${id}`)
export const retryAutoLog = (id) => request.post(`/ecom/automation/log/${id}/retry`, null, { timeout: 60000 })
/** 统计：params { days, ruleId }，返回 { results, days, rules, enabledRules } */
export const autoLogStats = (params) => request.get('/ecom/automation/log/stats', { params })

/** 待办提醒：params { current, size, mine, status, keyword } */
export const pageAutoTodos = (params) => request.get('/ecom/automation/todo', { params })
export const changeAutoTodoStatus = (ids, status) => request.put('/ecom/automation/todo/status', { ids, status })
