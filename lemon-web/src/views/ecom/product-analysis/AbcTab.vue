<template>
  <div class="filters">
    <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" @change="load" />
    <StoreSelect v-model:value="storeId" allow-clear :placeholder="$t('ecomAnalytics.common.allStores')" class="filter" @update:value="load" />
    <a-input v-model:value="keyword" allow-clear :placeholder="$t('ecomAnalytics.common.keywordPh')" class="filter" @press-enter="load" />
    <a-radio-group v-model:value="metric" button-style="solid" @change="load">
      <a-radio-button :value="AbcMetric.SALES">{{ $t('ecomAnalytics.product.bySales') }}</a-radio-button>
      <a-radio-button :value="AbcMetric.PROFIT">{{ $t('ecomAnalytics.product.byProfit') }}</a-radio-button>
    </a-radio-group>
    <span class="threshold">
      {{ $t('ecomAnalytics.product.thresholdA') }}
      <a-input-number v-model:value="aPercent" :min="1" :max="99" :precision="0" size="small" class="pct" />%
      {{ $t('ecomAnalytics.product.thresholdB') }}
      <a-input-number v-model:value="bPercent" :min="2" :max="100" :precision="0" size="small" class="pct" />%
    </span>
    <a-button type="primary" :loading="loading" @click="load">
      <template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}
    </a-button>
    <a-button v-perm="'ecom:productAnalysis:export'" :disabled="!rows.length" @click="doExport">
      <template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}
    </a-button>
  </div>
  <div class="sub-text tip">{{ $t('ecomAnalytics.product.abcTip') }}</div>

  <div v-if="result" class="summary">
    <div v-for="s in result.summary" :key="s.abcClass" class="stat" :class="`stat-${s.abcClass}`">
      <div class="stat-label"><DictTag type="ecom_abc_class" :value="s.abcClass" /></div>
      <div class="stat-value">{{ $t('ecomAnalytics.product.skuCount', { n: s.count }) }}</div>
      <div class="sub-text">
        {{ $t('ecomAnalytics.product.classShare', { value: money(s.value), share: percent(s.sharePercent, 1), count: percent(s.countPercent, 1) }) }}
      </div>
    </div>
  </div>

  <a-card v-if="rows.length" :bordered="false" size="small" :title="$t('ecomAnalytics.product.paretoTitle')" class="chart-card">
    <EChart :option="chartOption" height="260px" />
  </a-card>

  <AnalyticsTable :columns="columns" :rows="rows" :loading="loading" :row-key="(r) => `${r.storeId}|${r.sellerSku}`" />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { percent } from '@/components/charts/charts'
import { productAbc } from '@/api/ecom/analytics'
import { money, nz } from '@/utils/number'
import { AbcMetric } from '@/constants/enums'
import { t } from '@/locales'
import AnalyticsTable from './AnalyticsTable.vue'
import { col, exportTable, lastDays, materialCol, rangeParams, rangePresets, skuCol, storeCol } from './analytics'

/** ABC 分析：按 SKU 的销售额或毛利从高到低累计，阈值可调（默认 70% / 90%） */
const presets = rangePresets()
const range = ref(lastDays(30))
const storeId = ref()
const keyword = ref('')
const metric = ref(AbcMetric.SALES)
const aPercent = ref(70)
const bPercent = ref(90)
const result = ref(null)
const loading = ref(false)
const rows = computed(() => result.value?.rows || [])

const columns = [
  col('abcClass', t('ecomAnalytics.product.abcClass'), 'dict', { dict: 'ecom_abc_class', width: 90, fixed: 'left' }),
  storeCol(),
  skuCol(),
  materialCol(),
  col('orders', t('ecomAnalytics.common.orders'), 'int'),
  col('qty', t('ecomAnalytics.common.qty'), 'qty'),
  col('sales', t('ecomAnalytics.common.sales'), 'money'),
  col('profit', t('ecomAnalytics.common.profit'), 'money'),
  col('sharePercent', t('ecomAnalytics.product.share'), 'percent'),
  col('cumSharePercent', t('ecomAnalytics.product.cumShare'), 'percent')
]

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const res = await productAbc({
      ...rangeParams(range.value),
      storeId: storeId.value || undefined,
      keyword: keyword.value?.trim() || undefined,
      metric: metric.value,
      aPercent: aPercent.value,
      bPercent: bPercent.value
    })
    if (current === seq) result.value = res
  } finally {
    if (current === seq) loading.value = false
  }
}

/** 帕累托图：前 30 个 SKU 的指标（柱）与累计占比（线） */
const chartOption = computed(() => {
  const top = rows.value.slice(0, 30)
  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { left: 16, right: 16, top: 36, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: top.map((r) => r.sellerSku), axisLabel: { rotate: top.length > 8 ? 30 : 0 } },
    yAxis: [{ type: 'value' }, { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } }],
    series: [
      {
        name: metric.value === AbcMetric.PROFIT ? t('ecomAnalytics.common.profit') : t('ecomAnalytics.common.sales'),
        type: 'bar',
        barMaxWidth: 24,
        data: top.map((r) => nz(r.value))
      },
      {
        name: t('ecomAnalytics.product.cumShare'),
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        data: top.map((r) => nz(r.cumSharePercent)),
        // A / B 类阈值参考线
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { type: 'dashed', color: '#fa8c16' },
          label: { position: 'insideEndTop', formatter: (p) => `${p.value}%` },
          data: [{ yAxis: aPercent.value }, { yAxis: bPercent.value }]
        }
      }
    ]
  }
})

async function doExport() {
  await exportTable(`${t('ecomAnalytics.product.tabAbc')}_${range.value[0]}_${range.value[1]}`, columns, rows.value)
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
  width: 200px;
}
.pct {
  width: 64px;
  margin: 0 4px;
}
.threshold {
  white-space: nowrap;
}
.tip {
  margin: 8px 0 12px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.summary {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.stat {
  padding: 12px 16px;
  border: 1px solid var(--app-border);
  border-left-width: 3px;
  border-radius: 8px;
}
.stat-A {
  border-left-color: #f5222d;
}
.stat-B {
  border-left-color: #fa8c16;
}
.stat-C {
  border-left-color: #8c8c8c;
}
.stat-value {
  margin: 4px 0;
  font-size: 18px;
  font-weight: 600;
}
.chart-card {
  margin-bottom: 16px;
  border: 1px solid var(--app-border);
}
</style>
