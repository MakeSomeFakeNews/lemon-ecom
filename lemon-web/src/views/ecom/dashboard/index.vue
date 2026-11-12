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

    <!-- 本月利润（扣除广告与费用分摊）与费用构成 -->
    <div class="stats">
      <div v-for="c in METRICS" :key="c.key" class="page-card stat">
        <div class="stat-label">{{ $t(`ecomReport.dashboard.${c.key}`) }}</div>
        <div class="stat-value" :class="{ neg: c.money && nz(overview[c.key]) < 0 }" :title="c.money ? money(overview[c.key]) : undefined">
          <template v-if="c.money"><span class="stat-unit">¥</span>{{ shortMoney(overview[c.key]) }}</template>
          <template v-else>{{ num(overview[c.key]) }}<span class="stat-unit suffix">{{ $t('ecomReport.dashboard.orderUnit') }}</span></template>
        </div>
      </div>
    </div>

    <!-- 待处理事项：各功能注册的待办，点击到对应页面 -->
    <TodoPanel :items="todos" :loaded="todosLoaded" />

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :xl="10">
        <HourlyChart :data="hourly" />
      </a-col>
      <a-col :xs="24" :xl="14">
        <a-card :bordered="false" class="page-card" :title="$t('ecomReport.dashboard.trend')">
          <EChart :option="trendOption" height="328px" />
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
import { shortMoney } from '@/components/charts/charts'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { rangeParams, rangePresets, thisMonth, yuan } from '@/components/ecom/ecomReport'
import { reportCountry, reportFees, reportOverview, reportTrend } from '@/api/ecom/report'
import { dashboardHourly, dashboardKpi, dashboardTodos } from '@/api/ecom/dashboard'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { findDict } from '@/utils/dict'
import { money, num, nz } from '@/utils/number'
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
const dict = useDict('ecom_settlement_item_type')
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const presets = rangePresets()

/** 本月金额指标卡（与 EcomReports.Overview 的字段一致；利润扣除广告花费和费用分摊，与利润分析的订单口径一致） */
const METRICS = [
  { key: 'monthProfit', money: true },
  { key: 'monthRefund', money: true },
  { key: 'monthFees', money: true },
  { key: 'monthCost', money: true }
]

const storeId = ref()
const range = ref(thisMonth())
const loading = ref(false)
const overview = ref({})
/** 看板没有数据（本月与今天都没有订单）：显示生成演示数据入口 */
const noData = computed(() => overview.value.monthOrders === 0 && !overview.value.todayOrders)
const trendRows = ref([])
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
    grid: { left: 8, right: 24, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: top.map((r) => r.country || '-') },
    series: [{ name: t('ecomReport.common.sales'), type: 'bar', barMaxWidth: 18, data: top.map((r) => nz(r.sales)) }]
  }
})

const typeLabel = (v) => findDict(dict.ecom_settlement_item_type, v)?.label || v
const feeOption = computed(() => ({
  tooltip: { trigger: 'item', ...moneyTip },
  legend: { type: 'scroll', orient: 'vertical', right: 0, top: 'middle' },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['35%', '50%'],
      data: feeRows.value.map((r) => ({ name: typeLabel(r.itemType), value: Math.abs(nz(r.amount)) }))
    }
  ]
}))

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
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 16px;
}
.stat {
  padding: 14px 16px;
  background: var(--app-card);
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
.stat-unit.suffix {
  margin-left: 4px;
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
