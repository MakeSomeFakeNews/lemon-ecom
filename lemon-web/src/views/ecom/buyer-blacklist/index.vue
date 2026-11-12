<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" :placeholder="$t('ecomOrd.blacklist.keywordPh')" allow-clear @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomOrd.blacklist.matchType')" name="matchType">
          <a-select v-model:value="query.matchType" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.ecom_blacklist_type)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" @change="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-alert type="info" show-icon :message="$t('ecomOrd.blacklist.help')" class="help" />
      <TableToolbar :exporter="exportAll" v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:blacklist:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomOrd.blacklist.add') }}
        </a-button>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination"
               :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'matchType'"><DictTag type="ecom_blacklist_type" :value="record.matchType" /></template>
          <template v-else-if="column.key === 'store'">{{ record.storeName || $t('ecomOrd.blacklist.allStores') }}</template>
          <template v-else-if="column.key === 'status'"><DictTag type="sys_common_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'edit', perm: 'ecom:blacklist:edit', onClick: () => formRef.openEdit(record) },
                { type: 'delete', perm: 'ecom:blacklist:remove', confirm: $t('ecomOrd.blacklist.confirmDelete', { value: record.matchValue }), onClick: () => remove(record.id) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>
    <BlacklistForm ref="formRef" @success="load" />
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
import BlacklistForm from './BlacklistForm.vue'
import { deleteBlacklist, pageBlacklist } from '@/api/ecom/orderEnhance'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'

/**
 * 买家黑名单：按买家 ID / 邮箱 / 地址 / 电话匹配（规范化后比较），订单写入或审核时命中即自动挂起（写明命中的条目），
 * 审单规则也可按「命中黑名单」打标签。订单详情可「拉黑买家」直接加入
 */
const { t } = useI18n()
const dict = useDict('ecom_blacklist_type', 'sys_common_status')
const columns = [
  { title: t('ecomOrd.blacklist.matchType'), key: 'matchType', dataIndex: 'matchType', width: 100, dict: 'ecom_blacklist_type' },
  { title: t('ecomOrd.blacklist.matchValue'), dataIndex: 'matchValue', width: 260, ellipsis: true },
  { title: t('ecomOrd.blacklist.store'), key: 'store', dataIndex: 'storeName', width: 150 },
  { title: t('ecomOrd.blacklist.reason'), dataIndex: 'reason', width: 220, ellipsis: true },
  { title: t('ecomOrd.blacklist.hitCount'), dataIndex: 'hitCount', width: 90, align: 'right' },
  { title: t('ecomOrd.blacklist.lastHit'), dataIndex: 'lastHitTime', width: 170 },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'sys_common_status' },
  { title: t('common.field.createTime'), dataIndex: 'createTime', width: 170 },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-buyer-blacklist')
const showSearch = ref(true)
const emptyQuery = () => ({ keyword: undefined, matchType: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageBlacklist, query)
onMounted(load)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
const formRef = ref()
const remove = useAction(deleteBlacklist, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.help {
  margin-bottom: 12px;
}
</style>
