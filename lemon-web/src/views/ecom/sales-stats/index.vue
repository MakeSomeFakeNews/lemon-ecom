<template>
  <div class="sales-stats-page">
    <a-card :bordered="false" class="page-card">
      <div class="filters">
        <a-select v-model:value="query.dimension" :options="dimensionOptions" class="dim" @change="load" />
        <a-radio-group v-model:value="query.grain" button-style="solid" @change="onGrain">
          <a-radio-button v-for="g in GRAINS" :key="g" :value="g">{{ $t(`ecomAnalytics.sales.grain${g}`) }}</a-radio-button>
        </a-radio-group>
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" />
        <StoreSelect v-model:value="query.storeId" allow-clear :placeholder="$t('ecomAnalytics.common.allStores')" class="filter" />
        <a-select
          v-model:value="query.platform"
          allow-clear
          :placeholder="$t('ecomAnalytics.common.allPlatforms')"
          :options="toOptions(dict.ecom_platform)"
          class="filter-sm"
        />
        <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomAnalytics.common.keywordPh')" class="filter" @press-enter="load" />
        <a-button type="primary" :loading="loading" @click="load">
          <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
        </a-button>
        <a-button v-perm="'ecom:salesStats:export'" :disabled="!matrix?.rows.length" :loading="exporting" @click="doExport">
          <template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}
        </a-button>
      </div>
      <div class="sub-text tip">{{ $t('ecomAnalytics.sales.tip') }}</div>
    </a-card>

    <a-card v-if="matrix" :bordered="false" class="page-card">
      <div class="head">
        <div class="kpis">
          <div class="kpi">
            <div class="sub-text">{{ $t('ecomAnalytics.common.qty') }}</div>
            <div class="kpi-value">{{ qty(matrix.total.totalQty) }}</div>
            <div class="sub-text">
              {{ $t('ecomAnalytics.sales.mom') }} <GrowthText :value="matrix.total.momQtyPercent" />
              {{ $t('ecomAnalytics.sales.yoy') }} <GrowthText :value="matrix.total.yoyQtyPercent" />
            </div>
          </div>
          <div class="kpi">
            <div class="sub-text">{{ $t('ecomAnalytics.common.sales') }}</div>
            <div class="kpi-value">{{ money(matrix.total.totalAmount) }}</div>
            <div class="sub-text">
              {{ $t('ecomAnalytics.sales.mom') }} <GrowthText :value="matrix.total.momAmountPercent" />
              {{ $t('ecomAnalytics.sales.yoy') }} <GrowthText :value="matrix.total.yoyAmountPercent" />
            </div>
          </div>
          <div class="kpi">
            <div class="sub-text">{{ $t('ecomAnalytics.common.orders') }}</div>
            <div class="kpi-value">{{ num(matrix.total.totalOrders) }}</div>
            <div class="sub-text">
              {{ $t('ecomAnalytics.sales.compareRange', { prev: `${matrix.prevBegin} ~ ${matrix.prevEnd}`, yoy: `${matrix.yoyBegin} ~ ${matrix.yoyEnd}` }) }}
            </div>
          </div>
        </div>
        <a-radio-group v-model:value="measure" size="small">
          <a-radio-button value="qty">{{ $t('ecomAnalytics.common.qty') }}</a-radio-button>
          <a-radio-button value="amount">{{ $t('ecomAnalytics.common.sales') }}</a-radio-button>
        </a-radio-group>
      </div>
      <EChart :option="chartOption" height="260px" class="chart" />
      <SalesMatrixTable :matrix="matrix" :measure="measure" :loading="loading" @trend="openTrend" />
    </a-card>

    <SkuTrendDrawer ref="trendDrawer" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { salesMatrix } from '@/api/ecom/analytics'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'
import { money, num, nz, qty } from '@/utils/number'
import { SalesDimension, SalesGrain } from '@/constants/enums'
import { t } from '@/locales'
import { lastDays, rangeParams, rangePresets } from '../product-analysis/analytics'
import GrowthText from './GrowthText.vue'
import SalesMatrixTable from './SalesMatrixTable.vue'
import SkuTrendDrawer from './SkuTrendDrawer.vue'
import { exportMatrix, periodTitle, rowLabel } from './salesStats'

/**
 * 销量统计（GET /ecom/analytics/sales/matrix）：按 SKU / SPU / 物料 / 店铺 / 平台 / 国家 × 日 / 周 / 月的件数与销售额矩阵，
 * 环比、同比；SKU 行可打开销量走势（叠加库存快照与断货日）。导出 Excel 需要 ecom:salesStats:export
 */
const GRAINS = [SalesGrain.DAY, SalesGrain.WEEK, SalesGrain.MONTH]
/** 按日最多 92 天，切换粒度时给出合适的默认范围 */
const DEFAULT_DAYS = { [SalesGrain.DAY]: 30, [SalesGrain.WEEK]: 84, [SalesGrain.MONTH]: 365 }
const dict = useDict('ecom_platform')
const presets = rangePresets()
const dimensionOptions = Object.values(SalesDimension).map((d) => ({ value: d, label: t(`ecomAnalytics.sales.dim${d}`) }))
const query = reactive({ dimension: SalesDimension.SKU, grain: SalesGrain.DAY, storeId: undefined, platform: undefined, keyword: '' })
const range = ref(lastDays(30))
const measure = ref('qty')
const matrix = ref(null)
const loading = ref(false)
const exporting = ref(false)
const trendDrawer = ref()

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const res = await salesMatrix({ ...query, ...rangeParams(range.value), keyword: query.keyword?.trim() || undefined })
    if (current === seq) matrix.value = res
  } finally {
    if (current === seq) loading.value = false
  }
}

function onGrain() {
  range.value = lastDays(DEFAULT_DAYS[query.grain])
  load()
}

const openTrend = (row) => trendDrawer.value.open(row.listingId, range.value)

/** 合计走势（柱）与销售额前 5 的对象（线） */
const chartOption = computed(() => {
  const m = matrix.value
  const key = measure.value
  const top = m.rows.slice(0, 5)
  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0, type: 'scroll' },
    grid: { left: 16, right: 16, top: 36, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: m.periods.map((p) => periodTitle(m.grain, p)) },
    yAxis: { type: 'value' },
    series: [
      { name: t('ecomAnalytics.common.total'), type: 'bar', barMaxWidth: 24, itemStyle: { opacity: 0.45 }, data: m.total[key].map(nz) },
      ...top.map((r) => ({ name: rowLabel(m.dimension, r), type: 'line', smooth: true, showSymbol: false, data: r[key].map(nz) }))
    ]
  }
})

async function doExport() {
  exporting.value = true
  try {
    await exportMatrix(matrix.value, measure.value)
  } catch (e) {
    message.error(e.message)
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
.dim {
  width: 140px;
}
.filter {
  width: 200px;
}
.filter-sm {
  width: 140px;
}
.tip {
  margin-top: 8px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.kpis {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.kpi {
  min-width: 200px;
  padding: 10px 16px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
}
.kpi-value {
  margin: 2px 0;
  font-size: 20px;
  font-weight: 600;
}
.chart {
  margin: 12px 0;
}
</style>
