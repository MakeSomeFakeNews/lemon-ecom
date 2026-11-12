import { t } from '@/locales'
import { EcomMonitorType } from '@/constants/enums'

/** 商品监控页面共用 */

export const isOwn = (m) => m?.monitorType === EcomMonitorType.OWN

/** 排名变化：正数为上升（排名数值变小） */
export function changeText(v) {
  if (v == null || Number(v) === 0) return ''
  return Number(v) > 0 ? t('ecomOper.monitor.up', { n: v }) : t('ecomOper.monitor.down', { n: Math.abs(v) })
}

/** 评分 / 评论数变化：+0.1、−3 */
export function signed(v, digits = 0) {
  if (v == null || Number(v) === 0) return ''
  const n = Number(v)
  return (n > 0 ? '+' : '−') + Math.abs(n).toFixed(digits)
}

/** 排名显示：#1,234 */
export const rankText = (v) => (v == null ? '-' : `#${Number(v).toLocaleString()}`)
