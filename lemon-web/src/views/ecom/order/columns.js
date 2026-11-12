import { t } from '@/locales'
import { actionColumn } from '@/components/common/tableActions'

/**
 * 平台订单列表的列：订单（单号 / 店铺 / 平台 / 下单时间 / 标签）、商品（缩略图 + SKU × 数量）、买家与收件国家、金额、利润、状态、发货信息；
 * 页面上多项信息紧凑显示在一列中，导出时拆成多列（exportColumns）
 */
export const orderColumns = () => [
  {
    title: t('ecomOrdUi.list.order'),
    key: 'order',
    dataIndex: 'platformOrderNo',
    width: 220,
    fixed: 'left',
    exportColumns: [
      { title: t('ecomOrder.common.platformOrderNo'), dataIndex: 'platformOrderNo' },
      { title: t('ecomOrder.common.store'), dataIndex: 'storeName' },
      { title: t('ecomOrder.common.platform'), dataIndex: 'platform', dict: 'ecom_platform' },
      { title: t('ecomOrder.order.orderTime'), dataIndex: 'orderTime' },
      { title: t('ecomOrd.order.orderType'), dataIndex: 'orderType', dict: 'ecom_order_type' },
      { title: t('ecomOrd.order.tag'), exportValue: (r) => (r.tags || []).map((x) => x.name).join(', ') },
      { title: t('ecomOrd.resend.sourceOrder'), dataIndex: 'sourceOrderNo' }
    ]
  },
  {
    title: t('ecomOrdUi.list.status'),
    key: 'localStatus',
    dataIndex: 'localStatus',
    width: 125,
    exportColumns: [
      { title: t('ecomOrder.order.localStatus'), dataIndex: 'localStatus', dict: 'ecom_local_status' },
      { title: t('ecomOrder.order.status'), dataIndex: 'status', dict: 'ecom_order_status' },
      { title: t('ecomOrder.order.fulfillment'), dataIndex: 'fulfillment', dict: 'ecom_fulfillment' },
      { title: t('ecomOrder.order.exception'), dataIndex: 'exception' },
      { title: t('ecomOrder.ops.holdBy'), dataIndex: 'holdReason' }
    ]
  },
  {
    title: t('ecomOrdUi.list.items'),
    key: 'items',
    width: 240,
    exportColumns: [
      { title: t('ecomOrdUi.list.items'), exportValue: (r) => (r.items || []).map((x) => `${x.sellerSku} x ${Number(x.qty)}`).join('; ') },
      { title: t('ecomOrder.order.itemCount'), dataIndex: 'itemCount' }
    ]
  },
  {
    title: t('ecomOrdUi.list.buyer'),
    key: 'buyer',
    width: 160,
    exportColumns: [
      { title: t('ecomOrder.order.buyer'), dataIndex: 'buyerName' },
      { title: t('ecomOrder.order.country'), dataIndex: 'shipCountry' },
      { title: t('ecomOrd.order.addressIssue'), dataIndex: 'addressIssue' }
    ]
  },
  {
    title: t('ecomOrder.order.paid'),
    key: 'paid',
    width: 125,
    align: 'right',
    exportColumns: [
      { title: t('ecomOrder.common.currency'), dataIndex: 'currency' },
      { title: t('ecomOrder.order.paid'), dataIndex: 'totalAmount' },
      { title: t('ecomOrder.order.paidBase'), dataIndex: 'totalBase' }
    ]
  },
  {
    title: t('ecomOrdUi.list.profit'),
    key: 'profit',
    width: 110,
    align: 'right',
    exportColumns: [
      { title: t('ecomOrder.order.profit'), dataIndex: 'profit' },
      { title: t('ecomOrd.estimate.shipping'), dataIndex: 'estShipping' },
      { title: t('ecomOrd.estimate.fee'), dataIndex: 'estFee' },
      { title: t('ecomOrd.estimate.cost'), dataIndex: 'estCost' },
      { title: t('ecomOrd.estimate.profit'), dataIndex: 'estProfit' },
      { title: t('ecomOrd.estimate.rate'), dataIndex: 'estProfitRate' }
    ]
  },
  {
    title: t('ecomOrdUi.list.ship'),
    key: 'ship',
    width: 190,
    exportColumns: [
      { title: t('ecomOrder.order.shipBy'), dataIndex: 'shipBy' },
      { title: t('ecomOrder.ops.planShipTime'), dataIndex: 'planShipTime' },
      { title: t('ecomOrder.order.outWarehouse'), dataIndex: 'warehouseName' },
      { title: t('ecomOrder.ops.packageCount'), dataIndex: 'packageCount' },
      { title: t('ecomOrder.common.carrier'), dataIndex: 'carrier' },
      { title: t('ecomOrder.common.trackingNo'), dataIndex: 'trackingNo' }
    ]
  },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 140, ellipsis: true },
  // 查看、编辑、审核 / 出库 / 发货（按状态互斥）、更多
  actionColumn(4, [t('common.action.view')])
]
