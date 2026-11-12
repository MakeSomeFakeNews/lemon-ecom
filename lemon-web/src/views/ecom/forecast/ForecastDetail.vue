<template>
  <SubPage :open="open" :title="title" @close="open = false">
    <template #extra>
      <a-button v-if="detail?.fixedMethod" v-perm="'ecom:forecast:edit'" :loading="saving" @click="useMethod(null)">
        <template #icon><UndoOutlined /></template>{{ $t('ecomAnalytics.forecast.autoMethod') }}
      </a-button>
    </template>
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions size="small" :column="{ xs: 1, sm: 2, lg: 4 }" class="info">
          <a-descriptions-item :label="$t('ecomAnalytics.common.store')">{{ detail.info.storeName }}</a-descriptions-item>
          <a-descriptions-item :label="$t('ecomAnalytics.common.material')">
            {{ [detail.info.materialCode, detail.info.materialName].filter(Boolean).join(' ') || '-' }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomAnalytics.forecast.method')">
            <DictTag type="ecom_forecast_method" :value="detail.method" />
            <a-tag v-if="detail.fixedMethod" color="purple" :bordered="false">{{ $t('ecomAnalytics.forecast.manual') }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomAnalytics.forecast.params')">
            {{ $t('ecomAnalytics.forecast.paramsText', { alpha: detail.setting.alpha, beta: detail.setting.beta, window: detail.setting.windowDays, weeks: detail.setting.seasonWeeks }) }}
          </a-descriptions-item>
          <a-descriptions-item :label="$t('ecomFix.stockout.days')">
            <a-tag v-if="stockoutCount" color="red" :bordered="false">{{ $t('ecomFix.stockout.daysN', { n: stockoutCount }) }}</a-tag>
            <span v-else>0</span>
            <span class="sub-text">{{ excludeOn ? $t('ecomFix.stockout.excludeOn') : $t('ecomFix.stockout.excludeOff') }}</span>
          </a-descriptions-item>
        </a-descriptions>
        <a-alert v-if="stockoutCount" type="warning" show-icon class="stockout-alert">
          <template #message>
            {{ excludeOn ? $t('ecomFix.stockout.detailTip', { n: stockoutCount }) : $t('ecomFix.stockout.detailTipOff', { n: stockoutCount }) }}
          </template>
          <template #description>
            <span class="sub-text">{{ $t('ecomFix.stockout.recent') }}:</span>
            <a-tag v-for="d in recentStockout" :key="d" :bordered="false" class="day-tag">{{ d }}</a-tag>
            <span v-if="stockoutCount > recentStockout.length" class="sub-text">…</span>
          </template>
        </a-alert>

        <div class="chart-head">
          <span class="chart-title">{{ $t('ecomAnalytics.forecast.chartTitle') }}</span>
          <a-checkbox v-model:checked="showAll">{{ $t('ecomAnalytics.forecast.showAllMethods') }}</a-checkbox>
        </div>
        <EChart :option="chartOption" height="360px" />
        <div class="sub-text">{{ $t('ecomAnalytics.forecast.chartTip') }}</div>

        <h4 class="section">{{ $t('ecomAnalytics.forecast.compare') }}</h4>
        <a-table row-key="method" size="small" :columns="methodColumns" :data-source="detail.methods" :pagination="false" :scroll="{ x: 760 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'method'">
              <DictTag type="ecom_forecast_method" :value="record.method" />
              <a-tag v-if="record.selected" color="green" :bordered="false">{{ $t('ecomAnalytics.forecast.inUse') }}</a-tag>
            </template>
            <template v-else-if="column.key === 'mape'">
              <span :class="{ best: record.method === bestMethod }">{{ percent(record.mape, 2) }}</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button
                v-if="!record.selected || !detail.fixedMethod"
                v-perm="'ecom:forecast:edit'"
                type="link"
                size="small"
                :disabled="saving"
                @click="useMethod(record.method)"
              >{{ $t('ecomAnalytics.forecast.useMethod') }}</a-button>
            </template>
            <template v-else>{{ qty(record[column.key], column.key === 'mae' ? 2 : 0) }}</template>
          </template>
        </a-table>
        <div class="sub-text tip">{{ $t('ecomAnalytics.forecast.compareTip') }}</div>

        <template v-if="detail.events.length">
          <h4 class="section">{{ $t('ecomAnalytics.forecast.events') }}</h4>
          <div v-for="e in detail.events" :key="e.id" class="event">
            <a-tag color="orange" :bordered="false">× {{ qty(e.factor, 2) }}</a-tag>
            {{ e.name }} ({{ e.startDate }} ~ {{ e.endDate }})<span class="sub-text">{{ e.storeName || $t('ecomAnalytics.common.allStores') }}</span>
          </div>
        </template>
      </template>
    </a-spin>
  </SubPage>
</template>

<script setup>
import { computed, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { UndoOutlined } from '@ant-design/icons-vue'
import SubPage from '@/components/SubPage.vue'
import DictTag from '@/components/DictTag.vue'
import EChart from '@/components/charts/EChart.vue'
import { percent } from '@/components/charts/charts'
import { fixForecastMethod, getForecast } from '@/api/ecom/analytics'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { nz, qty } from '@/utils/number'
import { t } from '@/locales'

/**
 * 单品预测：最近 8 周的实际日销量、最近 28 天的回测预测、从今天起 90 天的预测与 80% 区间（可叠加其他方法），各方法回测误差对比，
 * 可手工指定方法（重新生成该 SKU 的预测）。open(listingId) 打开，指定方法后触发 changed
 */
const HISTORY_SHOWN = 56
const emit = defineEmits(['changed'])
const dict = useDict('ecom_forecast_method')
const open = ref(false)
const loading = ref(false)
const saving = ref(false)
const showAll = ref(false)
const listingId = ref()
const detail = ref(null)

const stockoutCount = computed(() => detail.value?.stockoutDates?.length || 0)
const excludeOn = computed(() => detail.value?.setting?.excludeStockout !== false)
const recentStockout = computed(() => [...(detail.value?.stockoutDates || [])].reverse().slice(0, 20))

const title = computed(() => (detail.value ? `${t('ecomAnalytics.forecast.detailTitle')} · ${detail.value.info.sellerSku}` : t('ecomAnalytics.forecast.detailTitle')))
const methodLabel = (m) => findDict(dict.ecom_forecast_method, m)?.label || m
const bestMethod = computed(() => {
  const withMape = (detail.value?.methods || []).filter((m) => m.mape != null)
  return withMape.length ? withMape.reduce((a, b) => (nz(b.mape) < nz(a.mape) ? b : a)).method : null
})

const methodColumns = [
  { title: t('ecomAnalytics.forecast.method'), key: 'method', width: 200 },
  { title: t('ecomAnalytics.forecast.mape'), key: 'mape', width: 100, align: 'right' },
  { title: t('ecomAnalytics.forecast.mae'), key: 'mae', width: 110, align: 'right' },
  { title: t('ecomAnalytics.forecast.next30'), key: 'total30', width: 100, align: 'right' },
  { title: t('ecomAnalytics.forecast.next60'), key: 'total60', width: 100, align: 'right' },
  { title: t('ecomAnalytics.forecast.next90'), key: 'total90', width: 100, align: 'right' },
  { title: t('common.action.operation'), key: 'action', width: 110, align: 'right' }
]

async function load() {
  loading.value = true
  try {
    detail.value = await getForecast(listingId.value)
  } finally {
    loading.value = false
  }
}

function show(id) {
  listingId.value = id
  detail.value = null
  open.value = true
  load()
}

async function useMethod(method) {
  saving.value = true
  try {
    await fixForecastMethod(listingId.value, method)
    message.success(t('common.msg.saveSuccess'))
    await load()
    emit('changed')
  } finally {
    saving.value = false
  }
}

/** 横轴：最近 56 天历史 + 90 天预测；区间用堆叠面积（下限 + 区间宽度） */
const chartOption = computed(() => {
  const d = detail.value
  const history = d.history.slice(-HISTORY_SHOWN).map(nz)
  const histStart = dayjs(d.historyStart).add(d.history.length - history.length, 'day')
  const horizon = d.methods[0].daily.length
  const dates = [...history.map((_, i) => histStart.add(i, 'day')), ...Array.from({ length: horizon }, (_, i) => dayjs(d.forecastStart).add(i, 'day'))]
    .map((x) => x.format('MM-DD'))
  const pad = (arr, before) => [...Array(before).fill(null), ...arr]
  const selected = d.methods.find((m) => m.selected)
  const backtest = d.backtest ? [...Array(history.length - d.backtest.length).fill(null), ...d.backtest.map(nz), ...Array(horizon).fill(null)] : []
  const lower = d.lower.map(nz)
  const width = d.upper.map((u, i) => Math.max(0, nz(u) - lower[i]))
  const others = showAll.value ? d.methods.filter((m) => !m.selected) : []
  // 断货日：在实际销量上标红点，补偿后的历史用虚线（只在有断货且开启排除时显示）
  const shownDates = history.map((_, i) => histStart.add(i, 'day').format('YYYY-MM-DD'))
  const stockSet = new Set(d.stockoutDates || [])
  const stockPoints = shownDates.map((day, i) => (stockSet.has(day) ? history[i] : null))
  const adjusted = (d.adjusted || []).slice(-HISTORY_SHOWN).map(nz)
  const showAdjusted = stockSet.size > 0 && excludeOn.value && adjusted.some((v, i) => v !== history[i])
  const extraLegend = [...(stockSet.size ? [t('ecomFix.stockout.day')] : []), ...(showAdjusted ? [t('ecomFix.stockout.adjusted')] : [])]
  const extraSeries = [
    ...(stockSet.size
      ? [{ name: t('ecomFix.stockout.day'), type: 'scatter', symbolSize: 9, itemStyle: { color: '#ff4d4f' }, data: [...stockPoints, ...Array(horizon).fill(null)] }]
      : []),
    ...(showAdjusted
      ? [{ name: t('ecomFix.stockout.adjusted'), type: 'line', showSymbol: false, itemStyle: { color: '#fa8c16' }, lineStyle: { type: 'dotted' },
        data: [...adjusted, ...Array(horizon).fill(null)] }]
      : [])
  ]
  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0, type: 'scroll', data: [t('ecomAnalytics.forecast.actual'), ...extraLegend, t('ecomAnalytics.forecast.backtest'), methodLabel(selected.method),
      t('ecomAnalytics.forecast.interval'), ...others.map((m) => methodLabel(m.method))] },
    grid: { left: 16, right: 16, top: 40, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: dates, boundaryGap: false },
    yAxis: { type: 'value' },
    series: [
      { name: t('ecomAnalytics.forecast.actual'), type: 'line', showSymbol: false, data: [...history, ...Array(horizon).fill(null)] },
      ...extraSeries,
      { name: t('ecomAnalytics.forecast.backtest'), type: 'line', showSymbol: false, lineStyle: { type: 'dashed' }, data: backtest },
      { name: 'lower', type: 'line', stack: 'band', showSymbol: false, lineStyle: { opacity: 0 }, tooltip: { show: false }, data: pad(lower, history.length) },
      {
        name: t('ecomAnalytics.forecast.interval'),
        type: 'line',
        stack: 'band',
        showSymbol: false,
        lineStyle: { opacity: 0 },
        areaStyle: { opacity: 0.18 },
        tooltip: { show: false },
        data: pad(width, history.length)
      },
      { name: methodLabel(selected.method), type: 'line', showSymbol: false, lineStyle: { width: 2 }, data: pad(selected.daily.map(nz), history.length) },
      ...others.map((m) => ({ name: methodLabel(m.method), type: 'line', showSymbol: false, lineStyle: { width: 1 }, data: pad(m.daily.map(nz), history.length) }))
    ]
  }
})

defineExpose({ open: show })
</script>

<style scoped>
.info {
  margin-bottom: 8px;
}
.chart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0;
}
.chart-title {
  font-weight: 600;
}
.section {
  margin: 20px 0 8px;
}
.tip {
  margin-top: 6px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.best {
  color: var(--status-success, #389e0d);
  font-weight: 600;
}
.event {
  margin-bottom: 4px;
}
.stockout-alert {
  margin-bottom: 8px;
}
.day-tag {
  margin: 2px 4px 2px 0;
}
</style>
