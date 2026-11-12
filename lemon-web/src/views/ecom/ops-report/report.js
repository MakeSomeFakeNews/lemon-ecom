import { money } from '@/utils/number'

/** 变化百分比：(cur − base) ÷ |base| × 100，基准为 0 / 空时为 null */
export function change(cur, base) {
  const b = Number(base)
  if (!b) return null
  return ((Number(cur || 0) - b) / Math.abs(b)) * 100
}

/** 带符号的百分比文字 */
export const signedPct = (v, digits = 1) => (v == null || Number.isNaN(Number(v)) ? '-' : `${Number(v) > 0 ? '+' : ''}${Number(v).toFixed(digits)}%`)

/** 百分比文字 */
export const pct = (v, digits = 1) => (v == null ? '-' : `${Number(v).toFixed(digits)}%`)

/** 变化的颜色类：上升为 up（绿色），下降为 down（红色）；reverse 时反过来（如 ACOS、退款越低越好） */
export function trendClass(v, reverse = false) {
  if (v == null || Number(v) === 0) return ''
  const up = Number(v) > 0
  return (reverse ? !up : up) ? 'trend-up' : 'trend-down'
}

/** 金额（本位币） */
export const amount = (v) => money(v)
