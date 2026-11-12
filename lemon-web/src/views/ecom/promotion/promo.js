import { EcomPromoPushStatus, EcomPromoStatus } from '@/constants/enums'

/** 促销页面共用：状态判断 */
export const isDraft = (p) => p?.status === EcomPromoStatus.DRAFT
export const isActive = (p) => p?.status === EcomPromoStatus.ACTIVE
export const isCancelled = (p) => p?.status === EcomPromoStatus.CANCELLED
export const isPushed = (p) => p?.pushStatus === EcomPromoPushStatus.PUSHED
/** 可修改：未取消、未推送平台 */
export const canEdit = (p) => !isCancelled(p) && !isPushed(p)
/** 可删除：草稿或已取消 */
export const canDelete = (p) => isDraft(p) || isCancelled(p)
/** 可推送：已生效、店铺平台支持 */
export const canPush = (p) => isActive(p) && !!p?.canPush

/** 活动时间：MM-DD HH:mm ~ MM-DD HH:mm（同一年省略年份） */
export function periodText(p) {
  if (!p?.startTime) return '-'
  const s = String(p.startTime).slice(0, 16)
  const e = String(p.endTime).slice(0, 16)
  return s.slice(0, 4) === e.slice(0, 4) ? `${s} ~ ${e.slice(5)}` : `${s} ~ ${e}`
}
