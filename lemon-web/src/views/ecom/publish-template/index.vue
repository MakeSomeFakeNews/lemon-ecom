<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomPublish.common.platform')" name="platform">
          <a-select v-model:value="query.platform" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_platform)" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomPublish.template.keywordPh')" @press-enter="search" />
        </a-form-item>
        <a-form-item :label="$t('common.field.status')" name="status">
          <a-select v-model:value="query.status" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.sys_common_status, true)" />
        </a-form-item>
      </SearchForm>
    </a-card>
    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" :exporter="exportAll" @refresh="load">
        <a-button v-perm="'ecom:publishTemplate:add'" type="primary" @click="formRef.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomPublish.template.add') }}
        </a-button>
        <a-tooltip :title="$t('ecomPublish.template.tip')">
          <QuestionCircleOutlined class="tip-icon" />
        </a-tooltip>
      </TableToolbar>
      <a-table row-key="id" :size="size" :columns="visibleColumns" :data-source="list" :loading="loading" :pagination="pagination"
               :scroll="{ x: scrollX, y: '100%' }" @change="onTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'"><a @click="formRef.openEdit(record)">{{ record.name }}</a></template>
          <template v-else-if="column.key === 'platform'"><DictTag type="ecom_platform" :value="record.platform" /></template>
          <template v-else-if="column.key === 'category'">
            <a-tooltip :title="record.categoryName" placement="topLeft">
              <div class="cell-ellipsis">{{ record.categoryName || '-' }}</div>
            </a-tooltip>
            <div v-if="record.categoryId" class="sub-text cell-ellipsis" :title="record.categoryId">{{ record.categoryId }}</div>
          </template>
          <template v-else-if="column.key === 'attributes'">{{ Object.keys(record.attributes || {}).length }}</template>
          <template v-else-if="column.key === 'shippingTemplate' || column.key === 'returnPolicy' || column.key === 'priceTemplateName'">
            <span :title="record[column.key] || undefined">{{ record[column.key] || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'fulfillment'"><DictTag type="ecom_fulfillment" :value="record.fulfillment" /></template>
          <template v-else-if="column.key === 'status'"><DictTag type="sys_common_status" :value="record.status" /></template>
          <template v-else-if="column.key === 'action'"><TableActions :actions="rowActions(record)" /></template>
        </template>
      </a-table>
    </a-card>
    <PublishTemplateForm ref="formRef" @success="load" />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'
import DictTag from '@/components/DictTag.vue'
import SearchForm from '@/components/SearchForm.vue'
import TableToolbar from '@/components/TableToolbar.vue'
import TableActions from '@/components/common/TableActions.vue'
import { actionColumn } from '@/components/common/tableActions'
import { deletePublishTemplate, pagePublishTemplate } from '@/api/ecom/publish'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import PublishTemplateForm from './PublishTemplateForm.vue'

/** 刊登模板：按平台保存常用类目、属性、品牌、运费模板、退货政策、备货天数、配送方式与默认价格模板，新建刊登时选择后带入 */
const { t } = useI18n()
const dict = useDict('ecom_platform', 'ecom_fulfillment', 'sys_common_status')
const columns = [
  { title: t('ecomPublish.template.name'), key: 'name', dataIndex: 'name', width: 200, ellipsis: true },
  { title: t('ecomPublish.common.platform'), key: 'platform', dataIndex: 'platform', width: 110, dict: 'ecom_platform' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 80, dict: 'sys_common_status' },
  { title: t('ecomPublish.item.category'), key: 'category', dataIndex: 'categoryName', width: 260 },
  { title: t('ecomPublish.template.attrCount'), key: 'attributes', width: 80, align: 'right' },
  { title: t('ecomPublish.item.fulfillment'), key: 'fulfillment', dataIndex: 'fulfillment', width: 110, dict: 'ecom_fulfillment' },
  { title: t('ecomPublish.item.priceTemplate'), key: 'priceTemplateName', dataIndex: 'priceTemplateName', width: 170, ellipsis: true },
  { title: t('ecomPublish.item.shippingTemplate'), key: 'shippingTemplate', dataIndex: 'shippingTemplate', width: 140, ellipsis: true },
  { title: t('ecomPublish.item.returnPolicy'), key: 'returnPolicy', dataIndex: 'returnPolicy', width: 130, ellipsis: true },
  actionColumn(2)
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-publish-template')
const showSearch = ref(true)
const emptyQuery = () => ({ platform: undefined, keyword: undefined, status: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pagePublishTemplate, query)
function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}
onMounted(load)

const formRef = ref()
const remove = useAction(deletePublishTemplate, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const rowActions = (r) => [
  { type: 'edit', perm: 'ecom:publishTemplate:edit', onClick: () => formRef.value.openEdit(r) },
  { type: 'delete', perm: 'ecom:publishTemplate:remove', confirm: { title: t('ecomPublish.common.deleteConfirm', { name: r.name }), okType: 'danger' }, onClick: () => remove(r.id) }
]
</script>

<style scoped>
.tip-icon {
  align-self: center;
  color: var(--app-text-3);
  cursor: help;
}
.cell-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
