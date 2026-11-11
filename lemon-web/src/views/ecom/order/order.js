import dayjs from 'dayjs'
import { EcomLocalStatus } from '@/constants/enums'
import { money } from '@/utils/number'

/** 平台订单按处理状态可做的操作（与后端 EcomOrderService 的校验一致） */

/** 出库：待处理、异常 */
export const canOutbound = (o) => o.localStatus === EcomLocalStatus.PENDING || o.localStatus === EcomLocalStatus.EXCEPTION
/** 发货：已出库 */
export const canShip = (o) => o.localStatus === EcomLocalStatus.OUTBOUND
/** 重新匹配 SKU：异常 */
export const canRematch = (o) => o.localStatus === EcomLocalStatus.EXCEPTION
/** 取消：尚未出库（已出库的走退货退款，已关闭的不再取消） */
export const canCancel = (o) => canOutbound(o)
/** 修改收件信息、配送方式、备注：未发货、未关闭 */
export const canEdit = (o) => o.localStatus !== EcomLocalStatus.SHIPPED && o.localStatus !== EcomLocalStatus.CLOSED
/** 退货退款：未关闭（仅退款不要求已出库，退货退款由后端校验） */
export const canReturn = (o) => o.localStatus !== EcomLocalStatus.CLOSED
/** 承诺发货时间已过且还没有发货 */
export const isOverdue = (o) =>
  !!o.shipBy && o.localStatus !== EcomLocalStatus.SHIPPED && o.localStatus !== EcomLocalStatus.CLOSED && dayjs(o.shipBy).isBefore(dayjs())

/** 原币金额：币种 + 千分位金额，如 USD 12.50 */
export const fcMoney = (currency, n) => `${currency || ''} ${money(n)}`.trim()
/** 本位币金额 */
export const baseMoney = (n) => `¥${money(n)}`
