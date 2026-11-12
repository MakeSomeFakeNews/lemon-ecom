<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomOrder.order.country')" name="country">
          <a-input v-model:value="query.country" :maxlength="8" :placeholder="$t('ecomOrder.order.countryPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOrd.remote.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-alert type="info" show-icon :message="$t('ecomOrd.remote.help')" class="help" />
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:remoteArea:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomOrd.remote.add') }}
        </a-button>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination"
               :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'range'">{{ record.postcodeFrom === record.postcodeTo ? `${record.postcodeFrom}*` : `${record.postcodeFrom} ~ ${record.postcodeTo}` }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="sys_common_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'edit', perm: 'ecom:remoteArea:edit', onClick: () => formRef.openEdit(record) },
                { type: 'delete', perm: 'ecom:remoteArea:remove', confirm: $t('ecomOrd.remote.confirmDelete'), onClick: () => remove(record.id) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>
    <RemoteAreaForm ref="formRef" @success="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import RemoteAreaForm from './RemoteAreaForm.vue'
import { deleteRemoteArea, pageRemoteArea } from '@/api/ecom/orderEnhance'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useTable } from '@/composables/useTable'

/** 偏远地区邮编段：地址校验时识别偏远地区（提示偏远附加费），审单规则可按「偏远地区」指定渠道、挂起或打标签 */
const { t } = useI18n()
const columns = [
  { title: t('ecomOrder.order.country'), dataIndex: 'country', width: 100 },
  { title: t('ecomOrd.remote.range'), key: 'range', width: 200, exportValue: (r) => `${r.postcodeFrom} ~ ${r.postcodeTo}` },
  { title: t('ecomOrd.remote.name'), dataIndex: 'name', width: 200 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'sys_common_status' },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 220, ellipsis: true },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-remote-area')
const showSearch = ref(true)
const emptyQuery = () => ({ country: undefined, keyword: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageRemoteArea, query, {
  params: (q) => ({ ...q, country: q.country?.trim().toUpperCase() || undefined })
})
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
const formRef = ref()
const remove = useAction(deleteRemoteArea, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.help {
  margin-bottom: 12px;
}
</style>
