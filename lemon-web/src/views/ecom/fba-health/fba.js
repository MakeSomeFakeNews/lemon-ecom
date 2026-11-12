import { AmzReportType, AmzReportStatus } from '@/constants/enums'
import { useUserStore } from '@/store/user'

/** 按日期范围生成的报告类型（与后端 AmzReportType.dated 一致） */
export const DATED_TYPES = [AmzReportType.LEDGER_DETAIL, AmzReportType.REIMBURSEMENTS, AmzReportType.REMOVAL_ORDERS]

/** 仍在生成、可以刷新状态的报告任务 */
export const isRunning = (r) => !!r.reportId && (r.status === AmzReportStatus.SUBMITTED || r.status === AmzReportStatus.PROCESSING)

/** 数量显示：去掉多余的小数 */
export const n0 = (v) => (v == null || v === '' ? '-' : Number(v).toLocaleString(undefined, { maximumFractionDigits: 2 }))

/** 按钮权限判断（模板外使用） */
export function useCan() {
  const store = useUserStore()
  return (perm) => store.hasPerm(perm)
}
