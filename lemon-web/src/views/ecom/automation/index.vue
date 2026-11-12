<template>
  <div class="table-page ecom-automation">
    <a-card :bordered="false" class="page-card">
      <div class="kpis">
        <div class="kpi">
          <div class="kpi-label"><ThunderboltOutlined /> {{ $t('ecomAuto.kpi.enabled') }}</div>
          <div class="kpi-value">{{ stats.enabledRules ?? '-' }}</div>
          <div class="kpi-sub">{{ $t('ecomAuto.kpi.enabledSub', { n: stats.rules ?? 0 }) }}</div>
        </div>
        <div class="kpi ok clickable" @click="openLogs({ executed: true })">
          <div class="kpi-label"><CheckCircleOutlined /> {{ $t('ecomAuto.kpi.executed') }}</div>
          <div class="kpi-value">{{ executed }}</div>
          <div class="kpi-sub">{{ $t('ecomAuto.kpi.executedSub', { n: todayExecuted }) }}</div>
        </div>
        <div class="kpi clickable" :class="failed ? 'hot' : ''" @click="openLogs({ result: AutoResult.FAILED })">
          <div class="kpi-label"><CloseCircleOutlined /> {{ $t('ecomAuto.kpi.failed') }}</div>
          <div class="kpi-value">{{ failed }}</div>
          <div class="kpi-sub">{{ $t('ecomAuto.kpi.failedSub') }}</div>
        </div>
        <div class="kpi warn clickable" @click="openLogs({ result: AutoResult.DEBOUNCED })">
          <div class="kpi-label"><FieldTimeOutlined /> {{ $t('ecomAuto.kpi.skipped') }}</div>
          <div class="kpi-value">{{ skipped }}</div>
          <div class="kpi-sub">{{ $t('ecomAuto.kpi.skippedSub', { n: stats.results?.NOT_MATCHED ?? 0 }) }}</div>
        </div>
      </div>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" :columns="columns" @refresh="reload">
        <a-button v-perm="'ecom:automation:add'" type="primary" @click="editorRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomAuto.rule.add') }}
        </a-button>
        <a-button v-perm="'ecom:automation:add'" @click="templateRef.open()">
          <template #icon><AppstoreAddOutlined /></template>{{ $t('ecomAuto.rule.fromTemplate') }}
        </a-button>
        <a-tooltip :title="$t('ecomAuto.rule.help')" placement="bottomLeft">
          <QuestionCircleOutlined class="help" />
        </a-tooltip>
        <template #extra>
          <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomAuto.rule.keywordPh')" class="kw" @press-enter="search">
            <template #prefix><SearchOutlined /></template>
          </a-input>
          <a-select v-model:value="query.trigger" allow-clear show-search option-filter-prop="label" :options="triggerOptions"
            :placeholder="$t('ecomAuto.common.allTriggers')" class="trigger" @change="search" />
          <a-select v-model:value="query.status" allow-clear :options="toOptions(dict.sys_common_status, true)" :placeholder="$t('ecomAuto.common.status')"
            class="status" @change="search" />
        </template>
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
        <template #emptyText>
          <a-empty :description="$t('ecomAuto.rule.emptyTitle')">
            <div class="empty-desc">{{ $t('ecomAuto.rule.emptyDesc') }}</div>
            <a-space>
              <a-button v-perm="'ecom:automation:add'" type="primary" @click="templateRef.open()">{{ $t('ecomAuto.rule.fromTemplate') }}</a-button>
              <a-button v-perm="'ecom:automation:add'" @click="editorRef.openCreate()">{{ $t('ecomAuto.rule.add') }}</a-button>
            </a-space>
          </a-empty>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a class="name" @click="editorRef.openEdit(record)">{{ record.name }}</a>
            <div class="sub-text">
              <a-tag v-if="record.templateCode" :bordered="false" color="purple" class="mini">{{ $t('ecomAuto.rule.template') }}</a-tag>
              {{ record.createByName || '' }}
            </div>
          </template>
          <template v-else-if="column.key === 'trigger'">
            <DictTag type="ecom_auto_trigger" :value="record.trigger" />
            <div class="sub-text">
              {{ $t(`ecomAuto.mode.${modeOf(record.trigger)}`) }} · {{ record.debounceHours ? $t('ecomAuto.rule.debounce', { n: record.debounceHours }) : $t('ecomAuto.rule.noDebounce') }}<template v-if="record.dailyLimit"> · {{ $t('ecomAuto.rule.limit', { n: record.dailyLimit }) }}</template>
            </div>
          </template>
          <template v-else-if="column.key === 'summary'">
            <a-tooltip :title="ruleSentence(record, catalog, storeName)" placement="topLeft">
              <div class="summary">{{ conditionSummary(record) }}</div>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'actions'">
            <div class="acts">
              <template v-for="(a, i) in record.actions" :key="i">
                <span v-if="i" class="arrow">→</span>
                <DictTag type="ecom_auto_action" :value="a.type" />
              </template>
            </div>
          </template>
          <template v-else-if="column.key === 'stats'">
            <div class="stat-line">
              <a class="num" :title="$t('ecomAuto.rule.logs')" @click="openLogs({ ruleId: record.id })">{{ $t('ecomAuto.rule.countN', { n: record.triggerCount || 0 }) }}</a>
              <DictTag v-if="record.lastResult" type="ecom_auto_result" :value="record.lastResult" />
            </div>
            <div class="sub-text">{{ record.lastTriggerTime || $t('ecomAuto.rule.never') }}</div>
            <div v-if="record.failedCount" class="sub-text danger">{{ $t('ecomAuto.rule.failed7', { n: record.failedCount }) }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changeAutoRuleStatus(record.id, s)"
              :disabled="!userStore.hasPerm('ecom:automation:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'ops'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <RuleEditor ref="editorRef" @success="reload" />
    <TemplateLibrary ref="templateRef" @created="reload" @edit="(r) => editorRef.openCreate(r)" />
    <TestRunPanel ref="testRef" />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  AppstoreAddOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  FieldTimeOutlined,
  PlusOutlined,
  QuestionCircleOutlined,
  SearchOutlined,
  ThunderboltOutlined
} from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useStores } from '@/components/ecom/StoreSelect.vue'
import { autoLogStats, changeAutoRuleStatus, copyAutoRule, deleteAutoRule, getAutoRule, pageAutoRules, runAutoRule, testAutoRule } from '@/api/ecom/automation'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { AutoResult, AutoScanMode } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { t } from '@/locales'
import RuleEditor from './RuleEditor.vue'
import TemplateLibrary from './TemplateLibrary.vue'
import TestRunPanel from './TestRunPanel.vue'
import { groupText, ruleSentence, scopeText, triggerOf, useCatalog } from './autoText'

/**
 * 自动化规则：顶部近 14 天的执行概况（点击进入执行日志），规则列表（触发器、条件与范围摘要、动作链、防抖 / 上限、执行次数、最近结果、启停），
 * 新建 / 编辑（子页）、从模板创建、试运行、立即执行（扫描类）、复制、删除
 */
const router = useRouter()
const userStore = useUserStore()
const catalog = useCatalog()
const dict = useDict('sys_common_status', 'ecom_auto_trigger')
const stores = useStores()
const storeName = (id) => stores.value.find((s) => s.id === id)?.name || `#${id}`
const editorRef = ref()
const templateRef = ref()
const testRef = ref()

const triggerOptions = computed(() => toOptions(dict.ecom_auto_trigger))
const modeOf = (code) => triggerOf(catalog.value, code)?.mode || AutoScanMode.REALTIME

const columns = [
  { title: t('ecomAuto.rule.name'), key: 'name', dataIndex: 'name', width: 200, fixed: 'left' },
  { title: t('ecomAuto.rule.trigger'), key: 'trigger', dataIndex: 'trigger', width: 170, dict: 'ecom_auto_trigger' },
  { title: t('ecomAuto.rule.summary'), key: 'summary', width: 220, exportValue: (r) => ruleSentence(r, catalog.value, storeName) },
  { title: t('ecomAuto.rule.actions'), key: 'actions', width: 190 },
  { title: t('ecomAuto.rule.last'), key: 'stats', dataIndex: 'triggerCount', width: 165, exportValue: (r) => r.triggerCount || 0 },
  { title: t('ecomAuto.common.status'), key: 'status', width: 72, fixed: 'right', align: 'center' },
  { ...actionColumn(4), key: 'ops' }
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-automation-rule')

const query = reactive({ keyword: undefined, trigger: undefined, status: undefined })
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageAutoRules, query)

const stats = ref({})
const executed = computed(() => [AutoResult.SUCCESS, AutoResult.PARTIAL, AutoResult.FAILED].reduce((s, k) => s + (stats.value.results?.[k] || 0), 0))
const failed = computed(() => (stats.value.results?.[AutoResult.FAILED] || 0) + (stats.value.results?.[AutoResult.PARTIAL] || 0))
const skipped = computed(() => (stats.value.results?.[AutoResult.DEBOUNCED] || 0) + (stats.value.results?.[AutoResult.LIMITED] || 0))
const todayExecuted = computed(() => stats.value.days?.[stats.value.days.length - 1]?.executed || 0)
async function loadStats() {
  stats.value = (await autoLogStats({ days: 14 })) || {}
}
function reload() {
  load()
  loadStats()
}
onMounted(reload)

/** 范围 + 条件摘要（没有时显示「不限」） */
function conditionSummary(r) {
  const parts = [groupText(r.condition, r.trigger, catalog.value), scopeText(r.scope, storeName, true)].filter(Boolean)
  return parts.length ? parts.join('; ') : t('ecomAuto.common.any')
}

function openLogs(q) {
  router.push({ path: '/ecom/automation-log', query: q })
}

const run = useAction((r) => runAutoRule(r.id), {
  confirm: (r) => t('ecomAuto.rule.runConfirm', { name: r.name }),
  successKey: (res) => t('ecomAuto.rule.runDone', { events: res.events, executed: res.executed }),
  reload: () => reload()
})
const copy = useAction((r) => copyAutoRule(r.id), { successKey: 'ecomAuto.rule.copyDone', reload: () => load() })
const remove = useAction((r) => deleteAutoRule(r.id), { successKey: 'common.msg.deleteSuccess', reload: () => reload() })

async function test(r) {
  const full = await getAutoRule(r.id)
  const res = await testAutoRule(full)
  testRef.value.open(res, full)
}

const rowActions = (r) => [
  { type: 'edit', perm: 'ecom:automation:edit', onClick: () => editorRef.value.openEdit(r) },
  { type: 'run', label: t('ecomAuto.rule.test'), perm: 'ecom:automation:test', onClick: () => test(r) },
  { type: 'log', label: t('ecomAuto.rule.logs'), onClick: () => openLogs({ ruleId: r.id }) },
  ...(modeOf(r.trigger) !== AutoScanMode.REALTIME ? [{ type: 'run', label: t('ecomAuto.rule.run'), perm: 'ecom:automation:edit', onClick: () => run(r) }] : []),
  { type: 'copy', perm: 'ecom:automation:add', onClick: () => copy(r) },
  { type: 'delete', perm: 'ecom:automation:remove', confirm: t('ecomAuto.rule.confirmDelete', { name: r.name }), onClick: () => remove(r) }
]
</script>

<style scoped>
.kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
@media (max-width: 1100px) {
  .kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
.kpi {
  padding: 10px 16px;
  border-radius: 8px;
  border: 1px solid var(--app-border);
  border-left: 3px solid var(--app-primary);
}
.kpi.ok {
  border-left-color: #52c41a;
}
.kpi.warn {
  border-left-color: #faad14;
}
.kpi.hot {
  border-left-color: #ff4d4f;
  background: rgba(245, 34, 45, 0.04);
}
.kpi.clickable {
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.kpi.clickable:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.kpi-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--app-text-2);
  font-size: 13px;
}
.kpi-value {
  margin-top: 2px;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.3;
  font-variant-numeric: tabular-nums;
}
.kpi.hot .kpi-value {
  color: #cf1322;
}
.kpi-sub {
  color: var(--app-text-3);
  font-size: 12px;
}
.kw {
  width: 200px;
}
.trigger {
  width: 170px;
}
.status {
  width: 120px;
}
.help {
  color: var(--app-text-3);
  cursor: help;
}
.name {
  font-weight: 500;
}
.mini {
  margin-right: 4px;
  font-size: 11px;
  line-height: 18px;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.sub-text.danger {
  color: #cf1322;
}
.summary {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: normal;
  word-break: break-word;
  color: var(--app-text-2);
}
.acts {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px 0;
}
.acts :deep(.ant-tag) {
  margin-inline-end: 0;
}
.arrow {
  margin: 0 4px;
  color: var(--app-text-3);
}
.num {
  font-variant-numeric: tabular-nums;
}
.stat-line {
  display: flex;
  align-items: center;
  gap: 6px;
}
.stat-line :deep(.ant-tag) {
  margin: 0;
}
.empty-desc {
  margin-bottom: 12px;
  color: var(--app-text-3);
}
</style>

<style>
html[data-theme='dark'] .ecom-automation .kpi.hot {
  background: rgba(245, 34, 45, 0.1);
}
html[data-theme='dark'] .ecom-automation .kpi.hot .kpi-value,
html[data-theme='dark'] .ecom-automation .sub-text.danger {
  color: #ff7875;
}
</style>
