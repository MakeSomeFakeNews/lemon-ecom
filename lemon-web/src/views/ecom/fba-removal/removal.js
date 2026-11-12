import { FbaRemovalStatus, FbaRemovalType } from '@/constants/enums'

/** 计划（草稿）或已提交：可修改、可提交 */
export const isEditable = (r) => r.status === FbaRemovalStatus.DRAFT || r.status === FbaRemovalStatus.SUBMITTED
/** 可取消：未取消、未收完、没有库存单据 */
export const canCancel = (r) => r.status !== FbaRemovalStatus.CANCELLED && r.status !== FbaRemovalStatus.RECEIVED && !r.stockDocNos
/** 可删除：草稿或已取消，且没有库存单据 */
export const canDelete = (r) => (r.status === FbaRemovalStatus.DRAFT || r.status === FbaRemovalStatus.CANCELLED) && !r.stockDocNos
const active = (r) => [FbaRemovalStatus.SUBMITTED, FbaRemovalStatus.PROCESSING, FbaRemovalStatus.COMPLETED].includes(r.status)
/** 可收货：退回类型，已提交 / 处理中 / 亚马逊已完成 */
export const canReceive = (r) => r.removalType === FbaRemovalType.RETURN && active(r)
/** 可弃置出库：弃置类型，已提交 / 处理中 / 亚马逊已完成 */
export const canDispose = (r) => r.removalType === FbaRemovalType.DISPOSAL && active(r)
