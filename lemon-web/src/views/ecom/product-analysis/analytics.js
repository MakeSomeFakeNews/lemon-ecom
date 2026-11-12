import dayjs from 'dayjs'
import { t } from '@/locales'
import { writeExcel } from '@/utils/excel'
import { findDict } from '@/utils/dict'
import { useDictStore } from '@/store/dict'

/** 数据分析页面共用：列定义、日期范围、导出 */

const fmt = (d) => d.format('YYYY-MM-DD')

/** 最近 n 天（含今天） */
export const lastDays = (n) => [fmt(dayjs().subtract(n - 1, 'day')), fmt(dayjs())]

/** 日期范围快捷选项 */
export function rangePresets() {
  return [7, 30, 90, 180, 365].map((n) => ({
    label: t('ecomAnalytics.common.lastNDays', { n }),
    value: [dayjs().subtract(n - 1, 'day'), dayjs()]
  }))
}

/** [begin, end] → beginDate / endDate */
export const rangeParams = (range) => ({ beginDate: range?.[0], endDate: range?.[1] })

/**
 * 列：kind 决定格式（money 金额、qty 数量、int 整数、percent 百分数、days 天数、dict 字典、text 文本），
 * exportValue(row) 自定义导出值
 */
export const col = (key, title, kind = 'text', extra = {}) => ({
  key,
  dataIndex: key,
  title,
  kind,
  width: { money: 120, qty: 100, int: 90, percent: 100, days: 100, dict: 110 }[kind] || 140,
  align: ['money', 'qty', 'int', 'percent', 'days'].includes(kind) ? 'right' : undefined,
  ...extra
})

/** 店铺、SKU（含标题）、物料（编码 + 名称）列 */
export const storeCol = () => col('storeName', t('ecomAnalytics.common.store'), 'text', { width: 150, ellipsis: true })
export const skuCol = () => col('sku', t('ecomAnalytics.common.sku'), 'sku', {
  width: 220,
  exportValue: (r) => [r.sellerSku, r.title].filter(Boolean).join(' ')
})
export const materialCol = (width = 200) => col('material', t('ecomAnalytics.common.material'), 'material', {
  width,
  exportValue: (r) => [r.materialCode, r.materialName].filter(Boolean).join(' ')
})

/** 导出：列为表格列定义，dict 列导出字典标签，数值导出原值 */
export async function exportTable(name, columns, rows, totalRow) {
  const cols = columns.filter((c) => c.key !== 'action')
  const dictStore = useDictStore()
  const dicts = {}
  await Promise.all([...new Set(cols.map((c) => c.dict).filter(Boolean))].map(async (d) => (dicts[d] = await dictStore.load(d))))
  const valueOf = (c, r) => {
    if (c.exportValue) return c.exportValue(r)
    let v = r[c.dataIndex || c.key]
    if (c.dict) v = findDict(dicts[c.dict], v)?.label ?? v
    if (v === null || v === undefined) return ''
    return ['money', 'qty', 'int', 'percent', 'days'].includes(c.kind) && v !== '' && !Number.isNaN(Number(v)) ? Number(v) : v
  }
  const data = (totalRow ? [...rows, totalRow] : rows).map((r) => cols.map((c) => valueOf(c, r)))
  await writeExcel(name, cols.map((c) => ({ title: c.title, width: c.width ? Math.round(c.width / 7) : 16 })), data)
}
