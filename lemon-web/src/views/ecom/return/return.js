import { EcomReturnStatus, ReturnType } from '@/constants/enums'

/** 退货退款单按类型、状态可做的操作（与后端 EcomReturnService 的校验一致） */

/** 退货入库：退货退款、待处理 */
export const canReceive = (r) => r.returnType === ReturnType.RETURN_REFUND && r.status === EcomReturnStatus.PENDING
/** 登记退款：仅退款的待处理单、退货退款的已入库单 */
export const canRefund = (r) =>
  (r.returnType === ReturnType.REFUND_ONLY && r.status === EcomReturnStatus.PENDING) ||
  (r.returnType === ReturnType.RETURN_REFUND && r.status === EcomReturnStatus.RECEIVED)
/** 关闭：待处理 */
export const canClose = (r) => r.status === EcomReturnStatus.PENDING
/** 删除：待处理、已关闭 */
export const canDelete = (r) => r.status === EcomReturnStatus.PENDING || r.status === EcomReturnStatus.CLOSED
