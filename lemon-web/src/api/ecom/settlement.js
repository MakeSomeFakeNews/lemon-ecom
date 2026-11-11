import request from '@/utils/request'

const ids = (v) => [].concat(v).join(',')

/** 平台结算单：params { current, size, storeId, status, keyword } */
export const pageSettlement = (params) => request.get('/ecom/settlement', { params })
/** 详情（含明细 items） */
export const getSettlement = (id) => request.get(`/ecom/settlement/${id}`)
/** 导入结算报告：{ rows: [{ rowNum, data }] } → { total, imported, errors }，同一店铺 + 结算编号的行合并为一张结算单 */
export const importSettlements = (data) => request.post('/ecom/settlement/import', data)
/** 确认：佣金 / 配送费 / 退款回写订单；accountId 不为空时按净额生成收款单核销店铺客户的应收 */
export const confirmSettlement = (id, accountId) => request.post(`/ecom/settlement/${id}/confirm`, null, { params: { accountId } })
export const deleteSettlement = (id) => request.delete(`/ecom/settlement/${ids(id)}`)
