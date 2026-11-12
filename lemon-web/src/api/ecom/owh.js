import request from '@/utils/request'

/** 海外仓：账号与仓库映射、物流产品、库存、入库单、出库单、费用 */

// ---------------------------------------------------------------- 账号
/** 账号分页：keyword、provider、status */
export const pageOwhAccount = (params) => request.get('/ecom/owh/account', { params })
/** 启用的账号（选择用） */
export const owhAccountOptions = () => request.get('/ecom/owh/account/options')
/** 可对接的服务商：[{ provider, name, fields: [{ key, label, secret, required }], productionUrl, sandboxUrl, capabilities, docUrl }] */
export const owhProviders = () => request.get('/ecom/owh/account/providers')
/** 全部仓库映射（选择海外仓用） */
export const owhAllMappings = () => request.get('/ecom/owh/account/mappings')
export const getOwhAccount = (id) => request.get(`/ecom/owh/account/${id}`)
/** 新增 / 修改：auth 只提交填写了的授权字段，与已保存的合并 */
export const addOwhAccount = (data) => request.post('/ecom/owh/account', data)
export const updateOwhAccount = (id, data) => request.put(`/ecom/owh/account/${id}`, data)
export const changeOwhAccountStatus = (id, status) => request.put(`/ecom/owh/account/${id}/status`, null, { params: { status } })
export const clearOwhCredentials = (id) => request.delete(`/ecom/owh/account/${id}/credentials`)
export const deleteOwhAccount = (id) => request.delete(`/ecom/owh/account/${id}`)
/** 测试授权，返回服务商的仓库数 */
export const testOwhAccount = (id) => request.post(`/ecom/owh/account/${id}/test`)
/** 服务商的仓库列表：[{ code, name, country }] */
export const owhRemoteWarehouses = (id) => request.get(`/ecom/owh/account/${id}/remote-warehouses`)
export const owhMappings = (id) => request.get(`/ecom/owh/account/${id}/mappings`)
export const addOwhMapping = (data) => request.post('/ecom/owh/account/mapping', data)
export const updateOwhMapping = (id, data) => request.put(`/ecom/owh/account/mapping/${id}`, data)
export const deleteOwhMapping = (id) => request.delete(`/ecom/owh/account/mapping/${id}`)
export const owhProducts = (id) => request.get(`/ecom/owh/account/${id}/products`)
/** 从服务商拉取物流产品，返回产品数 */
export const syncOwhProducts = (id) => request.post(`/ecom/owh/account/${id}/products/sync`)
/** 物流产品对应的物流渠道：{ channelId, carrierName } */
export const updateOwhProduct = (productId, data) => request.put(`/ecom/owh/account/product/${productId}`, data)

// ---------------------------------------------------------------- 库存
/** 库存分页：accountId、warehouseId、keyword、diffOnly */
export const pageOwhStock = (params) => request.get('/ecom/owh/stock', { params })
export const owhStockSummary = (params) => request.get('/ecom/owh/stock/summary', { params })
/** 同步库存（accountId 为空时全部），返回 [{ name, count, message }] */
export const syncOwhStock = (accountId) => request.post('/ecom/owh/stock/sync', null, { params: { accountId } })
/** 按海外仓数量调整：{ ids, bizDate } → { inDocNo, outDocNo, lines } */
export const adjustOwhStock = (data) => request.post('/ecom/owh/stock/adjust', data)

// ---------------------------------------------------------------- 入库单
export const pageOwhInbound = (params) => request.get('/ecom/owh/inbound', { params })
export const owhInboundStatusCounts = (params) => request.get('/ecom/owh/inbound/status-counts', { params })
/** 可生成入库单的来源：[{ sourceType, sourceId, sourceNo, warehouseName, totalQty, bizDate, status, boxCount }] */
export const owhInboundSources = (keyword) => request.get('/ecom/owh/inbound/sources', { params: { keyword } })
export const getOwhInbound = (id) => request.get(`/ecom/owh/inbound/${id}`)
/** 生成：{ sourceType, sourceId } */
export const addOwhInbound = (data) => request.post('/ecom/owh/inbound', data)
/** 修改草稿：{ transportMode, carrier, trackingNo, etaDate, remark, boxes: [{ boxNo, length, width, height, weight }], lines: [{ boxNo, materialId, qty }] } */
export const updateOwhInbound = (id, data) => request.put(`/ecom/owh/inbound/${id}`, data)
export const pushOwhInbound = (id) => request.post(`/ecom/owh/inbound/${id}/push`)
/** 同步收货，返回 { id: 失败原因 } */
export const syncOwhInbound = (ids) => request.post('/ecom/owh/inbound/sync', { ids: [].concat(ids) })
export const cancelOwhInbound = (id) => request.post(`/ecom/owh/inbound/${id}/cancel`)

// ---------------------------------------------------------------- 出库单
export const pageOwhOutbound = (params) => request.get('/ecom/owh/outbound', { params })
/** 待推送的包裹 */
export const pageOwhPending = (params) => request.get('/ecom/owh/outbound/pending', { params })
export const owhOutboundStatusCounts = (params) => request.get('/ecom/owh/outbound/status-counts', { params })
export const getOwhOutbound = (id) => request.get(`/ecom/owh/outbound/${id}`)
export const owhOutboundItems = (id) => request.get(`/ecom/owh/outbound/${id}/items`)
/** 包裹的海外仓状态：[{ packageId, outboundId, outboundNo, status, owhOrderNo, trackingNo, errorMsg, accountName }] */
export const owhByPackages = (ids) => request.get('/ecom/owh/outbound/by-packages', { params: { ids: [].concat(ids).join(',') } })
export const owhByOrder = (orderId) => request.get(`/ecom/owh/outbound/by-order/${orderId}`)
/** 推送海外仓（包裹 ID）：{ ids, productCode } → { 包裹号: 失败原因 } */
export const pushOwhOutbound = (data) => request.post('/ecom/owh/outbound/push', data)
/** 同步出库状态（出库单 ID）→ { 出库单号: 失败原因 } */
export const syncOwhOutbound = (ids) => request.post('/ecom/owh/outbound/sync', { ids: [].concat(ids) })
/** 取消 / 截单：{ reason, cancelPackage } → CANCELLED / INTERCEPTING */
export const cancelOwhOutbound = (id, data) => request.post(`/ecom/owh/outbound/${id}/cancel`, data)

// ---------------------------------------------------------------- 费用
export const pageOwhFee = (params) => request.get('/ecom/owh/fee', { params })
/** 汇总：{ rows: [{ period, feeType, amount, cnt }], byType, total } */
export const owhFeeSummary = (params) => request.get('/ecom/owh/fee/summary', { params })
export const owhFeeAllocations = (id) => request.get(`/ecom/owh/fee/${id}/allocations`)
/** 同步费用：{ accountId, beginDate, endDate } → [{ name, count, message }] */
export const syncOwhFee = (data) => request.post('/ecom/owh/fee/sync', data)
/** 重新入账 → 入账成功的条数 */
export const repostOwhFee = (ids) => request.post('/ecom/owh/fee/repost', { ids: [].concat(ids) })

// ---------------------------------------------------------------- SKU 映射
/** 账号的 SKU 映射：[{ id, materialId, materialCode, materialName, owhSku, remark }] */
export const owhSkus = (accountId, keyword) => request.get(`/ecom/owh/account/${accountId}/skus`, { params: { keyword } })
/** 新增 / 修改：{ materialId, owhSku, remark } */
export const saveOwhSku = (accountId, data) => request.post(`/ecom/owh/account/${accountId}/skus`, data)
export const deleteOwhSku = (id) => request.delete(`/ecom/owh/account/sku/${id}`)

// ---------------------------------------------------------------- 退件
export const pageOwhReturn = (params) => request.get('/ecom/owh/return', { params })
export const owhReturnStatusCounts = (params) => request.get('/ecom/owh/return/status-counts', { params })
/** 可生成退件单的退货退款单：[{ id, docNo, platformOrderNo, storeName, warehouseName, reason, createTime }] */
export const owhReturnSources = (keyword) => request.get('/ecom/owh/return/sources', { params: { keyword } })
export const getOwhReturn = (id) => request.get(`/ecom/owh/return/${id}`)
/** 生成：{ ecomReturnId, carrier, trackingNo, remark } */
export const addOwhReturn = (data) => request.post('/ecom/owh/return', data)
export const pushOwhReturn = (id) => request.post(`/ecom/owh/return/${id}/push`)
export const syncOwhReturn = (ids) => request.post('/ecom/owh/return/sync', { ids: [].concat(ids) })
export const cancelOwhReturn = (id) => request.post(`/ecom/owh/return/${id}/cancel`)

// ---------------------------------------------------------------- 调拨
export const pageOwhTransfer = (params) => request.get('/ecom/owh/transfer', { params })
export const owhTransferStatusCounts = (params) => request.get('/ecom/owh/transfer/status-counts', { params })
export const getOwhTransfer = (id) => request.get(`/ecom/owh/transfer/${id}`)
/** 新增 / 修改：{ fromWarehouseId, toWarehouseId, productCode, remark, items: [{ materialId, qty }] } */
export const addOwhTransfer = (data) => request.post('/ecom/owh/transfer', data)
export const updateOwhTransfer = (id, data) => request.put(`/ecom/owh/transfer/${id}`, data)
/** 提交 → OUT_PUSHED / IN_TRANSIT */
export const submitOwhTransfer = (id) => request.post(`/ecom/owh/transfer/${id}/submit`)
export const syncOwhTransfer = (ids) => request.post('/ecom/owh/transfer/sync', { ids: [].concat(ids) })
/** 确认收货：{ receiveDate, items: [{ materialId, qty }]（实收，为空全部收到）, reason } */
export const receiveOwhTransfer = (id, data) => request.post(`/ecom/owh/transfer/${id}/receive`, data || {})
/** 取消 → CANCELLED（截单中时仍为 OUT_PUSHED） */
export const cancelOwhTransfer = (id) => request.post(`/ecom/owh/transfer/${id}/cancel`)

// ---------------------------------------------------------------- 推送后取消 / 标记已作废、收货差异
/** 标记已作废（服务商不支持接口取消，已在服务商后台作废）：{ reason } */
export const voidOwhInbound = (id, reason) => request.post(`/ecom/owh/inbound/${id}/void`, { reason })
export const voidOwhReturn = (id, reason) => request.post(`/ecom/owh/return/${id}/void`, { reason })
/** 收货差异分页：sourceType、status、warehouseId、keyword */
export const pageOwhDiff = (params) => request.get('/ecom/owh/diff', { params })
/** 某张单据的差异：sourceType（INBOUND / RETURN / TRANSFER）、sourceId */
export const owhDiffsBySource = (sourceType, sourceId) => request.get('/ecom/owh/diff/by-source', { params: { sourceType, sourceId } })
/** 处理差异：{ action: LOSS / FOUND / ACCEPTED, reason } */
export const handleOwhDiff = (id, data) => request.post(`/ecom/owh/diff/${id}/handle`, data)
