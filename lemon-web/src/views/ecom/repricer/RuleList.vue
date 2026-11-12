<template>
  <div class="toolbar">
    <a-button v-perm="'ecom:repricer:add'" type="primary" @click="formRef.openCreate()"><template #icon><PlusOutlined /></template>{{ $t('ecomPublish.repricer.add') }}</a-button>
    <span class="spacer" />
    <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomPublish.repricer.keywordPh')" class="kw" @press-enter="search">
      <template #suffix><SearchOutlined class="kw-icon" @click="search" /></template>
    </a-input>
    <a-select v-model:value="query.status" allow-clear :placeholder="$t('common.field.status')" :options="toOptions(dict.sys_common_status, true)" class="sel" @change="search" />
    <a-tooltip :title="$t('common.action.refresh')">
      <a-button @click="load"><template #icon><ReloadOutlined /></template></a-button>
    </a-tooltip>
  </div>
  <a-alert type="info" show-icon class="tip" :message="$t('ecomPublish.repricer.tip')" />
  <a-table row-key="id" size="middle" :columns="columns" :data-source="list" :loading="loading" :pagination="pagination" :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'name'">
        <a @click="formRef.openEdit(record)">{{ record.name }}</a>
        <a-tooltip v-if="record.remark" :title="record.remark"><div class="sub-text clamp2">{{ record.remark }}</div></a-tooltip>
      </template>
      <template v-else-if="column.key === 'scope'">
        <a-tooltip :title="storeNames(record.storeIds)"><div class="ellipsis">{{ storeNames(record.storeIds) }}</div></a-tooltip>
        <div class="sub-text ellipsis" :title="record.skus">{{ record.skus || $t('ecomPublish.repricer.allSkus') }}</div>
      </template>
      <template v-else-if="column.key === 'strategy'">
        <DictTag type="ecom_reprice_strategy" :value="record.strategy" />
        <a-tooltip :title="strategyText(record)"><div class="sub-text clamp2">{{ strategyText(record) }}</div></a-tooltip>
      </template>
      <template v-else-if="column.key === 'limits'">
        <div>{{ floorText(record) }}</div>
        <div class="sub-text">{{ $t('ecomPublish.repricer.max') }}：{{ record.maxPrice ?? '-' }}</div>
      </template>
      <template v-else-if="column.key === 'mode'">
        <a-tag :color="record.suggestOnly ? 'orange' : 'green'" :bordered="false">{{ record.suggestOnly ? $t('ecomPublish.repricer.suggestOnly') : $t('ecomPublish.repricer.autoPush') }}</a-tag>
        <div class="sub-text">{{ $t('ecomPublish.repricer.every', { n: record.intervalMinutes }) }}</div>
      </template>
      <template v-else-if="column.key === 'lastRun'">
        <div>{{ record.lastRunTime || '-' }}</div>
        <a-tooltip v-if="record.lastRunMessage" :title="record.lastRunMessage"><div class="sub-text ellipsis">{{ record.lastRunMessage }}</div></a-tooltip>
        <a v-if="record.suggestedCount" @click="emit('logs', record.id)">{{ $t('ecomPublish.repricer.suggestedN', { n: record.suggestedCount }) }}</a>
      </template>
      <template v-else-if="column.key === 'status'">
        <a-switch :checked="record.status === STATUS_ENABLED" size="small" :disabled="!userStore.hasPerm('ecom:repricer:edit')" @change="(v) => toggle(record, v)" />
      </template>
      <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
    </template>
  </a-table>
  <RuleForm ref="formRef" @success="load" />
  <RepricePreview ref="previewRef" @done="load" />
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useStores } from '@/components/ecom/StoreSelect.vue'
import { changeRepriceRuleStatus, deleteRepriceRule, pageRepriceRule } from '@/api/ecom/publish'
import { useAction } from '@/composables/useAction'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { PriceAdjustMode, RepriceFloorMode, RepriceStrategy } from '@/constants/enums'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'
import RepricePreview from './RepricePreview.vue'
import RuleForm from './RuleForm.vue'

const emit = defineEmits(['logs'])
const { t } = useI18n()
const userStore = useUserStore()
const stores = useStores()
const dict = useDict('sys_common_status', 'ecom_reprice_strategy')
const columns = [
  { title: t('ecomPublish.repricer.name'), key: 'name', width: 190, fixed: 'left' },
  { title: t('ecomPublish.repricer.scope'), key: 'scope', width: 180 },
  { title: t('ecomPublish.repricer.strategy'), key: 'strategy', width: 190 },
  { title: t('ecomPublish.repricer.limits'), key: 'limits', width: 160 },
  { title: t('ecomPublish.repricer.mode'), key: 'mode', width: 120 },
  { title: t('ecomPublish.repricer.lastRun'), key: 'lastRun', width: 230 },
  { title: t('common.field.status'), key: 'status', width: 70, align: 'center' },
  actionColumn(4)
]
const scrollX = columns.reduce((s, c) => s + c.width, 0)
const query = reactive({ keyword: undefined, status: undefined })
const { list, loading, pagination, load, search, onTableChange } = useTable(pageRepriceRule, query)
onMounted(load)

const storeNames = (ids) => {
  if (!ids) return t('ecomPublish.repricer.allStores')
  return ids.split(',').map((id) => stores.value.find((s) => String(s.id) === id)?.name || id).join('、')
}
const amount = (r) => (r.adjustMode === PriceAdjustMode.PERCENT ? `${Number(r.adjustValue)}%` : Number(r.adjustValue))
function strategyText(r) {
  switch (r.strategy) {
    case RepriceStrategy.BEAT_LOWEST:
      return t('ecomPublish.repricer.beatBy', { v: amount(r) })
    case RepriceStrategy.TARGET_MARGIN:
      return t('ecomPublish.repricer.marginOf', { v: r.targetMargin ?? '-' })
    case RepriceStrategy.BY_STOCK:
      return t('ecomPublish.repricer.stockRule', { high: r.stockHighDays ?? '-', low: r.stockLowDays ?? '-', v: amount(r) })
    default:
      return ''
  }
}
const floorText = (r) => (r.floorMode === RepriceFloorMode.COST_MARGIN ? t('ecomPublish.repricer.floorCost', { v: r.minMargin }) : `${t('ecomPublish.repricer.floor')}：${r.minPrice ?? '-'}`)

const formRef = ref()
const previewRef = ref()
async function toggle(r, on) {
  await changeRepriceRuleStatus(r.id, on ? STATUS_ENABLED : STATUS_DISABLED)
  load()
}
const remove = useAction(deleteRepriceRule, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const rowActions = (r) => [
  { type: 'edit', perm: 'ecom:repricer:edit', onClick: () => formRef.value.openEdit(r) },
  { type: 'view', label: t('ecomPublish.repricer.preview'), onClick: () => previewRef.value.preview(r) },
  { type: 'run', label: t('ecomPublish.repricer.run'), perm: 'ecom:repricer:execute', onClick: () => previewRef.value.run(r) },
  { type: 'log', label: t('ecomPublish.repricer.logs'), onClick: () => emit('logs', r.id) },
  { type: 'delete', perm: 'ecom:repricer:remove', confirm: { title: t('ecomPublish.common.deleteConfirm', { name: r.name }), okType: 'danger' }, onClick: () => remove(r.id) }
]
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
  width: 220px;
}
.kw-icon {
  color: var(--app-text-3);
  cursor: pointer;
}
.sel {
  width: 120px;
}
.clamp2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.spacer {
  flex: 1;
}
.tip {
  margin-bottom: 12px;
}
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
