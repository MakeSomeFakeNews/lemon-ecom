<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomOrder.rule.name')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOrder.rule.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" @change="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-alert type="info" show-icon :message="$t('ecomOrder.rule.help')" class="help" />
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:orderRule:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomOrder.rule.add') }}
        </a-button>
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
          <template v-if="column.key === 'name'">
            <a class="ellipsis rule-name" :title="record.name" @click="userStore.hasPerm('ecom:orderRule:edit') && formRef.openEdit(record)">{{ record.name }}</a>
          </template>
          <template v-else-if="column.key === 'priority'">
            <span class="priority">{{ record.priority }}</span>
          </template>
          <template v-else-if="column.key === 'conditions'">
            <a-tooltip :title="conditionText(record)" placement="topLeft">
              <div class="chips">
                <span v-for="(p, i) in conditionParts(record).slice(0, 3)" :key="i" class="chip"><span v-if="p.label" class="chip-label">{{ p.label }}</span>{{ p.value }}</span>
                <span v-if="conditionParts(record).length > 3" class="chip more">+{{ conditionParts(record).length - 3 }}</span>
              </div>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-tooltip :title="actionText(record)" placement="topLeft">
              <div class="chips">
                <a-tag v-for="(p, i) in actionParts(record)" :key="i" :color="p.color" :bordered="false" class="action-tag">{{ p.text }}</a-tag>
                <span v-if="!actionParts(record).length">-</span>
              </div>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changeOrderRuleStatus(record.id, s)"
              :disabled="!userStore.hasPerm('ecom:orderRule:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'edit', perm: 'ecom:orderRule:edit', onClick: () => formRef.openEdit(record) },
                { type: 'delete', perm: 'ecom:orderRule:remove', confirm: $t('ecomOrder.rule.confirmDelete', { name: record.name }), onClick: () => remove(record.id) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <RuleForm ref="formRef" @success="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useStores } from '@/components/ecom/StoreSelect.vue'
import RuleForm from './RuleForm.vue'
import { changeOrderRuleStatus, deleteOrderRule, pageOrderRule } from '@/api/ecom/orderRule'
import { orderTagOptions } from '@/api/ecom/orderEnhance'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { findDict, toOptions } from '@/utils/dict'

/**
 * 订单规则（审单规则）：订单导入 / 同步后按优先级自动执行——指定发货仓、物流渠道、截单时间，要求人工审核（挂起）或自动审核
 */
const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('sys_common_status', 'ecom_platform', 'ecom_fulfillment')
const stores = useStores()
const tags = ref([])
orderTagOptions().then((list) => (tags.value = list || []))

const split = (s) => (s ? String(s).split(',').filter(Boolean) : [])
const range = (min, max) => (min == null && max == null ? null : `${min ?? ''} ~ ${max ?? ''}`)

/** 条件：店铺、平台、配送方式、国家、SKU、物料数、金额、重量、利润率、黑名单、地址异常、偏远地区（{ label, value }，没有条件时为「任意订单」） */
function conditionParts(r) {
  const parts = []
  const add = (label, value) => parts.push({ label, value })
  if (r.storeIds) add(t('ecomOrder.rule.stores'), split(r.storeIds).map((id) => stores.value.find((s) => String(s.id) === id)?.name || id).join(', '))
  if (r.platforms) add(t('ecomOrder.rule.platforms'), split(r.platforms).map((p) => findDict(dict.ecom_platform, p)?.label || p).join(', '))
  if (r.fulfillment) add(t('ecomOrder.rule.fulfillment'), findDict(dict.ecom_fulfillment, r.fulfillment)?.label || r.fulfillment)
  if (r.countries) add(t('ecomOrder.rule.countries'), r.countries)
  if (r.skus) add(t('ecomOrder.rule.skus'), r.skus)
  if (r.materialIds) add(t('ecomOrder.rule.materials'), split(r.materialIds).length)
  const amount = range(r.amountMin, r.amountMax)
  if (amount) add(t('ecomOrder.rule.amount'), amount)
  const weight = range(r.weightMin, r.weightMax)
  if (weight) add(t('ecomOrder.rule.weightRange'), weight)
  if (r.profitRateMax != null) add('', t('ecomOrd.rule.profitRateText', { rate: Number(r.profitRateMax) }))
  if (r.blacklistHit === 1) add('', t('ecomOrd.rule.blacklistHit'))
  if (r.addressIssue === 1) add('', t('ecomOrd.rule.addressIssue'))
  if (r.remoteArea === 1) add('', t('ecomOrd.rule.remoteArea'))
  return parts.length ? parts : [{ label: '', value: t('ecomOrder.rule.any') }]
}
const conditionText = (r) => conditionParts(r).map((p) => (p.label ? `${p.label}: ${p.value}` : p.value)).join('; ')

/** 动作（{ text, color }）：指定仓库 / 渠道 / 截单时间为蓝色，挂起为橙色，自动审核为绿色 */
function actionParts(r) {
  const parts = []
  if (r.warehouseName) parts.push({ text: `${t('ecomOrder.rule.warehouse')}: ${r.warehouseName}`, color: 'blue' })
  if (r.channelName) parts.push({ text: `${t('ecomOrder.rule.channel')}: ${r.channelName}`, color: 'blue' })
  if (r.cutoffTime) parts.push({ text: `${t('ecomOrder.rule.cutoffTime')}: ${r.cutoffTime}`, color: 'blue' })
  if (r.holdOrder === 1) parts.push({ text: r.holdReason ? `${t('ecomOrder.rule.holdOrder')} (${r.holdReason})` : t('ecomOrder.rule.holdOrder'), color: 'orange' })
  if (r.autoAudit === 1) parts.push({ text: t('ecomOrder.rule.autoAudit'), color: 'green' })
  if (r.tagIds) parts.push({ text: `${t('ecomOrd.rule.tags')}: ${split(r.tagIds).map((id) => tags.value.find((x) => String(x.id) === id)?.name || id).join(', ')}`, color: 'purple' })
  if (r.stopOnMatch === 1) parts.push({ text: t('ecomOrder.rule.stopOnMatch'), color: 'default' })
  return parts
}
const actionText = (r) => actionParts(r).map((p) => p.text).join('; ') || '-'

const columns = [
  { title: t('ecomOrder.rule.priority'), key: 'priority', dataIndex: 'priority', width: 80, align: 'center' },
  { title: t('ecomOrder.rule.name'), key: 'name', dataIndex: 'name', width: 200 },
  { title: t('ecomOrder.rule.conditionText'), key: 'conditions', width: 380, exportValue: (r) => conditionText(r) },
  { title: t('ecomOrder.rule.actionText'), key: 'actions', width: 340, exportValue: (r) => actionText(r) },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'sys_common_status' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-order-rule')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageOrderRule, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const formRef = ref()
const remove = useAction(deleteOrderRule, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.help {
  margin-bottom: 12px;
}
.ellipsis {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.rule-name {
  font-weight: 500;
}
.priority {
  display: inline-block;
  min-width: 28px;
  padding: 0 6px;
  border-radius: 10px;
  background: var(--app-hover);
  color: var(--app-text-2);
  font-weight: 500;
  line-height: 20px;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-height: 52px;
  overflow: hidden;
}
.chip {
  max-width: 100%;
  overflow: hidden;
  padding: 0 6px;
  border: 1px solid var(--app-border);
  border-radius: 4px;
  font-size: 12px;
  line-height: 20px;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.chip-label {
  margin-right: 4px;
  color: var(--app-text-3);
}
.chip.more {
  color: var(--app-text-3);
}
.action-tag {
  max-width: 100%;
  margin-inline-end: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
