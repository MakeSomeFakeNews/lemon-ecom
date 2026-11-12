import dayjs from 'dayjs'
import { t } from '@/locales'
import { shortMoney } from '@/components/charts/charts'
import { nz } from '@/utils/number'
import { writeExcel } from '@/utils/excel'
import { findDict } from '@/utils/dict'
import { useDictStore } from '@/store/dict'

/** 结算与分析页面共用：日期范围、比率、导出 */

const fmt = (d) => d.format('YYYY-MM-DD')

/** 默认日期范围：本月（月初到今天）、近 n 天（含今天） */
export const thisMonth = () => [fmt(dayjs().startOf('month')), fmt(dayjs())]
export const lastDays = (n) => [fmt(dayjs().subtract(n - 1, 'day')), fmt(dayjs())]

/** 日期范围快捷选项（a-range-picker 的 presets） */
export function rangePresets() {
  return [
    { label: t('ecomReport.common.last7Days'), value: [dayjs().subtract(6, 'day'), dayjs()] },
    { label: t('ecomReport.common.last30Days'), value: [dayjs().subtract(29, 'day'), dayjs()] },
    { label: t('ecomReport.common.thisMonth'), value: [dayjs().startOf('month'), dayjs()] },
    { label: t('ecomReport.common.lastMonth'), value: [dayjs().subtract(1, 'month').startOf('month'), dayjs().subtract(1, 'month').endOf('month')] }
  ]
}

/** [begin, end] → 接口的 beginDate / endDate */
export const rangeParams = (range) => ({ beginDate: range?.[0], endDate: range?.[1] })

/** 百分比：num / den × 100，分母为 0 或空时为 null（显示为 -） */
export const ratio = (num, den) => (nz(den) ? (nz(num) / nz(den)) * 100 : null)

/** 指标卡、图表提示上的金额：¥ 1.2 万 */
export const yuan = (v) => `${nz(v) < 0 ? '-' : ''}${t('ecomReport.common.yuan', { v: shortMoney(Math.abs(nz(v))) })}`

/** 接口返回数组、前端分页时 a-table 的 pagination（受控 current，重新查询后由页面回到第一页） */
export const localPagination = (pageSize = 20) => ({
  current: 1,
  pageSize,
  showSizeChanger: true,
  showTotal: (n) => t('common.table.total', { total: n })
})

/** 把 a-table 的分页变化写回 pagination */
export const applyPageChange = (pagination, p) => Object.assign(pagination, { current: p.current, pageSize: p.pageSize })

/** 按字段合计：返回 { field: 合计 }，全部为空的字段为 null */
export function sumFields(rows, fields) {
  const total = {}
  for (const f of fields) {
    let has = false
    let sum = 0
    for (const r of rows) {
      if (r[f] !== null && r[f] !== undefined) {
        has = true
        sum += Number(r[f]) || 0
      }
    }
    total[f] = has ? sum : null
  }
  return total
}

/**
 * 导出数组数据为 Excel：columns 为表格列定义（title、key / dataIndex、dict、exportValue、kind），操作列不导出；
 * 金额、数量导出原始数值。totalRow 不为空时作为最后一行（合计）
 */
export async function exportRows(name, columns, rows, totalRow) {
  const cols = columns.filter((c) => c.key !== 'action')
  const dictStore = useDictStore()
  const dicts = {}
  await Promise.all([...new Set(cols.map((c) => c.dict).filter(Boolean))].map(async (d) => (dicts[d] = await dictStore.load(d))))
  const valueOf = (c, r) => {
    if (c.exportValue) return c.exportValue(r)
    let v = r[c.dataIndex || c.key]
    if (c.dict) v = findDict(dicts[c.dict], v)?.label ?? v
    if (v === null || v === undefined) return ''
    return ['money', 'int', 'qty', 'percent'].includes(c.kind) && typeof v === 'string' && v !== '' && !Number.isNaN(Number(v)) ? Number(v) : v
  }
  const data = (totalRow ? [...rows, totalRow] : rows).map((r) => cols.map((c) => valueOf(c, r)))
  await writeExcel(name, cols.map((c) => ({ title: c.title, width: c.width ? Math.round(c.width / 7) : 16 })), data)
}
