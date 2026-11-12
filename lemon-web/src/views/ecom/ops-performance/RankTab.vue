<template>
  <div class="tab-body">
    <div class="toolbar">
      <a-space wrap>
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" @change="load" />
        <a-button type="primary" :loading="loading" @click="load"><template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}</a-button>
        <a-button @click="exportExcel"><template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}</a-button>
      </a-space>
      <span class="sub-text">{{ $t('ecomOper.perf.tip') }}</span>
    </div>
    <a-spin :spinning="loading">
      <EChart v-if="rows.length" :option="chartOption" height="260px" class="chart" />
      <a-table row-key="userId" size="middle" :columns="columns" :data-source="rows" :pagination="false" :scroll="{ x: 1500 }" :locale="{ emptyText: $t('ecomOper.perf.empty') }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'rank'">
            <span class="medal" :class="`m${record.rank}`">{{ record.rank }}</span>
          </template>
          <template v-else-if="column.key === 'money'">{{ money(record[column.dataIndex]) }}</template>
          <template v-else-if="column.key === 'pct'">
            <span :class="pctClass(column.dataIndex, record[column.dataIndex])">{{ pct(record[column.dataIndex]) }}</span>
          </template>
        </template>
      </a-table>
    </a-spin>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import EChart from '@/components/charts/EChart.vue'
import { rangeParams, rangePresets, thisMonth } from '@/components/ecom/ecomReport'
import { opsPerformance } from '@/api/ecom/oper'
import { writeExcel } from '@/utils/excel'
import { money } from '@/utils/number'

/** 运营人员绩效排行：按店铺负责人汇总，按销售额排名；ACOS 超过 30%、退货率超过 10% 标红，整月查询时显示销售额达成率 */
const { t } = useI18n()
const presets = rangePresets()
const range = ref(thisMonth())
const loading = ref(false)
const rows = ref([])

const pct = (v) => (v == null ? '-' : `${Number(v).toFixed(2)}%`)
const LIMITS = { acos: 30, returnRate: 10 }
const pctClass = (field, v) => (v != null && LIMITS[field] != null && Number(v) > LIMITS[field] ? 'danger' : field === 'salesRate' && Number(v) >= 100 ? 'ok' : '')

const columns = [
  { title: t('ecomOper.perf.rank'), key: 'rank', dataIndex: 'rank', width: 70, align: 'center', fixed: 'left' },
  { title: t('ecomOper.perf.user'), dataIndex: 'userName', width: 120, fixed: 'left' },
  { title: t('ecomOper.perf.storeCount'), dataIndex: 'storeCount', width: 80, align: 'right' },
  { title: t('ecomOper.common.orders'), dataIndex: 'orders', width: 90, align: 'right' },
  { title: t('ecomOper.common.sales'), key: 'money', dataIndex: 'sales', width: 130, align: 'right' },
  { title: t('ecomOper.common.profit'), key: 'money', dataIndex: 'profit', width: 120, align: 'right' },
  { title: t('ecomOper.common.profitRate'), key: 'pct', dataIndex: 'profitRate', width: 90, align: 'right' },
  { title: t('ecomOper.perf.adSpend'), key: 'money', dataIndex: 'adSpend', width: 110, align: 'right' },
  { title: t('ecomOper.perf.adSales'), key: 'money', dataIndex: 'adSales', width: 120, align: 'right' },
  { title: t('ecomOper.perf.acos'), key: 'pct', dataIndex: 'acos', width: 90, align: 'right' },
  { title: t('ecomOper.perf.returns'), dataIndex: 'returns', width: 80, align: 'right' },
  { title: t('ecomOper.perf.returnRate'), key: 'pct', dataIndex: 'returnRate', width: 90, align: 'right' },
  { title: t('ecomOper.perf.badReviews'), dataIndex: 'badReviews', width: 90, align: 'right' },
  { title: t('ecomOper.perf.salesTarget'), key: 'money', dataIndex: 'salesTarget', width: 120, align: 'right' },
  { title: t('ecomOper.perf.salesRate'), key: 'pct', dataIndex: 'salesRate', width: 90, align: 'right' }
]

const chartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 8, right: 8, top: 36, bottom: 8, containLabel: true },
  xAxis: { type: 'category', data: rows.value.map((r) => r.userName || r.userId) },
  yAxis: [{ type: 'value' }, { type: 'value', splitLine: { show: false }, axisLabel: { formatter: '{value}%' } }],
  series: [
    { name: t('ecomOper.common.sales'), type: 'bar', barMaxWidth: 24, itemStyle: { color: '#1677ff' }, data: rows.value.map((r) => Number(r.sales)) },
    { name: t('ecomOper.common.profit'), type: 'bar', barMaxWidth: 24, itemStyle: { color: '#52c41a' }, data: rows.value.map((r) => Number(r.profit)) },
    { name: t('ecomOper.common.profitRate'), type: 'line', yAxisIndex: 1, itemStyle: { color: '#fa8c16' }, data: rows.value.map((r) => (r.profitRate == null ? null : Number(r.profitRate))) }
  ]
}))

async function load() {
  loading.value = true
  try {
    rows.value = (await opsPerformance(rangeParams(range.value))) || []
  } finally {
    loading.value = false
  }
}

function exportExcel() {
  writeExcel(`${t('ecomOper.perf.title')}-${range.value[0]}-${range.value[1]}`, columns.map((c) => ({ title: c.title, width: Math.round(c.width / 7) })),
    rows.value.map((r) => columns.map((c) => (r[c.dataIndex] == null ? null : c.key === 'money' || c.key === 'pct' ? Number(r[c.dataIndex]) : r[c.dataIndex]))))
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
.chart {
  margin-bottom: 12px;
}
.medal {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  border-radius: 50%;
  background: var(--app-fill, #f0f0f0);
  font-weight: 600;
}
.medal.m1 {
  background: #ffd666;
}
.medal.m2 {
  background: #d9d9d9;
}
.medal.m3 {
  background: #ffbb96;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.danger {
  color: #ff4d4f;
}
.ok {
  color: #52c41a;
}
</style>
