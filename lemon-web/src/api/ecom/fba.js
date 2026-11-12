import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

// ---------------------------------------------------------------- 库存健康 /ecom/fba/health

/** 库存健康分页：storeId、keyword、advice、excessOnly、agedOnly、excessDays */
export const pageFbaHealth = (params) => request.get('/ecom/fba/health', { params })
export const fbaHealthSummary = (params) => request.get('/ecom/fba/health/summary', { params })
/** 从亚马逊同步（创建库存规划、FBA 库存报告任务）→ 任务 ID 列表 */
export const syncFbaHealth = (storeId) => request.post(`/ecom/fba/health/sync/${storeId}`)
/** 按头程签收先进先出推算库龄 → 写入条数 */
export const rebuildFbaHealth = (storeId) => request.post('/ecom/fba/health/rebuild', null, { params: { storeId } })
export const listStorageRates = (marketplace) => request.get('/ecom/fba/health/rates', { params: { marketplace } })
export const addStorageRate = (data) => request.post('/ecom/fba/health/rates', data)
export const updateStorageRate = (id, data) => request.put(`/ecom/fba/health/rates/${id}`, data)
export const deleteStorageRate = (id) => request.delete(`/ecom/fba/health/rates/${ids(id)}`)

// ---------------------------------------------------------------- 移除订单 /ecom/fba/removal

export const pageRemoval = (params) => request.get('/ecom/fba/removal', { params })
/** 各状态的移除订单数（状态标签页）：{ 状态: 数量 }，条件 storeId、removalType、keyword */
export const removalStatusCounts = (params) => request.get('/ecom/fba/removal/status-counts', { params })
export const getRemoval = (id) => request.get(`/ecom/fba/removal/${id}`)
export const addRemoval = (data) => request.post('/ecom/fba/removal', data)
export const updateRemoval = (id, data) => request.put(`/ecom/fba/removal/${id}`, data)
export const deleteRemoval = (id) => request.delete(`/ecom/fba/removal/${ids(id)}`)
export const submitRemoval = (id, platformOrderId) => request.post(`/ecom/fba/removal/${id}/submit`, { platformOrderId })
export const cancelRemoval = (id) => request.post(`/ecom/fba/removal/${id}/cancel`)
/** 本地仓收货：{ receiveDate, warehouseId, defectiveWarehouseId, carrier, trackingNo, lines: [{ itemId, goodQty, defectiveQty }] } → 调拨单号 */
export const receiveRemoval = (id, data) => request.post(`/ecom/fba/removal/${id}/receive`, data)
export const disposeRemoval = (id, date) => request.post(`/ecom/fba/removal/${id}/dispose`, null, { params: { date } })
export const syncRemoval = (storeId) => request.post(`/ecom/fba/removal/sync/${storeId}`)

// ---------------------------------------------------------------- 索赔 /ecom/fba/claim

export const pageClaim = (params) => request.get('/ecom/fba/claim', { params })
export const claimSummary = (params) => request.get('/ecom/fba/claim/summary', { params })
export const detectClaims = (storeId) => request.post('/ecom/fba/claim/detect', null, { params: { storeId } })
export const addClaim = (data) => request.post('/ecom/fba/claim', data)
export const updateClaim = (id, data) => request.put(`/ecom/fba/claim/${id}`, data)
export const deleteClaim = (id) => request.delete(`/ecom/fba/claim/${ids(id)}`)
export const syncClaims = (storeId) => request.post(`/ecom/fba/claim/sync/${storeId}`)
export const pageLedger = (params) => request.get('/ecom/fba/claim/ledger', { params })
export const pageReimbursement = (params) => request.get('/ecom/fba/claim/reimbursement', { params })
export const matchReimbursements = (storeId) => request.post('/ecom/fba/claim/reimbursement/match', null, { params: { storeId } })
export const pageReceipt = (params) => request.get('/ecom/fba/claim/receipt', { params })
export const saveReceipt = (data) => request.put('/ecom/fba/claim/receipt', data)
export const syncReceipt = (inboundId) => request.post(`/ecom/fba/claim/receipt/sync/${inboundId}`)

// ---------------------------------------------------------------- 费用核查 /ecom/fba/fee

/** 费用核查：storeId、beginDate、endDate、overOnly → { rows, skuCount, overSkus, missingDims, overchargeBase, missingRates } */
export const feeAudit = (params) => request.get('/ecom/fba/fee/audit', { params })
export const auditClaims = (data) => request.post('/ecom/fba/fee/audit/claims', data)
export const pageDims = (params) => request.get('/ecom/fba/fee/dims', { params })
export const saveDim = (data) => request.put('/ecom/fba/fee/dims', data)
export const deleteDim = (id) => request.delete(`/ecom/fba/fee/dims/${ids(id)}`)
export const syncDims = (storeId) => request.post(`/ecom/fba/fee/dims/sync/${storeId}`)
export const listTiers = (marketplace) => request.get('/ecom/fba/fee/tiers', { params: { marketplace } })
export const addTier = (data) => request.post('/ecom/fba/fee/tiers', data)
export const updateTier = (id, data) => request.put(`/ecom/fba/fee/tiers/${id}`, data)
export const deleteTier = (id) => request.delete(`/ecom/fba/fee/tiers/${ids(id)}`)
/** 配送费试算（cm / kg）：marketplace、length、width、height、weight */
export const quoteFee = (params) => request.get('/ecom/fba/fee/quote', { params })

// ---------------------------------------------------------------- 亚马逊报告 /ecom/amz-report

export const pageAmzReport = (params) => request.get('/ecom/amz-report', { params })
export const createAmzReport = (data) => request.post('/ecom/amz-report', data)
/** 上传报告文件：{ storeId, reportType, content, snapshotDate } */
export const uploadAmzReport = (data) => request.post('/ecom/amz-report/upload', data)
export const pollAmzReport = (id) => request.post(`/ecom/amz-report/${id}/poll`)
export const deleteAmzReport = (id) => request.delete(`/ecom/amz-report/${ids(id)}`)
