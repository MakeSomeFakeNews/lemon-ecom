import { t } from '@/locales'
import { qty } from '@/utils/number'

/**
 * 补货建议的公共定义：日均加权的预设、请求参数、需求构成文字
 */

/** 日均加权预设：w7 / w14 / w30 / w60 为最近 7 / 14 / 30 / 60 天日均的权重（与后端 ReplenishWeights 一致，默认 7 天 × 0.5 + 30 天 × 0.5） */
export const WEIGHT_PRESETS = [
  { key: 'default', weights: { w7: 0.5, w30: 0.5 } },
  // 按销量预测（数据分析 → 销量预测）：含趋势、星期季节性、促销 / 节假日系数；没有预测结果的 SKU 按默认权重
  { key: 'forecast', weights: { w7: 0.5, w30: 0.5, forecast: true } },
  { key: 'd7', weights: { w7: 1 } },
  { key: 'd14', weights: { w14: 1 } },
  { key: 'd30', weights: { w30: 1 } },
  { key: 'd60', weights: { w60: 1 } },
  { key: 'trend', weights: { w7: 0.4, w14: 0.3, w30: 0.3 } },
  { key: 'steady', weights: { w30: 0.5, w60: 0.5 } }
]
export const PERIODS = ['w7', 'w14', 'w30', 'w60']
export const CUSTOM = 'custom'

/** 请求参数：所选权重（未选的期间不传） */
export function weightParams(w) {
  const params = {}
  if (w?.forecast) params.forecast = true
  PERIODS.forEach((k) => {
    if (w?.[k] != null && w[k] !== '') params[k] = w[k]
  })
  return params
}

/** 权重至少有一个大于 0 */
export const validWeights = (w) => PERIODS.some((k) => Number(w?.[k]) > 0) && PERIODS.every((k) => w?.[k] == null || Number(w[k]) >= 0)

/** 需求构成（PurchaseSuggestRow.demands 或采购计划明细 demand 的 JSON） */
export function demandList(v) {
  if (!v) return []
  if (Array.isArray(v)) return v
  try {
    const list = JSON.parse(v)
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}
export const demandText = (d) => t('ecomPurchase.replenish.demandText', { store: d.storeName || '-', sku: d.sellerSku || '-', qty: qty(d.qty) })
