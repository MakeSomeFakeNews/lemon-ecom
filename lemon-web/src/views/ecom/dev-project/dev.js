import { DevProjectStatus } from '@/constants/enums'

/** 手工流转允许的目标状态（与后端 DevProjectStatus.MOVES 一致） */
export const MOVES = {
  [DevProjectStatus.PENDING_RESEARCH]: [DevProjectStatus.RESEARCHING, DevProjectStatus.ABANDONED],
  [DevProjectStatus.RESEARCHING]: [DevProjectStatus.PENDING_RESEARCH, DevProjectStatus.SAMPLING, DevProjectStatus.ABANDONED],
  [DevProjectStatus.SAMPLING]: [DevProjectStatus.RESEARCHING, DevProjectStatus.ABANDONED],
  [DevProjectStatus.REJECTED]: [DevProjectStatus.RESEARCHING, DevProjectStatus.SAMPLING, DevProjectStatus.ABANDONED],
  [DevProjectStatus.ABANDONED]: [DevProjectStatus.PENDING_RESEARCH, DevProjectStatus.RESEARCHING]
}

/** 看板列顺序（与状态枚举一致） */
export const BOARD_COLUMNS = Object.values(DevProjectStatus)

const EDITABLE = [DevProjectStatus.PENDING_RESEARCH, DevProjectStatus.RESEARCHING, DevProjectStatus.SAMPLING, DevProjectStatus.REJECTED]
const SUBMITTABLE = [DevProjectStatus.RESEARCHING, DevProjectStatus.SAMPLING, DevProjectStatus.REJECTED]
const DELETABLE = [DevProjectStatus.PENDING_RESEARCH, DevProjectStatus.REJECTED, DevProjectStatus.ABANDONED]

export const canMove = (from, to) => (MOVES[from] || []).includes(to)
export const isEditable = (p) => EDITABLE.includes(p?.status)
export const canSubmit = (p) => SUBMITTABLE.includes(p?.status)
export const isPending = (p) => p?.status === DevProjectStatus.PENDING_APPROVAL
/** 手工审批：待审批且没有发起审批流程 */
export const canManualApprove = (p) => isPending(p) && !p.flowInstanceId
export const canDelete = (p) => DELETABLE.includes(p?.status) && !p.materialId
export const canGenerateMaterial = (p) => p?.status === DevProjectStatus.APPROVED && !p.materialName

/** 图片地址数组 */
export function imagesOf(p) {
  if (!p?.images) return []
  try {
    const list = JSON.parse(p.images)
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

/** 包装尺寸文字 */
export const sizeText = (p) => (p?.lengthCm != null && p?.widthCm != null && p?.heightCm != null ? `${Number(p.lengthCm)} × ${Number(p.widthCm)} × ${Number(p.heightCm)} cm` : '')

/** 利润率着色 */
export const marginClass = (m) => (m == null ? '' : Number(m) < 0 ? 'loss' : Number(m) < 15 ? 'low' : 'good')
