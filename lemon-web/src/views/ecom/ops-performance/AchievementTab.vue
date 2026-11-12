<template>
  <div class="tab-body">
    <div class="toolbar">
      <a-space wrap>
        <a-date-picker v-model:value="month" picker="month" value-format="YYYY-MM" :allow-clear="false" @change="load" />
        <a-radio-group v-model:value="dimension" option-type="button" button-style="solid" :options="dimensionOptions" @change="load" />
      </a-space>
      <span class="sub-text">{{ $t('ecomOper.target.timeProgress') }} {{ pct(data.timeProgress) }}</span>
    </div>
    <a-spin :spinning="loading">
      <a-row :gutter="[12, 12]" class="cards">
        <a-col v-for="c in cards" :key="c.key" :xs="24" :md="8">
          <div class="card">
            <div class="label">{{ c.label }}</div>
            <div class="value">{{ c.actual }} <span class="sub-text">/ {{ c.target }}</span></div>
            <a-progress :percent="Math.min(100, Number(c.rate) || 0)" :status="progressStatus(c.rate)" :format="() => pct(c.rate)" />
          </div>
        </a-col>
      </a-row>
      <EChart v-if="rows.length" :option="chartOption" height="260px" class="chart" />
      <a-table :row-key="(r) => r.key" size="middle" :columns="columns" :data-source="rows" :pagination="false" :scroll="{ x: 1100 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <template v-if="dimension === EcomAchievementDimension.PLATFORM"><DictTag type="ecom_platform" :value="record.platform" /></template>
            <template v-else>
              <div>{{ record.name || (dimension === EcomAchievementDimension.OWNER ? $t('ecomOper.target.noOwner') : record.key) }}</div>
              <div v-if="record.platform && dimension === EcomAchievementDimension.STORE" class="sub-text"><DictTag type="ecom_platform" :value="record.platform" /></div>
            </template>
          </template>
          <template v-else-if="column.key === 'sales'"><Metric :actual="money(record.salesActual)" :target="money(record.salesTarget)" :rate="record.salesRate" :progress="data.timeProgress" /></template>
          <template v-else-if="column.key === 'profit'"><Metric :actual="money(record.profitActual)" :target="money(record.profitTarget)" :rate="record.profitRate" :progress="data.timeProgress" /></template>
          <template v-else-if="column.key === 'orders'"><Metric :actual="record.orderActual" :target="record.orderTarget" :rate="record.orderRate" :progress="data.timeProgress" /></template>
        </template>
      </a-table>
    </a-spin>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { useI18n } from 'vue-i18n'
import DictTag from '@/components/DictTag.vue'
import EChart from '@/components/charts/EChart.vue'
import { targetAchievement } from '@/api/ecom/oper'
import { useDict } from '@/composables/useDict'
import { findDict } from '@/utils/dict'
import { money } from '@/utils/number'
import { EcomAchievementDimension } from '@/constants/enums'

/** 月度达成率：合计卡片（销售额、利润、订单数的实际 / 目标与进度条）、目标与实际柱状图、按店铺 / 负责人 / 平台的明细（达成率低于时间进度标橙） */
const { t } = useI18n()
const dict = useDict('ecom_platform')

const month = ref(dayjs().format('YYYY-MM'))
const dimension = ref(EcomAchievementDimension.STORE)
const dimensionOptions = computed(() => [
  { value: EcomAchievementDimension.STORE, label: t('ecomOper.common.store') },
  { value: EcomAchievementDimension.OWNER, label: t('ecomOper.common.owner') },
  { value: EcomAchievementDimension.PLATFORM, label: t('ecomOper.common.platform') }
])
const loading = ref(false)
const data = ref({})
const rows = computed(() => data.value.rows || [])

const pct = (v) => (v == null ? '-' : `${Number(v).toFixed(1)}%`)
const progressStatus = (rate) => (rate == null ? 'normal' : Number(rate) >= 100 ? 'success' : Number(rate) < Number(data.value.timeProgress || 0) ? 'exception' : 'active')

/** 单元格：实际 / 目标 与达成率（低于时间进度标橙） */
const Metric = (props) => h('div', [
  h('div', [props.actual, h('span', { class: 'sub-text' }, ` / ${props.target}`)]),
  h('div', { class: props.rate == null ? 'sub-text' : Number(props.rate) >= 100 ? 'ok' : Number(props.rate) < Number(props.progress || 0) ? 'warn' : '' }, pct(props.rate))
])
Metric.props = ['actual', 'target', 'rate', 'progress']

const cards = computed(() => {
  const s = data.value.total || {}
  return [
    { key: 'sales', label: t('ecomOper.common.sales'), actual: money(s.salesActual), target: money(s.salesTarget), rate: s.salesRate },
    { key: 'profit', label: t('ecomOper.common.profit'), actual: money(s.profitActual), target: money(s.profitTarget), rate: s.profitRate },
    { key: 'orders', label: t('ecomOper.common.orders'), actual: s.orderActual ?? 0, target: s.orderTarget ?? 0, rate: s.orderRate }
  ]
})

const columns = computed(() => [
  { title: t('ecomOper.target.name'), key: 'name', width: 220 },
  { title: t('ecomOper.target.storeCount'), dataIndex: 'storeCount', width: 90, align: 'right' },
  { title: `${t('ecomOper.common.sales')}（${t('ecomOper.common.actual')} / ${t('ecomOper.common.target')}）`, key: 'sales', width: 240 },
  { title: `${t('ecomOper.common.profit')}（${t('ecomOper.common.actual')} / ${t('ecomOper.common.target')}）`, key: 'profit', width: 240 },
  { title: `${t('ecomOper.common.orders')}（${t('ecomOper.common.actual')} / ${t('ecomOper.common.target')}）`, key: 'orders', width: 200 }
])

const rowName = (r) => {
  if (dimension.value === EcomAchievementDimension.PLATFORM) return findDict(dict.ecom_platform, r.platform)?.label || r.platform
  return r.name || (dimension.value === EcomAchievementDimension.OWNER ? t('ecomOper.target.noOwner') : r.key)
}
const chartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 8, right: 8, top: 36, bottom: 8, containLabel: true },
  xAxis: { type: 'category', data: rows.value.map(rowName), axisLabel: { interval: 0, rotate: rows.value.length > 6 ? 30 : 0 } },
  yAxis: { type: 'value' },
  series: [
    { name: `${t('ecomOper.common.sales')} ${t('ecomOper.common.target')}`, type: 'bar', barMaxWidth: 22, itemStyle: { color: '#d9d9d9' }, data: rows.value.map((r) => Number(r.salesTarget)) },
    { name: `${t('ecomOper.common.sales')} ${t('ecomOper.common.actual')}`, type: 'bar', barMaxWidth: 22, itemStyle: { color: '#1677ff' }, data: rows.value.map((r) => Number(r.salesActual)) },
    { name: `${t('ecomOper.common.profit')} ${t('ecomOper.common.actual')}`, type: 'bar', barMaxWidth: 22, itemStyle: { color: '#52c41a' }, data: rows.value.map((r) => Number(r.profitActual)) }
  ]
}))

async function load() {
  loading.value = true
  try {
    data.value = (await targetAchievement(month.value, dimension.value)) || {}
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.tab-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.cards {
  margin-bottom: 12px;
}
.card {
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--app-fill, #fafafa);
}
.label {
  color: var(--app-text-3);
  font-size: 12px;
}
.value {
  font-size: 20px;
  font-weight: 600;
}
.chart {
  margin-bottom: 12px;
}
.sub-text,
:deep(.sub-text) {
  color: var(--app-text-3);
  font-size: 12px;
}
:deep(.ok) {
  color: #52c41a;
}
:deep(.warn) {
  color: #fa8c16;
}
</style>
