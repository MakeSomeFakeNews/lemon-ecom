<template>
  <div class="ecom-dashboard">
    <div class="page-card header">
      <StoreSelect v-model:value="storeId" :placeholder="$t('ecomReport.common.allStores')" class="store" @update:value="loadCharts" />
      <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" @change="loadCharts" />
      <a-button :loading="loading" @click="loadAll">
        <template #icon><ReloadOutlined /></template>{{ $t('common.action.refresh') }}
      </a-button>
      <span class="sub-text">{{ $t('ecomReport.common.baseCurrencyTip') }}</span>
    </div>

    <!-- 今日 / 本月经营指标 -->
    <div class="stats">
      <div v-for="c in METRICS" :key="c.key" class="page-card stat">
        <div class="stat-label">{{ $t(`ecomReport.dashboard.${c.key}`) }}</div>
        <div class="stat-value" :class="{ neg: c.money && nz(overview[c.key]) < 0 }" :title="c.money ? money(overview[c.key]) : undefined">
          <template v-if="c.money"><span class="stat-unit">¥</span>{{ shortMoney(overview[c.key]) }}</template>
          <template v-else>{{ num(overview[c.key]) }}<span class="stat-unit suffix">{{ $t('ecomReport.dashboard.orderUnit') }}</span></template>
        </div>
      </div>
    </div>

    <!-- 待处理事项：点击到对应页面 -->
    <div class="alerts">
      <div v-for="c in ALERTS" :key="c.key" class="page-card alert" :class="[c.tone, { clickable: !!c.to }]" @click="go(c)">
        <span class="alert-icon"><component :is="c.icon" /></span>
        <div class="alert-body">
          <div class="stat-label">{{ $t(`ecomReport.dashboard.${c.key}`) }}</div>
          <div class="stat-value">{{ num(overview[c.key]) }}<span class="stat-unit suffix">{{ $t(`ecomReport.dashboard.${c.unit}`) }}</span></div>
        </div>
        <RightOutlined v-if="c.to" class="alert-arrow" />
      </div>
    </div>

    <a-card :bordered="false" class="page-card" :title="$t('ecomReport.dashboard.trend')">
      <EChart :option="trendOption" height="320px" />
    </a-card>

    <a-row v-if="canReport" :gutter="[16, 16]" class="row">
      <a-col :xs="24" :xl="8">
        <a-card :bordered="false" class="page-card" :title="$t('ecomReport.dashboard.storeShare')">
          <EChart v-if="storeRows.length" :option="storeOption" height="300px" />
          <a-empty v-else :image="simpleImage" class="empty" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="8">
        <a-card :bordered="false" class="page-card" :title="$t('ecomReport.dashboard.countryTop')">
          <EChart v-if="countryRows.length" :option="countryOption" height="300px" />
          <a-empty v-else :image="simpleImage" class="empty" />
        </a-card>
      </a-col>
      <a-col :xs="24" :xl="8">
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
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { Empty } from 'ant-design-vue'
import {
  AlertOutlined,
  ClockCircleOutlined,
  FieldTimeOutlined,
  ReloadOutlined,
  RightOutlined,
  SendOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import { shortMoney } from '@/components/charts/charts'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import { rangeParams, rangePresets, thisMonth, yuan } from '@/components/ecom/ecomReport'
import { reportCountry, reportFees, reportOverview, reportStore, reportTrend } from '@/api/ecom/report'
import { useDict } from '@/composables/useDict'
import { useUserStore } from '@/store/user'
import { findDict } from '@/utils/dict'
import { money, num, nz } from '@/utils/number'
import { EcomLocalStatus, ReplenishLevel, SettlementItemType } from '@/constants/enums'
import { t } from '@/locales'

/**
 * 电商看板：概览指标（今日 / 本月，固定口径）、待处理事项（点击到订单 / 货件 / 补货页面），
 * 以及按所选店铺与日期范围的销售趋势、店铺销售额、国家分布、结算费用构成。
 * 店铺 / 国家 / 费用构成接口需要利润分析权限（ecom:report:list），没有权限时不显示
 */
const router = useRouter()
const canReport = useUserStore().hasPerm('ecom:report:list')
const dict = useDict('ecom_settlement_item_type')
const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE
const presets = rangePresets()

/** 概览指标卡（与 EcomReports.Overview 的字段一致） */
const METRICS = [
  { key: 'todayOrders' },
  { key: 'todaySales', money: true },
  { key: 'monthOrders' },
  { key: 'monthSales', money: true },
  { key: 'monthProfit', money: true },
  { key: 'monthRefund', money: true },
  { key: 'monthFees', money: true },
  { key: 'monthCost', money: true }
]
/** 待处理事项：to 为跳转目标（订单页按 query 的 localStatus / overdue 作为初始筛选） */
const ALERTS = [
  { key: 'pendingOrders', unit: 'orderUnit', tone: 'warning', icon: ClockCircleOutlined, to: { path: '/ecom/order', query: { localStatus: EcomLocalStatus.PENDING } } },
  { key: 'exceptionOrders', unit: 'orderUnit', tone: 'danger', icon: WarningOutlined, to: { path: '/ecom/order', query: { localStatus: EcomLocalStatus.EXCEPTION } } },
  { key: 'overdueOrders', unit: 'orderUnit', tone: 'danger', icon: FieldTimeOutlined, to: { path: '/ecom/order', query: { overdue: '1' } } },
  { key: 'inTransitInbounds', unit: 'shipmentUnit', tone: 'info', icon: SendOutlined, to: { path: '/ecom/inbound' } },
  { key: 'urgentReplenish', unit: 'skuUnit', tone: 'warning', icon: AlertOutlined, to: { path: '/ecom/replenish', query: { level: ReplenishLevel.URGENT } } }
]

const storeId = ref()
const range = ref(thisMonth())
const loading = ref(false)
const overview = ref({})
const trendRows = ref([])
const storeRows = ref([])
const countryRows = ref([])
const feeRows = ref([])

const go = (c) => c.to && router.push(c.to)

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

const storeOption = computed(() => ({
  tooltip: { trigger: 'item', ...moneyTip },
  legend: { type: 'scroll', orient: 'vertical', right: 0, top: 'middle' },
  series: [
    {
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['35%', '50%'],
      data: storeRows.value.filter((r) => nz(r.sales) > 0).map((r) => ({ name: r.storeName, value: nz(r.sales) }))
    }
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

let seq = 0
async function loadCharts() {
  const current = ++seq
  loading.value = true
  try {
    const params = { ...rangeParams(range.value), storeId: storeId.value || undefined }
    const [trend, stores, countries, fees] = await Promise.all([
      reportTrend(params),
      canReport ? reportStore(params) : [],
      canReport ? reportCountry(params) : [],
      canReport ? reportFees(params) : []
    ])
    if (current !== seq) return
    trendRows.value = trend || []
    storeRows.value = stores || []
    countryRows.value = countries || []
    // 费用构成不含订单收入，金额为 0 的类型不显示
    feeRows.value = (fees || []).filter((r) => r.itemType !== SettlementItemType.ORDER && nz(r.amount) !== 0)
  } finally {
    if (current === seq) loading.value = false
  }
}

function loadAll() {
  loadOverview()
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
.alerts {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 16px;
}
.alert {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--app-card);
  transition: transform 0.2s, box-shadow 0.2s;
}
.alert.clickable {
  cursor: pointer;
}
.alert.clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}
.alert-body {
  flex: 1;
  min-width: 0;
}
.alert .stat-value {
  margin-top: 2px;
  font-size: 20px;
}
.alert-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex: none;
}
.alert.warning .alert-icon {
  color: #d48806;
  background: rgba(250, 178, 25, 0.14);
}
.alert.danger .alert-icon {
  color: var(--status-critical, #cf1322);
  background: rgba(208, 59, 59, 0.1);
}
.alert.info .alert-icon {
  color: var(--app-primary);
  background: var(--app-primary-bg);
}
.alert-arrow {
  color: var(--app-text-3);
  font-size: 12px;
}
.no-perm {
  padding: 12px 16px;
  background: var(--app-card);
}
.empty {
  padding: 80px 0;
}
</style>

<style>
/* 暗色：待处理事项图标底色调淡（需要匹配根元素的 data-theme，不能放在 scoped 中） */
html[data-theme='dark'] .ecom-dashboard .alert.warning .alert-icon {
  color: #ffd666;
  background: rgba(250, 178, 25, 0.1);
}
html[data-theme='dark'] .ecom-dashboard .alert.danger .alert-icon {
  color: #ff7875;
  background: rgba(208, 59, 59, 0.14);
}
</style>
