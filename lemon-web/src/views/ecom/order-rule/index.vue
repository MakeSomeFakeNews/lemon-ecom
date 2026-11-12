<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomOrder.rule.name')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOrder.rule.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
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
          <template v-if="column.key === 'conditions'">
            <span class="multi-line">{{ conditionText(record) }}</span>
          </template>
          <template v-else-if="column.key === 'actions'">
            <span class="multi-line">{{ actionText(record) }}</span>
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

const split = (s) => (s ? String(s).split(',').filter(Boolean) : [])
const range = (min, max) => (min == null && max == null ? null : `${min ?? ''} ~ ${max ?? ''}`)

/** 条件摘要：店铺、平台、配送方式、国家、SKU、物料数、金额、重量 */
function conditionText(r) {
  const parts = []
  if (r.storeIds) parts.push(`${t('ecomOrder.rule.stores')}: ${split(r.storeIds).map((id) => stores.value.find((s) => String(s.id) === id)?.name || id).join(', ')}`)
  if (r.platforms) parts.push(`${t('ecomOrder.rule.platforms')}: ${split(r.platforms).map((p) => findDict(dict.ecom_platform, p)?.label || p).join(', ')}`)
  if (r.fulfillment) parts.push(`${t('ecomOrder.rule.fulfillment')}: ${findDict(dict.ecom_fulfillment, r.fulfillment)?.label || r.fulfillment}`)
  if (r.countries) parts.push(`${t('ecomOrder.rule.countries')}: ${r.countries}`)
  if (r.skus) parts.push(`${t('ecomOrder.rule.skus')}: ${r.skus}`)
  if (r.materialIds) parts.push(`${t('ecomOrder.rule.materials')}: ${split(r.materialIds).length}`)
  const amount = range(r.amountMin, r.amountMax)
  if (amount) parts.push(`${t('ecomOrder.rule.amount')}: ${amount}`)
  const weight = range(r.weightMin, r.weightMax)
  if (weight) parts.push(`${t('ecomOrder.rule.weightRange')}: ${weight}`)
  return parts.length ? parts.join('; ') : t('ecomOrder.rule.any')
}

/** 动作摘要 */
function actionText(r) {
  const parts = []
  if (r.warehouseName) parts.push(`${t('ecomOrder.rule.warehouse')}: ${r.warehouseName}`)
  if (r.channelName) parts.push(`${t('ecomOrder.rule.channel')}: ${r.channelName}`)
  if (r.cutoffTime) parts.push(`${t('ecomOrder.rule.cutoffTime')}: ${r.cutoffTime}`)
  if (r.holdOrder === 1) parts.push(r.holdReason ? `${t('ecomOrder.rule.holdOrder')} (${r.holdReason})` : t('ecomOrder.rule.holdOrder'))
  if (r.autoAudit === 1) parts.push(t('ecomOrder.rule.autoAudit'))
  if (r.stopOnMatch === 1) parts.push(t('ecomOrder.rule.stopOnMatch'))
  return parts.join('; ') || '-'
}

const columns = [
  { title: t('ecomOrder.rule.priority'), dataIndex: 'priority', width: 80, align: 'right' },
  { title: t('ecomOrder.rule.name'), dataIndex: 'name', width: 200, ellipsis: true },
  { title: t('ecomOrder.rule.conditionText'), key: 'conditions', width: 360, exportValue: (r) => conditionText(r) },
  { title: t('ecomOrder.rule.actionText'), key: 'actions', width: 320, exportValue: (r) => actionText(r) },
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
.multi-line {
  white-space: normal;
  word-break: break-word;
}
</style>
