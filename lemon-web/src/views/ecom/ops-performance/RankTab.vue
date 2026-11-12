<template>
  <div class="tab-body">
    <div class="toolbar">
      <a-space wrap>
        <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" :allow-clear="false" :presets="presets" @change="load" />
        <a-button type="primary" :loading="loading" @click="load"><template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}</a-button>
        <a-button :disabled="!rows.length" @click="exportExcel"><template #icon><DownloadOutlined /></template>{{ $t('common.action.export') }}</a-button>
        <a-tooltip :title="$t('ecomOper.perf.tip')"><QuestionCircleOutlined class="tip-icon" /></a-tooltip>
      </a-space>
      <a-segmented v-model:value="sortBy" :options="sortOptions" />
    </div>
    <a-spin :spinning="loading">
      <!-- 前三名 -->
      <a-row v-if="rows.length" :gutter="[12, 12]" class="podium">
        <a-col v-for="(r, i) in sorted.slice(0, 3)" :key="r.userId" :xs="24" :md="8">
          <div :class="['podium-card', `p${i + 1}`]">
            <div class="podium-head">
              <span class="medal" :class="`m${i + 1}`">{{ i + 1 }}</span>
              <a-avatar :size="32" class="avatar">{{ (r.userName || '?').slice(0, 1) }}</a-avatar>
              <div class="podium-name">
                <div class="name">{{ r.userName || r.userId }}</div>
                <div class="sub-text">{{ $t('ecomUiOps.perf.storesOrders', { s: r.storeCount, o: num(r.orders) }) }}</div>
              </div>
            </div>
            <div class="podium-metrics">
              <div>
                <div class="sub-text">{{ $t('ecomOper.common.sales') }}</div>
                <div class="big">{{ money(r.sales) }}</div>
              </div>
              <div>
                <div class="sub-text">{{ $t('ecomOper.common.profit') }}</div>
                <div class="big" :class="{ danger: Number(r.profit) < 0 }">{{ money(r.profit) }}</div>
              </div>
              <div>
                <div class="sub-text">{{ $t('ecomOper.common.profitRate') }}</div>
                <div class="big">{{ pct(r.profitRate) }}</div>
              </div>
            </div>
            <div v-if="r.salesRate != null" class="podium-rate">
              <span class="sub-text">{{ $t('ecomOper.perf.salesRate') }}</span>
              <a-progress :percent="Math.min(100, Number(r.salesRate) || 0)" :show-info="false" size="small" :stroke-color="Number(r.salesRate) >= 100 ? '#52c41a' : '#1677ff'" class="rate-bar" />
              <span :class="Number(r.salesRate) >= 100 ? 'ok' : ''">{{ pct(r.salesRate) }}</span>
            </div>
          </div>
        </a-col>
      </a-row>
      <a-table row-key="userId" size="middle" :columns="columns" :data-source="sorted" :pagination="false" :scroll="{ x: sumWidth(columns) }" :locale="{ emptyText: $t('ecomOper.perf.empty') }">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'rank'">
            <span class="medal" :class="`m${index + 1}`">{{ index + 1 }}</span>
          </template>
          <template v-else-if="column.key === 'bar'">
            <div class="bar-cell">
              <span :class="{ danger: Number(record[column.dataIndex]) < 0 }">{{ money(record[column.dataIndex]) }}</span>
              <div class="bar-track"><div class="bar-fill" :style="{ width: barWidth(column.dataIndex, record) + '%', background: column.color }" /></div>
            </div>
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
import { DownloadOutlined, QuestionCircleOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { sumWidth } from '@/composables/useColumns'
import { rangeParams, rangePresets, thisMonth } from '@/components/ecom/ecomReport'
import { opsPerformance } from '@/api/ecom/oper'
import { writeExcel } from '@/utils/excel'
import { money, num } from '@/utils/number'

/** 运营人员绩效排行：前三名卡片、销售额 / 利润条形对比，可按销售额 / 利润 / 利润率 / 达成率排名；按店铺负责人汇总；ACOS 超过 30%、退货率超过 10% 标红，整月查询时显示销售额达成率 */
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
  { title: t('ecomOper.common.sales'), key: 'bar', dataIndex: 'sales', width: 170, align: 'right', color: '#1677ff' },
  { title: t('ecomOper.common.profit'), key: 'bar', dataIndex: 'profit', width: 160, align: 'right', color: '#52c41a' },
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

/** 排名依据（前端排序，后端按销售额排名） */
const sortBy = ref('sales')
const sortOptions = computed(() => [
  { value: 'sales', label: t('ecomOper.common.sales') },
  { value: 'profit', label: t('ecomOper.common.profit') },
  { value: 'profitRate', label: t('ecomOper.common.profitRate') },
  { value: 'salesRate', label: t('ecomOper.perf.salesRate') }
])
const sorted = computed(() => rows.value.slice().sort((a, b) => (Number(b[sortBy.value]) || -Infinity) - (Number(a[sortBy.value]) || -Infinity)))

/** 条形宽度：相对销售额最大值（利润也按销售额最大值，便于对比利润占比），负数不画 */
const maxSales = computed(() => Math.max(0, ...rows.value.map((r) => Number(r.sales) || 0)))
const barWidth = (field, r) => (maxSales.value ? Math.max(0, Math.min(100, ((Number(r[field]) || 0) / maxSales.value) * 100)) : 0)

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
    sorted.value.map((r, i) => columns.map((c) => {
      if (c.key === 'rank') return i + 1
      const v = r[c.dataIndex]
      return v == null ? null : ['money', 'pct', 'bar'].includes(c.key) ? Number(v) : v
    })))
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
.tip-icon {
  color: var(--app-text-3);
  cursor: help;
}
.podium {
  margin-bottom: 16px;
}
.podium-card {
  height: 100%;
  padding: 14px 16px;
  border: 1px solid var(--app-border);
  border-top-width: 3px;
  border-radius: 8px;
}
.podium-card.p1 {
  border-top-color: #faad14;
}
.podium-card.p2 {
  border-top-color: #bfbfbf;
}
.podium-card.p3 {
  border-top-color: #ff9c6e;
}
.podium-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar {
  flex: none;
  background: var(--ant-color-primary, #1677ff);
}
.podium-name {
  min-width: 0;
}
.name {
  font-size: 15px;
  font-weight: 600;
}
.podium-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}
.big {
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.podium-rate {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  font-size: 12px;
  white-space: nowrap;
}
.rate-bar {
  flex: 1;
  margin: 0;
}
.bar-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}
.bar-track {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background: var(--app-hover);
}
.bar-fill {
  height: 100%;
  border-radius: 3px;
}
.medal {
  display: inline-block;
  flex: none;
  text-align: center;
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
