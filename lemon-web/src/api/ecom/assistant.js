import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

// ---------------------------------------------------------------- 运营日报

/** 日报分页：beginDate、endDate、storeId */
export const pageOpsReport = (params) => request.get('/ecom/ops-report', { params })
/** 详情 → { report, content } */
export const getOpsReport = (id) => request.get(`/ecom/ops-report/${id}`)
/** 立即生成：{ date, storeIds, ai } → 日报 ID */
export const generateOpsReport = (data) => request.post('/ecom/ops-report/generate', data, { timeout: 180000 })
/** 重新生成 AI 要点与建议 */
export const regenerateOpsAdvice = (id) => request.post(`/ecom/ops-report/${id}/advice`, null, { timeout: 180000 })
/** 推送给店铺范围一致的订阅人 → 人数 */
export const pushOpsReport = (id) => request.post(`/ecom/ops-report/${id}/push`)
export const deleteOpsReport = (id) => request.delete(`/ecom/ops-report/${ids(id)}`)

// ---------------------------------------------------------------- 订阅

export const listOpsReportSubs = () => request.get('/ecom/ops-report/sub')
/** 订阅选项 → { imChannels, jobKey, defaultCron } */
export const opsReportSubOptions = () => request.get('/ecom/ops-report/sub/options')
/** 保存订阅：{ userId, storeIds, channels, status, remark } */
export const saveOpsReportSub = (data) => request.put('/ecom/ops-report/sub', data)
export const deleteOpsReportSub = (id) => request.delete(`/ecom/ops-report/sub/${ids(id)}`)

// ---------------------------------------------------------------- 违禁词库

export const pageBannedWord = (params) => request.get('/ecom/banned-word', { params })
export const getBannedWord = (id) => request.get(`/ecom/banned-word/${id}`)
export const addBannedWord = (data) => request.post('/ecom/banned-word', data)
/** 批量添加：{ words（一行一个）, wordType, level, matchMode, platform, marketplace, suggestion } → 新增条数 */
export const batchAddBannedWord = (data) => request.post('/ecom/banned-word/batch', data)
export const updateBannedWord = (id, data) => request.put(`/ecom/banned-word/${id}`, data)
export const changeBannedWordStatus = (id, status) => request.put(`/ecom/banned-word/${id}/status`, null, { params: { status } })
export const deleteBannedWord = (id) => request.delete(`/ecom/banned-word/${ids(id)}`)
/** 适用于平台 / 站点的启用词（刊登编辑页实时检查） */
export const applicableBannedWords = (platform, marketplace) => request.get('/ecom/banned-word/applicable', { params: { platform, marketplace } })
/** 合规检查：{ platform, marketplace, title, bullets, description } → { hits, blockCount, warnCount } */
export const checkBannedWords = (data) => request.post('/ecom/banned-word/check', data)

// ---------------------------------------------------------------- AI 写 Listing

/** 生成 / 优化：{ itemId, title, bullets, description, parts, keywords, language, instruction } → { title, bullets, description, language, check } */
export const generateListingCopy = (data) => request.post('/ecom/ai/listing-copy', data, { timeout: 180000 })
/** 高转化搜索词：itemId、sameCategory、days → { rows } */
export const listingKeywords = (params) => request.get('/ecom/ai/listing-copy/keywords', { params })
