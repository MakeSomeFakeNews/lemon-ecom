import { h, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { t } from '@/locales'
import { owhProviders } from '@/api/ecom/owh'
import { OwhInboundStatus, OwhOutboundStatus, OwhReturnStatus, OwhTransferStatus } from '@/constants/enums'

/** 批量操作结果的提示键（与订单页 showBatchResult 配合使用） */
export const OWH_RESULT_KEYS = { done: 'ecomOwh.common.done', partial: 'ecomOwh.common.partial', title: 'ecomOwh.common.failTitle' }

/**
 * 同步结果提示：results 为 [{ name, count, message }]，全部成功时提示 doneKey（{ n } 为条数合计），有失败时列出失败的账号 / 单据
 */
export function showSyncResults(results, doneKey) {
  const list = results || []
  const failed = list.filter((r) => r.message)
  const n = list.reduce((s, r) => s + (r.count || 0), 0)
  if (!failed.length) {
    message.success(t(doneKey, { n }))
    return
  }
  Modal.warning({
    title: t('ecomOwh.common.failTitle'),
    width: 600,
    content: h(
      'ul',
      { style: 'max-height: 320px; margin: 0; padding-left: 20px; overflow: auto' },
      failed.map((r) => h('li', { style: 'margin-bottom: 4px' }, [h('b', r.name), ' ', r.message]))
    )
  })
}

/** 出库单可以同步：已推送、截单中、已出库（上次发货失败） */
export const canSyncOutbound = (r) => [OwhOutboundStatus.PUSHED, OwhOutboundStatus.INTERCEPTING, OwhOutboundStatus.OUTBOUND].includes(r?.status)
/** 可以取消：推送失败、已推送、截单中 */
export const canCancelOutbound = (r) => [OwhOutboundStatus.FAILED, OwhOutboundStatus.PUSHED, OwhOutboundStatus.INTERCEPTING].includes(r?.status)
/** 可以重推：推送失败且包裹仍待打单 */
export const canRepush = (r) => r?.status === OwhOutboundStatus.FAILED

/** 入库单可以同步：已推送、收货中，或已收货但 ERP 还没签收 */
export const canSyncInbound = (r) =>
  [OwhInboundStatus.PUSHED, OwhInboundStatus.RECEIVING].includes(r?.status) || (r?.status === OwhInboundStatus.RECEIVED && r?.erpReceived !== 1)
export const isInboundDraft = (r) => r?.status === OwhInboundStatus.DRAFT
/** 已推送且服务商还没有收货的入库单（可取消或标记已作废） */
export const isInboundPushedOnly = (r) => r?.status === OwhInboundStatus.PUSHED && !(Number(r?.receivedQty) > 0)

/** 各服务商的能力（CANCEL_INBOUND、CANCEL_RETURN 等），页面共用一次加载 */
const caps = ref({})
let capsLoading
export function useOwhCaps() {
  if (!capsLoading) {
    capsLoading = owhProviders().then((list) => {
      caps.value = Object.fromEntries((list || []).map((p) => [p.provider, new Set(p.capabilities || [])]))
    })
  }
  return (provider, cap) => !!caps.value[provider]?.has(cap)
}

/** 退件单可以同步：已推送，或已收货但 ERP 还没入库 */
export const canSyncReturn = (r) => r?.status === OwhReturnStatus.PUSHED || (r?.status === OwhReturnStatus.RECEIVED && r?.erpReceived !== 1)
export const isReturnDraft = (r) => r?.status === OwhReturnStatus.DRAFT
export const isReturnPushed = (r) => r?.status === OwhReturnStatus.PUSHED
/** 调拨单可以同步：调出仓处理中，或调入海外仓的在途单 */
export const canSyncTransfer = (r) => r?.status === OwhTransferStatus.OUT_PUSHED || (r?.status === OwhTransferStatus.IN_TRANSIT && !!r?.toMappingId)
/** 调入本地仓的在途单可以确认收货 */
export const canReceiveTransfer = (r) => r?.status === OwhTransferStatus.IN_TRANSIT && !r?.toMappingId
export const canCancelTransfer = (r) => [OwhTransferStatus.DRAFT, OwhTransferStatus.OUT_PUSHED].includes(r?.status)
