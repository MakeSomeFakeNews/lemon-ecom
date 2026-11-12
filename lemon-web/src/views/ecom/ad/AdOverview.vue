<template>
  <a-spin :spinning="loading">
    <div class="sub-text head-tip">{{ $t('ecomAds.overview.baseCurrency') }}</div>
    <div class="kpis">
      <div v-for="k in kpis" :key="k.key" class="kpi">
        <div class="sub-text">{{ k.label }}</div>
        <div class="kpi-value">{{ k.value }}</div>
        <div class="kpi-compare">
          <span v-if="k.prev" :class="['delta', k.prev.color]">{{ $t('ecomAds.metric.prev') }} {{ k.prev.text }}</span>
          <span v-if="k.yoy" :class="['delta', k.yoy.color]">{{ $t('ecomAds.metric.yoy') }} {{ k.yoy.text }}</span>
        </div>
      </div>
    </div>
    <div class="kpis">
      <div class="kpi">
        <div class="sub-text">{{ $t('ecomAds.overview.campaigns') }}</div>
        <div class="kpi-value">{{ num(data.campaigns) }}</div>
        <div class="sub-text">{{ $t('ecomAds.overview.enabledCampaigns', { n: num(data.enabledCampaigns) }) }}</div>
      </div>
      <div class="kpi link" @click="emit('open-tab', 'logs')">
        <div class="sub-text">{{ $t('ecomAds.overview.pending') }}</div>
        <div class="kpi-value">{{ num(data.pendingSuggestions) }}</div>
      </div>
      <div class="kpi link" @click="emit('open-tab', 'searchTerms')">
        <div class="sub-text">{{ $t('ecomAds.overview.waste') }}</div>
        <div class="kpi-value">{{ num(data.wasteTerms) }}</div>
        <div class="sub-text">{{ $t('ecomAds.overview.wasteSpend', { v: money(data.wasteSpend) }) }}</div>
      </div>
      <div class="kpi">
        <div class="sub-text">{{ $t('ecomAds.metric.totalSales') }}</div>
        <div class="kpi-value">{{ money(data.totalSales) }}</div>
      </div>
    </div>

    <a-row :gutter="16">
      <a-col :xs="24" :xl="14">
        <a-card :bordered="false" size="small" :title="$t('ecomAds.overview.trend')" class="chart-card">
          <EChart :option="trendOption" height="300px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="10">
        <a-card :bordered="false" size="small" :title="$t('ecomAds.overview.topCampaigns')" class="chart-card">
          <a-table row-key="id" size="small" :columns="topColumns" :data-source="top" :pagination="false" :scroll="{ x: 520 }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.kind">{{ fmt(column.kind, record[column.key]) }}</template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </a-spin>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import EChart from '@/components/charts/EChart.vue'
import { adsHierarchy, adsOverview, adsTrend } from '@/api/ecom/ads'
import { money, num, nz } from '@/utils/number'
import { t } from '@/locales'
import { LOWER_BETTER, changeTag, fmt } from './adMetrics'

/**
 * 概览：本位币的花费、广告销售额、ACOS、ROAS、TACOS 等指标卡（含环比、同比），活动数、待处理建议、零转化搜索词，
 * 按天趋势和花费最高的活动
 */
const props = defineProps({ params: { type: Object, required: true } })
const emit = defineEmits(['open-tab'])

const loading = ref(false)
const data = ref({ current: {}, prev: {}, yoy: {} })
const trend = ref([])
const top = ref([])

const CARDS = [
  { key: 'spend', kind: 'money' },
  { key: 'sales', kind: 'money' },
  { key: 'acos', kind: 'percent' },
  { key: 'roas', kind: 'ratio' },
  { key: 'orders', kind: 'int' },
  { key: 'clicks', kind: 'int' },
  { key: 'impressions', kind: 'int' },
  { key: 'ctr', kind: 'percent' },
  { key: 'cpc', kind: 'money' },
  { key: 'cvr', kind: 'percent' }
]

const kpis = computed(() => {
  const c = data.value.current || {}
  const list = CARDS.map((k) => ({
    key: k.key,
    label: t(`ecomAds.metric.${k.key}`),
    value: fmt(k.kind, c[k.key]),
    prev: changeTag(c[k.key], data.value.prev?.[k.key], LOWER_BETTER.has(k.key)),
    yoy: changeTag(c[k.key], data.value.yoy?.[k.key], LOWER_BETTER.has(k.key))
  }))
  list.splice(4, 0, { key: 'tacos', label: t('ecomAds.metric.tacos'), value: fmt('percent', data.value.tacos) })
  return list
})

const topColumns = [
  { title: t('ecomAds.common.campaign'), dataIndex: 'name', key: 'name', width: 180, ellipsis: true },
  { title: t('ecomAds.metric.spend'), key: 'spend', kind: 'money', width: 100, align: 'right' },
  { title: t('ecomAds.metric.sales'), key: 'sales', kind: 'money', width: 110, align: 'right' },
  { title: t('ecomAds.metric.acos'), key: 'acos', kind: 'percent', width: 80, align: 'right' },
  { title: t('ecomAds.common.currency'), dataIndex: 'currency', key: 'currency', width: 60 }
]

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 16, right: 16, top: 36, bottom: 8, containLabel: true },
  xAxis: { type: 'category', data: trend.value.map((r) => String(r.date).slice(5)) },
  yAxis: [{ type: 'value' }, { type: 'value', axisLabel: { formatter: '{value}%' }, splitLine: { show: false } }],
  series: [
    { name: t('ecomAds.metric.spend'), type: 'bar', barMaxWidth: 18, data: trend.value.map((r) => nz(r.spend)), tooltip: { valueFormatter: (v) => money(v) } },
    { name: t('ecomAds.metric.sales'), type: 'line', smooth: true, data: trend.value.map((r) => nz(r.sales)), tooltip: { valueFormatter: (v) => money(v) } },
    {
      name: t('ecomAds.metric.acos'),
      type: 'line',
      yAxisIndex: 1,
      data: trend.value.map((r) => (r.acos === null || r.acos === undefined ? null : Number(r.acos))),
      tooltip: { valueFormatter: (v) => fmt('percent', v) }
    }
  ]
}))

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const [o, tr, h] = await Promise.all([adsOverview(props.params), adsTrend(props.params), adsHierarchy({ ...props.params, level: 'CAMPAIGN' })])
    if (current !== seq) return
    data.value = o || { current: {}, prev: {}, yoy: {} }
    trend.value = tr || []
    top.value = (h?.rows || []).filter((r) => nz(r.spend) > 0).slice(0, 10)
  } finally {
    if (current === seq) loading.value = false
  }
}

watch(() => props.params, load, { immediate: true })
</script>

<style scoped>
.head-tip {
  margin-bottom: 8px;
}
.kpis {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.kpi {
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--app-hover);
}
.kpi.link {
  cursor: pointer;
}
.kpi-value {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}
.kpi-compare {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 8px;
  margin-top: 2px;
  font-size: 12px;
}
.delta.green {
  color: #389e0d;
}
.delta.red {
  color: #cf1322;
}
.chart-card {
  margin-bottom: 16px;
  border: 1px solid var(--app-border);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
