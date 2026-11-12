<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('system.tenant.name')" name="name">
          <a-input v-model:value="query.name" :placeholder="$t('system.tenant.name')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'sys:tenant:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('system.tenant.add') }}
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
            <span>{{ record.name }}</span>
            <a-tag v-if="record.platform" color="gold" class="platform-tag">{{ $t('system.tenant.platform') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'packageName'">
            {{ record.platform ? $t('system.tenant.unlimited') : record.packageName || '-' }}
          </template>
          <template v-else-if="column.key === 'contact'">
            <div>{{ record.contactName || '-' }}</div>
            <div class="sub-text">{{ record.contactPhone }}</div>
          </template>
          <template v-else-if="column.key === 'account'">
            <span :class="{ over: record.accountLimit != null && record.accountCount >= record.accountLimit }">{{ record.accountCount }}</span>
            <span class="sub-text"> / {{ record.accountLimit ?? $t('system.tenant.unlimited') }}</span>
          </template>
          <template v-else-if="column.key === 'expireTime'">
            <span v-if="!record.expireTime" class="sub-text">{{ $t('system.tenant.permanent') }}</span>
            <span v-else :class="{ over: isExpired(record) }">{{ record.expireTime.slice(0, 10) }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changeTenantStatus(record.id, s)"
              :disabled="record.platform || !userStore.hasPerm('sys:tenant:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'edit', perm: 'sys:tenant:edit', onClick: () => formRef.openEdit(record) },
                {
                  type: 'delete',
                  show: !record.platform,
                  perm: 'sys:tenant:remove',
                  confirm: $t('system.tenant.confirmDelete', { name: record.name }),
                  onClick: () => remove(record.id)
                }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <TenantForm ref="formRef" @saved="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import { changeTenantStatus, deleteTenant, pageTenant } from '@/api/system/tenant'
import TenantForm from './TenantForm.vue'
import { useColumns } from '@/composables/useColumns'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { useAction } from '@/composables/useAction'

const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('sys_common_status')
const showSearch = ref(true)

const columns = [
  { title: t('system.tenant.name'), key: 'name', width: 200 },
  { title: t('system.tenant.package'), key: 'packageName', width: 140 },
  { title: t('system.tenant.contact'), key: 'contact', width: 150 },
  { title: t('system.tenant.accountCol'), key: 'account', width: 130, align: 'right' },
  { title: t('system.tenant.expireTime'), key: 'expireTime', width: 120 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 200, ellipsis: true },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  { title: t('common.field.status'), key: 'status', width: 90 },
  actionColumn(2, [t('common.action.edit')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'system-tenant')

const emptyQuery = () => ({ name: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange } = useTable(pageTenant, query)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(load)

const isExpired = (record) => record.expireTime && new Date(record.expireTime.replace(' ', 'T')) <= new Date()

// 新增 / 编辑（子页）
const formRef = ref()

const remove = useAction(deleteTenant, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.platform-tag {
  margin-left: 6px;
}
.over {
  color: var(--status-critical);
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
