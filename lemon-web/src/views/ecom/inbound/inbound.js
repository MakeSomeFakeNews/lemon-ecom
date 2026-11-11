import { InboundStatus } from '@/constants/enums'
import { nz, round2 } from '@/utils/number'

/** 头程货件按状态可做的操作（与后端 EcomInboundService 的校验一致） */

/** 修改、提交发运：草稿 */
export const isDraft = (i) => i.status === InboundStatus.DRAFT
/** 签收：已发运 */
export const canReceive = (i) => i.status === InboundStatus.SHIPPED
/** 补录物流与费用：已发运、已签收 */
export const canEditCosts = (i) => i.status === InboundStatus.SHIPPED || i.status === InboundStatus.RECEIVED
/** 取消：草稿、审批中 */
export const canCancel = (i) => i.status === InboundStatus.DRAFT || i.status === InboundStatus.APPROVING
/** 删除：草稿、已取消 */
export const canDelete = (i) => i.status === InboundStatus.DRAFT || i.status === InboundStatus.CANCELLED

/** 头程费用合计（本位币）= 头程运费 + 关税 + 其他费用 */
export const totalCost = (i) => round2(nz(i.freight) + nz(i.duty) + nz(i.otherCost))
/** 单件头程成本 = 费用合计 ÷ 合计数量，数量为 0 时为 0 */
export const unitLandedOf = (i, qty) => (nz(qty) > 0 ? Math.round((totalCost(i) / nz(qty)) * 10000) / 10000 : 0)
