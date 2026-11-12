import { printDocs } from '@/utils/print'
import { t } from '@/locales'
import { qty } from '@/utils/number'

/** 头程货件的装箱单（模板 ecom.packingList）与箱唛（模板 ecom.cartonLabel，每箱一页） */

const sizeText = (b) => ([b.length, b.width, b.height].every((v) => v != null) ? `${Number(b.length)} × ${Number(b.width)} × ${Number(b.height)}` : '')
const weightText = (b) => (b.weight != null ? qty(b.weight, 3) : '')

/** SKU → 货件按平台 SKU 汇总的一行（FNSKU、标题） */
const skuMap = (d) => Object.fromEntries((d.skuLines || []).map((s) => [s.sellerSku, s]))

/** 装箱单：每箱每个平台 SKU 一行 */
export function printPackingList(d) {
  const skus = skuMap(d)
  const rows = []
  ;(d.boxes || []).forEach((b) => {
    ;(b.items || []).forEach((i, k) => {
      const s = skus[i.sellerSku] || {}
      rows.push({ boxNo: k === 0 ? b.boxNo : '', size: k === 0 ? sizeText(b) : '', weight: k === 0 ? weightText(b) : '', sku: i.sellerSku, fnsku: s.fnsku || '',
        title: s.title || s.materialName || '', qty: qty(i.qty) })
    })
  })
  const total = (d.boxes || []).reduce((sum, b) => sum + (b.items || []).reduce((s, i) => s + Number(i.qty || 0), 0), 0)
  return printDocs({
    title: t('print.name.packingList'),
    no: d.docNo,
    fields: [
      [t('print.name.shipmentId'), d.shipmentId || '-'],
      [t('print.name.store'), d.storeName],
      [t('print.name.shipFrom'), d.fromWarehouseName],
      [t('print.name.shipTo'), d.toWarehouseName],
      [t('print.name.boxCount'), (d.boxes || []).length],
      [t('print.name.totalQty'), qty(total)],
      [t('print.name.grossWeight'), d.weight != null ? qty(d.weight, 3) : '-']
    ],
    columns: [
      { title: t('print.name.boxNo'), key: 'boxNo' },
      { title: t('print.name.boxSize'), key: 'size' },
      { title: t('print.name.grossWeight'), key: 'weight', align: 'right' },
      { title: t('print.name.sellerSku'), key: 'sku' },
      { title: t('print.name.fnsku'), key: 'fnsku' },
      { title: t('print.name.productTitle'), key: 'title' },
      { title: t('print.name.quantity'), key: 'qty', align: 'right' }
    ],
    rows,
    signs: [t('print.name.packer'), t('print.name.prepared')]
  }, t('print.name.packingList'), 'ecom.packingList')
}

/** 箱唛：每箱一页，箱号「n / 共 N 箱」、发货 / 收货仓、尺寸重量与箱内 SKU */
export function printCartonLabels(d) {
  const skus = skuMap(d)
  const count = (d.boxes || []).length
  const docs = (d.boxes || []).map((b) => ({
    title: t('print.name.cartonLabel'),
    no: d.shipmentId || d.docNo,
    fields: [
      [t('print.name.shipmentId'), d.shipmentId || d.docNo],
      [t('print.name.boxNo'), `${b.boxNo} / ${count}`],
      [t('print.name.shipFrom'), d.fromWarehouseName],
      [t('print.name.shipTo'), d.toWarehouseName],
      [t('print.name.boxSize'), sizeText(b) || '-'],
      [t('print.name.grossWeight'), weightText(b) || '-']
    ],
    columns: [
      { title: t('print.name.sellerSku'), key: 'sku' },
      { title: t('print.name.fnsku'), key: 'fnsku' },
      { title: t('print.name.quantity'), key: 'qty', align: 'right' }
    ],
    rows: (b.items || []).map((i) => ({ sku: i.sellerSku, fnsku: skus[i.sellerSku]?.fnsku || '', qty: qty(i.qty) }))
  }))
  return printDocs(docs, t('print.name.cartonLabel'), 'ecom.cartonLabel')
}
