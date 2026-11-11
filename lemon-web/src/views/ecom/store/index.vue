<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecom.common.store')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecom.store.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('ecom.store.platform')" name="platform">
          <a-select v-model:value="query.platform" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_platform)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:store:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecom.store.add') }}
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
            <div>{{ record.name }}</div>
            <div class="sub-text">{{ record.code }}</div>
          </template>
          <template v-else-if="column.key === 'platform'">
            <DictTag type="ecom_platform" :value="record.platform" />
          </template>
          <template v-else-if="column.key === 'marketplace'">
            <DictTag type="ecom_marketplace" :value="record.marketplace" />
          </template>
          <template v-else-if="column.key === 'listingCount'">{{ num(record.listingCount) }}</template>
          <template v-else-if="column.key === 'hasCredentials'">
            <a-tag v-if="record.hasCredentials" color="success" :bordered="false"><CheckCircleOutlined /> {{ $t('ecom.store.authorized') }}</a-tag>
            <a-tag v-else :bordered="false">{{ $t('ecom.store.unauthorized') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'lastSyncTime'">
            <span v-if="record.lastSyncTime">{{ record.lastSyncTime }}</span>
            <span v-else class="sub-text">{{ $t('ecom.store.neverSynced') }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="sys_common_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <StoreForm ref="formRef" @success="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { ApiOutlined, CheckCircleOutlined, PlusOutlined, SyncOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import StoreForm from './StoreForm.vue'
import { changeStoreStatus, deleteStore, pageStore, syncStore, testStore } from '@/api/ecom/store'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import { num } from '@/utils/number'
import { STATUS_DISABLED, STATUS_ENABLED } from '@/utils/constants'

const { t } = useI18n()
const dict = useDict('sys_common_status', 'ecom_platform', 'ecom_marketplace')

const columns = [
  { title: t('ecom.common.store'), key: 'name', width: 200, exportValue: (r) => `${r.code} ${r.name}` },
  { title: t('ecom.store.platform'), key: 'platform', dataIndex: 'platform', width: 110, dict: 'ecom_platform' },
  { title: t('ecom.store.marketplace'), key: 'marketplace', dataIndex: 'marketplace', width: 100, dict: 'ecom_marketplace' },
  { title: t('ecom.store.currency'), dataIndex: 'currency', width: 80 },
  { title: t('ecom.store.customer'), dataIndex: 'customerName', width: 180, ellipsis: true },
  { title: t('ecom.store.warehouse'), dataIndex: 'warehouseName', width: 150, ellipsis: true },
  { title: t('ecom.store.platformWarehouse'), dataIndex: 'platformWarehouseName', width: 150, ellipsis: true },
  { title: t('ecom.store.listingCount'), key: 'listingCount', dataIndex: 'listingCount', width: 110, align: 'right' },
  { title: t('ecom.store.owner'), dataIndex: 'ownerName', width: 110 },
  { title: t('ecom.store.credentials'), key: 'hasCredentials', width: 110, exportValue: (r) => (r.hasCredentials ? t('ecom.store.authorized') : t('ecom.store.unauthorized')) },
  { title: t('ecom.store.lastSyncTime'), key: 'lastSyncTime', dataIndex: 'lastSyncTime', width: 170 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'sys_common_status' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  // 编辑、同步、启停、删除直接显示，测试授权在「更多」中
  actionColumn(4)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-store')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, platform: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageStore, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const formRef = ref()

const remove = useAction(deleteStore, { successKey: 'common.msg.deleteSuccess', reload: () => load() })

/** 启用 / 停用 */
const toggleStatus = useAction((record) => changeStoreStatus(record.id, record.status === STATUS_ENABLED ? STATUS_DISABLED : STATUS_ENABLED), {
  confirm: (record) => {
    const enable = record.status !== STATUS_ENABLED
    return {
      title: enable ? t('components.status.confirmEnable') : t('components.status.confirmDisable'),
      content: enable ? t('components.status.enableContent', { name: record.name }) : t('components.status.disableContent', { name: record.name }),
      okType: enable ? 'primary' : 'danger'
    }
  },
  successKey: (_res, record) => (record.status !== STATUS_ENABLED ? t('components.status.enabled') : t('components.status.disabled')),
  reload: () => load()
})

/** 测试渠道授权：成功提示授权有效，失败由拦截器提示后端返回的原因 */
const testingId = ref()
async function test(record) {
  testingId.value = record.id
  try {
    await testStore(record.id)
    message.success(t('ecom.store.testOk', { name: record.name }))
  } finally {
    testingId.value = undefined
  }
}

/** 手工同步订单：结果带 message 表示失败 */
const syncingId = ref()
async function sync(record) {
  syncingId.value = record.id
  try {
    const r = await syncStore(record.id)
    if (r.message) {
      Modal.warning({ title: t('ecom.store.syncFailed', { name: r.storeName || record.name }), content: r.message })
    } else {
      message.success(t('ecom.store.syncDone', { name: r.storeName || record.name, fetched: r.fetched, created: r.created, updated: r.updated }))
    }
    load()
  } finally {
    syncingId.value = undefined
  }
}

const rowActions = (record) => {
  const enabled = record.status === STATUS_ENABLED
  const noCred = !record.hasCredentials
  return [
    { type: 'edit', perm: 'ecom:store:edit', onClick: () => formRef.value.openEdit(record) },
    {
      key: 'sync',
      label: t('ecom.store.sync'),
      icon: SyncOutlined,
      perm: 'ecom:store:sync',
      disabled: noCred || !enabled,
      tip: noCred ? t('ecom.store.needCred') : !enabled ? t('ecom.store.needEnabled') : undefined,
      loading: syncingId.value === record.id,
      onClick: () => sync(record)
    },
    { type: enabled ? 'disable' : 'enable', key: 'toggle', perm: 'ecom:store:edit', onClick: () => toggleStatus(record) },
    { type: 'delete', perm: 'ecom:store:remove', confirm: t('ecom.store.confirmDelete', { name: record.name }), onClick: () => remove(record.id) },
    {
      key: 'test',
      label: t('ecom.store.testAuth'),
      icon: ApiOutlined,
      perm: 'ecom:store:sync',
      more: true,
      disabled: noCred,
      tip: noCred ? t('ecom.store.needCred') : undefined,
      loading: testingId.value === record.id,
      onClick: () => test(record)
    }
  ]
}
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
