import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 回款记录：params { current, size, status, storeId, beginDate, endDate, keyword } */
export const pagePayout = (params) => request.get('/ecom/payout', { params })
/** 各状态的记录数与本位币金额：[{ status, cnt, expectedBase, amountBase }] */
export const payoutSummary = (params) => request.get('/ecom/payout/summary', { params })
/** 导入到账流水：{ rows, accountId, channel, storeId, windowDays, tolerance } → { total, imported, matched, diff, unmatched, skipped, errors } */
export const importPayouts = (data) => request.post('/ecom/payout/import', data)
/** 手工登记到账：{ payoutDate, amount, payCurrency, accountId, channel, bankRef, exchangeRate, remark } */
export const receivePayout = (id, data) => request.post(`/ecom/payout/${id}/receive`, data)
/** 手工匹配：未匹配的流水 id 并入未到账记录 pendingId */
export const matchPayout = (id, pendingId) => request.post(`/ecom/payout/${id}/match`, null, { params: { pendingId } })
export const unmatchPayout = (id) => request.post(`/ecom/payout/${id}/unmatch`)
export const deletePayout = (id) => request.delete(`/ecom/payout/${ids(id)}`)
