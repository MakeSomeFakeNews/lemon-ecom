<template>
  <div class="table-page ecom-automation-log">
    <a-card :bordered="false" class="page-card">
      <div class="overview">
        <div class="counts">
          <div class="counts-title">{{ $t('ecomAuto.log.trend') }}</div>
          <div class="result-tags">
            <a-tag
              v-for="r in resultOrder"
              :key="r"
              :color="findDict(dict.ecom_auto_result, r)?.tagColor"
              :bordered="query.result === r"
              class="result-tag"
              :class="{ active: query.result === r }"
              @click="filterResult(r)"
            >
              {{ findDict(dict.ecom_auto_result, r)?.label || r }} <b>{{ stats.results?.[r] ?? 0 }}</b>
            </a-tag>
          </div>
        </div>
        <EChart :option="chartOption" height="110px" class="chart" />
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" :exporter="exportAll" @refresh="reload">
        <a-select v-model:value="query.ruleId" allow-clear show-search option-filter-prop="label" :options="ruleOptions" :placeholder="$t('ecomAuto.log.rule')"
          class="rule" @change="search" />
        <a-select v-model:value="query.trigger" allow-clear show-search option-filter-prop="label" :options="toOptions(dict.ecom_auto_trigger)"
          :placeholder="$t('ecomAuto.common.allTriggers')" class="trigger" @change="search" />
        <a-select v-model:value="query.result" allow-clear :options="toOptions(dict.ecom_auto_result)" :placeholder="$t('ecomAuto.log.result')" class="result"
          @change="search" />
        <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" :presets="presets" @change="search" />
        <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomAuto.log.keywordPh')" class="kw" @press-enter="search">
          <template #prefix><SearchOutlined /></template>
        </a-input>
        <a-checkbox v-model:checked="query.executed" @change="search">{{ $t('ecomAuto.log.onlyExecuted') }}</a-checkbox>
      </TableToolbar>
      <a-table
        row-key="id"
        :size="size"
        :columns="visibleColumns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: scrollX, y: '100%' }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'createTime'">
            <a @click="detailRef.open(record.id)">{{ record.createTime }}</a>
          </template>
          <template v-else-if="column.key === 'ruleName'">
            <div class="ellipsis" :title="record.ruleName">{{ record.ruleName }}</div>
            <div class="sub-text"><DictTag type="ecom_auto_trigger" :value="record.trigger" /></div>
          </template>
          <template v-else-if="column.key === 'object'">
            <div class="ellipsis strong" :title="record.objectLabel">{{ record.objectLabel || '-' }}</div>
            <div class="sub-text ellipsis">{{ record.storeName || record.objectKey }}</div>
          </template>
          <template v-else-if="column.key === 'result'">
            <DictTag type="ecom_auto_result" :value="record.result" />
            <div v-if="record.retryCount" class="sub-text">{{ $t('ecomAuto.log.retried', { n: record.retryCount }) }}</div>
          </template>
          <template v-else-if="column.key === 'actions'">
            <div v-if="record.actions?.length" class="acts">
              <a-tooltip v-for="a in record.actions" :key="a.index" :title="a.message">
                <a-tag :color="statusColor(a.status)" :bordered="false" class="act">
                  <CheckOutlined v-if="a.status === AutoActionStatus.SUCCESS" />
                  <CloseOutlined v-else-if="a.status === AutoActionStatus.FAILED" />
                  <BulbOutlined v-else-if="a.status === AutoActionStatus.SUGGESTED" />
                  <MinusOutlined v-else />
                  {{ findDict(dict.ecom_auto_action, a.type)?.label || a.type }}
                </a-tag>
              </a-tooltip>
            </div>
            <div v-else class="sub-text ellipsis" :title="record.matchDetail">{{ record.matchDetail || $t('ecomAuto.log.noActions') }}</div>
            <div v-if="record.errorMsg" class="error ellipsis" :title="record.errorMsg">{{ record.errorMsg }}</div>
          </template>
          <template v-else-if="column.key === 'costMs'">{{ $t('ecomAuto.log.costMs', { n: record.costMs ?? 0 }) }}</template>
          <template v-else-if="column.key === 'ops'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>
    <LogDetail ref="detailRef" @retried="reload" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BulbOutlined, CheckOutlined, CloseOutlined, MinusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import EChart from '@/components/charts/EChart.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { rangeParams, rangePresets } from '@/components/ecom/ecomReport'
import { autoLogStats, pageAutoLogs, pageAutoRules, retryAutoLog } from '@/api/ecom/automation'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { AutoActionStatus, AutoResult } from '@/constants/enums'
import { findDict, toOptions } from '@/utils/dict'
import { t } from '@/locales'
import LogDetail from './LogDetail.vue'

/**
 * 自动化执行日志：近 14 天按结果计数（点击筛选）与每天的执行 / 失败趋势；按规则、触发器、结果、日期、对象筛选，
 * 列表显示每个动作的结果（悬浮看说明），详情（子页）看事件字段、条件判断、动作耗时，失败的可重试。
 * 规则列表跳转带 ?ruleId=、?result=、?executed=true
 */
const route = useRoute()
const router = useRouter()
const dict = useDict('ecom_auto_trigger', 'ecom_auto_result', 'ecom_auto_action')
const presets = rangePresets()
const detailRef = ref()
const resultOrder = Object.values(AutoResult)

const columns = [
  { title: t('ecomAuto.log.time'), key: 'createTime', dataIndex: 'createTime', width: 170, fixed: 'left' },
  { title: t('ecomAuto.log.rule'), key: 'ruleName', dataIndex: 'ruleName', width: 230 },
  { title: t('ecomAuto.log.object'), key: 'object', dataIndex: 'objectLabel', width: 220 },
  { title: t('ecomAuto.log.result'), key: 'result', dataIndex: 'result', width: 110, dict: 'ecom_auto_result' },
  { title: t('ecomAuto.log.actions'), key: 'actions', width: 380, exportValue: (r) => r.errorMsg || r.matchDetail || '' },
  { title: t('ecomAuto.log.cost'), key: 'costMs', dataIndex: 'costMs', width: 100, align: 'right' },
  { ...actionColumn(2), key: 'ops' }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-automation-log')

const query = reactive({
  ruleId: Number(route.query.ruleId) || undefined,
  trigger: route.query.trigger || undefined,
  result: route.query.result || undefined,
  executed: route.query.executed === 'true',
  range: undefined,
  keyword: undefined
})
const params = (q) => ({ ruleId: q.ruleId, trigger: q.trigger, result: q.result, executed: q.executed || undefined, keyword: q.keyword || undefined,
  ...rangeParams(q.range) })
const { list, loading, size, pagination, load, search: doSearch, onTableChange, exportAll } = useTable(pageAutoLogs, query, { params })

const rules = ref([])
const ruleOptions = computed(() => rules.value.map((r) => ({ value: r.id, label: r.name })))
const stats = ref({})
async function loadStats() {
  stats.value = (await autoLogStats({ days: 14, ruleId: query.ruleId })) || {}
}
function search() {
  doSearch()
  loadStats()
}
function reload() {
  load()
  loadStats()
}
function filterResult(r) {
  query.result = query.result === r ? undefined : r
  search()
}

const chartOption = computed(() => {
  const days = stats.value.days || []
  return {
    grid: { left: 8, right: 8, top: 8, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: days.map((d) => d.date.slice(5)), axisTick: { show: false } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { show: false }, axisLabel: { show: false } },
    series: [
      { name: t('ecomAuto.log.executed'), type: 'bar', stack: 'a', data: days.map((d) => d.executed - d.failed), itemStyle: { color: '#52c41a' }, barMaxWidth: 18 },
      { name: t('ecomAuto.log.failed'), type: 'bar', stack: 'a', data: days.map((d) => d.failed), itemStyle: { color: '#ff4d4f' }, barMaxWidth: 18 }
    ]
  }
})

const statusColor = (s) =>
  s === AutoActionStatus.SUCCESS ? 'green' : s === AutoActionStatus.FAILED ? 'red' : s === AutoActionStatus.SUGGESTED ? 'blue' : 'default'

const retry = useAction((r) => retryAutoLog(r.id), {
  confirm: () => t('ecomAuto.log.retryConfirm'),
  successKey: (res) => t('ecomAuto.log.retryDone', { result: findDict(dict.ecom_auto_result, res.result)?.label || res.result }),
  reload: () => reload()
})
const canRetry = (r) => r.result === AutoResult.FAILED || r.result === AutoResult.PARTIAL
const rowActions = (r) => [
  { type: 'detail', onClick: () => detailRef.value.open(r.id) },
  ...(canRetry(r) ? [{ type: 'run', label: t('ecomAuto.log.retry'), perm: 'ecom:automationLog:retry', onClick: () => retry(r) }] : [])
]

onMounted(async () => {
  loadStats()
  load()
  if (Object.keys(route.query).length) router.replace({ query: {} })
  rules.value = (await pageAutoRules({ current: 1, size: 500 }))?.records || []
})
</script>

<style scoped>
.overview {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.3fr);
  gap: 16px;
  align-items: center;
}
@media (max-width: 1100px) {
  .overview {
    grid-template-columns: minmax(0, 1fr);
  }
}
.counts-title {
  margin-bottom: 8px;
  font-weight: 500;
}
.result-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.result-tag {
  margin: 0;
  padding: 2px 10px;
  cursor: pointer;
}
.result-tag.active {
  box-shadow: 0 0 0 2px var(--app-primary);
}
.result-tag b {
  margin-left: 4px;
  font-variant-numeric: tabular-nums;
}
.rule {
  width: 220px;
}
.trigger {
  width: 180px;
}
.result {
  width: 130px;
}
.kw {
  width: 200px;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.strong {
  font-weight: 500;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.acts {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.act {
  margin: 0;
}
.error {
  margin-top: 2px;
  color: #cf1322;
  font-size: 12px;
}
</style>

<style>
html[data-theme='dark'] .ecom-automation-log .error {
  color: #ff7875;
}
</style>
