import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

// ---------------------------------------------------------------- 刊登中心

/** 刊登记录分页：storeId、platform、status、keyword（标题 / 物料 / 平台 SKU / 平台商品 ID）、draftId */
export const pagePublish = (params) => request.get('/ecom/publish', { params })
/** 各状态数量：{ DRAFT, PENDING, ..., ALL } */
export const publishCounts = (params) => request.get('/ecom/publish/counts', { params })
export const getPublishDraft = (id) => request.get(`/ecom/publish/draft/${id}`)
/** 从物料 / SPU 创建草稿：{ materialId, title, storeIds, templateIds: { 平台: 模板ID }, priceTemplateId } → 草稿 ID */
export const createPublishDraft = (data) => request.post('/ecom/publish/draft', data)
export const updatePublishDraft = (id, data) => request.put(`/ecom/publish/draft/${id}`, data)
/** 给草稿增加店铺：{ storeIds, templateIds, priceTemplateId } → 新增的记录数 */
export const addPublishStores = (id, data) => request.post(`/ecom/publish/draft/${id}/stores`, data)
/** 主内容应用到可修改的店铺记录：fields 为空全部 */
export const applyPublishMaster = (id, fields) => request.post(`/ecom/publish/draft/${id}/apply`, { fields })
export const materialPublishImages = (materialId) => request.get(`/ecom/publish/material-images/${materialId}`)
export const getPublishItem = (id) => request.get(`/ecom/publish/item/${id}`)
export const updatePublishItem = (id, data) => request.put(`/ecom/publish/item/${id}`, data)
export const recalcPublishItem = (id) => request.post(`/ecom/publish/item/${id}/recalc`)
export const deletePublishItem = (id) => request.delete(`/ecom/publish/item/${ids(id)}`)
export const publishLogs = (id) => request.get(`/ecom/publish/item/${id}/logs`)
/** 提交发布 / 重试 → { total, succeeded, errors: [{ id, name, message }] } */
export const submitPublish = (idList) => request.post('/ecom/publish/submit', { ids: [].concat(idList) })
export const offlinePublish = (idList) => request.post('/ecom/publish/offline', { ids: [].concat(idList) })
/** 店铺的刊登能力 → { connector, hasCredentials, capabilities, categoryRequired, note } */
/** 刊登设置 → { publicUrlConfigured, publicUrl } */
export const publishSettings = () => request.get('/ecom/publish/settings')
export const publishCapabilities = (storeId) => request.get('/ecom/publish/capabilities', { params: { storeId } })
export const publishCategories = (params) => request.get('/ecom/publish/categories', { params })
export const publishAttributes = (storeId, categoryId) => request.get('/ecom/publish/attributes', { params: { storeId, categoryId } })

// ---------------------------------------------------------------- 刊登模板

export const pagePublishTemplate = (params) => request.get('/ecom/publish-template', { params })
export const publishTemplateOptions = (platform) => request.get('/ecom/publish-template/options', { params: { platform } })
export const getPublishTemplate = (id) => request.get(`/ecom/publish-template/${id}`)
export const addPublishTemplate = (data) => request.post('/ecom/publish-template', data)
export const updatePublishTemplate = (id, data) => request.put(`/ecom/publish-template/${id}`, data)
export const deletePublishTemplate = (id) => request.delete(`/ecom/publish-template/${ids(id)}`)

// ---------------------------------------------------------------- 价格模板

export const pagePriceTemplate = (params) => request.get('/ecom/price-template', { params })
export const priceTemplateOptions = (platform) => request.get('/ecom/price-template/options', { params: { platform } })
export const getPriceTemplate = (id) => request.get(`/ecom/price-template/${id}`)
export const addPriceTemplate = (data) => request.post('/ecom/price-template', data)
export const updatePriceTemplate = (id, data) => request.put(`/ecom/price-template/${id}`, data)
export const deletePriceTemplate = (id) => request.delete(`/ecom/price-template/${ids(id)}`)
/** 定价试算：{ priceTemplateId, storeId | currency, materialId | cost, price } → { cost, rate, currency, suggestedPrice, price, profit, margin, fees } */
export const quotePrice = (data) => request.post('/ecom/price-template/quote', data)

// ---------------------------------------------------------------- 批量改价与改价历史

export const pagePriceChange = (params) => request.get('/ecom/price-change', { params })
/** 批量改价 / 改库存：{ listingIds, changeType: PRICE | STOCK, mode: FIXED | PERCENT | AMOUNT | FORMULA, value, priceTemplateId, push } */
export const previewPriceChange = (data) => request.post('/ecom/price-change/preview', data)
export const applyPriceChange = (data) => request.post('/ecom/price-change/apply', data)
/** 锁定 / 解除手工库存（自动库存回传跳过锁定的商品） */
export const lockListingStock = (idList, locked) => request.put('/ecom/price-change/stock-lock', { ids: [].concat(idList) }, { params: { locked } })

// ---------------------------------------------------------------- 自动调价

export const pageRepriceRule = (params) => request.get('/ecom/repricer', { params })
export const getRepriceRule = (id) => request.get(`/ecom/repricer/${id}`)
export const addRepriceRule = (data) => request.post('/ecom/repricer', data)
export const updateRepriceRule = (id, data) => request.put(`/ecom/repricer/${id}`, data)
export const changeRepriceRuleStatus = (id, status) => request.put(`/ecom/repricer/${id}/status`, null, { params: { status } })
export const deleteRepriceRule = (id) => request.delete(`/ecom/repricer/${ids(id)}`)
export const previewRepriceRule = (id) => request.get(`/ecom/repricer/${id}/preview`)
export const runRepriceRule = (id) => request.post(`/ecom/repricer/${id}/run`)
export const pageRepriceLog = (params) => request.get('/ecom/repricer/logs', { params })
export const applyRepriceLogs = (idList) => request.post('/ecom/repricer/logs/apply', { ids: [].concat(idList) })
export const ignoreRepriceLogs = (idList) => request.post('/ecom/repricer/logs/ignore', { ids: [].concat(idList) })

// ---------------------------------------------------------------- 竞品价格

/** 竞品价格快照：storeId、listingId、keyword、source、latestOnly（每个商品只看最近一条） */
export const pageCompetitorPrice = (params) => request.get('/ecom/competitor-price', { params })
export const addCompetitorPrice = (data) => request.post('/ecom/competitor-price', data)
export const importCompetitorPrice = (data) => request.post('/ecom/competitor-price/import', data)
/** 经平台接口拉取：{ storeId, listingIds } → { requested, saved } */
export const fetchCompetitorPrice = (data) => request.post('/ecom/competitor-price/fetch', data)
export const deleteCompetitorPrice = (id) => request.delete(`/ecom/competitor-price/${ids(id)}`)
