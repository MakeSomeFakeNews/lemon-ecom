import { PurchasePlanStatus } from '@/constants/enums'
import { nz, round2 } from '@/utils/number'

/** 采购计划按状态可做的操作（与后端 EcomPurchasePlanService 的校验一致） */

/** 修改、删除、审核：草稿 */
export const isDraft = (p) => p.status === PurchasePlanStatus.DRAFT
/** 反审核：已审核且没有已生成的采购订单 */
export const canUnapprove = (p) => p.status === PurchasePlanStatus.APPROVED && !nz(p.orderedQty)
/** 生成采购订单：已审核、已下单（还有没下单的明细时） */
export const canGenerate = (p) =>
  (p.status === PurchasePlanStatus.APPROVED || p.status === PurchasePlanStatus.ORDERED) && nz(p.orderedQty) < nz(p.totalQty)
/** 关闭：已审核、已下单 */
export const canClose = (p) => p.status === PurchasePlanStatus.APPROVED || p.status === PurchasePlanStatus.ORDERED

/** 到货进度（%） */
export const progressOf = (p) => (nz(p.totalQty) > 0 ? Math.min(100, Math.round((nz(p.arrivedQty) / nz(p.totalQty)) * 100)) : 0)
/** 明细金额（单价为空时为空） */
export const amountOf = (i) => (i.price == null || i.price === '' ? null : round2(nz(i.qty) * nz(i.price)))
