<template>
  <div class="toolbar">
    <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomAds.log.keywordPh')" class="kw" @press-enter="search" />
    <a-select v-model:value="query.status" allow-clear :placeholder="$t('ecomAds.common.status')" :options="toOptions(dict.ecom_ad_log_status)" class="sel" />
    <a-select v-model:value="query.source" allow-clear :placeholder="$t('ecomAds.log.source')" :options="sourceOptions" class="sel" />
    <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" />
    <a-button @click="search"><template #icon><SearchOutlined /></template>{{ $t('common.action.query') }}</a-button>
    <span class="spacer" />
    <template v-if="selectedKeys.length">
      <a-button v-perm="'ecom:adRule:execute'" type="primary" @click="applySelected">{{ $t('ecomAds.log.applyN', { n: selectedKeys.length }) }}</a-button>
      <a-button v-perm="'ecom:adRule:execute'" @click="manualSelected">{{ $t('ecomAds.log.manualN', { n: selectedKeys.length }) }}</a-button>
      <a-button v-perm="'ecom:adRule:execute'" @click="ignoreSelected">{{ $t('ecomAds.log.ignoreN', { n: selectedKeys.length }) }}</a-button>
    </template>
  </div>
  <a-table
    row-key="id"
    size="middle"
    :columns="columns"
    :data-source="list"
    :loading="loading"
    :pagination="pagination"
    :row-selection="{ ...rowSelection, getCheckboxProps: (r) => ({ disabled: r.status !== EcomAdLogStatus.PENDING }) }"
    :scroll="{ x: 1500 }"
    @change="onTableChange"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'runTime'">
        {{ record.runTime }}
        <div class="sub-text">{{ record.source === EcomAdLogSource.RULE ? record.ruleName || '-' : $t('ecomAds.enum.source.MANUAL') }}</div>
      </template>
      <template v-else-if="column.key === 'entityName'">
        {{ record.entityName }}
        <div class="sub-text">{{ $t(`ecomAds.enum.level.${record.level}`) }} · {{ record.storeName }}<template v-if="record.campaignName && record.level !== EcomAdLevel.CAMPAIGN"> · {{ record.campaignName }}</template></div>
      </template>
      <template v-else-if="column.key === 'action'"><DictTag type="ecom_ad_rule_action" :value="record.action" /></template>
      <template v-else-if="column.key === 'change'">{{ changeText(record) }}</template>
      <template v-else-if="column.key === 'metrics'"><span class="sub-text multi-line">{{ metricsText(record.metrics) }}</span></template>
      <template v-else-if="column.key === 'status'">
        <DictTag type="ecom_ad_log_status" :value="record.status" />
        <div v-if="record.message" class="sub-text multi-line">{{ record.message }}</div>
      </template>
      <template v-else-if="column.key === 'appliedBy'">
        {{ record.appliedByName || '-' }}
        <div class="sub-text">{{ record.appliedTime }}</div>
      </template>
      <template v-else-if="column.key === 'ops'">
        <TableActions :actions="rowActions(record)" />
      </template>
    </template>
  </a-table>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { applyAdRuleLogs, ignoreAdRuleLogs, markAdRuleLogsManual, pageAdRuleLogs } from '@/api/ecom/adRule'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { EcomAdLevel, EcomAdLogSource, EcomAdLogStatus } from '@/constants/enums'
import { findDict, toOptions } from '@/utils/dict'
import { t } from '@/locales'
import { fmt } from '../ad/adMetrics'

/**
 * 执行日志：规则执行的建议 / 结果与页面手工修改的记录；待处理的建议可执行（经广告接口）、标记已手工执行（只更新本地）、忽略
 */
const props = defineProps({ ruleId: { type: Number, default: undefined }, storeId: { type: Number, default: undefined } })
const dict = useDict('ecom_ad_log_status', 'ecom_ad_rule_action', 'ecom_ad_status')
const sourceOptions = Object.values(EcomAdLogSource).map((v) => ({ value: v, label: t(`ecomAds.enum.source.${v}`) }))
const range = ref([])

const columns = [
  { title: t('ecomAds.log.time'), key: 'runTime', width: 180, fixed: 'left' },
  { title: t('ecomAds.log.object'), key: 'entityName', width: 260 },
  { title: t('ecomAds.log.action'), key: 'action', width: 150 },
  { title: t('ecomAds.log.change'), key: 'change', width: 160 },
  { title: t('ecomAds.log.metrics'), key: 'metrics', width: 260 },
  { title: t('ecomAds.common.status'), key: 'status', width: 200 },
  { title: t('ecomAds.log.appliedBy'), key: 'appliedBy', width: 160 },
  { ...actionColumn(3), key: 'ops' }
]

const query = reactive({ ruleId: props.ruleId, storeId: props.storeId, status: undefined, source: undefined, keyword: undefined, beginDate: undefined, endDate: undefined })
const { list, loading, pagination, selectedKeys, rowSelection, load, search: doSearch, onTableChange } = useTable(pageAdRuleLogs, query)
function search() {
  query.beginDate = range.value?.[0]
  query.endDate = range.value?.[1]
  doSearch()
}
onMounted(load)
watch(
  () => [props.ruleId, props.storeId],
  ([ruleId, storeId]) => {
    query.ruleId = ruleId
    query.storeId = storeId
    doSearch()
  }
)

function changeText(r) {
  if (r.newValue != null) return `${fmt('money', r.oldValue)} → ${fmt('money', r.newValue)}`
  if (r.newStatus) {
    const label = (s) => findDict(dict.ecom_ad_status, s)?.label || s
    return `${label(r.oldStatus)} → ${label(r.newStatus)}`
  }
  return '-'
}

/** 指标快照：花费、销售额、ACOS、点击、订单 */
function metricsText(json) {
  if (!json) return ''
  try {
    const m = JSON.parse(json)
    return [
      `${t('ecomAds.metric.spend')} ${fmt('money', m.spend)}`,
      `${t('ecomAds.metric.sales')} ${fmt('money', m.sales)}`,
      `${t('ecomAds.metric.acos')} ${fmt('percent', m.acos)}`,
      `${t('ecomAds.metric.clicks')} ${fmt('int', m.clicks)}`,
      `${t('ecomAds.metric.orders')} ${fmt('int', m.orders)}`
    ].join(' · ')
  } catch {
    return ''
  }
}

const reload = () => {
  selectedKeys.value = []
  return load()
}
const apply = useAction((ids) => applyAdRuleLogs(ids), { successKey: (res) => t('ecomAds.log.applyDone', { msg: res.message }), reload })
const manual = useAction((ids) => markAdRuleLogsManual(ids), {
  confirm: () => t('ecomAds.log.manualConfirm'),
  successKey: (n) => t('ecomAds.log.manualDone', { n }),
  reload
})
const ignore = useAction((ids) => ignoreAdRuleLogs(ids), { successKey: (n) => t('ecomAds.log.ignoreDone', { n }), reload })
const applySelected = () => apply(selectedKeys.value)
const manualSelected = () => manual(selectedKeys.value)
const ignoreSelected = () => ignore(selectedKeys.value)

const rowActions = (r) => {
  const pending = r.status === EcomAdLogStatus.PENDING
  return [
    { type: 'run', label: t('ecomAds.log.apply'), perm: 'ecom:adRule:execute', show: pending, onClick: () => apply([r.id]) },
    { type: 'confirm', label: t('ecomAds.log.manual'), perm: 'ecom:adRule:execute', show: pending, onClick: () => manual([r.id]) },
    { type: 'cancel', label: t('ecomAds.log.ignore'), perm: 'ecom:adRule:execute', show: pending, onClick: () => ignore([r.id]) }
  ]
}
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.kw {
  width: 180px;
}
.sel {
  width: 140px;
}
.spacer {
  flex: 1;
}
.multi-line {
  white-space: normal;
  word-break: break-word;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
