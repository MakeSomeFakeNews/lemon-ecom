<template>
  <a-spin :spinning="loading">
    <!-- 核心指标：点击卡片切换趋势图展示的指标（最多两个） -->
    <div class="kpis">
      <div
        v-for="k in primary"
        :key="k.key"
        :class="['kpi', { active: selected.includes(k.key) }]"
        :style="selected.includes(k.key) ? { '--kpi-color': colorOf(k.key) } : null"
        role="button"
        tabindex="0"
        @click="toggleMetric(k.key)"
        @keydown.enter="toggleMetric(k.key)"
      >
        <div class="kpi-head">
          <span class="kpi-label">{{ k.label }}</span>
          <span v-if="selected.includes(k.key)" class="kpi-dot" />
        </div>
        <div class="kpi-value">{{ k.value }}</div>
        <div class="kpi-compare">
          <a-tooltip v-if="k.prev" :title="$t('ecomUiOps.ad.prevValue', { v: k.prevValue })">
            <span :class="['delta', k.prev.color]">{{ $t('ecomAds.metric.prev') }} {{ k.prev.text }}</span>
          </a-tooltip>
          <span v-else class="sub-text">{{ $t('ecomAds.metric.prev') }} -</span>
          <span v-if="k.yoy" :class="['delta', k.yoy.color]">{{ $t('ecomAds.metric.yoy') }} {{ k.yoy.text }}</span>
        </div>
        <Sparkline :values="series(k.key)" :color="colorOf(k.key)" :height="26" class="kpi-spark" />
      </div>
    </div>

    <!-- 次要指标与待办 -->
    <div class="minor">
      <div v-for="k in secondary" :key="k.key" class="minor-item">
        <div class="sub-text">{{ k.label }}</div>
        <div class="minor-value">{{ k.value }}</div>
        <div v-if="k.prev" :class="['delta', 'small', k.prev.color]">{{ k.prev.text }}</div>
        <div v-else-if="k.sub" class="sub-text">{{ k.sub }}</div>
      </div>
      <a-divider type="vertical" class="minor-divider" />
      <div class="minor-item">
        <div class="sub-text">{{ $t('ecomAds.overview.campaigns') }}</div>
        <div class="minor-value">{{ num(data.campaigns) }}</div>
        <div class="sub-text">{{ $t('ecomAds.overview.enabledCampaigns', { n: num(data.enabledCampaigns) }) }}</div>
      </div>
      <div class="minor-item link" role="button" tabindex="0" @click="emit('open-tab', 'logs')" @keydown.enter="emit('open-tab', 'logs')">
        <div class="sub-text">{{ $t('ecomAds.overview.pending') }} <RightOutlined class="link-icon" /></div>
        <div :class="['minor-value', { warn: data.pendingSuggestions > 0 }]">{{ num(data.pendingSuggestions) }}</div>
      </div>
      <div class="minor-item link" role="button" tabindex="0" @click="emit('open-tab', 'searchTerms')" @keydown.enter="emit('open-tab', 'searchTerms')">
        <div class="sub-text">{{ $t('ecomAds.overview.waste') }} <RightOutlined class="link-icon" /></div>
        <div :class="['minor-value', { warn: data.wasteTerms > 0 }]">{{ num(data.wasteTerms) }}</div>
        <div class="sub-text">{{ $t('ecomAds.overview.wasteSpend', { v: money(data.wasteSpend) }) }}</div>
      </div>
    </div>

    <a-row :gutter="16">
      <a-col :xs="24" :xl="16">
        <a-card :bordered="false" size="small" class="chart-card">
          <template #title>
            {{ $t('ecomUiOps.ad.trend') }}
            <span class="sub-text card-sub">{{ $t('ecomUiOps.ad.trendHint') }}</span>
          </template>
          <template #extra><span class="sub-text">{{ $t('ecomAds.overview.baseCurrency') }}</span></template>
          <EChart v-if="trend.length" :option="trendOption" height="300px" />
          <a-empty v-else :image="simpleImage" class="chart-empty" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="8">
        <a-card :bordered="false" size="small" :title="$t('ecomAds.overview.topCampaigns')" class="chart-card">
          <template #extra><a @click="emit('open-tab', 'campaigns')">{{ $t('ecomUiOps.ad.viewAll') }}</a></template>
          <div v-if="top.length" class="top-list">
            <div v-for="(r, i) in top" :key="r.id" class="top-item">
              <span :class="['top-rank', { hot: i < 3 }]">{{ i + 1 }}</span>
              <div class="top-main">
                <div class="top-line">
                  <span class="top-name" :title="r.name">{{ r.name }}</span>
                  <a-tooltip :title="r.baseSpend != null ? $t('ecomUiOps.ad.baseSpend', { v: money(r.baseSpend) }) : undefined">
                    <span class="top-spend">{{ money(r.spend) }} <span class="sub-text">{{ r.currency }}</span></span>
                  </a-tooltip>
                </div>
                <div class="top-bar"><div class="top-fill" :style="{ width: spendPercent(r) + '%' }" /></div>
                <div class="sub-text top-meta">
                  <span>{{ $t('ecomAds.metric.sales') }} {{ money(r.sales) }} {{ r.currency }}</span>
                  <span :class="acosClass(r.acos)">{{ $t('ecomAds.metric.acos') }} {{ fmt('percent', r.acos) }}</span>
                </div>
              </div>
            </div>
          </div>
          <a-empty v-else :image="simpleImage" class="chart-empty" />
        </a-card>
      </a-col>
    </a-row>
  </a-spin>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { Empty } from 'ant-design-vue'
import { RightOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import Sparkline from '@/components/ecom/Sparkline.vue'
import { adsHierarchy, adsOverview, adsTrend } from '@/api/ecom/ads'
import { EcomAdLevel } from '@/constants/enums'
import { money, num, nz } from '@/utils/number'
import { t } from '@/locales'
import { LOWER_BETTER, changeTag, fmt } from './adMetrics'

/**
 * 概览（参照领星广告）：核心指标卡（花费、广告销售额、ACOS、ROAS、点击率、转化率，含环比 / 同比与迷你趋势，点击切换趋势图指标），
 * 次要指标（TACOS、订单、点击、曝光、CPC、订单销售额）与待办（待处理建议、零转化搜索词），按天趋势和花费最高的活动（按本位币花费排序，显示原币）
 */
const props = defineProps({ params: { type: Object, required: true } })
const emit = defineEmits(['open-tab'])
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

const loading = ref(false)
const data = ref({ current: {}, prev: {}, yoy: {} })
const trend = ref([])
const top = ref([])

const KIND = { spend: 'money', sales: 'money', acos: 'percent', roas: 'ratio', ctr: 'percent', cvr: 'percent', orders: 'int', clicks: 'int', impressions: 'int', cpc: 'money' }
const COLORS = { spend: '#1677ff', sales: '#52c41a', acos: '#fa8c16', roas: '#722ed1', ctr: '#13c2c2', cvr: '#eb2f96' }
const colorOf = (k) => COLORS[k] || '#1677ff'
const PRIMARY = ['spend', 'sales', 'acos', 'roas', 'ctr', 'cvr']
const SECONDARY = ['orders', 'clicks', 'impressions', 'cpc']

const card = (key) => {
  const c = data.value.current || {}
  return {
    key,
    label: t(`ecomAds.metric.${key}`),
    value: fmt(KIND[key], c[key]),
    prevValue: fmt(KIND[key], data.value.prev?.[key]),
    prev: changeTag(c[key], data.value.prev?.[key], LOWER_BETTER.has(key)),
    yoy: changeTag(c[key], data.value.yoy?.[key], LOWER_BETTER.has(key))
  }
}
const primary = computed(() => PRIMARY.map(card))
const secondary = computed(() => [
  { key: 'tacos', label: t('ecomAds.metric.tacos'), value: fmt('percent', data.value.tacos) },
  ...SECONDARY.map(card),
  { key: 'totalSales', label: t('ecomAds.metric.totalSales'), value: money(data.value.totalSales) }
])

const series = (key) => trend.value.map((r) => (r[key] === null || r[key] === undefined ? null : Number(r[key])))

/** 趋势图指标：默认花费 + 广告销售额，点击卡片切换，最多两个（第三个替换最早选的） */
const selected = ref(['spend', 'sales'])
function toggleMetric(key) {
  const list = selected.value
  if (list.includes(key)) {
    if (list.length > 1) selected.value = list.filter((k) => k !== key)
  } else {
    selected.value = list.length >= 2 ? [list[1], key] : [...list, key]
  }
}

const isPercent = (k) => KIND[k] === 'percent'
const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 8, right: 8, top: 36, bottom: 4, containLabel: true },
  xAxis: { type: 'category', data: trend.value.map((r) => String(r.date).slice(5)), boundaryGap: selected.value.some((k) => k === 'spend') },
  yAxis: selected.value.map((k, i) => ({
    type: 'value',
    position: i ? 'right' : 'left',
    splitLine: { show: i === 0 },
    ...(isPercent(k) ? { axisLabel: { formatter: '{value}%' } } : {})
  })),
  series: selected.value.map((k, i) => ({
    name: t(`ecomAds.metric.${k}`),
    type: k === 'spend' ? 'bar' : 'line',
    yAxisIndex: i,
    smooth: true,
    showSymbol: false,
    barMaxWidth: 16,
    itemStyle: { color: colorOf(k) },
    lineStyle: { width: 2 },
    areaStyle: k === 'spend' ? undefined : { opacity: 0.06 },
    data: series(k),
    tooltip: { valueFormatter: (v) => fmt(KIND[k], v) }
  }))
}))

/** 花费条按本位币花费比较（后端已按本位币排序；没有汇率的活动按原币） */
const baseOf = (r) => nz(r.baseSpend ?? r.spend)
const maxSpend = computed(() => Math.max(0, ...top.value.map(baseOf)))
const spendPercent = (r) => (maxSpend.value ? Math.max(2, (baseOf(r) / maxSpend.value) * 100) : 0)
/** ACOS 颜色：超过 30% 偏高、超过 50% 很高（与常见卖家目标 ACOS 一致，仅作视觉提示） */
const acosClass = (v) => (v == null ? '' : Number(v) > 50 ? 'acos-bad' : Number(v) > 30 ? 'acos-warn' : 'acos-good')

let seq = 0
async function load() {
  const current = ++seq
  loading.value = true
  try {
    const [o, tr, h] = await Promise.all([adsOverview(props.params), adsTrend(props.params), adsHierarchy({ ...props.params, level: EcomAdLevel.CAMPAIGN })])
    if (current !== seq) return
    data.value = o || { current: {}, prev: {}, yoy: {} }
    trend.value = tr || []
    top.value = (h?.rows || []).filter((r) => nz(r.spend) > 0).slice(0, 8)
  } finally {
    if (current === seq) loading.value = false
  }
}

watch(() => props.params, load, { immediate: true })
</script>

<style scoped>
.kpis {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}
@media (max-width: 1280px) {
  .kpis {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
.kpi {
  --kpi-color: transparent;
  position: relative;
  padding: 12px 14px 8px;
  border: 1px solid var(--app-border);
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.kpi::before {
  content: '';
  position: absolute;
  top: -1px;
  left: 12px;
  right: 12px;
  height: 3px;
  border-radius: 0 0 3px 3px;
  background: var(--kpi-color);
}
.kpi:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.kpi.active {
  border-color: var(--kpi-color);
}
.kpi-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.kpi-label {
  color: var(--app-text-3);
  font-size: 13px;
}
.kpi-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--kpi-color);
}
.kpi-value {
  margin-top: 4px;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.kpi-compare {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 10px;
  min-height: 20px;
  margin-top: 2px;
  font-size: 12px;
}
.kpi-spark {
  margin-top: 6px;
}
.minor {
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
  gap: 8px 0;
  margin-bottom: 16px;
  padding: 10px 4px;
  border-radius: 8px;
  background: var(--app-hover);
}
.minor-item {
  flex: 1 1 110px;
  padding: 0 14px;
  min-width: 0;
}
.minor-item.link {
  cursor: pointer;
}
.minor-item.link:hover .minor-value {
  color: var(--ant-color-primary, #1677ff);
}
.minor-divider {
  height: auto;
  margin: 0 4px;
}
.minor-value {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}
.minor-value.warn {
  color: #d46b08;
}
.link-icon {
  font-size: 10px;
}
.delta.green {
  color: #389e0d;
}
.delta.red {
  color: #cf1322;
}
.delta.small {
  font-size: 12px;
}
.chart-card {
  margin-bottom: 16px;
  border: 1px solid var(--app-border);
}
.chart-card :deep(.ant-card-body) {
  min-height: 316px;
}
.card-sub {
  margin-left: 8px;
  font-weight: normal;
}
.chart-empty {
  padding-top: 90px;
}
.top-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.top-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.top-rank {
  flex: none;
  width: 20px;
  height: 20px;
  margin-top: 1px;
  border-radius: 50%;
  background: var(--app-hover);
  color: var(--app-text-3);
  font-size: 12px;
  line-height: 20px;
  text-align: center;
}
.top-rank.hot {
  background: #1677ff;
  color: #fff;
}
.top-main {
  flex: 1;
  min-width: 0;
}
.top-line {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
.top-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.top-spend {
  flex: none;
  font-weight: 500;
}
.top-bar {
  height: 4px;
  margin: 4px 0 2px;
  border-radius: 2px;
  background: var(--app-hover);
}
.top-fill {
  height: 100%;
  border-radius: 2px;
  background: #1677ff;
}
.top-meta {
  display: flex;
  justify-content: space-between;
}
.acos-good {
  color: #389e0d;
}
.acos-warn {
  color: #d46b08;
}
.acos-bad {
  color: #cf1322;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
