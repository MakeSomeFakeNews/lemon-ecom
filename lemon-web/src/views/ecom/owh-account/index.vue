<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOwh.account.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOwh.common.provider')" name="provider">
          <a-select v-model:value="query.provider" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_owh_provider)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" @change="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:owhAccount:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomOwh.account.add') }}
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
            <a @click="panelRef.open(record)">{{ record.name }}</a>
            <div class="sub-text">{{ record.code }}</div>
          </template>
          <template v-else-if="column.key === 'provider'">
            <DictTag type="ecom_owh_provider" :value="record.provider" />
          </template>
          <template v-else-if="column.key === 'env'">
            <a-tag :bordered="false" :color="record.sandbox === 1 ? 'orange' : 'green'">
              {{ record.sandbox === 1 ? $t('ecomOwh.account.sandbox') : $t('ecomOwh.account.production') }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'autoSync'">
            <a-tag :bordered="false" :color="record.autoSync === 0 ? 'default' : 'blue'">
              {{ record.autoSync === 0 ? $t('ecomOwh.account.autoSyncOff') : $t('ecomOwh.account.autoSyncOn') }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'credentials'">
            <a-tag :bordered="false" :color="record.hasCredentials ? 'green' : 'default'">
              {{ record.hasCredentials ? $t('ecomOwh.account.credSaved') : $t('ecomOwh.account.credMissing') }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'stockSyncTime'">
            <span>{{ record.stockSyncTime || '-' }}</span>
            <a-tooltip v-if="record.lastError" :title="record.lastError">
              <ExclamationCircleOutlined class="error-icon" />
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changeOwhAccountStatus(record.id, s)"
              :disabled="!userStore.hasPerm('ecom:owhAccount:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions :actions="rowActions(record)" />
          </template>
        </template>
      </a-table>
    </a-card>

    <AccountForm ref="formRef" @success="load" />
    <AccountPanel ref="panelRef" @changed="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { ApiOutlined, ClusterOutlined, ExclamationCircleOutlined, PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import AccountForm from './AccountForm.vue'
import AccountPanel from './AccountPanel.vue'
import { changeOwhAccountStatus, deleteOwhAccount, pageOwhAccount, testOwhAccount } from '@/api/ecom/owh'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'

/**
 * 海外仓账号：服务商（谷仓 / 万邑通 / 4PX 海外仓）账号与加密授权；点账号名称打开仓库映射与物流产品
 */
const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('sys_common_status', 'ecom_owh_provider')

const columns = [
  { title: t('ecomOwh.account.name'), key: 'name', width: 200, exportValue: (r) => `${r.code} ${r.name}` },
  { title: t('ecomOwh.common.provider'), key: 'provider', dataIndex: 'provider', width: 150, dict: 'ecom_owh_provider' },
  { title: t('ecomOwh.account.env'), key: 'env', width: 90, exportValue: (r) => (r.sandbox === 1 ? t('ecomOwh.account.sandbox') : t('ecomOwh.account.production')) },
  { title: t('ecomOwh.account.credentials'), key: 'credentials', width: 100, exportValue: (r) => (r.hasCredentials ? t('ecomOwh.account.credSaved') : t('ecomOwh.account.credMissing')) },
  { title: t('ecomOwh.account.autoSync'), key: 'autoSync', width: 100, exportValue: (r) => (r.autoSync === 0 ? t('ecomOwh.account.autoSyncOff') : t('ecomOwh.account.autoSyncOn')) },
  { title: t('ecomOwh.account.warehouseCount'), dataIndex: 'warehouseCount', width: 90, align: 'right' },
  { title: t('ecomOwh.account.currency'), dataIndex: 'currency', width: 80 },
  { title: t('ecomOwh.account.stockSyncTime'), key: 'stockSyncTime', dataIndex: 'stockSyncTime', width: 180 },
  { title: t('ecomOwh.account.feeSyncTime'), dataIndex: 'feeSyncTime', width: 170 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'sys_common_status' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 160, ellipsis: true },
  actionColumn(4)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-owh-account')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, provider: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageOwhAccount, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const formRef = ref()
const panelRef = ref()
const remove = useAction(deleteOwhAccount, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
async function test(record) {
  try {
    const n = await testOwhAccount(record.id)
    message.success(t('ecomOwh.account.testOk', { n }))
  } finally {
    load()
  }
}

const rowActions = (record) => [
  { type: 'edit', perm: 'ecom:owhAccount:edit', onClick: () => formRef.value.openEdit(record) },
  { key: 'mapping', label: t('ecomOwh.account.mappings'), icon: ClusterOutlined, perm: 'ecom:owhAccount:list', onClick: () => panelRef.value.open(record) },
  { key: 'test', label: t('ecomOwh.account.test'), icon: ApiOutlined, perm: 'ecom:owhAccount:edit', more: true, onClick: () => test(record) },
  { type: 'delete', perm: 'ecom:owhAccount:remove', more: true, confirm: t('ecomOwh.account.confirmDelete', { name: record.name }), onClick: () => remove(record.id) }
]
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
.error-icon {
  margin-left: 4px;
  color: var(--ant-color-error, #ff4d4f);
  cursor: help;
}
</style>
