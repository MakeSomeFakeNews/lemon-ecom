import dayjs from 'dayjs'
import { t } from '@/locales'
import { EcomPackageStatus } from '@/constants/enums'
import { qty } from '@/utils/number'
import { materialColumns, printDocs } from '@/utils/print'
import { ecomPickList } from '@/api/ecom/package'

/** 包裹按状态可做的操作（与后端 EcomPackageService 的校验一致） */

/** 申请运单号：待打单 */
export const canLabel = (p) => p.status === EcomPackageStatus.WAIT_LABEL
/** 手工填写运单号：待打单，或手工运单的待发货（改运单号）；在线面单的先取消 */
export const canTracking = (p) => p.status === EcomPackageStatus.WAIT_LABEL || (p.status === EcomPackageStatus.LABELED && !p.carrierOrderNo)
/** 发货：待发货 */
export const canShipPackage = (p) => p.status === EcomPackageStatus.LABELED
/** 修改重量、运费、渠道：未发货 */
export const canEditPackage = (p) => p.status === EcomPackageStatus.WAIT_LABEL || p.status === EcomPackageStatus.LABELED
/** 取消：未发货 */
export const canCancelPackage = canEditPackage
/** 重新获取面单：在线下单但没有面单文件 */
export const canRefetch = (p) => p.status === EcomPackageStatus.LABELED && !!p.carrierOrderNo && !p.hasLabel
/** 物流轨迹：对接了物流商且有运单号 */
export const canTrack = (p) => !!p.carrier && !!p.trackingNo

/** 包裹状态标签页 */
export const PACKAGE_TABS = [
  { key: EcomPackageStatus.WAIT_LABEL, label: 'ecomOrder.pkg.tabWaitLabel' },
  { key: EcomPackageStatus.LABELED, label: 'ecomOrder.pkg.tabLabeled' },
  { key: EcomPackageStatus.SHIPPED, label: 'ecomOrder.pkg.tabShipped' },
  { key: EcomPackageStatus.CANCELLED, label: 'ecomOrder.pkg.tabCancelled' },
  { key: 'all', label: 'ecomOrder.pkg.tabAll' }
]

export const PACKAGE_RESULT_KEYS = { done: 'ecomOrder.pkg.shipDone', partial: 'ecomOrder.pkg.partial', title: 'ecomOrder.pkg.failTitle' }
export const LABEL_RESULT_KEYS = { done: 'ecomOrder.pkg.labelDone', partial: 'ecomOrder.pkg.partial', title: 'ecomOrder.pkg.failTitle' }

/**
 * 打印拣货单（打印模板 ecom.pickList）：后端按仓库分组、按库位排序汇总物料数量，每个仓库一张
 */
export async function printPickList(ids) {
  const lists = (await ecomPickList(ids)) || []
  const [index, ...material] = materialColumns()
  const docs = lists.map((p) => ({
    title: t('print.name.pickList'),
    no: dayjs().format('YYYYMMDD-HHmm'),
    fields: [
      [t('print.name.warehouse'), p.warehouseName],
      [t('print.name.date'), dayjs().format('YYYY-MM-DD HH:mm')],
      [t('print.name.packageCount'), p.packageCount],
      [t('print.name.totalQty'), qty(p.totalQty)],
      [t('print.name.packageNos'), p.packageNos, true]
    ],
    columns: [
      index,
      {
        title: t('print.name.location'),
        value: (r) => (r.locationCode ? r.locationCode + (r.otherLocations ? ` (${t('ecomOrder.pkg.otherLocations', { n: r.otherLocations })})` : '') : t('ecomOrder.pkg.noLocation'))
      },
      ...material,
      { title: t('print.name.quantity'), value: (r) => qty(r.qty), align: 'right' },
      { title: t('print.name.sellerSku'), key: 'skus' },
      { title: t('print.name.packageCount'), key: 'packageCount', align: 'right' }
    ],
    rows: p.rows,
    signs: [t('print.name.prepared'), t('print.name.picker')]
  }))
  if (docs.length) await printDocs(docs, t('ecomOrder.pkg.pickListTitle'), 'ecom.pickList')
}
