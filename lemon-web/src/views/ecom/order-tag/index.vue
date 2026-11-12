<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="load" @reset="resetQuery">
        <a-form-item :label="$t('ecomOrd.tag.name')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear @press-enter="load" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" :placeholder="$t('common.placeholder.all')" allow-clear :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <a-alert type="info" show-icon :message="$t('ecomOrd.tag.help')" class="help" />
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" @refresh="load">
        <a-button v-perm="'ecom:orderTag:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomOrd.tag.add') }}
        </a-button>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="false" :scroll="{ x: scrollX, y: '100%' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a-tag :color="record.color" :bordered="false">{{ record.name }}</a-tag>
          </template>
          <template v-else-if="column.key === 'orderCount'">
            <a v-if="record.orderCount" @click="router.push({ path: '/ecom/order', query: { tagId: record.id } })">{{ record.orderCount }}</a>
            <span v-else>0</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <DictTag type="sys_common_status" :value="record.status" />
          </template>
          <template v-else-if="column.key === 'action'">
            <TableActions
              :actions="[
                { type: 'edit', perm: 'ecom:orderTag:edit', onClick: () => formRef.openEdit(record) },
                { type: 'delete', perm: 'ecom:orderTag:remove', confirm: $t('ecomOrd.tag.confirmDelete', { name: record.name }), onClick: () => remove(record.id) }
              ]"
            />
          </template>
        </template>
      </a-table>
    </a-card>
    <TagForm ref="formRef" @success="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import DictTag from '@/components/DictTag.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import TagForm from './TagForm.vue'
import { deleteOrderTag, listOrderTags } from '@/api/ecom/orderEnhance'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { toOptions } from '@/utils/dict'

/**
 * 订单标签：名称、颜色、排序；订单列表批量打标 / 去标、按标签筛选，审单规则的「打标签」动作自动打上。点订单数到订单列表查看
 */
const { t } = useI18n()
const router = useRouter()
const dict = useDict('sys_common_status')
const columns = [
  { title: t('ecomOrd.tag.name'), key: 'name', dataIndex: 'name', width: 200 },
  { title: t('ecomOrd.tag.color'), dataIndex: 'color', width: 110 },
  { title: t('ecomOrd.tag.sort'), dataIndex: 'sort', width: 80, align: 'right' },
  { title: t('ecomOrd.tag.orderCount'), key: 'orderCount', dataIndex: 'orderCount', width: 100, align: 'right' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90 },
  { title: t('common.field.remark'), dataIndex: 'remark', width: 220, ellipsis: true },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-order-tag')
const showSearch = ref(true)
const size = ref('middle')
const query = reactive({ keyword: undefined, status: undefined })
const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    list.value = (await listOrderTags({ ...query })) || []
  } finally {
    loading.value = false
  }
}
function resetQuery() {
  Object.assign(query, { keyword: undefined, status: undefined })
  load()
}
onMounted(load)

const formRef = ref()
const remove = useAction(deleteOrderTag, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
</script>

<style scoped>
.help {
  margin-bottom: 12px;
}
</style>
