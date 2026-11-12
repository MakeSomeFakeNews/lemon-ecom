<template>
  <div class="tab-body">
    <SearchForm v-show="showSearch" :model="query" @search="search" @reset="resetQuery">
      <a-form-item :label="$t('ecomAsst.report.date')" name="range">
        <a-range-picker v-model:value="query.range" value-format="YYYY-MM-DD" :presets="presets" class="full" @change="search" />
      </a-form-item>
      <a-form-item :label="$t('ecomAsst.common.store')" name="storeId">
        <StoreSelect v-model:value="query.storeId" :placeholder="$t('ecomAsst.common.allStores')" />
      </a-form-item>
    </SearchForm>

    <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" :exporter="exportAll" @refresh="load">
      <a-button v-perm="'ecom:opsReport:generate'" type="primary" @click="emit('generate')">
        <template #icon><ThunderboltOutlined /></template>{{ $t('ecomAsst.report.generate') }}
      </a-button>
      <a-tooltip :title="$t('ecomAsst.report.scheduleTip')">
        <QuestionCircleOutlined class="tip-icon" />
      </a-tooltip>
    </TableToolbar>
    <a-table
      row-key="id"
      :size="size"
      :columns="visibleColumns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: scrollX, y: '100%' }"
      :locale="{ emptyText: $t('ecomAsst.report.empty') }"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'reportDate'">
          <a class="date-link" @click="emit('detail', record.id)">{{ record.reportDate }}</a>
          <div class="sub-text">{{ weekday(record.reportDate) }}</div>
        </template>
        <template v-else-if="column.key === 'scope'">
          <div class="ellipsis" :title="record.scopeName">{{ record.scopeName }}</div>
          <div class="sub-text">
            {{ $t('ecomAsst.report.storeCount', { n: record.storeCount }) }} · <DictTag type="ecom_ops_report_source" :value="record.source" />
          </div>
        </template>
        <template v-else-if="column.key === 'sales'">
          <div class="num">{{ money(record.sales) }}</div>
          <div class="num sub" :class="trendClass(record.salesChange)">{{ signedPct(record.salesChange) }}</div>
        </template>
        <template v-else-if="column.key === 'profit'">
          <div class="num" :class="{ danger: Number(record.profit) < 0 }">{{ money(record.profit) }}</div>
          <div class="num sub" :class="trendClass(record.profitChange)">{{ signedPct(record.profitChange) }}</div>
        </template>
        <template v-else-if="column.key === 'issues'">
          <div class="issues">
            <a-tooltip :title="$t('ecomAsst.report.abnormal')"><span class="issue" :class="{ on: record.abnormalCount }"><FallOutlined />{{ record.abnormalCount }}</span></a-tooltip>
            <a-tooltip :title="$t('ecomAsst.report.badReviews')"><span class="issue" :class="{ on: record.badReviewCount }"><DislikeOutlined />{{ record.badReviewCount }}</span></a-tooltip>
            <a-tooltip :title="$t('ecomAsst.report.stockRisks')"><span class="issue" :class="{ on: record.stockRiskCount }"><InboxOutlined />{{ record.stockRiskCount }}</span></a-tooltip>
            <a-tooltip :title="$t('ecomAsst.report.adAlerts')"><span class="issue" :class="{ on: record.adAlertCount }"><NotificationOutlined />{{ record.adAlertCount }}</span></a-tooltip>
          </div>
        </template>
        <template v-else-if="column.key === 'summary'">
          <a-tooltip :title="record.summary" placement="topLeft"><div class="ellipsis-2">{{ record.summary }}</div></a-tooltip>
        </template>
        <template v-else-if="column.key === 'aiStatus'">
          <DictTag type="ecom_ops_report_ai_status" :value="record.aiStatus" />
          <a-tag v-if="record.aiSample" color="gold">{{ $t('ecomAsst.report.sample') }}</a-tag>
        </template>
        <template v-else-if="column.key === 'push'">
          <span v-if="record.pushCount">{{ $t('ecomAsst.report.pushed', { n: record.pushCount }) }}</span>
          <span v-else class="sub-text">-</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <TableActions :actions="rowActions(record)" />
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { useI18n } from 'vue-i18n'
import {
  DislikeOutlined,
  FallOutlined,
  InboxOutlined,
  NotificationOutlined,
  QuestionCircleOutlined,
  RobotOutlined,
  SendOutlined,
  ThunderboltOutlined
} from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import StoreSelect from '@/components/ecom/StoreSelect.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { rangeParams, rangePresets } from '@/components/ecom/ecomReport'
import { deleteOpsReport, pageOpsReport, pushOpsReport, regenerateOpsAdvice } from '@/api/ecom/assistant'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { money } from '@/utils/number'
import { signedPct, trendClass } from './report'

/** 日报列表：日期、范围、销售额 / 利润与环比、异常计数、要点摘要、AI 建议状态；ref 调用 reload() */
const emit = defineEmits(['detail', 'generate'])
const { t } = useI18n()
useDict('ecom_ops_report_source', 'ecom_ops_report_ai_status')
const presets = rangePresets()

const columns = [
  { title: t('ecomAsst.report.date'), key: 'reportDate', dataIndex: 'reportDate', width: 110, fixed: 'left' },
  { title: t('ecomAsst.report.scope'), key: 'scope', dataIndex: 'scopeName', width: 180 },
  { title: t('ecomAsst.report.sales'), key: 'sales', dataIndex: 'sales', width: 110, align: 'right' },
  { title: t('ecomAsst.report.profit'), key: 'profit', dataIndex: 'profit', width: 110, align: 'right' },
  { title: t('ecomAsst.report.orders'), dataIndex: 'orders', width: 70, align: 'right' },
  { title: t('ecomAsst.report.issues'), key: 'issues', width: 180, exportValue: (r) => `${r.abnormalCount}/${r.badReviewCount}/${r.stockRiskCount}/${r.adAlertCount}` },
  { title: t('ecomAsst.report.summary'), key: 'summary', dataIndex: 'summary', width: 240 },
  { title: t('ecomAsst.report.aiStatus'), key: 'aiStatus', dataIndex: 'aiStatus', width: 110, dict: 'ecom_ops_report_ai_status' },
  { title: t('ecomAsst.report.push'), key: 'push', dataIndex: 'pushCount', width: 80 },
  actionColumn(3)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-ops-report')
const showSearch = ref(true)

const emptyQuery = () => ({ range: undefined, storeId: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageOpsReport, query, {
  params: ({ range, ...q }) => ({ ...q, ...rangeParams(range) })
})
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
// 下拉筛选变化即查询
watch(() => query.storeId, () => search())

const weekday = (d) => (d ? dayjs(d).format('dddd') : '')
const remove = useAction(deleteOpsReport, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const push = useAction(pushOpsReport, { successKey: (n) => t('ecomAsst.report.pushDone', { n }), reload: () => load() })
const advice = useAction(regenerateOpsAdvice, { successKey: 'ecomAsst.report.adviceDone', reload: () => load() })

const rowActions = (r) => [
  { type: 'detail', onClick: () => emit('detail', r.id) },
  { type: 'run', label: t('ecomAsst.report.push'), icon: SendOutlined, perm: 'ecom:opsReport:generate', confirm: t('ecomAsst.report.pushConfirm'), onClick: () => push(r.id) },
  { type: 'run', label: t('ecomAsst.report.regenAdvice'), icon: RobotOutlined, perm: 'ecom:opsReport:generate', onClick: () => advice(r.id) },
  { type: 'delete', perm: 'ecom:opsReport:remove', confirm: { title: t('ecomAsst.report.deleteConfirm', { date: r.reportDate }), okType: 'danger' }, onClick: () => remove(r.id) }
]

onMounted(load)
defineExpose({ reload: () => load() })
</script>

<style scoped>
.search-form {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--app-border);
}
.tab-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.full {
  width: 100%;
}
.date-link {
  font-weight: 600;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.num {
  font-variant-numeric: tabular-nums;
}
.num.sub {
  font-size: 12px;
}
.trend-up {
  color: #389e0d;
}
.trend-down {
  color: #cf1322;
}
.danger {
  color: #cf1322;
}
.issues {
  display: flex;
  gap: 10px;
}
.issue {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--app-text-3);
  font-variant-numeric: tabular-nums;
}
.issue.on {
  color: #d4380d;
  font-weight: 600;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ellipsis-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.5;
}
.tip-icon {
  color: var(--app-text-3);
  cursor: help;
}
</style>
