<template>
  <div class="profit-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" />
        <StoreSelect v-model:value="storeId" :placeholder="$t('ecomReport.common.allStores')" class="filter" />
        <a-input
          v-if="tab === 'bySku'"
          v-model:value="keyword"
          allow-clear
          :placeholder="$t('ecomReport.profit.keywordPlaceholder')"
          class="filter"
          @press-enter="load"
        />
        <a-button type="primary" :loading="loading" @click="load">
          <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
        </a-button>
        <a-button :disabled="!rows.length" :loading="exporting" @click="doExport">
          <template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}
        </a-button>
      </div>
      <div class="sub-text tip">{{ $t('ecomReport.profit.tip') }}</div>
    </a-card>

    <a-card :bordered="false" class="page-card">
      <a-tabs v-model:active-key="tab" class="tabs" @change="load">
        <a-tab-pane v-for="k in TABS" :key="k" :tab="$t(`ecomReport.profit.${k}`)" />
      </a-tabs>

      <a-card v-if="rows.length" :bordered="false" size="small" :title="tab === 'fees' ? $t('ecomReport.profit.feeChartTitle') : $t('ecomReport.profit.chartTitle')" class="chart-card">
        <EChart :option="chartOption" height="260px" />
      </a-card>

      <ProfitTable :columns="columns" :rows="rows" :total="total" :loading="loading" :row-key="ROW_KEYS[tab]" />
    </a-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { exportRows, rangeParams, rangePresets, ratio, sumFields, thisMonth, yuan } from '@/components/ecom/ecomReport'
import ProfitTable from './ProfitTable.vue'
import { reportCountry, reportFees, reportSku, reportStore } from '@/api/ecom/report'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { nz } from '@/utils/number'
import { SettlementItemType } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 利润分析：按店铺 / SKU / 国家汇总销售额、费用、退款、成本、物流、广告与利润（GET /ecom/report/*，本位币），
 * 费用构成按结算明细类型。接口返回数组，前端分页、合计与导出
 */
const TABS = ['byStore', 'bySku', 'byCountry', 'fees']
const API = { byStore: reportStore, bySku: reportSku, byCountry: reportCountry, fees: reportFees }
const ROW_KEYS = {
  byStore: (r) => String(r.storeId),
  bySku: (r) => `${r.storeId}|${r.sellerSku}`,
  byCountry: (r) => r.country || '-',
  fees: (r) => r.itemType
}

const dict = useDict('ecom_settlement_item_type')
const presets = rangePresets()
const tab = ref('byStore')
const range = ref(thisMonth())
const storeId = ref()
const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const exporting = ref(false)

const col = (title, key, extra = {}) => ({ title, key, dataIndex: key, ...extra })
const m = (key, title) => col(title, key, { kind: 'money', width: 120, align: 'right' })
const store = col(t('ecomReport.common.store'), 'storeName', { width: 160, ellipsis: true })
const orders = col(t('ecomReport.common.orders'), 'orders', { kind: 'int', width: 90, align: 'right' })
const qtyCol = col(t('common.field.quantity'), 'qty', { kind: 'qty', width: 100, align: 'right' })
const sales = m('sales', t('ecomReport.common.sales'))
const profit = m('profit', t('ecomReport.common.profit'))
const margin = col(t('ecomReport.common.margin'), 'margin', { kind: 'percent', width: 90, align: 'right' })
const costs = [
  m('fees', t('ecomReport.common.fees')),
  m('refund', t('ecomReport.common.refund')),
  m('cost', t('ecomReport.common.cost')),
  m('shipping', t('ecomReport.common.shipping')),
  m('adSpend', t('ecomReport.common.adSpend'))
]
const COLUMNS = {
  byStore: [
    store,
    col(t('ecomReport.common.platform'), 'platform', { kind: 'dict', dict: 'ecom_platform', width: 100 }),
    col(t('ecomReport.common.marketplace'), 'marketplace', { width: 80 }),
    orders, qtyCol, sales, ...costs, profit, margin
  ],
  bySku: [
    store,
    col(t('ecomReport.common.sellerSku'), 'sellerSku', { width: 150 }),
    col(t('ecomReport.profit.listingTitle'), 'title', { width: 220, ellipsis: true }),
    col(t('ecomReport.profit.material'), 'material', { width: 200, ellipsis: true, exportValue: (r) => [r.materialCode, r.materialName].filter(Boolean).join(' ') }),
    col(t('ecomReport.common.platformItemId'), 'platformItemId', { width: 140 }),
    orders, qtyCol, sales, ...costs, profit, margin
  ],
  byCountry: [col(t('ecomReport.profit.country'), 'country', { width: 120 }), orders, qtyCol, sales, profit, margin],
  fees: [
    col(t('ecomReport.profit.itemType'), 'itemType', { kind: 'dict', dict: 'ecom_settlement_item_type', width: 160 }),
    m('amount', t('common.field.amount')),
    col(t('ecomReport.common.share'), 'share', { kind: 'percent', width: 100, align: 'right' })
  ]
}
const columns = computed(() => COLUMNS[tab.value])

/** 合计行：数量、金额求和，利润率按合计重算；费用构成的占比不合计 */
const total = computed(() => {
  const numeric = columns.value.filter((c) => ['int', 'qty', 'money'].includes(c.kind)).map((c) => c.key)
  const s = sumFields(rows.value, numeric)
  if (tab.value !== 'fees') s.margin = ratio(s.profit, s.sales)
  return s
})

/** 国家行没有利润率、费用构成没有占比：在前端补算（占比按费用类型的绝对值，不含订单收入） */
function enrich(list) {
  if (tab.value === 'byCountry') return list.map((r) => ({ ...r, margin: ratio(r.profit, r.sales) }))
  if (tab.value === 'fees') {
    const feeRows = list.filter((r) => r.itemType !== SettlementItemType.ORDER)
    const feeTotal = feeRows.reduce((s, r) => s + Math.abs(nz(r.amount)), 0)
    return list.map((r) => ({ ...r, share: r.itemType === SettlementItemType.ORDER ? null : ratio(Math.abs(nz(r.amount)), feeTotal) }))
  }
  return list
}

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const params = { ...rangeParams(range.value), storeId: storeId.value || undefined }
    if (tab.value === 'bySku') params.keyword = keyword.value?.trim() || undefined
    const res = await API[tab.value](params)
    if (current !== seq) return
    rows.value = enrich(res || [])
  } finally {
    if (current === seq) loading.value = false
  }
}

const typeLabel = (v) => findDict(dict.ecom_settlement_item_type, v)?.label || v
const rowLabel = (r) => (tab.value === 'bySku' ? r.sellerSku : tab.value === 'byCountry' ? r.country : r.storeName) || '-'
const grid = { left: 16, right: 16, top: 36, bottom: 8, containLabel: true }

const chartOption = computed(() => {
  if (tab.value === 'fees') {
    const data = rows.value.filter((r) => r.itemType !== SettlementItemType.ORDER && nz(r.amount) !== 0)
    return {
      tooltip: { trigger: 'item', valueFormatter: (v) => yuan(v) },
      legend: { type: 'scroll', orient: 'vertical', right: 0, top: 'middle' },
      series: [{ type: 'pie', radius: ['40%', '70%'], center: ['35%', '50%'], data: data.map((r) => ({ name: typeLabel(r.itemType), value: Math.abs(nz(r.amount)) })) }]
    }
  }
  const top = [...rows.value].sort((a, b) => nz(b.sales) - nz(a.sales)).slice(0, 10)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => yuan(v) },
    legend: { top: 0 },
    grid,
    xAxis: {
      type: 'category',
      data: top.map(rowLabel),
      axisLabel: { interval: 0, rotate: top.length > 6 ? 20 : 0, formatter: (v) => (v.length > 14 ? `${v.slice(0, 14)}…` : v) }
    },
    yAxis: { type: 'value' },
    series: [
      { name: t('ecomReport.common.sales'), type: 'bar', barMaxWidth: 28, data: top.map((r) => nz(r.sales)) },
      { name: t('ecomReport.common.profit'), type: 'bar', barMaxWidth: 28, itemStyle: { color: '#52c41a' }, data: top.map((r) => nz(r.profit)) }
    ]
  }
})

async function doExport() {
  exporting.value = true
  try {
    const first = columns.value[0].key
    const name = `${t('ecomReport.profit.title')}_${t(`ecomReport.profit.${tab.value}`)}_${range.value[0]}_${range.value[1]}`
    await exportRows(name, columns.value, rows.value, { ...total.value, [first]: t('ecomReport.common.total') })
  } finally {
    exporting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}
.filter {
  width: 220px;
}
.tip {
  margin-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.tabs :deep(.ant-tabs-nav) {
  margin-bottom: 12px;
}
.chart-card {
  margin-bottom: 16px;
  border: 1px solid var(--app-border);
}
</style>
