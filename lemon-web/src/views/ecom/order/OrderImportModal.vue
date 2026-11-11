<template>
  <ImportModal v-model:open="open" :title="$t('ecomOrder.order.title')" :columns="columns" :importer="importer" :sample="sample" @success="emit('success', $event)" />
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import ImportModal from '@/components/ImportModal.vue'
import { importEcomOrder } from '@/api/ecom/order'
import { EcomOrderStatus, FulfillmentType } from '@/constants/enums'

/**
 * 平台订单 Excel 导入（POST /ecom/order/import，列对应后端 OrderImportRow）：
 * 一行一个订单行，同一订单的多行按店铺编码 + 平台订单号合并；下单 / 付款 / 承诺发货时间为日期时间，
 * 只填日期（或 Excel 日期单元格读出的 yyyy-MM-dd）时按 0 点补齐。ref 调用 open()
 */
const emit = defineEmits(['success'])
const { t } = useI18n()
const open = ref(false)

const DATETIME_KEYS = ['orderTime', 'paidTime', 'shipBy']

const columns = [
  { title: t('ecomOrder.order.importStoreCode'), key: 'storeCode', required: true },
  { title: t('ecomOrder.common.platformOrderNo'), key: 'platformOrderNo', required: true },
  { title: t('ecomOrder.order.orderTime'), key: 'orderTime', required: true },
  { title: t('ecomOrder.order.paidTime'), key: 'paidTime' },
  { title: t('ecomOrder.order.status'), key: 'status', dict: 'ecom_order_status' },
  { title: t('ecomOrder.order.fulfillment'), key: 'fulfillment', dict: 'ecom_fulfillment' },
  { title: t('ecomOrder.order.importBuyerName'), key: 'buyerName' },
  { title: t('ecomOrder.order.importBuyerEmail'), key: 'buyerEmail' },
  { title: t('ecomOrder.order.importShipName'), key: 'shipName' },
  { title: t('ecomOrder.order.importShipPhone'), key: 'shipPhone' },
  { title: t('ecomOrder.order.importShipCountry'), key: 'shipCountry' },
  { title: t('ecomOrder.order.importShipState'), key: 'shipState' },
  { title: t('ecomOrder.order.importShipCity'), key: 'shipCity' },
  { title: t('ecomOrder.order.importShipPostcode'), key: 'shipPostcode' },
  { title: t('ecomOrder.order.importShipAddress'), key: 'shipAddress' },
  { title: t('ecomOrder.common.currency'), key: 'currency' },
  { title: t('ecomOrder.order.importShipBy'), key: 'shipBy' },
  { title: t('ecomOrder.order.importNote'), key: 'note' },
  { title: t('ecomOrder.order.importPlatformLineId'), key: 'platformLineId' },
  { title: t('ecomOrder.common.sellerSku'), key: 'sellerSku', required: true },
  { title: t('ecomOrder.order.itemPlatformId'), key: 'platformItemId' },
  { title: t('ecomOrder.order.importTitleCol'), key: 'title' },
  { title: t('ecomOrder.common.qty'), key: 'qty', type: 'number', required: true },
  { title: t('ecomOrder.order.importPrice'), key: 'price', type: 'number', required: true },
  { title: t('ecomOrder.order.importShipping'), key: 'shipping', type: 'number' },
  { title: t('ecomOrder.order.importTax'), key: 'tax', type: 'number' },
  { title: t('ecomOrder.order.importDiscount'), key: 'discount', type: 'number' }
]

const sample = {
  storeCode: 'US01',
  platformOrderNo: '112-0000000-0000001',
  orderTime: '2026-01-02 10:30:00',
  paidTime: '2026-01-02 10:31:00',
  status: EcomOrderStatus.UNSHIPPED,
  fulfillment: FulfillmentType.SELLER,
  buyerName: 'John Smith',
  buyerEmail: 'john@example.com',
  shipName: 'John Smith',
  shipPhone: '+1 555 0100',
  shipCountry: 'US',
  shipState: 'CA',
  shipCity: 'Los Angeles',
  shipPostcode: '90001',
  shipAddress: '1 Main St',
  currency: 'USD',
  shipBy: '2026-01-04 23:59:59',
  platformLineId: '1',
  sellerSku: 'SKU-001',
  platformItemId: 'B000000001',
  title: 'Sample Product',
  qty: 1,
  price: 19.99,
  shipping: 0,
  tax: 0,
  discount: 0
}

/** 日期时间列：2026/1/2 10:30、2026-01-02T10:30 等统一为 yyyy-MM-dd HH:mm:ss，只有日期时补 00:00:00 */
function normalizeDateTime(v) {
  if (v == null || v === '') return v
  const s = String(v).trim().replace(/\//g, '-').replace('T', ' ')
  const m = /^(\d{4})-(\d{1,2})-(\d{1,2})(?:\s+(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?)?$/.exec(s)
  if (!m) return s
  const p = (n) => String(n).padStart(2, '0')
  return `${m[1]}-${p(m[2])}-${p(m[3])} ${p(m[4] || 0)}:${p(m[5] || 0)}:${p(m[6] || 0)}`
}

function importer({ rows }) {
  const normalized = rows.map(({ rowNum, data }) => {
    const d = { ...data }
    DATETIME_KEYS.forEach((k) => {
      if (d[k] !== undefined) d[k] = normalizeDateTime(d[k])
    })
    if (d.shipCountry) d.shipCountry = String(d.shipCountry).trim().toUpperCase()
    return { rowNum, data: d }
  })
  return importEcomOrder({ rows: normalized })
}

defineExpose({ open: () => (open.value = true) })
</script>
