import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

// ---------------------------------------------------------------- 买家消息 /ecom/cs/thread

/** 会话列表：params { current, size, storeId, platform, status（CsThreadStatus）, overdue, unread, ownerId, keyword } */
export const pageCsThread = (params) => request.get('/ecom/cs/thread', { params })
/** 各状态的会话数：{ PENDING, REPLIED, CLOSED, OVERDUE, ALL }，条件同列表（不含状态） */
export const csThreadCounts = (params) => request.get('/ecom/cs/thread/counts', { params })
/** 会话详情：{ thread, messages, canSend, replyHours, tickets } */
export const getCsThread = (id) => request.get(`/ecom/cs/thread/${id}`)
/** 订单侧栏：{ order（含 items、packages，没有权限时为空）, recentOrders } */
export const getCsThreadOrder = (id) => request.get(`/ecom/cs/thread/${id}/order`)
/** 标记已读 */
export const readCsThread = (id) => request.post(`/ecom/cs/thread/${id}/read`)
/** 回复：{ content, templateId, recordOnly }，返回保存的消息（sendStatus 为 FAILED 时带 errorMessage） */
export const replyCsThread = (id, data) => request.post(`/ecom/cs/thread/${id}/reply`, data)
/** 重新发送失败的回复 */
export const resendCsMessage = (messageId) => request.post(`/ecom/cs/thread/message/${messageId}/resend`)
/** 修改会话：{ ownerId, tags, remark } */
export const updateCsThread = (id, data) => request.put(`/ecom/cs/thread/${id}`, data)
export const closeCsThread = (id) => request.post(`/ecom/cs/thread/${id}/close`)
export const reopenCsThread = (id) => request.post(`/ecom/cs/thread/${id}/reopen`)
/** 关联订单：{ platformOrderNo }，为空取消关联 */
export const linkCsThreadOrder = (id, platformOrderNo) => request.put(`/ecom/cs/thread/${id}/order`, { platformOrderNo })
/** 手工同步店铺的买家消息，返回 { fetched, created, updated, message } */
export const syncCsMessages = (storeId) => request.post(`/ecom/cs/thread/sync/${storeId}`)

// ---------------------------------------------------------------- 回复模板 /ecom/cs/template

/** 分页：params { current, size, language, platform, keyword, status } */
export const pageCsTemplate = (params) => request.get('/ecom/cs/template', { params })
/** 回复时选择：启用的、适用于 platform 的模板 */
export const csTemplateOptions = (platform) => request.get('/ecom/cs/template/options', { params: { platform } })
export const getCsTemplate = (id) => request.get(`/ecom/cs/template/${id}`)
export const addCsTemplate = (data) => request.post('/ecom/cs/template', data)
export const updateCsTemplate = (id, data) => request.put(`/ecom/cs/template/${id}`, data)
export const deleteCsTemplate = (id) => request.delete(`/ecom/cs/template/${ids(id)}`)
/** 替换变量：params { threadId | reviewId | orderId }，返回 { content, missing } */
export const renderCsTemplate = (id, params) => request.get(`/ecom/cs/template/${id}/render`, { params })

// ---------------------------------------------------------------- 评价 /ecom/cs/review

/** 评价列表：params { current, size, storeId, platform, sellerSku, minRating, maxRating, handleStatus, replied, beginDate, endDate, keyword } */
export const pageCsReview = (params) => request.get('/ecom/cs/review', { params })
export const getCsReview = (id) => request.get(`/ecom/cs/review/${id}`)
/** 概况 { total, avgRating, lowCount, lowRatePercent, pendingCount, repliedCount } */
export const csReviewSummary = (params) => request.get('/ecom/cs/review/summary', { params })
/** 按 SKU 统计 */
export const csReviewSkuStats = (params) => request.get('/ecom/cs/review/sku-stats', { params })
/** 按天的评分趋势（默认最近 30 天） */
export const csReviewTrend = (params) => request.get('/ecom/cs/review/trend', { params })
/** 导入评价：{ rows: [{ rowNum, data }] } */
export const importCsReviews = (data) => request.post('/ecom/cs/review/import', data)
export const syncCsReviews = (storeId) => request.post(`/ecom/cs/review/sync/${storeId}`)
/** 回复评价：{ content, recordOnly } */
export const replyCsReview = (id, data) => request.post(`/ecom/cs/review/${id}/reply`, data)
/** 处理：{ handleStatus, handlerId, handleNote } */
export const handleCsReview = (id, data) => request.put(`/ecom/cs/review/${id}/handle`, data)
export const deleteCsReview = (id) => request.delete(`/ecom/cs/review/${ids(id)}`)

export const pageCsFeedback = (params) => request.get('/ecom/cs/review/feedback', { params })
export const importCsFeedback = (data) => request.post('/ecom/cs/review/feedback/import', data)
export const replyCsFeedback = (id, data) => request.post(`/ecom/cs/review/feedback/${id}/reply`, data)
export const handleCsFeedback = (id, data) => request.put(`/ecom/cs/review/feedback/${id}/handle`, data)
export const deleteCsFeedback = (id) => request.delete(`/ecom/cs/review/feedback/${ids(id)}`)

/** 可请求评价的订单：params { current, size, platform, storeId, keyword, pendingOnly, status } */
export const pageCsReviewRequest = (params) => request.get('/ecom/cs/review/request', { params })
/** 请求评价：orderIds，返回每张订单的结果 [{ orderId, platformOrderNo, status, message }] */
export const requestCsReviews = (orderIds) => request.post('/ecom/cs/review/request', { orderIds })

// ---------------------------------------------------------------- 售后工单 /ecom/cs/ticket

/** 分页：params { current, size, storeId, ticketType, sourceType, keyword } */
export const pageCsTicket = (params) => request.get('/ecom/cs/ticket', { params })
export const csTicketsBySource = (sourceType, sourceId) => request.get('/ecom/cs/ticket/by-source', { params: { sourceType, sourceId } })
/**
 * 转售后：{ sourceType, sourceId, ticketType, orderId, reason, refundAmount, warehouseId, remark, items: [{ orderItemId, qty, refundAmount }] }
 */
export const addCsTicket = (data) => request.post('/ecom/cs/ticket', data)
