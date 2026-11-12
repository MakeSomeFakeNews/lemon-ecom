import { ListingComposeType, ShipmentPlanStatus } from '@/constants/enums'
import { qty } from '@/utils/number'

/** 发货计划按状态可做的操作（与后端 EcomShipmentPlanService 的校验一致） */
export const isDraft = (p) => p.status === ShipmentPlanStatus.DRAFT
export const isConfirmed = (p) => p.status === ShipmentPlanStatus.CONFIRMED
/** 取消、退回草稿：没有未取消的货件 */
export const noShipped = (p) => !(Number(p.shippedQty) > 0)
export const canCancel = (p) => (isDraft(p) || isConfirmed(p)) && noShipped(p)
export const canDelete = (p) => isDraft(p) || p.status === ShipmentPlanStatus.CANCELLED

/** 计划明细未生成货件的件数 */
export const remainingOf = (i) => Math.max(0, Number(i.qty || 0) - Number(i.shippedQty || 0))

/** 在线商品的组成：组合显示「组合」，单品显示 物料编码 × 多件装 */
export const compositionText = (i, bundleLabel) =>
  i.composeType === ListingComposeType.BUNDLE ? bundleLabel : i.materialCode ? `${i.materialCode}${Number(i.packQty) !== 1 ? ` × ${qty(i.packQty)}` : ''}` : '-'
