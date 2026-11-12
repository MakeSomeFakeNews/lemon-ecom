import { t } from '@/locales'
import { money, num, nz } from '@/utils/number'
import { percent } from '@/components/charts/charts'

/**
 * 广告指标的列与格式：曝光 / 点击 / 订单为整数，花费 / 销售额 / CPC 为金额，ACOS / CTR / CVR 为百分数，ROAS 为倍数
 */
export const METRICS = [
  { key: 'impressions', kind: 'int', width: 100 },
  { key: 'clicks', kind: 'int', width: 90 },
  { key: 'spend', kind: 'money', width: 110 },
  { key: 'sales', kind: 'money', width: 120 },
  { key: 'orders', kind: 'int', width: 90 },
  { key: 'acos', kind: 'percent', width: 90 },
  { key: 'roas', kind: 'ratio', width: 80 },
  { key: 'ctr', kind: 'percent', width: 80 },
  { key: 'cpc', kind: 'money', width: 80 },
  { key: 'cvr', kind: 'percent', width: 80 }
]

/** 指标列（a-table），compare 为 true 时花费、销售额、ACOS 下方显示环比 */
export function metricColumns() {
  return METRICS.map((m) => ({ title: t(`ecomAds.metric.${m.key}`), key: m.key, dataIndex: m.key, kind: m.kind, width: m.width, align: 'right' }))
}

/** 按指标类型格式化 */
export function fmt(kind, v) {
  if (v === null || v === undefined || v === '') return '-'
  if (kind === 'money') return money(v)
  if (kind === 'int') return num(v)
  if (kind === 'percent') return percent(v, 2)
  if (kind === 'ratio') return Number(v).toFixed(2)
  return v
}

/** 变化率（%）：上期为 0 或空时为空 */
export function change(cur, prev) {
  if (prev === null || prev === undefined || nz(prev) === 0 || cur === null || cur === undefined) return null
  return ((nz(cur) - nz(prev)) / Math.abs(nz(prev))) * 100
}

/** 变化率的显示与颜色：越低越好的指标（ACOS、CPC、花费）下降为绿色 */
export function changeTag(cur, prev, lowerIsBetter = false) {
  const c = change(cur, prev)
  if (c === null) return null
  const good = lowerIsBetter ? c <= 0 : c >= 0
  return { text: `${c >= 0 ? '↑' : '↓'} ${Math.abs(c).toFixed(1)}%`, color: good ? 'green' : 'red' }
}

export const LOWER_BETTER = new Set(['acos', 'cpc', 'spend'])
