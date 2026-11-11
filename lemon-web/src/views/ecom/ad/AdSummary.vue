<template>
  <div class="ad-summary">
    <div class="kpis">
      <div v-for="k in kpis" :key="k.key" class="kpi">
        <div class="sub-text">{{ k.label }}</div>
        <div class="kpi-value">{{ k.value }}</div>
      </div>
    </div>

    <div class="head">
      <a-radio-group v-model:value="groupBy" button-style="solid" @change="load">
        <a-radio-button v-for="g in GROUPS" :key="g" :value="g">{{ $t(`ecomReport.ad.${GROUP_LABELS[g]}`) }}</a-radio-button>
      </a-radio-group>
      <a-button :disabled="!rows.length" :loading="exporting" @click="doExport">
        <template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}
      </a-button>
    </div>

    <a-card v-if="rows.length" :bordered="false" size="small" :title="$t('ecomReport.ad.spendChart')" class="chart-card">
      <EChart :option="chartOption" height="260px" />
    </a-card>

    <a-table
      :row-key="rowKey"
      size="middle"
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: scrollX }"
      @change="(p) => applyPageChange(pagination, p)"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.kind === 'money'">{{ money(record[column.key]) }}</template>
        <template v-else-if="column.kind === 'int'">{{ num(record[column.key]) }}</template>
        <template v-else-if="column.kind === 'percent'">{{ percent(record[column.key], 2) }}</template>
        <template v-else-if="column.key === 'campaign'">{{ record.campaign || '-' }}</template>
        <template v-else-if="column.key === 'sellerSku'">{{ record.sellerSku || '-' }}</template>
      </template>
      <template v-if="rows.length" #summary>
        <a-table-summary fixed>
          <a-table-summary-row class="total-row">
            <a-table-summary-cell v-for="(c, i) in columns" :key="c.key" :index="i" :align="c.align">
              <template v-if="i === 0">{{ $t('ecomReport.common.total') }}</template>
              <template v-else-if="c.kind === 'money'">{{ money(total[c.key]) }}</template>
              <template v-else-if="c.kind === 'int'">{{ num(total[c.key]) }}</template>
              <template v-else-if="c.kind === 'percent'">{{ percent(total[c.key], 2) }}</template>
            </a-table-summary-cell>
          </a-table-summary-row>
        </a-table-summary>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import { percent } from '@/components/charts/charts'
import { applyPageChange, exportRows, localPagination, ratio, sumFields } from '@/components/ecom/ecomReport'
import { adSummary } from '@/api/ecom/ad'
import { sumWidth } from '@/composables/useColumns'
import { money, num, nz } from '@/utils/number'
import { t } from '@/locales'

/**
 * 广告汇总页签：按店铺 / 广告活动 / SKU 汇总（GET /ecom/ad/summary），顶部指标卡为全部行的合计，
 * 花费 TOP 10 的柱状图叠加 ACOS 折线。params 变化且页签可见时重新加载
 */
const props = defineProps({
  params: { type: Object, required: true },
  active: Boolean
})

/** 汇总方式（与后端 groupBy 一致） */
const GROUPS = ['store', 'campaign', 'sku']
const GROUP_LABELS = { store: 'byStore', campaign: 'byCampaign', sku: 'bySku' }

const storeCol = { title: t('ecomReport.common.store'), key: 'storeName', dataIndex: 'storeName', width: 160, ellipsis: true }
const DIMS = {
  store: [storeCol],
  campaign: [storeCol, { title: t('ecomReport.common.campaign'), key: 'campaign', dataIndex: 'campaign', width: 220, ellipsis: true }],
  sku: [storeCol, { title: t('ecomReport.common.sellerSku'), key: 'sellerSku', dataIndex: 'sellerSku', width: 160 }]
}
const metric = (key, kind, width) => ({ title: t(`ecomReport.ad.${key}`), key, dataIndex: key, kind, width, align: 'right' })
const METRICS = [
  metric('impressions', 'int', 110),
  metric('clicks', 'int', 100),
  metric('spend', 'money', 120),
  metric('adSales', 'money', 130),
  metric('adOrders', 'int', 100),
  metric('acos', 'percent', 100),
  metric('ctr', 'percent', 100),
  metric('cpc', 'money', 100)
]

const groupBy = ref('store')
const rows = ref([])
const loading = ref(false)
const exporting = ref(false)
const pagination = reactive(localPagination(20))

const columns = computed(() => [...DIMS[groupBy.value], ...METRICS])
const scrollX = computed(() => sumWidth(columns.value))
const rowKey = (r) => [r.storeId, r.campaign, r.sellerSku].join('|')

/** 合计：数量、金额求和，比率按合计重算 */
const total = computed(() => {
  const s = sumFields(rows.value, ['impressions', 'clicks', 'spend', 'adSales', 'adOrders'])
  return { ...s, acos: ratio(s.spend, s.adSales), ctr: ratio(s.clicks, s.impressions), cpc: nz(s.clicks) ? nz(s.spend) / nz(s.clicks) : null }
})

const kpis = computed(() =>
  METRICS.map((c) => ({
    key: c.key,
    label: c.title,
    value: c.kind === 'money' ? money(total.value[c.key]) : c.kind === 'int' ? num(total.value[c.key]) : percent(total.value[c.key], 2)
  }))
)

/** 行的名称：按汇总方式取店铺 / 广告活动 / SKU */
const rowLabel = (r) => (groupBy.value === 'campaign' ? r.campaign : groupBy.value === 'sku' ? r.sellerSku : r.storeName) || '-'

const chartOption = computed(() => {
  const top = [...rows.value].sort((a, b) => nz(b.spend) - nz(a.spend)).slice(0, 10)
  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 16, right: 16, top: 36, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: top.map(rowLabel),
      axisLabel: { interval: 0, rotate: top.length > 6 ? 20 : 0, formatter: (v) => (v.length > 14 ? `${v.slice(0, 14)}…` : v) }
    },
    yAxis: [
      { type: 'value' },
      { type: 'value', axisLabel: { formatter: '{value}%' }, splitLine: { show: false } }
    ],
    series: [
      {
        name: t('ecomReport.ad.spend'),
        type: 'bar',
        barMaxWidth: 28,
        data: top.map((r) => nz(r.spend)),
        tooltip: { valueFormatter: (v) => money(v) }
      },
      {
        name: t('ecomReport.ad.acos'),
        type: 'line',
        yAxisIndex: 1,
        data: top.map((r) => (r.acos === null || r.acos === undefined ? null : Number(r.acos))),
        tooltip: { valueFormatter: (v) => percent(v, 2) }
      }
    ]
  }
})

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const { keyword, ...p } = props.params
    const res = await adSummary({ ...p, groupBy: groupBy.value })
    if (current !== seq) return
    rows.value = res || []
    pagination.current = 1
  } finally {
    if (current === seq) loading.value = false
  }
}

/** 页签可见且条件变化时加载（切到别的页签时不加载，切回来再加载） */
let loaded = null
watch(
  () => [props.params, props.active],
  () => {
    if (props.active && loaded !== props.params) {
      loaded = props.params
      load()
    }
  },
  { immediate: true }
)

async function doExport() {
  exporting.value = true
  try {
    const first = columns.value[0].key
    await exportRows(`${t('ecomReport.ad.title')}_${t('ecomReport.ad.summary')}`, columns.value, rows.value, { ...total.value, [first]: t('ecomReport.common.total') })
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.kpis {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.kpi {
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--app-hover);
}
.kpi-value {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.chart-card {
  margin-bottom: 16px;
  border: 1px solid var(--app-border);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
:deep(.total-row) {
  font-weight: 600;
}
</style>
