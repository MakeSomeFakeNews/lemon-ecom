import { t } from '@/locales'
import { actionColumn } from '@/components/common/tableActions'

/** 平台订单列表的列（导出时多项信息合并显示的列拆成多列） */
export const orderColumns = () => [
  {
    title: t('ecomOrder.common.platformOrderNo'),
    key: 'platformOrderNo',
    dataIndex: 'platformOrderNo',
    width: 200,
    exportColumns: [
      { title: t('ecomOrder.common.platformOrderNo'), dataIndex: 'platformOrderNo' },
      { title: t('ecomOrd.order.orderType'), dataIndex: 'orderType', dict: 'ecom_order_type' },
      { title: t('ecomOrd.order.tag'), exportValue: (r) => (r.tags || []).map((x) => x.name).join(', ') },
      { title: t('ecomOrd.resend.sourceOrder'), dataIndex: 'sourceOrderNo' }
    ]
  },
  { title: t('ecomOrder.common.store'), dataIndex: 'storeName', width: 150 },
  { title: t('ecomOrder.common.platform'), key: 'platform', dataIndex: 'platform', width: 100, dict: 'ecom_platform' },
  { title: t('ecomOrder.order.orderTime'), dataIndex: 'orderTime', width: 170 },
  {
    title: t('ecomOrder.order.buyer'),
    key: 'buyer',
    width: 160,
    exportColumns: [
      { title: t('ecomOrder.order.buyer'), dataIndex: 'buyerName' },
      { title: t('ecomOrder.order.country'), dataIndex: 'shipCountry' },
      { title: t('ecomOrd.order.addressIssue'), dataIndex: 'addressIssue' }
    ]
  },
  { title: t('ecomOrder.order.itemCount'), dataIndex: 'itemCount', width: 90, align: 'right' },
  {
    title: t('ecomOrder.order.paid'),
    key: 'paid',
    width: 150,
    align: 'right',
    exportColumns: [
      { title: t('ecomOrder.common.currency'), dataIndex: 'currency' },
      { title: t('ecomOrder.order.paid'), dataIndex: 'totalAmount' },
      { title: t('ecomOrder.order.paidBase'), dataIndex: 'totalBase' }
    ]
  },
  { title: t('ecomOrder.order.status'), key: 'status', dataIndex: 'status', width: 100, dict: 'ecom_order_status' },
  {
    title: t('ecomOrder.order.localStatus'),
    key: 'localStatus',
    dataIndex: 'localStatus',
    width: 120,
    dict: 'ecom_local_status',
    exportColumns: [
      { title: t('ecomOrder.order.localStatus'), dataIndex: 'localStatus', dict: 'ecom_local_status' },
      { title: t('ecomOrder.order.exception'), dataIndex: 'exception' },
      { title: t('ecomOrder.ops.holdBy'), dataIndex: 'holdReason' }
    ]
  },
  { title: t('ecomOrder.order.fulfillment'), key: 'fulfillment', dataIndex: 'fulfillment', width: 110, dict: 'ecom_fulfillment' },
  {
    title: t('ecomOrder.order.shipBy'),
    key: 'shipBy',
    dataIndex: 'shipBy',
    width: 180,
    exportColumns: [
      { title: t('ecomOrder.order.shipBy'), dataIndex: 'shipBy' },
      { title: t('ecomOrder.ops.planShipTime'), dataIndex: 'planShipTime' }
    ]
  },
  { title: t('ecomOrder.order.outWarehouse'), dataIndex: 'warehouseName', width: 130, ellipsis: true },
  { title: t('ecomOrder.ops.packageCount'), dataIndex: 'packageCount', width: 80, align: 'right' },
  {
    title: t('ecomOrder.common.trackingNo'),
    key: 'trackingNo',
    width: 170,
    exportColumns: [
      { title: t('ecomOrder.common.carrier'), dataIndex: 'carrier' },
      { title: t('ecomOrder.common.trackingNo'), dataIndex: 'trackingNo' }
    ]
  },
  { title: t('ecomOrder.order.profit'), key: 'profit', dataIndex: 'profit', width: 120, align: 'right' },
  {
    title: t('ecomOrd.estimate.listTitle'),
    key: 'estProfit',
    dataIndex: 'estProfit',
    width: 120,
    align: 'right',
    exportColumns: [
      { title: t('ecomOrd.estimate.shipping'), dataIndex: 'estShipping' },
      { title: t('ecomOrd.estimate.fee'), dataIndex: 'estFee' },
      { title: t('ecomOrd.estimate.cost'), dataIndex: 'estCost' },
      { title: t('ecomOrd.estimate.profit'), dataIndex: 'estProfit' },
      { title: t('ecomOrd.estimate.rate'), dataIndex: 'estProfitRate' }
    ]
  },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  // 查看、编辑、审核 / 出库 / 发货（按状态互斥）、更多
  actionColumn(4, [t('common.action.view')])
]
