<template>
  <div class="table-page">
    <FreightEstimate />

    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecom.logistics.channel')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecom.logistics.keywordPh')" allow-clear />
        </a-form-item>
        <a-form-item :label="$t('ecom.logistics.transportMode')" name="transportMode">
          <a-select v-model:value="query.transportMode" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_transport_mode)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:logistics:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecom.logistics.add') }}
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
          <template v-else-if="column.key === 'transportMode'">
            <DictTag v-if="record.transportMode" type="ecom_transport_mode" :value="record.transportMode" />
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'days'">{{ daysText(record) }}</template>
          <template v-else-if="column.key === 'rateCount'">{{ num(record.rateCount) }}</template>
          <template v-else-if="column.key === 'status'">
            <StatusSwitch
              :value="record.status"
              :name="record.name"
              :api="(s) => changeLogisticsStatus(record.id, s)"
              :disabled="!userStore.hasPerm('ecom:logistics:edit')"
              @changed="(s) => (record.status = s)"
            />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'edit', perm: 'ecom:logistics:edit', onClick: () => formRef.openEdit(record) },
                { type: 'delete', perm: 'ecom:logistics:remove', confirm: $t('ecom.logistics.confirmDelete', { name: record.name }), onClick: () => remove(record.id) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>

    <LogisticsForm ref="formRef" @success="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import StatusSwitch from '@/components/StatusSwitch.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import FreightEstimate from './FreightEstimate.vue'
import LogisticsForm from './LogisticsForm.vue'
import { changeLogisticsStatus, deleteLogistics, pageLogistics } from '@/api/ecom/logistics'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { useUserStore } from '@/store/user'
import { toOptions } from '@/utils/dict'
import { num } from '@/utils/number'
import { daysText } from './logistics'

const { t } = useI18n()
const userStore = useUserStore()
const dict = useDict('sys_common_status', 'ecom_transport_mode')

const columns = [
  { title: t('ecom.logistics.channel'), key: 'name', width: 220, exportValue: (r) => `${r.code} ${r.name}` },
  { title: t('ecom.logistics.provider'), dataIndex: 'provider', width: 160, ellipsis: true },
  { title: t('ecom.logistics.transportMode'), key: 'transportMode', dataIndex: 'transportMode', width: 110, dict: 'ecom_transport_mode' },
  { title: t('ecom.logistics.supplier'), dataIndex: 'supplierName', width: 180, ellipsis: true },
  { title: t('ecom.logistics.days'), key: 'days', width: 110, exportValue: (r) => daysText(r) },
  { title: t('ecom.logistics.rateCount'), key: 'rateCount', dataIndex: 'rateCount', width: 90, align: 'right' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'sys_common_status' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 180, ellipsis: true },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-logistics')
const showSearch = ref(true)

const emptyQuery = () => ({ keyword: undefined, transportMode: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageLogistics, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const formRef = ref()
const remove = useAction(deleteLogistics, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
