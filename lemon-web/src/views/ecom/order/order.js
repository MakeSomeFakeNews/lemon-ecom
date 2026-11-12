import { h } from 'vue'
import { Modal, message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { t } from '@/locales'
import { EcomLocalStatus, FulfillmentType } from '@/constants/enums'
import { money, symMoney } from '@/utils/number'

/** 平台订单按处理状态可做的操作（与后端 EcomOrderService / EcomOrderOpsService 的校验一致） */

const is = (o, ...statuses) => statuses.includes(o.localStatus)

/** 直接出库（不经过包裹，平台配送订单或简单情况）：待审核、异常、缺货 */
export const canOutbound = (o) => is(o, EcomLocalStatus.PENDING, EcomLocalStatus.EXCEPTION, EcomLocalStatus.OUT_OF_STOCK) && !o.packageCount
/** 发货（直接出库后填写物流）：已出库 */
export const canShip = (o) => is(o, EcomLocalStatus.OUTBOUND)
/** 重新匹配 SKU：异常 */
export const canRematch = (o) => is(o, EcomLocalStatus.EXCEPTION)
/** 审核：待审核、异常的自发货订单 */
export const canAudit = (o) => is(o, EcomLocalStatus.PENDING, EcomLocalStatus.EXCEPTION) && o.fulfillment === FulfillmentType.SELLER
/** 重新检查库存：缺货、部分商品未分配包裹 */
export const canRecheck = (o) => is(o, EcomLocalStatus.OUT_OF_STOCK) || (is(o, EcomLocalStatus.AUDITED, EcomLocalStatus.PARTIAL) && !!o.exception)
/** 挂起：发货前 */
export const canHold = (o) =>
  is(o, EcomLocalStatus.PENDING, EcomLocalStatus.EXCEPTION, EcomLocalStatus.OUT_OF_STOCK, EcomLocalStatus.AUDITED, EcomLocalStatus.LABELED)
export const canUnhold = (o) => is(o, EcomLocalStatus.ON_HOLD)
/** 拆分：待打单、缺货、部分发货 */
export const canSplit = (o) => is(o, EcomLocalStatus.AUDITED, EcomLocalStatus.OUT_OF_STOCK, EcomLocalStatus.PARTIAL)
/** 取消：尚未出库、没有已打单的包裹（后端校验）；已出库的走退货退款 */
export const canCancel = (o) =>
  is(o, EcomLocalStatus.PENDING, EcomLocalStatus.EXCEPTION, EcomLocalStatus.OUT_OF_STOCK, EcomLocalStatus.AUDITED, EcomLocalStatus.ON_HOLD)
/** 修改收件信息、配送方式、重量、备注：未打单、未发货、未关闭 */
export const canEdit = (o) => !is(o, EcomLocalStatus.SHIPPED, EcomLocalStatus.CLOSED, EcomLocalStatus.PARTIAL, EcomLocalStatus.LABELED)
/** 退货退款：未关闭（仅退款不要求已出库，退货退款由后端校验） */
export const canReturn = (o) => !is(o, EcomLocalStatus.CLOSED)
/** 重新回传发货：已发货 */
export const canShipConfirm = (o) => is(o, EcomLocalStatus.SHIPPED, EcomLocalStatus.PARTIAL) && !!o.trackingNo
/** 承诺发货时间已过且还没有发货 */
export const isOverdue = (o) => !!o.shipBy && !is(o, EcomLocalStatus.SHIPPED, EcomLocalStatus.CLOSED) && dayjs(o.shipBy).isBefore(dayjs())
/** 距承诺发货截止不到 24 小时（还没有逾期）时返回剩余小时数 */
export function dueHours(o) {
  if (!o.shipBy || is(o, EcomLocalStatus.SHIPPED, EcomLocalStatus.CLOSED)) return null
  const h = dayjs(o.shipBy).diff(dayjs(), 'hour', true)
  return h >= 0 && h < 24 ? Math.max(1, Math.ceil(h)) : null
}

/**
 * 处理状态标签页：每个标签对应若干处理状态（GET /ecom/order?localStatuses=），数量由 /ecom/order/status-counts 汇总
 */
export const ORDER_TABS = [
  { key: 'pending', label: 'ecomOrder.ops.tabPending', statuses: [EcomLocalStatus.PENDING] },
  { key: 'outOfStock', label: 'ecomOrder.ops.tabOutOfStock', statuses: [EcomLocalStatus.OUT_OF_STOCK] },
  { key: 'toLabel', label: 'ecomOrder.ops.tabToLabel', statuses: [EcomLocalStatus.AUDITED] },
  { key: 'toShip', label: 'ecomOrder.ops.tabToShip', statuses: [EcomLocalStatus.LABELED, EcomLocalStatus.OUTBOUND, EcomLocalStatus.PARTIAL] },
  { key: 'shipped', label: 'ecomOrder.ops.tabShipped', statuses: [EcomLocalStatus.SHIPPED] },
  { key: 'abnormal', label: 'ecomOrder.ops.tabAbnormal', statuses: [EcomLocalStatus.EXCEPTION, EcomLocalStatus.ON_HOLD] },
  { key: 'all', label: 'ecomOrder.ops.tabAll', statuses: [] }
]

/** 处理状态所在的标签页（首页待办带 ?localStatus= 进入时定位） */
export const tabOfStatus = (status) => ORDER_TABS.find((tab) => tab.statuses.includes(status))?.key || 'all'

/**
 * 批量操作的结果：后端返回 { 单号: 原因 }，全部成功时提示，有失败时弹窗列出
 *
 * @param {object} result  接口结果
 * @param {number} total   提交的数量
 * @param {object} keys    { done: 全部成功的消息键, partial: 部分成功的消息键, title: 弹窗标题的消息键 }
 */
export function showBatchResult(result, total, keys) {
  const failed = Object.entries(result || {})
  const ok = total - failed.length
  if (!failed.length) {
    message.success(t(keys.done, { n: ok }))
    return
  }
  Modal.warning({
    title: t(keys.title),
    width: 600,
    content: h('div', [
      h('div', { style: 'margin-bottom: 8px' }, t(keys.partial, { ok, fail: failed.length })),
      h(
        'ul',
        { style: 'max-height: 320px; margin: 0; padding-left: 20px; overflow: auto' },
        failed.map(([no, reason]) => h('li', { style: 'margin-bottom: 4px' }, [h('b', no), ' ', reason]))
      )
    ])
  })
}

export const ORDER_RESULT_KEYS = { done: 'ecomOrder.ops.done', partial: 'ecomOrder.ops.partial', title: 'ecomOrder.ops.failTitle' }

/** 原币金额：币种 + 千分位金额，如 USD 12.50 */
export const fcMoney = (currency, n) => `${currency || ''} ${money(n)}`.trim()
/** 本位币金额 */
export const baseMoney = (n) => `${symMoney(n)}`
