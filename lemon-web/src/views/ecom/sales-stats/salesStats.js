import { t } from '@/locales'
import { writeExcel } from '@/utils/excel'
import { findDict } from '@/utils/dict'
import { useDictStore } from '@/store/dict'
import { SalesDimension, SalesGrain } from '@/constants/enums'

/** 销量统计：时间段标题、行名称、导出 */

/** 列标题：日 MM-DD、周「MM-DD 起」、月 YYYY-MM */
export function periodTitle(grain, p) {
  if (grain === SalesGrain.DAY) return p.key.slice(5)
  if (grain === SalesGrain.WEEK) return t('ecomAnalytics.sales.weekOf', { d: p.begin.slice(5) })
  return p.key
}

/** 行的显示名称（图例、导出） */
export function rowLabel(dimension, r, platformLabel = (v) => v) {
  switch (dimension) {
    case SalesDimension.SKU:
      return `${r.sellerSku}${r.storeName ? ` (${r.storeName})` : ''}`
    case SalesDimension.SPU:
      return `${r.spu}${r.storeName ? ` (${r.storeName})` : ''}`
    case SalesDimension.MATERIAL:
      return [r.materialCode, r.materialName].filter(Boolean).join(' ')
    case SalesDimension.STORE:
      return r.storeName || String(r.storeId)
    case SalesDimension.PLATFORM:
      return platformLabel(r.platform)
    default:
      return r.country || '-'
  }
}

/** 维度的标签列：[标题, 取值] */
export function labelColumns(dimension, platformLabel) {
  const store = [t('ecomAnalytics.common.store'), (r) => r.storeName]
  switch (dimension) {
    case SalesDimension.SKU:
      return [store, [t('ecomAnalytics.common.sku'), (r) => r.sellerSku], [t('ecomAnalytics.common.title'), (r) => r.title],
        [t('ecomAnalytics.common.material'), (r) => [r.materialCode, r.materialName].filter(Boolean).join(' ')]]
    case SalesDimension.SPU:
      return [store, [t('ecomAnalytics.sales.spu'), (r) => r.spu], [t('ecomAnalytics.common.title'), (r) => r.title],
        [t('ecomAnalytics.sales.skuCount'), (r) => r.skuCount]]
    case SalesDimension.MATERIAL:
      return [[t('ecomAnalytics.common.materialCode'), (r) => r.materialCode], [t('ecomAnalytics.common.materialName'), (r) => r.materialName],
        [t('ecomAnalytics.common.spec'), (r) => r.materialSpec]]
    case SalesDimension.STORE:
      return [store, [t('ecomAnalytics.common.platform'), (r) => platformLabel(r.platform)]]
    case SalesDimension.PLATFORM:
      return [[t('ecomAnalytics.common.platform'), (r) => platformLabel(r.platform)]]
    default:
      return [[t('ecomAnalytics.common.country'), (r) => r.country || '-']]
  }
}

/** 导出矩阵：标签列 + 各时间段（当前指标）+ 合计件数 / 销售额 + 环比 / 同比，最后一行合计 */
export async function exportMatrix(m, measure) {
  const platforms = await useDictStore().load('ecom_platform')
  const platformLabel = (v) => findDict(platforms, v)?.label || v
  const labels = labelColumns(m.dimension, platformLabel)
  const measureTitle = measure === 'qty' ? t('ecomAnalytics.common.qty') : t('ecomAnalytics.common.sales')
  const headers = [
    ...labels.map(([title]) => ({ title, width: 18 })),
    ...m.periods.map((p) => ({ title: `${periodTitle(m.grain, p)} ${measureTitle}`, width: 12 })),
    { title: t('ecomAnalytics.sales.totalQty'), width: 12 },
    { title: t('ecomAnalytics.sales.totalAmount'), width: 14 },
    { title: t('ecomAnalytics.sales.prevQty'), width: 12 },
    { title: t('ecomAnalytics.sales.prevAmount'), width: 14 },
    { title: t('ecomAnalytics.sales.momPercent'), width: 12 },
    { title: t('ecomAnalytics.sales.yoyQty'), width: 12 },
    { title: t('ecomAnalytics.sales.yoyAmount'), width: 14 },
    { title: t('ecomAnalytics.sales.yoyPercent'), width: 12 }
  ]
  const n = (v) => (v === null || v === undefined || v === '' ? '' : Number(v))
  const line = (r, first) => [
    ...labels.map(([, get], i) => (first !== undefined ? (i === 0 ? first : '') : get(r) ?? '')),
    ...r[measure].map(n),
    n(r.totalQty),
    n(r.totalAmount),
    n(r.prevQty),
    n(r.prevAmount),
    n(measure === 'qty' ? r.momQtyPercent : r.momAmountPercent),
    n(r.yoyQty),
    n(r.yoyAmount),
    n(measure === 'qty' ? r.yoyQtyPercent : r.yoyAmountPercent)
  ]
  const rows = [...m.rows.map((r) => line(r)), line(m.total, t('ecomAnalytics.common.total'))]
  const dim = t(`ecomAnalytics.sales.dim${m.dimension}`)
  await writeExcel(`${t('ecomAnalytics.sales.title')}_${dim}_${m.beginDate}_${m.endDate}`, headers, rows)
}
