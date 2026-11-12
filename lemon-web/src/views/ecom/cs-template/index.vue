<template>
  <div class="table-page">
    <a-card v-show="showSearch" :bordered="false" class="page-card">
      <SearchForm :model="query" @search="search" @reset="resetQuery">
        <a-form-item :label="$t('ecomCs.template.language')" name="language">
          <a-select v-model:value="query.language" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_cs_language)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('ecomCs.template.platform')" name="platform">
          <a-select v-model:value="query.platform" allow-clear :placeholder="$t('common.placeholder.all')" :options="toOptions(dict.ecom_platform)" @change="search" />
        </a-form-item>
        <a-form-item :label="$t('common.field.keyword')" name="keyword">
          <a-input v-model:value="query.keyword" allow-clear :placeholder="$t('ecomCs.template.keywordPh')" @press-enter="search" />
        </a-form-item>
      </SearchForm>
    </a-card>

    <a-card :bordered="false" class="page-card table-card">
      <TableToolbar v-model:size="size" v-model:checked="checked" v-model:show-search="showSearch" :columns="columns" :exporter="exportAll" @refresh="load">
        <a-button v-perm="'ecom:csTemplate:add'" type="primary" @click="form.openCreate()">
          <template #icon><PlusOutlined /></template>{{ $t('ecomCs.template.add') }}
        </a-button>
        <span class="sub-text tip">{{ $t('ecomCs.template.tip') }}</span>
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
            <a @click="form.openEdit(record)">{{ record.name }}</a>
          </template>
          <template v-else-if="column.key === 'language'">
            <DictTag type="ecom_cs_language" :value="record.language" />
          </template>
          <template v-else-if="column.key === 'platform'">
            <DictTag v-if="record.platform" type="ecom_platform" :value="record.platform" />
            <span v-else class="sub-text">{{ $t('ecomCs.template.anyPlatform') }}</span>
          </template>
          <template v-else-if="column.key === 'content'">
            <div class="content" :title="record.content">{{ record.content }}</div>
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

    <TemplateForm ref="form" @success="load" />
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
import { deleteCsTemplate, pageCsTemplate } from '@/api/ecom/cs'
import { useAction } from '@/composables/useAction'
import { useColumns } from '@/composables/useColumns'
import { useDict } from '@/composables/useDict'
import { useTable } from '@/composables/useTable'
import { toOptions } from '@/utils/dict'
import TemplateForm from './TemplateForm.vue'

/**
 * 回复模板：多语言快捷回复，内容可含变量（买家名、订单号、运单号、承运商、商品名、店铺），在买家消息、评价回复中选择后按订单替换
 */
const { t } = useI18n()
const dict = useDict('ecom_cs_language', 'ecom_platform', 'sys_common_status')

const columns = [
  { title: t('ecomCs.template.name'), key: 'name', dataIndex: 'name', width: 160 },
  { title: t('ecomCs.template.language'), key: 'language', dataIndex: 'language', width: 100, dict: 'ecom_cs_language' },
  { title: t('ecomCs.template.platform'), key: 'platform', dataIndex: 'platform', width: 120, dict: 'ecom_platform' },
  { title: t('ecomCs.template.scene'), dataIndex: 'scene', width: 120 },
  { title: t('ecomCs.template.content'), key: 'content', dataIndex: 'content', width: 420 },
  { title: t('ecomCs.template.sort'), dataIndex: 'sort', width: 80, align: 'right' },
  { title: t('common.field.status'), key: 'status', dataIndex: 'status', width: 90, dict: 'sys_common_status' },
  actionColumn(2, [t('common.action.edit')])
]
const { checked, visibleColumns, scrollX } = useColumns(columns, 'ecom-cs-template')
const showSearch = ref(true)

const emptyQuery = () => ({ language: undefined, platform: undefined, keyword: undefined })
const query = reactive(emptyQuery())
const { list, loading, size, pagination, load, search, onTableChange, exportAll } = useTable(pageCsTemplate, query)

function resetQuery() {
  Object.assign(query, emptyQuery())
  search()
}

const form = ref()
const remove = useAction(deleteCsTemplate, { successKey: 'common.msg.deleteSuccess', reload: () => load() })
const rowActions = (record) => [
  { type: 'edit', perm: 'ecom:csTemplate:edit', onClick: () => form.value.openEdit(record) },
  { type: 'delete', perm: 'ecom:csTemplate:remove', confirm: { title: t('ecomCs.template.deleteConfirm', { name: record.name }), okType: 'danger' }, onClick: () => remove(record.id) }
]

onMounted(load)
</script>

<style scoped>
.tip {
  align-self: center;
}
.content {
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub-text {
  color: var(--app-text-3);
  font-size: 12px;
}
</style>
