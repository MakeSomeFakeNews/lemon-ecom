<template>
  <div class="ecom-dashboard">
    <div class="page-card header">
      <StoreSelect v-model:value="storeId" :placeholder="$t('ecomReport.common.allStores')" class="store" @update:value="onStoreChange" />
      <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" @change="loadCharts" />
      <a-button :loading="loading" @click="loadAll">
        <template #icon><ReloadOutlined /></template>{{ $t('common.action.refresh') }}
      </a-button>
      <span class="sub-text">{{ $t('ecomReport.common.baseCurrencyTip') }}</span>
    </div>

    <!-- 没有数据时：生成演示数据入口与进度 -->
    <DemoDataBanner :empty="noData" @done="loadAll" />

    <!-- 今日 / 昨日 / 本月关键指标（环比、同比） -->
    <KpiPanel :periods="kpi" />

    <!-- 本月经营（利润扣除广告与费用分摊，各项占销售额）与待办 / 预警 -->
    <a-row :gutter="[16, 16]" class="eq-row">
      <a-col :xs="24" :xl="9">
        <a-card :bordered="false" class="page-card month-card" :title="$t('ecomDat.dash.monthTitle')">
          <template #extra>
            <span class="sub-text">{{ $t('ecomReport.dashboard.monthSales') }}
              <b class="month-sales">¥{{ shortMoney(overview.monthSales) }}</b></span>
          </template>
          <div class="month-grid">
            <div v-for="c in METRICS" :key="c.key" class="month-item">
              <div class="stat-label">{{ $t(`ecomReport.dashboard.${c.key}`) }}</div>
              <div class="stat-value" :class="{ neg: nz(overview[c.key]) < 0 }" :title="money(overview[c.key])">
                <span class="stat-unit">¥</span>{{ shortMoney(overview[c.key]) }}
              </div>
              <div class="ratio">
                <span class="sub-text">{{ $t(`ecomDat.dash.ratio.${c.key}`) }}</span>
                <span class="ratio-value" :class="c.tone(shareOf(c.key))">{{ percent(shareOf(c.key)) }}</span>
              </div>
              <div class="ratio-bar"><span :class="c.tone(shareOf(c.key))" :style="{ width: `${Math.min(100, Math.abs(nz(shareOf(c.key))))}%` }" /></div>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="15">
        <!-- 待处理事项：各功能注册的待办，点击到对应页面 -->
        <TodoPanel :items="todos" :loaded="todosLoaded" />
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :xl="10">
        <HourlyChart :data="hourly" />
      </a-col>
      <a-col :xs="24" :xl="14">
        <a-card :bordered="false" class="page-card" :title="$t('ecomReport.dashboard.trend')">
          <EChart v-if="hasTrend" :option="trendOption" height="328px" />
          <a-empty v-else :image="simpleImage" :description="$t('ecomDat.dash.noData')" class="empty" />
        </a-card>
      </a-col>
    </a-row>

    <!-- Top SKU、店铺排行、平台占比（按所选日期范围） -->
    <RankPanel v-if="canReport && rankParams" :params="rankParams" />

    <a-row v-if="canReport" :gutter="[16, 16]" class="row">
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card" :title="$t('ecomReport.dashboard.countryTop')">
          <EChart v-if="countryRows.length" :option="countryOption" height="300px" />
          <a-empty v-else :image="simpleImage" class="empty" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="12">
        <a-card :bordered="false" class="page-card" :title="$t('ecomReport.dashboard.feeMix')">
          <EChart v-if="feeRows.length" :option="feeOption" height="300px" />
          <a-empty v-else :image="simpleImage" class="empty" />
        </a-card>
      </a-col>
    </a-row>
    <div v-else class="page-card no-perm sub-text">{{ $t('ecomReport.common.noReportPerm') }}</div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { Empty } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import { percent, shortMoney } from '@/components/charts/charts'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { rangeParams, rangePresets, thisMonth, yuan } from '@/components/ecom/ecomReport'
import { reportCountry, reportFees, reportOverview, reportTrend } from '@/api/ecom/report'
import { dashboardHourly, dashboardKpi, dashboardTodos } from '@/api/ecom/dashboard'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { findDict } from '@/utils/dict'
import { money, nz } from '@/utils/number'
import { SettlementItemType } from '@/constants/enums'
import { t } from '@/locales'
import KpiPanel from './KpiPanel.vue'
import TodoPanel from './TodoPanel.vue'
import HourlyChart from './HourlyChart.vue'
import RankPanel from './RankPanel.vue'
import DemoDataBanner from '@/components/ecom/DemoDataBanner.vue'

/**
 * 电商看板：今日 / 昨日 / 本月关键指标（环比较昨日 / 较上月同期、同比较去年同期，按所选店铺）、本月利润与费用、
 * 待处理事项（后端各功能注册的待办，点击到对应页面）、实时销量（今日按小时 vs 昨日），
 * 以及按所选店铺与日期范围的销售趋势、Top 10 SKU、店铺排行、平台占比、国家分布、结算费用构成。
 * 排行 / 国家 / 费用构成需要利润分析权限（ecom:report:list），没有权限时不显示
 */
const canReport = useUserStore().hasPerm('ecom:report:list')
const app = useAppStore()
const dict = useDict('ecom_settlement_item_type')
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const presets = rangePresets()

/** 本月金额指标卡（与 EcomReports.Overview 的字段一致；利润扣除广告花费和费用分摊，与利润分析的订单口径一致） */
const goodHigh = (v) => (v == null ? '' : nz(v) < 0 ? 'bad' : nz(v) < 10 ? 'warn' : 'good')
const badHigh = (limit) => (v) => (v == null ? '' : nz(v) > limit ? 'warn' : 'normal')
const METRICS = [
  { key: 'monthProfit', tone: goodHigh },
  { key: 'monthRefund', tone: badHigh(5) },
  { key: 'monthFees', tone: badHigh(30) },
  { key: 'monthCost', tone: badHigh(50) }
]
/** 占本月销售额的百分比（利润为利润率） */
const shareOf = (key) => (nz(overview.value.monthSales) ? (nz(overview.value[key]) / nz(overview.value.monthSales)) * 100 : null)

const storeId = ref()
const range = ref(thisMonth())
const loading = ref(false)
const overview = ref({})
/** 看板没有数据（本月与今天都没有订单）：显示生成演示数据入口 */
const noData = computed(() => overview.value.monthOrders === 0 && !overview.value.todayOrders)
const trendRows = ref([])
const hasTrend = computed(() => trendRows.value.some((r) => nz(r.orders) || nz(r.sales)))
const countryRows = ref([])
const feeRows = ref([])
const kpi = ref([])
const hourly = ref({ today: [], yesterday: [] })
const todos = ref([])
const todosLoaded = ref(false)
/** 排行的查询条件（加载图表时设置，排行组件随之刷新） */
const rankParams = ref(null)

const grid = { left: 16, right: 16, top: 36, bottom: 8, containLabel: true }
const moneyTip = { valueFormatter: (v) => yuan(v) }

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid,
  xAxis: { type: 'category', data: trendRows.value.map((r) => dayjs(r.day).format('MM-DD')) },
  yAxis: [{ type: 'value' }, { type: 'value', minInterval: 1, splitLine: { show: false } }],
  series: [
    { name: t('ecomReport.common.orders'), type: 'bar', yAxisIndex: 1, barMaxWidth: 20, itemStyle: { color: '#91caff' }, data: trendRows.value.map((r) => nz(r.orders)) },
    { name: t('ecomReport.common.sales'), type: 'line', smooth: true, data: trendRows.value.map((r) => nz(r.sales)), tooltip: moneyTip },
    { name: t('ecomReport.common.profit'), type: 'line', smooth: true, itemStyle: { color: '#52c41a' }, data: trendRows.value.map((r) => nz(r.profit)), tooltip: moneyTip }
  ]
}))

const countryOption = computed(() => {
  const top = [...countryRows.value].sort((a, b) => nz(b.sales) - nz(a.sales)).slice(0, 10).reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...moneyTip },
    grid: { left: 8, right: 56, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: top.map((r) => r.country || '-') },
    series: [
      {
        name: t('ecomReport.common.sales'),
        type: 'bar',
        barMaxWidth: 16,
        itemStyle: { borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right', fontSize: 11, formatter: (p) => shortMoney(Math.round(p.value)), color: app.dark ? 'rgba(255,255,255,0.65)' : 'rgba(0,0,0,0.65)', textBorderWidth: 0 },
        data: top.map((r) => nz(r.sales))
      }
    ]
  }
})

const typeLabel = (v) => findDict(dict.ecom_settlement_item_type, v)?.label || v
const feeOption = computed(() => {
  const data = feeRows.value
    .map((r) => ({ name: typeLabel(r.itemType), value: Math.abs(nz(r.amount)) }))
    .sort((a, b) => b.value - a.value)
  const sum = data.reduce((a, b) => a + b.value, 0) || 1
  const share = Object.fromEntries(data.map((d) => [d.name, (d.value / sum) * 100]))
  return {
    tooltip: { trigger: 'item', formatter: (p) => `${p.marker}${p.name}<br/>${yuan(p.value)} (${p.percent}%)` },
    legend: { type: 'scroll', orient: 'vertical', right: 8, top: 'middle', icon: 'circle', itemWidth: 8, formatter: (n) => `${n}  ${percent(share[n])}` },
    series: [
      {
        type: 'pie',
        radius: ['48%', '72%'],
        center: ['30%', '50%'],
        label: { show: false },
        itemStyle: { borderRadius: 4, borderColor: 'transparent', borderWidth: 2 },
        data
      }
    ]
  }
})

async function loadOverview() {
  overview.value = (await reportOverview()) || {}
}

/** 关键指标与实时销量（按所选店铺） */
async function loadRealtime() {
  const params = { storeId: storeId.value || undefined }
  const [k, h] = await Promise.all([dashboardKpi(params), dashboardHourly(params)])
  kpi.value = k || []
  hourly.value = h || { today: [], yesterday: [] }
}

async function loadTodos() {
  try {
    todos.value = (await dashboardTodos()) || []
  } finally {
    todosLoaded.value = true
  }
}

function onStoreChange() {
  loadRealtime()
  loadCharts()
}

let seq = 0
async function loadCharts() {
  const current = ++seq
  loading.value = true
  try {
    const params = { ...rangeParams(range.value), storeId: storeId.value || undefined }
    rankParams.value = params
    const [trend, countries, fees] = await Promise.all([
      reportTrend(params),
      canReport ? reportCountry(params) : [],
      canReport ? reportFees(params) : []
    ])
    if (current !== seq) return
    trendRows.value = trend || []
    countryRows.value = countries || []
    // 费用构成不含订单收入，金额为 0 的类型不显示
    feeRows.value = (fees || []).filter((r) => r.itemType !== SettlementItemType.ORDER && nz(r.amount) !== 0)
  } finally {
    if (current === seq) loading.value = false
  }
}

function loadAll() {
  loadOverview()
  loadRealtime()
  loadTodos()
  loadCharts()
}

onMounted(loadAll)
</script>

<style scoped>
.ecom-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.ecom-dashboard .page-card + .page-card {
  margin-top: 0;
}
.header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--app-card);
}
.store {
  width: 240px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.eq-row > .ant-col > .ant-card {
  height: 100%;
}
.month-sales {
  margin-left: 4px;
  color: var(--app-text);
}
.month-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 24px;
}
.month-item {
  min-width: 0;
}
.ratio {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
}
.ratio-value {
  font-size: 12px;
  font-weight: 600;
  color: var(--app-text-2);
}
.ratio-value.good {
  color: #389e0d;
}
.ratio-value.warn {
  color: #d46b08;
}
.ratio-value.bad {
  color: var(--status-critical, #cf1322);
}
.ratio-bar {
  height: 4px;
  margin-top: 4px;
  border-radius: 2px;
  background: var(--app-hover);
  overflow: hidden;
}
.ratio-bar span {
  display: block;
  height: 100%;
  border-radius: 2px;
  background: var(--app-primary);
}
.ratio-bar span.good {
  background: #52c41a;
}
.ratio-bar span.warn {
  background: #fa8c16;
}
.ratio-bar span.bad {
  background: #ff4d4f;
}
.stat-label {
  font-size: 13px;
  color: var(--app-text-3);
}
.stat-value {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 600;
  color: var(--app-text);
  white-space: nowrap;
}
.stat-unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--app-text-3);
}
.neg {
  color: var(--status-critical, #cf1322);
}
.no-perm {
  padding: 12px 16px;
  background: var(--app-card);
}
.empty {
  padding: 80px 0;
}
</style>
