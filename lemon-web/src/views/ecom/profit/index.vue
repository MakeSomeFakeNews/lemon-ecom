<template>
  <div class="profit-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" />
        <StoreSelect v-model:value="storeId" :placeholder="$t('ecomReport.common.allStores')" class="filter" />
        <a-radio-group v-if="tab !== 'fees'" v-model:value="basis" button-style="solid" @change="load">
          <a-radio-button v-for="b in BASES" :key="b" :value="b">{{ $t(`ecomProfit.basis.${b}`) }}</a-radio-button>
        </a-radio-group>
        <a-input
          v-if="KEYWORD_TABS.includes(tab)"
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
      <div class="sub-text tip">{{ tab === 'fees' ? $t('ecomProfit.feesTip') : $t(`ecomProfit.basisTip.${basis}`) }}</div>
    </a-card>

    <a-card :bordered="false" class="page-card">
      <a-tabs v-model:active-key="tab" class="tabs" @change="load">
        <a-tab-pane v-for="k in TABS" :key="k" :tab="k === 'fees' ? $t('ecomReport.profit.fees') : $t(`ecomProfit.dimension.${k}`)" />
      </a-tabs>

      <a-card v-if="rows.length" :bordered="false" size="small" :title="chartTitle" class="chart-card">
        <EChart :option="chartOption" height="260px" />
      </a-card>

      <ProfitTable :columns="columns" :rows="rows" :total="total" :loading="loading" :row-key="rowKey" />
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
import { reportFees, reportProfit } from '@/api/ecom/report'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { nz } from '@/utils/number'
import { ProfitBasis, ProfitDimension, SettlementItemType } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 利润报表：按店铺 / 平台 / 站点 / SKU / SPU / 物料 / 负责人 / 国家 / 月份汇总（GET /ecom/report/profit，本位币），
 * 订单口径（下单时间，实时估算）与结算口径（结算入账日期，实际费用）切换；费用构成按结算明细类型。接口返回数组，前端分页、合计与导出
 */
const TABS = [...Object.values(ProfitDimension), 'fees']
const BASES = Object.values(ProfitBasis)
const KEYWORD_TABS = [ProfitDimension.SKU, ProfitDimension.SPU, ProfitDimension.MATERIAL]

const dict = useDict('ecom_settlement_item_type', 'ecom_platform')
const presets = rangePresets()
const tab = ref(ProfitDimension.STORE)
const basis = ref(ProfitBasis.ORDER)
const range = ref(thisMonth())
const storeId = ref()
const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const exporting = ref(false)

const col = (title, key, extra = {}) => ({ title, key, dataIndex: key, ...extra })
const m = (key, title, extra = {}) => col(title, key, { kind: 'money', width: 120, align: 'right', ...extra })
const pct = (key, title) => col(title, key, { kind: 'percent', width: 90, align: 'right' })
const store = col(t('ecomReport.common.store'), 'storeName', { width: 160, ellipsis: true })

/** 指标列：数量 → 收入 → 费用 → 成本 → 税 → 毛利 */
const METRICS = [
  col(t('ecomReport.common.orders'), 'orders', { kind: 'int', width: 90, align: 'right' }),
  col(t('common.field.quantity'), 'qty', { kind: 'qty', width: 100, align: 'right' }),
  m('salesGross', t('ecomProfit.col.salesGross'), { width: 130 }),
  m('salesNet', t('ecomProfit.col.salesNet'), { width: 130 }),
  m('commission', t('ecomProfit.col.commission')),
  m('fbaFee', t('ecomProfit.col.fbaFee')),
  m('storageFee', t('ecomProfit.col.storageFee')),
  m('adSpend', t('ecomReport.common.adSpend')),
  m('refund', t('ecomReport.common.refund')),
  m('returnFee', t('ecomProfit.col.returnFee')),
  m('otherFee', t('ecomProfit.col.otherFee')),
  m('cost', t('ecomReport.common.cost')),
  m('headCost', t('ecomProfit.col.headCost')),
  m('lastMile', t('ecomProfit.col.lastMile')),
  m('vat', t('ecomProfit.col.vat')),
  m('withheldTax', t('ecomProfit.col.withheldTax')),
  m('profit', t('ecomProfit.col.profit'), { width: 130 }),
  pct('margin', t('ecomProfit.col.margin')),
  pct('roi', t('ecomProfit.col.roi'))
]
const LABELS = {
  STORE: [
    store,
    col(t('ecomReport.common.platform'), 'platform', { kind: 'dict', dict: 'ecom_platform', width: 100 }),
    col(t('ecomReport.common.marketplace'), 'marketplace', { width: 80 })
  ],
  PLATFORM: [col(t('ecomReport.common.platform'), 'platform', { kind: 'dict', dict: 'ecom_platform', width: 120 })],
  MARKETPLACE: [col(t('ecomReport.common.marketplace'), 'marketplace', { width: 100 })],
  SKU: [
    store,
    col(t('ecomReport.common.sellerSku'), 'sellerSku', { width: 150 }),
    col(t('ecomReport.profit.listingTitle'), 'title', { width: 200, ellipsis: true }),
    col(t('ecomProfit.col.spu'), 'spu', { width: 130 }),
    col(t('ecomReport.profit.material'), 'material', { width: 180, ellipsis: true, exportValue: (r) => [r.materialCode, r.materialName].filter(Boolean).join(' ') })
  ],
  SPU: [store, col(t('ecomProfit.col.spu'), 'spu', { width: 150 }), col(t('ecomReport.profit.listingTitle'), 'title', { width: 220, ellipsis: true })],
  MATERIAL: [
    col(t('ecomReport.profit.material'), 'material', { width: 240, ellipsis: true, exportValue: (r) => [r.materialCode, r.materialName].filter(Boolean).join(' ') })
  ],
  OWNER: [col(t('ecomProfit.col.owner'), 'ownerName', { width: 140, exportValue: (r) => r.ownerName || '' })],
  COUNTRY: [col(t('ecomReport.profit.country'), 'country', { width: 110 })],
  MONTH: [col(t('ecomProfit.col.month'), 'month', { width: 110 })]
}
const FEE_COLUMNS = [
  col(t('ecomReport.profit.itemType'), 'itemType', { kind: 'dict', dict: 'ecom_settlement_item_type', width: 160 }),
  m('amount', t('common.field.amount')),
  col(t('ecomReport.common.share'), 'share', { kind: 'percent', width: 100, align: 'right' })
]
const columns = computed(() => (tab.value === 'fees' ? FEE_COLUMNS : [...LABELS[tab.value], ...METRICS]))
const rowKey = (r) => (tab.value === 'fees' ? r.itemType : r.dimKey || '-')

/** 合计行：数量、金额求和，毛利率、ROI 按合计重算；费用构成的占比不合计 */
const total = computed(() => {
  const numeric = columns.value.filter((c) => ['int', 'qty', 'money'].includes(c.kind)).map((c) => c.key)
  const s = sumFields(rows.value, numeric)
  if (tab.value !== 'fees') {
    s.margin = ratio(s.profit, s.salesNet)
    s.roi = ratio(s.profit, nz(s.cost) + nz(s.headCost))
  }
  return s
})

/** 费用构成的占比：按费用类型的绝对值，不含订单收入 */
function enrich(list) {
  if (tab.value !== 'fees') return list
  const feeRows = list.filter((r) => r.itemType !== SettlementItemType.ORDER)
  const feeTotal = feeRows.reduce((s, r) => s + Math.abs(nz(r.amount)), 0)
  return list.map((r) => ({ ...r, share: r.itemType === SettlementItemType.ORDER ? null : ratio(Math.abs(nz(r.amount)), feeTotal) }))
}

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const params = { ...rangeParams(range.value), storeId: storeId.value || undefined }
    let res
    if (tab.value === 'fees') {
      res = await reportFees(params)
    } else {
      params.basis = basis.value
      params.dimension = tab.value
      if (KEYWORD_TABS.includes(tab.value)) params.keyword = keyword.value?.trim() || undefined
      res = await reportProfit(params)
    }
    if (current !== seq) return
    rows.value = enrich(res || [])
  } finally {
    if (current === seq) loading.value = false
  }
}

const typeLabel = (v) => findDict(dict.ecom_settlement_item_type, v)?.label || v
const platformLabel = (v) => findDict(dict.ecom_platform, v)?.label || v
function rowLabel(r) {
  switch (tab.value) {
    case ProfitDimension.STORE:
      return r.storeName
    case ProfitDimension.PLATFORM:
      return r.platform ? platformLabel(r.platform) : ''
    case ProfitDimension.MARKETPLACE:
      return r.marketplace
    case ProfitDimension.SKU:
      return r.sellerSku
    case ProfitDimension.SPU:
      return r.spu
    case ProfitDimension.MATERIAL:
      return r.materialName || r.materialCode
    case ProfitDimension.OWNER:
      return r.ownerName
    case ProfitDimension.COUNTRY:
      return r.country
    default:
      return r.month
  }
}
const grid = { left: 16, right: 16, top: 36, bottom: 8, containLabel: true }
const chartTitle = computed(() =>
  tab.value === 'fees' ? t('ecomReport.profit.feeChartTitle') : tab.value === ProfitDimension.MONTH ? t('ecomProfit.monthChart') : t('ecomProfit.topChart')
)

const chartOption = computed(() => {
  if (tab.value === 'fees') {
    const data = rows.value.filter((r) => r.itemType !== SettlementItemType.ORDER && nz(r.amount) !== 0)
    return {
      tooltip: { trigger: 'item', valueFormatter: (v) => yuan(v) },
      legend: { type: 'scroll', orient: 'vertical', right: 0, top: 'middle' },
      series: [{ type: 'pie', radius: ['40%', '70%'], center: ['35%', '50%'], data: data.map((r) => ({ name: typeLabel(r.itemType), value: Math.abs(nz(r.amount)) })) }]
    }
  }
  const month = tab.value === ProfitDimension.MONTH
  const list = month ? rows.value : [...rows.value].sort((a, b) => nz(b.salesNet) - nz(a.salesNet)).slice(0, 10)
  const type = month ? 'line' : 'bar'
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: month ? 'line' : 'shadow' }, valueFormatter: (v) => yuan(v) },
    legend: { top: 0 },
    grid,
    xAxis: {
      type: 'category',
      data: list.map((r) => rowLabel(r) || '-'),
      axisLabel: { interval: 0, rotate: list.length > 6 ? 20 : 0, formatter: (v) => (v.length > 14 ? `${v.slice(0, 14)}…` : v) }
    },
    yAxis: { type: 'value' },
    series: [
      { name: t('ecomProfit.col.salesNet'), type, barMaxWidth: 28, smooth: true, data: list.map((r) => nz(r.salesNet)) },
      { name: t('ecomProfit.col.profit'), type, barMaxWidth: 28, smooth: true, itemStyle: { color: '#52c41a' }, data: list.map((r) => nz(r.profit)) }
    ]
  }
})

async function doExport() {
  exporting.value = true
  try {
    const first = columns.value[0].key
    const tabName = tab.value === 'fees' ? t('ecomReport.profit.fees') : `${t(`ecomProfit.dimension.${tab.value}`)}_${t(`ecomProfit.basis.${basis.value}`)}`
    const name = `${t('ecomReport.profit.title')}_${tabName}_${range.value[0]}_${range.value[1]}`
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
